package com.replaymod.simplepathing;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.simplepathing.properties.ExplicitInterpolationProperty;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replaystudio.pathing.PathingRegistry;
import com.replaymod.replaystudio.pathing.change.AddKeyframe;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.change.CombinedChange;
import com.replaymod.replaystudio.pathing.change.SetInterpolator;
import com.replaymod.replaystudio.pathing.change.UpdateKeyframeProperties;
import com.replaymod.replaystudio.pathing.impl.TimelineImpl;
import com.replaymod.replaystudio.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.replaystudio.util.EntityPositionTracker;
import com.replaymod.replaystudio.util.Location;
import lombok.Getter;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Triple;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.replaymod.replaystudio.pathing.change.RemoveKeyframe.create;
import static com.replaymod.simplepathing.ReplayModSimplePathing.LOGGER;

/**
 * Simplified timeline abstraction used in the SimplePathing module.
 */
public class SPTimeline implements PathingRegistry {
    public enum SPPath {
        TIME,
        POSITION,
    }

    @Getter
    private final Timeline timeline;
    @Getter
    private final Path timePath;
    @Getter
    private final Path positionPath;

    @Getter
    private EntityPositionTracker entityTracker;
    private InterpolatorType defaultInterpolatorType = InterpolatorType.CUBIC;

    public SPTimeline() {
        this(createInitialTimeline());
    }

    public SPTimeline(Timeline timeline) {
        this.timeline = timeline;
        this.timePath = timeline.getPaths().get(SPPath.TIME.ordinal());
        this.positionPath = timeline.getPaths().get(SPPath.POSITION.ordinal());
    }

    public Path getPath(SPPath path) {
        switch (path) {
            case TIME:
                return getTimePath();
            case POSITION:
                return getPositionPath();
        }
        throw new IllegalArgumentException("Unknown path " + path);
    }

    public Keyframe getKeyframe(SPPath path, long keyframe) {
        return getPath(path).getKeyframe(keyframe);
    }

    public void setEntityTracker(EntityPositionTracker entityTracker) {
        Preconditions.checkState(this.entityTracker == null, "Entity tracker already set");
        this.entityTracker = entityTracker;
    }

    public void setDefaultInterpolatorType(InterpolatorType defaultInterpolatorType) {
        Validate.isTrue(defaultInterpolatorType != InterpolatorType.DEFAULT, "Must not be DEFAULT");
        this.defaultInterpolatorType = Validate.notNull(defaultInterpolatorType);

        if (entityTracker != null) {
            timeline.pushChange(updateInterpolators());
        }
    }

    public boolean isTimeKeyframe(long time) {
        return timePath.getKeyframe(time) != null;
    }

    public boolean isPositionKeyframe(long time) {
        return positionPath.getKeyframe(time) != null;
    }

    public boolean isSpectatorKeyframe(long time) {
        Keyframe keyframe = positionPath.getKeyframe(time);
        return keyframe != null && keyframe.getValue(SpectatorProperty.PROPERTY).isPresent();
    }

    public void addPositionKeyframe(long time, double posX, double posY, double posZ,
                                    float yaw, float pitch, float roll, int spectated) {
        LOGGER.debug("Adding position keyframe at {} pos {}/{}/{} rot {}/{}/{} entId {}",
                time, posX, posY, posZ, yaw, pitch, roll, spectated);

        Path path = positionPath;

        Preconditions.checkState(positionPath.getKeyframe(time) == null, "Keyframe already exists");

        Change change = AddKeyframe.create(path, time);
        change.apply(timeline);
        Keyframe keyframe = path.getKeyframe(time);

        UpdateKeyframeProperties.Builder builder = UpdateKeyframeProperties.create(path, keyframe);
        builder.setValue(CameraProperties.POSITION, Triple.of(posX, posY, posZ));
        builder.setValue(CameraProperties.ROTATION, Triple.of(yaw, pitch, roll));
        if (spectated != -1) {
            builder.setValue(SpectatorProperty.PROPERTY, spectated);
        }
        UpdateKeyframeProperties updateChange = builder.done();
        updateChange.apply(timeline);
        change = CombinedChange.createFromApplied(change, updateChange);

        // If this new keyframe formed the first segment of the path
        if (path.getSegments().size() == 1) {
            // then create an initial interpolator of default type
            PathSegment segment = path.getSegments().iterator().next();
            Interpolator interpolator = createDefaultInterpolator();
            SetInterpolator setInterpolator = SetInterpolator.create(segment, interpolator);
            setInterpolator.apply(timeline);
            change = CombinedChange.createFromApplied(change, setInterpolator);
        }

        // Update interpolators for spectator keyframes
        // while this is overkill, it is far simpler than updating differently for every possible case
        change = CombinedChange.createFromApplied(change, updateInterpolators());

        Change specPosUpdate = updateSpectatorPositions();
        specPosUpdate.apply(timeline);
        change = CombinedChange.createFromApplied(change, specPosUpdate);

        timeline.pushChange(change);
    }

    public Change updatePositionKeyframe(long time, double posX, double posY, double posZ,
                                    float yaw, float pitch, float roll) {
        LOGGER.debug("Updating position keyframe at {} to pos {}/{}/{} rot {}/{}/{}",
                time, posX, posY, posZ, yaw, pitch, roll);

        Keyframe keyframe = positionPath.getKeyframe(time);

        Preconditions.checkState(keyframe != null, "Keyframe does not exists");
        Preconditions.checkState(!keyframe.getValue(SpectatorProperty.PROPERTY).isPresent(), "Cannot update spectator keyframe");

        Change change = UpdateKeyframeProperties.create(positionPath, keyframe)
                .setValue(CameraProperties.POSITION, Triple.of(posX, posY, posZ))
                .setValue(CameraProperties.ROTATION, Triple.of(yaw, pitch, roll))
                .done();
        change.apply(timeline);
        return change;
    }

    public void removePositionKeyframe(long time) {
        LOGGER.debug("Removing position keyframe at {}", time);

        Path path = positionPath;
        Keyframe keyframe = path.getKeyframe(time);

        Preconditions.checkState(keyframe != null, "No keyframe at that time");

        Change change = create(path, keyframe);
        change.apply(timeline);

        // Update interpolators for spectator keyframes
        // while this is overkill, it is far simpler than updating differently for every possible case
        change = CombinedChange.createFromApplied(change, updateInterpolators());

        Change specPosUpdate = updateSpectatorPositions();
        specPosUpdate.apply(timeline);
        change = CombinedChange.createFromApplied(change, specPosUpdate);

        timeline.pushChange(change);
    }

    public void addTimeKeyframe(long time, int replayTime) {
        LOGGER.debug("Adding time keyframe at {} time {}", time, replayTime);

        Path path = timePath;

        Preconditions.checkState(path.getKeyframe(time) == null, "Keyframe already exists");

        Change change = AddKeyframe.create(path, time);
        change.apply(timeline);
        Keyframe keyframe = path.getKeyframe(time);

        UpdateKeyframeProperties updateChange = UpdateKeyframeProperties.create(path, keyframe)
                .setValue(TimestampProperty.PROPERTY, replayTime)
                .done();
        updateChange.apply(timeline);
        change = CombinedChange.createFromApplied(change, updateChange);

        // If this new keyframe formed the first segment of the path
        if (path.getSegments().size() == 1) {
            // then create an initial interpolator
            PathSegment segment = path.getSegments().iterator().next();
            Interpolator interpolator = new LinearInterpolator();
            interpolator.registerProperty(TimestampProperty.PROPERTY);
            SetInterpolator setInterpolator = SetInterpolator.create(segment, interpolator);
            setInterpolator.apply(timeline);
            change = CombinedChange.createFromApplied(change, setInterpolator);
        }

        Change specPosUpdate = updateSpectatorPositions();
        specPosUpdate.apply(timeline);
        change = CombinedChange.createFromApplied(change, specPosUpdate);

        timeline.pushChange(change);
    }

    public Change updateTimeKeyframe(long time, int replayTime) {
        LOGGER.debug("Updating time keyframe at {} to time {}", time, replayTime);

        Keyframe keyframe = timePath.getKeyframe(time);

        Preconditions.checkState(keyframe != null, "Keyframe does not exists");

        Change change = UpdateKeyframeProperties.create(timePath, keyframe)
                .setValue(TimestampProperty.PROPERTY, replayTime)
                .done();
        change.apply(timeline);
        return change;
    }

    public void removeTimeKeyframe(long time) {
        LOGGER.debug("Removing time keyframe at {}", time);

        Path path = timePath;
        Keyframe keyframe = path.getKeyframe(time);

        Preconditions.checkState(keyframe != null, "No keyframe at that time");

        Change change = create(path, keyframe);
        change.apply(timeline);

        Change specPosUpdate = updateSpectatorPositions();
        specPosUpdate.apply(timeline);
        change = CombinedChange.createFromApplied(change, specPosUpdate);

        timeline.pushChange(change);
    }

    public Change setInterpolatorToDefault(long time) {
        LOGGER.debug("Setting interpolator of position keyframe at {} to the default", time);

        Keyframe keyframe = positionPath.getKeyframe(time);

        Preconditions.checkState(keyframe != null, "Keyframe does not exists");

        Change change = UpdateKeyframeProperties.create(positionPath, keyframe)
                .removeProperty(ExplicitInterpolationProperty.PROPERTY)
                .done();
        change.apply(timeline);
        return CombinedChange.createFromApplied(change, updateInterpolators());
    }

    public Change setInterpolator(long time, Interpolator interpolator) {
        LOGGER.debug("Setting interpolator of position keyframe at {} to {}", time, interpolator);

        Keyframe keyframe = positionPath.getKeyframe(time);
        Preconditions.checkState(keyframe != null, "Keyframe does not exists");
        PathSegment segment = positionPath.getSegments().stream().filter(s -> s.getStartKeyframe() == keyframe)
                .findFirst().orElseThrow(() -> new IllegalStateException("Keyframe has no following segment."));

        registerPositionInterpolatorProperties(interpolator);

        Change change = CombinedChange.create(
                UpdateKeyframeProperties.create(positionPath, keyframe)
                        .setValue(ExplicitInterpolationProperty.PROPERTY, ObjectUtils.NULL)
                        .done(),
                SetInterpolator.create(segment, interpolator)
        );
        change.apply(timeline);
        return CombinedChange.createFromApplied(change, updateInterpolators());
    }

    public Change moveKeyframe(SPPath spPath, long oldTime, long newTime) {
        LOGGER.debug("Moving keyframe on {} from {} to {}", spPath, oldTime, newTime);
        Path path = getPath(spPath);
        Keyframe keyframe = path.getKeyframe(oldTime);

        Preconditions.checkState(keyframe != null, "No keyframe at specified time");

        // Interpolator of the first segment might be required later if it is the only one
        Optional<Interpolator> firstInterpolator =
                path.getSegments().stream().findFirst().map(PathSegment::getInterpolator);

        // The interpolator of the previous segment
        Optional<Interpolator> interpolatorBefore =
                path.getSegments().stream().filter(s -> s.getEndKeyframe() == keyframe).findFirst().map(PathSegment::getInterpolator);

        // The interpolator of the following segment
        Optional<Interpolator> interpolatorAfter =
                path.getSegments().stream().filter(s -> s.getStartKeyframe() == keyframe).findFirst().map(PathSegment::getInterpolator);

        // First remove the old keyframe
        Change removeChange = create(path, keyframe);
        removeChange.apply(timeline);

        // and add a new one at the correct time
        Change addChange = AddKeyframe.create(path, newTime);
        addChange.apply(timeline);

        // Then copy over all properties
        UpdateKeyframeProperties.Builder builder = UpdateKeyframeProperties.create(path, path.getKeyframe(newTime));
        for (Property property : keyframe.getProperties()) {
            copyProperty(property, keyframe, builder);
        }
        Change propertyChange = builder.done();
        propertyChange.apply(timeline);


        Change restoreInterpolatorChange;
        Keyframe newKf = path.getKeyframe(newTime);
        if (Iterables.getLast(path.getKeyframes()) != newKf) { // Unless this is the last keyframe
            // the interpolator of the following segment has been lost and needs to be restored
            restoreInterpolatorChange = interpolatorAfter.<Change>flatMap(interpolator ->
                    path.getSegments().stream().filter(s -> s.getStartKeyframe() == newKf).findFirst().map(segment ->
                            SetInterpolator.create(segment, interpolator)
                    )
            ).orElseGet(CombinedChange::create);
        } else { // If it is the last keyframe however,
            // the interpolator of the previous segment has been lost and needs to be restored
            restoreInterpolatorChange = interpolatorBefore.<Change>flatMap(interpolator ->
                    path.getSegments().stream().filter(s -> s.getEndKeyframe() == newKf).findFirst().map(segment ->
                            SetInterpolator.create(segment, interpolator)
                    )
            ).orElseGet(CombinedChange::create);
        }
        restoreInterpolatorChange.apply(timeline);

        // Finally update the interpolators
        Change interpolatorUpdateChange;
        if (spPath == SPPath.POSITION) {
            // Position / Spectator keyframes need special handling
            interpolatorUpdateChange = updateInterpolators();
        } else {
            // Time keyframes only need updating when only one segment of them exists
            if (path.getSegments().size() == 1) {
                assert firstInterpolator.isPresent() : "One segment should have existed before as well";
                interpolatorUpdateChange = SetInterpolator.create(path.getSegments().iterator().next(), firstInterpolator.get());
            } else {
                interpolatorUpdateChange = CombinedChange.create(); // Noop change
            }
            interpolatorUpdateChange.apply(timeline);
        }

        // and update spectator positions
        Change spectatorChange = updateSpectatorPositions();
        spectatorChange.apply(timeline);

        return CombinedChange.createFromApplied(removeChange, addChange, propertyChange,
                restoreInterpolatorChange, interpolatorUpdateChange, spectatorChange);
    }

    // Helper method because generics cannot be defined on blocks
    private <T> void copyProperty(Property<T> property, Keyframe from, UpdateKeyframeProperties.Builder to) {
        from.getValue(property).ifPresent(value -> to.setValue(property, value));
    }

    private Change updateInterpolators() {
        Collection<PathSegment> pathSegments = positionPath.getSegments();
        // Contains updated pathsegment-interpolator mappings, may be changed multiple times over the course of this method
        Map<PathSegment, Interpolator> updates = new HashMap<>();

        // First, setup spectator interpolators
        Interpolator interpolator = null;
        // Iterate through all segments
        for (PathSegment segment : pathSegments) {
            if (isSpectatorSegment(segment)) {
                // If the last segment was a spectator segment, then use its interpolator for this segment as well
                if (interpolator == null) {
                    // otherwise create a new interpolator
                    interpolator = new LinearInterpolator();
                    interpolator.registerProperty(SpectatorProperty.PROPERTY);
                }
                // Now that we have an interpolator, set it for the current segment
                updates.put(segment, interpolator);
            } else {
                // Not a spectator segment, make sure we don't re-use the last interpolator (it would not be continuous)
                interpolator = null;
            }
        }

        // Then, if required, replace old default interpolators with new ones
        pathSegments.stream()
                // Ignore explicitly set segments
                .filter(s -> !s.getStartKeyframe().getValue(ExplicitInterpolationProperty.PROPERTY).isPresent())
                // Ignore spectator segments
                .filter(s -> !isSpectatorSegment(s))
                // Ignore already correct segments, should ignore all if default hasn't changed
                .filter(s -> !s.getInterpolator().getClass().equals(defaultInterpolatorType.getInterpolatorClass()))
                // Finally, set the interpolators
                // This will create one interpolator per segment, they will be merged later
                .forEach(segment -> updates.put(segment, createDefaultInterpolator()));

        // All interpolators should now be set appropriately but may still be fragmented.
        // Cleaning that up is a two step process

        // Firstly, all interpolators that are not continuous need to be split up
        Interpolator lastInterpolator = null;
        Set<Interpolator> used = Collections.newSetFromMap(new IdentityHashMap<>());
        for (PathSegment segment : pathSegments) {
            if (isSpectatorSegment(segment)) {
                lastInterpolator = null;
                continue; // Chain broken by spectator segment
            }

            Interpolator currentInterpolator = updates.getOrDefault(segment, segment.getInterpolator());
            if (lastInterpolator == currentInterpolator) {
                continue; // All fine, interpolator is still continuous
            }

            // New interpolator, make sure it hasn't been used before
            if (!used.add(interpolator)) {
                // It has been used before, we need to clone it and use the clone instead
                // This will create a new interpolator for each segment which will be merged later
                currentInterpolator = cloneInterpolator(currentInterpolator);
                updates.put(segment, currentInterpolator);
            }
            lastInterpolator = currentInterpolator;
        }

        // Secondly, all neighbouring interpolators that are equal need to be merged
        lastInterpolator = null;
        String lastInterpolatorSerialized = null;
        for (PathSegment segment : pathSegments) {
            if (isSpectatorSegment(segment)) {
                lastInterpolator = null;
                lastInterpolatorSerialized = null;
                continue; // Spectator segments are continuous by construction
            }
            Interpolator currentInterpolator = updates.getOrDefault(segment, segment.getInterpolator());
            String serialized = serializeInterpolator(currentInterpolator);
            if (lastInterpolator != currentInterpolator && serialized.equals(lastInterpolatorSerialized)) {
                // This interpolator is equal to the last one but not the same, needs merging
                updates.put(segment, lastInterpolator);
                continue;
            }
            // New interpolator, not related to the previous one, merged by definition
            lastInterpolator = currentInterpolator;
            lastInterpolatorSerialized = serialized;
        }

        Change change = CombinedChange.create(updates.entrySet().stream()
                .map(e -> SetInterpolator.create(e.getKey(), e.getValue())).toArray(Change[]::new));
        change.apply(timeline);
        return change;
    }

    private boolean isSpectatorSegment(PathSegment segment) {
        return segment.getStartKeyframe().getValue(SpectatorProperty.PROPERTY).isPresent()
                && segment.getEndKeyframe().getValue(SpectatorProperty.PROPERTY).isPresent();
    }

    private Change updateSpectatorPositions() {
        List<Change> changes = new ArrayList<>();
        timePath.updateAll();
        for (Keyframe keyframe : positionPath.getKeyframes()) {
            Optional<Integer> spectator = keyframe.getValue(SpectatorProperty.PROPERTY);
            if (spectator.isPresent()) {
                Optional<Integer> time = timePath.getValue(TimestampProperty.PROPERTY, keyframe.getTime());
                if (!time.isPresent()) {
                    continue; // No time keyframes set at this video time, cannot determine replay time
                }
                Location expected = entityTracker.getEntityPositionAtTimestamp(spectator.get(), time.get());
                if (expected == null) {
                    continue; // We don't have any data on this entity for some reason
                }
                Triple<Double, Double, Double> pos = keyframe.getValue(CameraProperties.POSITION).orElse(Triple.of(0D, 0D, 0D));
                Triple<Float, Float, Float> rot = keyframe.getValue(CameraProperties.ROTATION).orElse(Triple.of(0F, 0F, 0F));
                Location actual = new Location(pos.getLeft(), pos.getMiddle(), pos.getRight(), rot.getLeft(), rot.getRight());
                if (!expected.equals(actual)) {
                    changes.add(UpdateKeyframeProperties.create(positionPath, keyframe)
                            .setValue(CameraProperties.POSITION, Triple.of(expected.getX(), expected.getY(), expected.getZ()))
                            .setValue(CameraProperties.ROTATION, Triple.of(expected.getYaw(), expected.getPitch(), 0f)).done()
                    );
                }
            }
        }
        return CombinedChange.create(changes.toArray(new Change[changes.size()]));
    }

    private Interpolator createDefaultInterpolator() {
        return registerPositionInterpolatorProperties(defaultInterpolatorType.newInstance());
    }

    private Interpolator registerPositionInterpolatorProperties(Interpolator interpolator) {
        interpolator.registerProperty(CameraProperties.POSITION);
        interpolator.registerProperty(CameraProperties.ROTATION);
        return interpolator;
    }

    @Override
    public Timeline createTimeline() {
        return createTimelineStatic();
    }

    private static Timeline createInitialTimeline() {
        Timeline timeline = createTimelineStatic();
        timeline.createPath();
        timeline.createPath();
        return timeline;
    }

    private static Timeline createTimelineStatic() {
        Timeline timeline = new TimelineImpl();

        timeline.registerProperty(TimestampProperty.PROPERTY);
        timeline.registerProperty(CameraProperties.POSITION);
        timeline.registerProperty(CameraProperties.ROTATION);
        timeline.registerProperty(SpectatorProperty.PROPERTY);
        timeline.registerProperty(ExplicitInterpolationProperty.PROPERTY);

        return timeline;
    }

    @Override
    public void serializeInterpolator(JsonWriter writer, Interpolator interpolator) throws IOException {
        if (interpolator instanceof LinearInterpolator) {
            writer.value("linear");
        } else if (interpolator instanceof CubicSplineInterpolator) {
            writer.value("cubic-spline");
        } else {
            throw new IOException("Unknown interpolator type: " + interpolator);
        }
    }

    @Override
    public Interpolator deserializeInterpolator(JsonReader reader) throws IOException {
        String type = reader.nextString();
        switch (type) {
            case "linear":
                return new LinearInterpolator();
            case "cubic-spline":
                return new CubicSplineInterpolator();
            default:
                throw new IOException("Unknown interpolation type: " + type);

        }
    }

    /**
     * Clones an interpolator by de- and re-serializing it.
     * @param interpolator The interpolator to clone
     * @return The cloned interpolator
     */
    private Interpolator cloneInterpolator(Interpolator interpolator) {
        Interpolator cloned = deserializeInterpolator(serializeInterpolator(interpolator));
        interpolator.getKeyframeProperties().forEach(cloned::registerProperty);
        return cloned;
    }

    /**
     * Serializes the specific interpolator to String.
     * Does <b>not</b> serialize the registered keyframe properties.
     * @param interpolator The interpolator to serialize.
     * @return The serialized interpolator
     */
    private String serializeInterpolator(Interpolator interpolator) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        JsonWriter jsonWriter = new JsonWriter(new PrintWriter(baos));
        try {
            jsonWriter.beginArray();
            serializeInterpolator(jsonWriter, interpolator);
            jsonWriter.endArray();
            jsonWriter.flush();
        } catch (IOException e) {
            CrashReport crash = CrashReport.makeCrashReport(e, "Serializing interpolator");
            CrashReportCategory category = crash.makeCategory("Serializing interpolator");
            category.addCrashSectionCallable("Interpolator", interpolator::toString);
            throw new ReportedException(crash);
        }

        return baos.toString();
    }

    private Interpolator deserializeInterpolator(String json) {
        JsonReader jsonReader = new JsonReader(new StringReader(json));
        try {
            jsonReader.beginArray();
            return deserializeInterpolator(jsonReader);
        } catch (IOException e) {
            CrashReport crash = CrashReport.makeCrashReport(e, "De-serializing interpolator");
            CrashReportCategory category = crash.makeCategory("De-serializing interpolator");
            category.addCrashSection("Interpolator", json);
            throw new ReportedException(crash);
        }
    }
}
