package com.replaymod.simplepathing;

import com.google.common.collect.Iterables;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.interpolation.CatmullRomSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.util.EntityPositionTracker;
import com.replaymod.replaystudio.util.Location;
import com.replaymod.simplepathing.SPTimeline.SPPath;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SPTimelineTest {
    private SPTimeline impl;
    private Timeline timeline;

    @BeforeClass
    public static void setupLogger() {
        ReplayModSimplePathing.LOGGER = LogManager.getLogger();
    }

    @Before
    public void setup() {
        impl = new SPTimeline();
        impl.setDefaultInterpolatorType(InterpolatorType.CUBIC);
        impl.setEntityTracker(new EntityPositionTrackerMock());
        timeline = impl.getTimeline();
    }

    @Test
    public void testGetPath() {
        assertSame(impl.getTimeline().getPaths().get(0), impl.getTimePath());
        assertSame(impl.getTimeline().getPaths().get(0), impl.getPath(SPPath.TIME));
        assertSame(impl.getTimeline().getPaths().get(1), impl.getPositionPath());
        assertSame(impl.getTimeline().getPaths().get(1), impl.getPath(SPPath.POSITION));
    }

    @Test
    public void testGetKeyframe() {
        assertSame(timeline.getPaths().get(0).insert(123), impl.getKeyframe(SPPath.TIME, 123));
        assertSame(timeline.getPaths().get(1).insert(456), impl.getKeyframe(SPPath.POSITION, 456));
    }

    @Test
    public void testIsTimeKeyframe() {
        impl.getTimePath().insert(123);
        impl.getPositionPath().insert(456);
        impl.getPositionPath().insert(42).setValue(SpectatorProperty.PROPERTY, 42);
        assertTrue(impl.isTimeKeyframe(123));
        assertFalse(impl.isTimeKeyframe(456));
        assertFalse(impl.isTimeKeyframe(789));
        assertFalse(impl.isTimeKeyframe(42));
    }

    @Test
    public void testIsPositionKeyframe() {
        impl.getTimePath().insert(123);
        impl.getPositionPath().insert(456);
        impl.getPositionPath().insert(42).setValue(SpectatorProperty.PROPERTY, 42);
        assertFalse(impl.isPositionKeyframe(123));
        assertTrue(impl.isPositionKeyframe(456));
        assertFalse(impl.isPositionKeyframe(789));
        assertTrue(impl.isSpectatorKeyframe(42));
    }

    @Test
    public void testIsSpectatorKeyframe() {
        impl.getTimePath().insert(123);
        impl.getPositionPath().insert(456);
        impl.getPositionPath().insert(42).setValue(SpectatorProperty.PROPERTY, 42);
        assertFalse(impl.isSpectatorKeyframe(123));
        assertFalse(impl.isSpectatorKeyframe(456));
        assertFalse(impl.isSpectatorKeyframe(789));
        assertTrue(impl.isSpectatorKeyframe(42));
    }

    @Test
    public void testAddPositionKeyframe() {
        assertNull(timeline.peekUndoStack());
        impl.addPositionKeyframe(0, 1, 2, 3, 4, 5, 6, -1);
        assertNotNull(timeline.peekUndoStack());
        Keyframe keyframe = impl.getKeyframe(SPPath.POSITION, 0);
        assertNotNull(keyframe);
        assertEquals(keyframe.getValue(CameraProperties.POSITION), Optional.of(Triple.of(1d, 2d, 3d)));
        assertEquals(keyframe.getValue(CameraProperties.ROTATION), Optional.of(Triple.of(4f, 5f, 6f)));
        assertFalse(keyframe.getValue(SpectatorProperty.PROPERTY).isPresent());
        timeline.undoLastChange();
        assertNull(timeline.peekUndoStack());
        assertNull(impl.getKeyframe(SPPath.POSITION, 0));
    }

    @Test
    public void testAddPositionKeyframeSpectator() {
        assertNull(timeline.peekUndoStack());
        impl.addPositionKeyframe(0, 1, 2, 3, 4, 5, 6, 7);
        assertNotNull(timeline.peekUndoStack());
        Keyframe keyframe = impl.getKeyframe(SPPath.POSITION, 0);
        assertNotNull(keyframe);
        assertEquals(keyframe.getValue(CameraProperties.POSITION), Optional.of(Triple.of(1d, 2d, 3d)));
        assertEquals(keyframe.getValue(CameraProperties.ROTATION), Optional.of(Triple.of(4f, 5f, 6f)));
        assertEquals(keyframe.getValue(SpectatorProperty.PROPERTY), Optional.of(7));
        timeline.undoLastChange();
        assertNull(timeline.peekUndoStack());
        assertNull(impl.getKeyframe(SPPath.POSITION, 0));
    }

    @Test
    public void testPositionKeyframeSpectatorPosition() {
        Keyframe keyframe;
        impl.addTimeKeyframe(0, 0);
        impl.addTimeKeyframe(9, 9);

        impl.addPositionKeyframe(4, 1, 2, 3, 4, 5, 6, 1);
        keyframe = impl.getKeyframe(SPPath.POSITION, 4);
        assertEquals(keyframe.getValue(CameraProperties.POSITION), Optional.of(Triple.of(1d, 2d, 3d)));
        assertEquals(keyframe.getValue(CameraProperties.ROTATION), Optional.of(Triple.of(4f, 5f, 6f)));

        impl.addPositionKeyframe(6, 1, 2, 3, 4, 5, 6, 42);
        keyframe = impl.getKeyframe(SPPath.POSITION, 6);
        assertEquals(keyframe.getValue(CameraProperties.POSITION), Optional.of(Triple.of(12d, 0d, 0d)));
        assertEquals(keyframe.getValue(CameraProperties.ROTATION), Optional.of(Triple.of(0f, 0f, 0f)));

        impl.addPositionKeyframe(8, 1, 2, 3, 4, 5, 6, 42);
        keyframe = impl.getKeyframe(SPPath.POSITION, 8);
        assertEquals(keyframe.getValue(CameraProperties.POSITION), Optional.of(Triple.of(16d, 0d, 0d)));
        assertEquals(keyframe.getValue(CameraProperties.ROTATION), Optional.of(Triple.of(0f, 0f, 0f)));

        impl.addPositionKeyframe(10, 1, 2, 3, 4, 5, 6, 42);
        keyframe = impl.getKeyframe(SPPath.POSITION, 10);
        assertEquals(keyframe.getValue(CameraProperties.POSITION), Optional.of(Triple.of(1d, 2d, 3d)));
        assertEquals(keyframe.getValue(CameraProperties.ROTATION), Optional.of(Triple.of(4f, 5f, 6f)));
    }

    @Test(expected = IllegalStateException.class)
    public void testAddPositionKeyframeDuplicate() {
        impl.addPositionKeyframe(0, 1, 2, 3, 4, 5, 6, -1);
        impl.addPositionKeyframe(0, 1, 2, 3, 4, 5, 6, -1);
    }

    @Test
    public void testUpdatePositionKeyframe() {
        impl.addPositionKeyframe(0, 1, 2, 3, 4, 5, 6, -1);
        Change change = impl.updatePositionKeyframe(0, 7, 8, 9, 10, 11, 12);
        assertNotNull(change);
        Keyframe keyframe = impl.getKeyframe(SPPath.POSITION, 0);
        assertEquals(keyframe.getValue(CameraProperties.POSITION), Optional.of(Triple.of(7d, 8d, 9d)));
        assertEquals(keyframe.getValue(CameraProperties.ROTATION), Optional.of(Triple.of(10f, 11f, 12f)));
        assertFalse(keyframe.getValue(SpectatorProperty.PROPERTY).isPresent());
        change.undo(timeline);
        keyframe = impl.getKeyframe(SPPath.POSITION, 0);
        assertEquals(keyframe.getValue(CameraProperties.POSITION), Optional.of(Triple.of(1d, 2d, 3d)));
        assertEquals(keyframe.getValue(CameraProperties.ROTATION), Optional.of(Triple.of(4f, 5f, 6f)));
        assertFalse(keyframe.getValue(SpectatorProperty.PROPERTY).isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdatePositionKeyframeNoKeyframe() {
        impl.updatePositionKeyframe(0, 1, 2, 3, 4, 5, 6);
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdatePositionKeyframeSpectatorKeyframe() {
        impl.addPositionKeyframe(0, 1, 2, 3, 4, 5, 6, 7);
        impl.updatePositionKeyframe(0, 1, 2, 3, 4, 5, 6);
    }

    @Test
    public void testRemovePositionKeyframe() {
        impl.addPositionKeyframe(0, 1, 2, 3, 4, 5, 6, -1);
        impl.removePositionKeyframe(0);
        assertNull(impl.getKeyframe(SPPath.POSITION, 0));
    }

    @Test(expected = IllegalStateException.class)
    public void testRemovePositionKeyframeNoKeyframe() {
        impl.removePositionKeyframe(0);
    }

    @Test
    public void testAddTimeKeyframe() {
        assertNull(timeline.peekUndoStack());
        impl.addTimeKeyframe(0, 1);
        assertNotNull(timeline.peekUndoStack());
        Keyframe keyframe = impl.getKeyframe(SPPath.TIME, 0);
        assertNotNull(keyframe);
        assertEquals(keyframe.getValue(TimestampProperty.PROPERTY), Optional.of(1));
        timeline.undoLastChange();
        assertNull(timeline.peekUndoStack());
        assertNull(impl.getKeyframe(SPPath.TIME, 0));
    }

    @Test(expected = IllegalStateException.class)
    public void testAddTimeKeyframeDuplicate() {
        impl.addTimeKeyframe(0, 1);
        impl.addTimeKeyframe(0, 1);
    }

    @Test
    public void testUpdateTimeKeyframe() {
        impl.addTimeKeyframe(0, 1);
        Change change = impl.updateTimeKeyframe(0, 2);
        assertNotNull(change);
        assertEquals(impl.getKeyframe(SPPath.TIME, 0).getValue(TimestampProperty.PROPERTY), Optional.of(2));
        change.undo(timeline);
        assertEquals(impl.getKeyframe(SPPath.TIME, 0).getValue(TimestampProperty.PROPERTY), Optional.of(1));
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateTimeKeyframeNoKeyframe() {
        impl.updateTimeKeyframe(0, 1);
    }

    @Test
    public void testRemoveTimeKeyframe() {
        impl.addTimeKeyframe(0, 1);
        impl.removeTimeKeyframe(0);
        assertNull(impl.getKeyframe(SPPath.TIME, 0));
    }

    @Test(expected = IllegalStateException.class)
    public void testRemoveTimeKeyframeNoKeyframe() {
        impl.removeTimeKeyframe(0);
    }

    /**
     * Checks all interpolators to make sure that they are continuous and not fragmented.
     */
    private void assertValidInterpolators(SPPath path, int expectedNumberOfInterpolators) {
        String str = prettyPrintInterpolators(impl, path);
        List<Interpolator> interpolators = impl.getPath(path).getSegments().stream().map(PathSegment::getInterpolator)
                .distinct().collect(Collectors.toList());
        assertFalse("Not all interpolators set: " + str, interpolators.contains(null));
        assertEquals("Mismatched interpolator number: " + str, expectedNumberOfInterpolators, interpolators.size());
        int i = 0;
        for (Interpolator interpolator : interpolators) {
            Keyframe lastKeyframe = null;
            for (PathSegment segment : interpolator.getSegments().stream()
                    .sorted(Comparator.comparing(s -> s.getStartKeyframe().getTime())).collect(Collectors.toList())) {
                String message = "Fragmentation in interpolator " + i + ": " + str;
                assertTrue(message, lastKeyframe == null || segment.getStartKeyframe() == lastKeyframe);
                lastKeyframe = segment.getEndKeyframe();
            }
            i++;
        }
    }

    @Test
    public void testAddPositionKeyframeAppend() {
        addPosition(0, 0);
        addPosition(1, 1);
        addPosition(2, 1);
        addPosition(3, 1);
        addPosition(4, 1);
    }

    @Test
    public void testAddPositionKeyframePrepend() {
        addPosition(4, 0);
        addPosition(3, 1);
        addPosition(2, 1);
        addPosition(1, 1);
        addPosition(0, 1);
    }

    @Test
    public void testAddPositionKeyframeMixed() {
        addPosition(2, 0);
        addPosition(4, 1);
        addPosition(3, 1);
        addPosition(0, 1);
        addPosition(1, 1);
    }

    @Test
    public void testAddPositionSpectatorKeyframe() {
        addPosition(0, 0);
        addSpectator(1, 1);
        addSpectator(2, 2);
        addPosition(4, 3);
        addPosition(7, 3);
        addPosition(8, 3);
        addSpectator(3, 3);
        addSpectator(5, 3);
        addSpectator(6, 5);
    }

    @Test
    public void testRemovePositionKeyframeFromStart() {
        addPosition(0, 0);
        addPosition(1, 1);
        addPosition(2, 1);
        addPosition(3, 1);
        removePosition(0, 1);
        removePosition(1, 1);
        removePosition(2, 0);
        removePosition(3, 0);
    }

    @Test
    public void testRemovePositionKeyframeFromEnd() {
        addPosition(0, 0);
        addPosition(1, 1);
        addPosition(2, 1);
        addPosition(3, 1);
        removePosition(3, 1);
        removePosition(2, 1);
        removePosition(1, 0);
        removePosition(0, 0);
    }

    @Test
    public void testRemovePositionKeyframeFromMiddle() {
        addPosition(0, 0);
        addPosition(1, 1);
        addPosition(2, 1);
        addPosition(3, 1);
        removePosition(1, 1);
        removePosition(2, 1);
        removePosition(0, 0);
        removePosition(3, 0);
    }

    @Test
    public void testRemovePositionSpectatorKeyframe() {
        addPosition(0, 0);
        addPosition(1, 1);
        addPosition(2, 1);
        addSpectator(3, 1);
        addSpectator(4, 2);
        addSpectator(5, 2);
        addPosition(6, 3);
        addPosition(7, 3);
        addSpectator(8, 3);
        addSpectator(9, 4);
        removePosition(4, 4);
        removePosition(9, 3);
        removePosition(6, 3);
        removePosition(7, 2);
        removePosition(0, 2);
        removePosition(1, 2);
        removePosition(3, 2);
        removePosition(2, 1);
        removePosition(5, 0);
        removePosition(8, 0);
    }

    @Test
    public void testAddTimeKeyframeAppend() {
        addTime(0, 0);
        addTime(1, 1);
        addTime(2, 1);
        addTime(3, 1);
        addTime(4, 1);
    }

    @Test
    public void testAddTimeKeyframePrepend() {
        addTime(4, 0);
        addTime(3, 1);
        addTime(2, 1);
        addTime(1, 1);
        addTime(0, 1);
    }

    @Test
    public void testAddTimeKeyframeMixed() {
        addTime(2, 0);
        addTime(4, 1);
        addTime(3, 1);
        addTime(0, 1);
        addTime(1, 1);
    }

    @Test
    public void testRemoveTimeKeyframeFromStart() {
        addTime(0, 0);
        addTime(1, 1);
        addTime(2, 1);
        addTime(3, 1);
        removeTime(0, 1);
        removeTime(1, 1);
        removeTime(2, 0);
        removeTime(3, 0);
    }

    @Test
    public void testRemoveTimeKeyframeFromEnd() {
        addTime(0, 0);
        addTime(1, 1);
        addTime(2, 1);
        addTime(3, 1);
        removeTime(3, 1);
        removeTime(2, 1);
        removeTime(1, 0);
        removeTime(0, 0);
    }

    @Test
    public void testRemoveTimeKeyframeFromMiddle() {
        addTime(0, 0);
        addTime(1, 1);
        addTime(2, 1);
        addTime(3, 1);
        removeTime(1, 1);
        removeTime(2, 1);
        removeTime(0, 0);
        removeTime(3, 0);
    }

    @Test
    public void testSetInterpolator() {
        addPosition(0, 0);
        addPosition(1, 1);
        addPosition(2, 1);
        setInterpolator(1, new LinearInterpolator(), 2);
        assertIsLinear(1);
        Interpolator interpolator = Iterables.get(impl.getPositionPath().getSegments(), 1).getInterpolator();
        assertTrue(interpolator.getKeyframeProperties().contains(CameraProperties.POSITION));
        assertTrue(interpolator.getKeyframeProperties().contains(CameraProperties.ROTATION));
        addPosition(3, 3);
        addPosition(4, 3);
        addPosition(5, 3);
        setInterpolator(3, new LinearInterpolator(), 5);
        assertIsLinear(3);
        removePosition(2, 3);
        removePosition(3, 3);
        removePosition(0, 2);
        removePosition(1, 1);
        removePosition(4, 0);
        removePosition(5, 0);
    }

    private void assertIsLinear(int index) {
        assertTrue("Expected segment " + index + " to have linear interpolator: " + prettyPrintInterpolators(impl, SPPath.POSITION),
                Iterables.get(impl.getPositionPath().getSegments(), index).getInterpolator() instanceof LinearInterpolator);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetInterpolatorNoKeyframe() {
        addTime(0, 0);
        impl.setInterpolator(0, new LinearInterpolator());
    }

    @Test(expected = IllegalStateException.class)
    public void testSetInterpolatorToDefaultNoKeyframe() {
        addTime(0, 0);
        impl.setInterpolatorToDefault(0);
    }

    @Test
    public void testMoveKeyframe() {
        addPosition(1, 0);
        addPosition(3, 1);
        addPosition(5, 1);
        addPosition(7, 1);
        addSpectator(9, 1);
        addSpectator(11, 2);
        addSpectator(13, 2);
        addPosition(15, 3);
        addPosition(17, 3);
        addPosition(19, 3);
        setInterpolator(3, new LinearInterpolator(), 5);
        assertIsLinear(1);
        setInterpolator(5, new LinearInterpolator(), 5);
        assertIsLinear(2);
        // [P 1] C0 [P 3] L1 [P 5] L1 [P 7] C2 [S 9] L3 [S 11] L3 [S 13] C4 [P 15] C4 [P 17] C4 [P 19]

        impl.moveKeyframe(SPPath.POSITION, 5, 0);
        assertValidInterpolators(SPPath.POSITION, 6);
        assertIsLinear(0);
        impl.moveKeyframe(SPPath.POSITION, 0, 5);
        assertValidInterpolators(SPPath.POSITION, 5);
        assertIsLinear(2);

        impl.moveKeyframe(SPPath.POSITION, 3, 12);
        assertValidInterpolators(SPPath.POSITION, 7);
        assertIsLinear(5);
        impl.moveKeyframe(SPPath.POSITION, 12, 3);
        assertValidInterpolators(SPPath.POSITION, 5);
        assertIsLinear(1);

        impl.moveKeyframe(SPPath.POSITION, 7, 14);
        assertValidInterpolators(SPPath.POSITION, 4);
        impl.moveKeyframe(SPPath.POSITION, 14, 20);
        assertValidInterpolators(SPPath.POSITION, 4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDefaultInterpolatorDifferentType() {
        addPosition(0, 0);
        addPosition(1, 1);
        impl.setDefaultInterpolator(new CatmullRomSplineInterpolator(42));
    }

    @Test
    public void testSetDefaultInterpolatorSameType() {
        impl.setDefaultInterpolatorType(InterpolatorType.CATMULL_ROM);
        addPosition(0, 0);
        addSpectator(1, 1);
        addSpectator(2, 2);
        addPosition(3, 3);
        addPosition(4, 3);
        impl.setDefaultInterpolator(new CatmullRomSplineInterpolator(42));
        assertValidInterpolators(SPPath.POSITION, 3);
        assertIsCatmullRom(0, 42);
        assertIsCatmullRom(2, 42);
        assertIsCatmullRom(3, 42);
    }

    private void assertIsCatmullRom(int index, double alpha) {
        String str = prettyPrintInterpolators(impl, SPPath.POSITION);
        Interpolator interpolator = Iterables.get(impl.getPositionPath().getSegments(), index).getInterpolator();
        assertTrue("Expected segment " + index + " to be catmull rom interpolator: " + str,
                 interpolator instanceof CatmullRomSplineInterpolator);
        assertTrue("Expected interpolator of segment segment " + index + " to have alpha " + alpha + ": " + str,
                ((CatmullRomSplineInterpolator) interpolator).getAlpha() == 42);
    }

    private void addPosition(int time, int expectedNumberOfInterpolators) {
        impl.addPositionKeyframe(time, 1, 2, 3, 4, 5, 6, -1);
        assertNotNull(impl.getKeyframe(SPPath.POSITION, time));
        assertValidInterpolators(SPPath.POSITION, expectedNumberOfInterpolators);
    }

    private void addSpectator(int time, int expectedNumberOfInterpolators) {
        impl.addPositionKeyframe(time, 1, 2, 3, 4, 5, 6, 42);
        assertNotNull(impl.getKeyframe(SPPath.POSITION, time));
        assertTrue(impl.isSpectatorKeyframe(time));
        assertValidInterpolators(SPPath.POSITION, expectedNumberOfInterpolators);
    }

    private void addTime(int time, int expectedNumberOfInterpolators) {
        impl.addTimeKeyframe(time, 1);
        assertNotNull(impl.getKeyframe(SPPath.TIME, time));
        assertValidInterpolators(SPPath.TIME, expectedNumberOfInterpolators);
    }

    private void removePosition(int time, int expectedNumberOfInterpolators) {
        impl.removePositionKeyframe(time);
        assertNull(impl.getKeyframe(SPPath.POSITION, time));
        assertValidInterpolators(SPPath.POSITION, expectedNumberOfInterpolators);
    }

    private void removeTime(int time, int expectedNumberOfInterpolators) {
        impl.removeTimeKeyframe(time);
        assertNull(impl.getKeyframe(SPPath.TIME, time));
        assertValidInterpolators(SPPath.TIME, expectedNumberOfInterpolators);
    }

    private void setInterpolator(int time, Interpolator interpolator, int expectedNumberOfInterpolators) {
        impl.setInterpolator(time, interpolator);
        assertValidInterpolators(SPPath.POSITION, expectedNumberOfInterpolators);
    }

    private void assertPositionState(String expected) {
        assertEquals(expected, prettyPrintInterpolators(impl, SPPath.POSITION));
    }

    // Tracks only entity 42 which always is at x=time*2, y,z=0
    private class EntityPositionTrackerMock extends EntityPositionTracker {
        public EntityPositionTrackerMock() {
            super(null);
        }

        @Override
        public Location getEntityPositionAtTimestamp(int entityID, long timestamp) {
            if (entityID == 42) {
                return new Location(timestamp * 2, 0 ,0, 0, 0);
            } else {
                return null;
            }
        }
    }

    public static String prettyPrintInterpolators(SPTimeline timeline, SPPath spPath) {
        return prettyPrintInterpolators(timeline, spPath, Collections.emptyMap());
    }

    public static String prettyPrintInterpolators(SPTimeline timeline, SPPath spPath, Map<PathSegment, Interpolator> updates) {
        Map<Interpolator, String> interpolatorIdMap = new IdentityHashMap<>();
        Path path = timeline.getPath(spPath);
        StringBuilder sb = new StringBuilder();
        for (Keyframe keyframe : path.getKeyframes()) {
            if (spPath == SPPath.TIME) {
                sb.append("[T ").append(keyframe.getTime()).append("]");
            } else if (keyframe.getValue(SpectatorProperty.PROPERTY).isPresent()) {
                sb.append("[S ").append(keyframe.getTime()).append("]");
            } else {
                sb.append("[P ").append(keyframe.getTime()).append("]");
            }
            path.getSegments().stream().filter(s -> s.getStartKeyframe() == keyframe).findFirst().ifPresent(segment -> {
                Interpolator interpolator = updates.getOrDefault(segment, segment.getInterpolator());
                String id = interpolatorIdMap.computeIfAbsent(interpolator, i ->
                        (i instanceof LinearInterpolator ? "L" : "C") + interpolatorIdMap.size());
                sb.append(' ').append(id).append(' ');
            });
        }
        return sb.toString();
    }
}
