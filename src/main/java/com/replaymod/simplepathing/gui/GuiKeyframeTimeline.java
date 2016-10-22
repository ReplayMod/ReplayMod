package com.replaymod.simplepathing.gui;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline;
import de.johni0702.minecraft.gui.function.Draggable;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.util.Point;
import org.lwjgl.util.ReadableDimension;
import org.lwjgl.util.ReadablePoint;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Optional;

public class GuiKeyframeTimeline extends AbstractGuiTimeline<GuiKeyframeTimeline> implements Draggable {
    protected static final int KEYFRAME_SIZE = 5;
    protected static final int KEYFRAME_TEXTURE_X = 74;
    protected static final int KEYFRAME_TEXTURE_Y = 20;
    private static final int DOUBLE_CLICK_INTERVAL = 250;
    private static final int DRAGGING_THRESHOLD = KEYFRAME_SIZE;

    private final GuiPathing gui;

    /**
     * The keyframe that was last clicked on using the left mouse button.
     */
    private Keyframe lastClickedKeyframe;

    /**
     * Id of the path of {@link #lastClickedKeyframe}.
     */
    private int lastClickedPath;

    /**
     * The time at which {@link #lastClickedKeyframe} was updated.
     * According to {@link Minecraft#getSystemTime()}.
     */
    private long lastClickedTime;

    /**
     * Whether to handle dragging events.
     */
    private boolean dragging;

    /**
     * Whether we have surpassed the initial threshold and are actually dragging the keyframe.
     */
    private boolean actuallyDragging;

    /**
     * Where the mouse was when {@link #dragging} started.
     */
    private int draggingStartX;

    /**
     * Change caused by dragging. Whenever the user moves the keyframe further, the previous change is undone
     * and a new one is created. This way when the mouse is released, only one change is in the undo history.
     */
    private Change draggingChange;

    public GuiKeyframeTimeline(GuiPathing gui) {
        this.gui = gui;
    }

    @Override
    protected void drawTimelineCursor(GuiRenderer renderer, ReadableDimension size) {
        ReplayModSimplePathing mod = gui.getMod();

        int width = size.getWidth();
        int visibleWidth = width - BORDER_LEFT - BORDER_RIGHT;
        int startTime = getOffset();
        int visibleTime = (int) (getZoom() * getLength());
        int endTime = getOffset() + visibleTime;

        renderer.bindTexture(ReplayMod.TEXTURE);

        for (Keyframe keyframe : Iterables.concat(Iterables.transform(mod.getCurrentTimeline().getPaths(), new Function<Path, Iterable<Keyframe>>() {
            @Nullable
            @Override
            public Iterable<Keyframe> apply(@Nullable Path input) {
                assert input != null;
                return input.getKeyframes();
            }
        }))) {
            if (keyframe.getTime() >= startTime && keyframe.getTime() <= endTime) {
                double relativeTime = keyframe.getTime() - startTime;
                int positonX = BORDER_LEFT + (int) (relativeTime / visibleTime * visibleWidth) - KEYFRAME_SIZE / 2;
                int u = KEYFRAME_TEXTURE_X + (mod.getSelectedKeyframe() == keyframe ? KEYFRAME_SIZE : 0);
                int v = KEYFRAME_TEXTURE_Y;
                if (keyframe.getValue(CameraProperties.POSITION).isPresent()) {
                    if (keyframe.getValue(SpectatorProperty.PROPERTY).isPresent()) {
                        v += 2 * KEYFRAME_SIZE;
                    }
                    renderer.drawTexturedRect(positonX, BORDER_TOP, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);
                }
                if (keyframe.getValue(TimestampProperty.PROPERTY).isPresent()) {
                    v += KEYFRAME_SIZE;
                    renderer.drawTexturedRect(positonX, BORDER_TOP + KEYFRAME_SIZE, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);
                }
            }
        }

        // Draw colored quads on spectator path segments
        for (PathSegment segment : mod.getCurrentTimeline().getPaths().get(GuiPathing.POSITION_PATH).getSegments()) {
            if (segment.getInterpolator() == null
                    || !segment.getInterpolator().getKeyframeProperties().contains(SpectatorProperty.PROPERTY)) {
                continue; // Not a spectator segment
            }
            long startFrameTime = segment.getStartKeyframe().getTime();
            long endFrameTime = segment.getEndKeyframe().getTime();
            if (startFrameTime >= endTime || endFrameTime <= startTime) {
                continue; // Segment out of display range
            }

            double relativeStart = startFrameTime - startTime;
            double relativeEnd = endFrameTime - startTime;
            int startX = BORDER_LEFT + Math.max(0, (int) (relativeStart / visibleTime * visibleWidth) + KEYFRAME_SIZE / 2 + 1);
            int endX = BORDER_LEFT + Math.min(visibleWidth, (int) (relativeEnd / visibleTime * visibleWidth) - KEYFRAME_SIZE / 2);
            if (startX < endX) {
                renderer.drawRect(startX + 1, BORDER_TOP + 1, endX - startX - 2, KEYFRAME_SIZE - 2, 0xFF0088FF);
            }
        }

        super.drawTimelineCursor(renderer, size);
    }

    /**
     * Returns the keyframe at the specified position.
     * @param position The raw position
     * @return Pair of path id and keyframe or null when no keyframe was clicked
     */
    private Pair<Integer, Keyframe> getKeyframe(ReadablePoint position) {
        int time = getTimeAt(position.getX(), position.getY());
        if (time != -1) {
            Point mouse = new Point(position);
            getContainer().convertFor(this, mouse);
            int mouseY = mouse.getY();
            if (mouseY > BORDER_TOP && mouseY < BORDER_TOP + 2 * KEYFRAME_SIZE) {
                Timeline timeline = gui.getMod().getCurrentTimeline();
                int path;
                if (mouseY <= BORDER_TOP + KEYFRAME_SIZE) {
                    // Position keyframe
                    path = GuiPathing.POSITION_PATH;
                } else {
                    // Time keyframe
                    path = GuiPathing.TIME_PATH;
                }
                int visibleTime = (int) (getZoom() * getLength());
                int tolerance = visibleTime * KEYFRAME_SIZE / (size.getWidth() - BORDER_LEFT - BORDER_RIGHT) / 2;
                Optional<Keyframe> keyframe = timeline.getPaths().get(path).getKeyframes().stream()
                        .filter(k -> Math.abs(k.getTime() - time) <= tolerance)
                        .sorted(Comparator.comparing(k -> Math.abs(k.getTime() - time)))
                        .findFirst();
                return Pair.of(path, keyframe.orElse(null));
            }
        }
        return Pair.of(null, null);
    }

    @Override
    public boolean mouseClick(ReadablePoint position, int button) {
        int time = getTimeAt(position.getX(), position.getY());
        Pair<Integer, Keyframe> pathKeyframePair = getKeyframe(position);
        if (pathKeyframePair.getRight() != null) {
            // Clicked on keyframe
            Keyframe keyframe = pathKeyframePair.getRight();
            if (button == 0) { // Left click
                long now = Minecraft.getSystemTime();
                if (lastClickedKeyframe == keyframe) {
                    // Clicked the same keyframe again, potentially a double click
                    if (now - lastClickedTime < DOUBLE_CLICK_INTERVAL) {
                        // Yup, double click, open the edit keyframe gui
                        Path path = gui.getMod().getCurrentTimeline().getPaths().get(pathKeyframePair.getLeft());
                        gui.openEditKeyframePopup(path, keyframe);
                        return true;
                    }
                }
                // Not a double click, just update the click time and selection
                lastClickedTime = now;
                lastClickedKeyframe = keyframe;
                lastClickedPath = pathKeyframePair.getLeft();
                gui.getMod().setSelectedKeyframe(lastClickedKeyframe);
                // We might be dragging
                draggingStartX = position.getX();
                dragging = true;
            } else if (button == 1) { // Right click
                for (Property property : keyframe.getProperties()) {
                    applyPropertyToGame(property, keyframe);
                }
            }
            return true;
        } else if (time != -1) {
            // Clicked on timeline but not on any keyframe
            if (button == 0) { // Left click
                setCursorPosition(time);
                gui.getMod().setSelectedKeyframe(null);
            } else if (button == 1) { // Right click
                if (pathKeyframePair.getLeft() != null) {
                    // Apply the value of the clicked path at the clicked position
                    Timeline timeline = gui.getMod().getCurrentTimeline();
                    Path path = timeline.getPaths().get(pathKeyframePair.getLeft());
                    path.getKeyframes().stream().flatMap(k -> k.getProperties().stream()).distinct().forEach(
                            p -> applyPropertyToGame(p, path, time));
                }
            }
            return true;
        }
        // Missed timeline
        return false;
    }

    // Helper method because generics cannot be defined on blocks
    private <T> void applyPropertyToGame(Property<T> property, Path path, long time) {
        Optional<T> value = path.getValue(property, time);
        if (value.isPresent()) {
            property.applyToGame(value.get(), ReplayModReplay.instance.getReplayHandler());
        }
    }

    // Helper method because generics cannot be defined on blocks
    private <T> void applyPropertyToGame(Property<T> property, Keyframe keyframe) {
        Optional<T> value = keyframe.getValue(property);
        if (value.isPresent()) {
            property.applyToGame(value.get(), ReplayModReplay.instance.getReplayHandler());
        }
    }

    @Override
    public boolean mouseDrag(ReadablePoint position, int button, long timeSinceLastCall) {
        if (!dragging) {
            if (button == 0) {
                // Left click, the user might try to move the cursor by clicking and holding
                int time = getTimeAt(position.getX(), position.getY());
                if (time != -1) {
                    // and they are still on the timeline, so update the time appropriately
                    setCursorPosition(time);
                    return true;
                }
            }
            return false;
        }

        if (!actuallyDragging) {
            // Check if threshold has been passed by now
            if (Math.abs(position.getX() - draggingStartX) >= DRAGGING_THRESHOLD) {
                actuallyDragging = true;
            }
        }
        if (actuallyDragging) {
            // Threshold passed
            Path path = gui.getMod().getCurrentTimeline().getPaths().get(lastClickedPath);
            Point mouse = new Point(position);
            getContainer().convertFor(this, mouse);
            int mouseX = mouse.getX();
            int width = size.getWidth();
            int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;
            double segmentLength = getLength() * getZoom();
            double segmentTime =  segmentLength * (mouseX - BORDER_LEFT) / bodyWidth;
            int newTime = Math.min(Math.max((int) Math.round(getOffset() + segmentTime), 0), getLength());
            if (newTime < 0) {
                return true;
            }

            // If there already is a keyframe at the target time, then increase the time by one until there is none
            while (path.getKeyframe(newTime) != null) {
                newTime++;
            }

            // First undo any previous changes
            if (draggingChange != null) {
                draggingChange.undo(gui.getMod().getCurrentTimeline());
            }

            // Move keyframe to new position and
            // store change for later undoing / pushing to history
            draggingChange = gui.moveKeyframe(path, lastClickedKeyframe, newTime);

            // Selected keyframe has been replaced
            gui.getMod().setSelectedKeyframe(path.getKeyframe(newTime));

            // Path has been changed
            path.updateAll();
        }
        return true;
    }

    @Override
    public boolean mouseRelease(ReadablePoint position, int button) {
        if (dragging) {
            if (actuallyDragging) {
                gui.getMod().getCurrentTimeline().pushChange(draggingChange);
                draggingChange = null;
                actuallyDragging = false;
            }
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    protected GuiKeyframeTimeline getThis() {
        return this;
    }
}
