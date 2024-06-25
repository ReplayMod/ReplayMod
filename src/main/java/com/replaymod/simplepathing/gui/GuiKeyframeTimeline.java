package com.replaymod.simplepathing.gui;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.gui.overlay.GuiMarkerTimeline;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.pathing.property.Property;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.SPTimeline;
import com.replaymod.simplepathing.SPTimeline.SPPath;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline;
import de.johni0702.minecraft.gui.function.Draggable;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector2f;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.apache.commons.lang3.tuple.Pair;
import de.johni0702.minecraft.gui.utils.lwjgl.Point;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import org.lwjgl.opengl.GL11;

import java.util.Comparator;
import java.util.Optional;

import static com.replaymod.core.versions.MCVer.emitLine;
import static de.johni0702.minecraft.gui.versions.MCVer.popScissorState;
import static de.johni0702.minecraft.gui.versions.MCVer.pushScissorState;
import static de.johni0702.minecraft.gui.versions.MCVer.setScissorDisabled;

//#if MC>=11700
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import net.minecraft.client.render.GameRenderer;
//#endif

public class GuiKeyframeTimeline extends AbstractGuiTimeline<GuiKeyframeTimeline> implements Draggable {
    protected static final int KEYFRAME_SIZE = 5;
    protected static final int KEYFRAME_TEXTURE_X = 74;
    protected static final int KEYFRAME_TEXTURE_Y = 20;
    private static final int DOUBLE_CLICK_INTERVAL = 250;
    private static final int DRAGGING_THRESHOLD = KEYFRAME_SIZE;

    private final GuiPathing gui;

    /**
     * The keyframe (time on timeline) that was last clicked on using the left mouse button.
     */
    private long lastClickedKeyframe;

    /**
     * Path of {@link #lastClickedKeyframe}.
     */
    private SPPath lastClickedPath;

    /**
     * The time at which {@link #lastClickedKeyframe} was updated.
     * According to {@link MCVer#milliTime()}.
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

        SPTimeline timeline = mod.getCurrentTimeline();

        timeline.getTimeline().getPaths().stream().flatMap(path -> path.getKeyframes().stream()).forEach(keyframe -> {
            if (keyframe.getTime() >= startTime && keyframe.getTime() <= endTime) {
                double relativeTime = keyframe.getTime() - startTime;
                int positonX = BORDER_LEFT + (int) (relativeTime / visibleTime * visibleWidth) - KEYFRAME_SIZE / 2;
                int u = KEYFRAME_TEXTURE_X + (mod.isSelected(keyframe) ? KEYFRAME_SIZE : 0);
                int v = KEYFRAME_TEXTURE_Y;
                if (keyframe.getValue(CameraProperties.POSITION).isPresent()) {
                    if (keyframe.getValue(SpectatorProperty.PROPERTY).isPresent()) {
                        v += 2 * KEYFRAME_SIZE;
                    }
                    renderer.drawTexturedRect(positonX, BORDER_TOP, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);
                }
                Optional<Integer> timeProperty = keyframe.getValue(TimestampProperty.PROPERTY);
                if (timeProperty.isPresent()) {
                    v += KEYFRAME_SIZE;
                    renderer.drawTexturedRect(positonX, BORDER_TOP + KEYFRAME_SIZE, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);

                    GuiMarkerTimeline replayTimeline = gui.overlay.timeline;
                    GuiKeyframeTimeline keyframeTimeline = this;

                    ReadableDimension replayTimelineSize = replayTimeline.getLastSize();
                    ReadableDimension keyframeTimelineSize = this.getLastSize();
                    if (replayTimelineSize == null || keyframeTimelineSize == null) {
                        return;
                    }

                    // Determine absolute positions for both timelines
                    Point replayTimelinePos = new Point(0, 0);
                    Point keyframeTimelinePos = new Point(0, 0);
                    replayTimeline.getContainer().convertFor(replayTimeline, replayTimelinePos);
                    keyframeTimeline.getContainer().convertFor(keyframeTimeline, keyframeTimelinePos);
                    replayTimelinePos.setLocation(-replayTimelinePos.getX(), -replayTimelinePos.getY());
                    keyframeTimelinePos.setLocation(-keyframeTimelinePos.getX(), -keyframeTimelinePos.getY());

                    int replayTimelineLeft = replayTimelinePos.getX();
                    int replayTimelineRight = replayTimelinePos.getX() + replayTimelineSize.getWidth();
                    int replayTimelineTop = replayTimelinePos.getY();
                    int replayTimelineBottom = replayTimelinePos.getY() + replayTimelineSize.getHeight();
                    int replayTimelineWidth = replayTimelineRight - replayTimelineLeft - BORDER_LEFT - BORDER_RIGHT;

                    int keyframeTimelineLeft = keyframeTimelinePos.getX();
                    int keyframeTimelineTop = keyframeTimelinePos.getY();

                    float positionXReplayTimeline = BORDER_LEFT + timeProperty.get() / (float) replayTimeline.getLength() * replayTimelineWidth;
                    float positionXKeyframeTimeline = positonX + KEYFRAME_SIZE / 2f;

                    final int color = 0xff0000ff;
                    Tessellator tessellator = Tessellator.getInstance();
                    //#if MC>=12100
                    //$$ BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.LINE_STRIP, VertexFormats.LINES);
                    //#else
                    BufferBuilder buffer = tessellator.getBuffer();
                    buffer.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
                    //#endif

                    // Start just below the top border of the replay timeline
                    Vector2f p1 = new Vector2f(replayTimelineLeft + positionXReplayTimeline, replayTimelineTop + BORDER_TOP);
                    // Draw vertically over the replay timeline, including its bottom border
                    Vector2f p2 = new Vector2f(replayTimelineLeft + positionXReplayTimeline, replayTimelineBottom);
                    // Now for the important part: connecting to the keyframe timeline
                    Vector2f p3 = new Vector2f(keyframeTimelineLeft + positionXKeyframeTimeline, keyframeTimelineTop);
                    // And finally another vertical bit (the timeline is already crammed enough, so only the border)
                    Vector2f p4 = new Vector2f(keyframeTimelineLeft + positionXKeyframeTimeline, keyframeTimelineTop + BORDER_TOP);

                    MatrixStack matrixStack = renderer.getMatrixStack();
                    emitLine(matrixStack, buffer, p1, p2, color);
                    emitLine(matrixStack, buffer, p2, p3, color);
                    emitLine(matrixStack, buffer, p3, p4, color);

                    //#if MC>=11700
                    //$$ RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
                    //#else
                    GL11.glEnable(GL11.GL_LINE_SMOOTH);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    //#endif
                    pushScissorState();
                    setScissorDisabled();
                    GL11.glLineWidth(2);
                    //#if MC>=12100
                    //$$ try (var builtBuffer = buffer.end()) {
                    //$$     net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(builtBuffer);
                    //$$ }
                    //#else
                    tessellator.draw();
                    //#endif
                    popScissorState();
                    //#if MC<11700
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glDisable(GL11.GL_LINE_SMOOTH);
                    //#endif
                }
            }
        });

        // Draw colored quads on spectator path segments
        for (PathSegment segment : timeline.getPositionPath().getSegments()) {
            if (segment.getInterpolator() == null
                    || !segment.getInterpolator().getKeyframeProperties().contains(SpectatorProperty.PROPERTY)) {
                continue; // Not a spectator segment
            }
            drawQuadOnSegment(renderer, visibleWidth, segment, BORDER_TOP + 1, 0xFF0088FF);
        }

        // Draw red quads on time path segments that would require time going backwards
        for (PathSegment segment : timeline.getTimePath().getSegments()) {
            long startTimestamp = segment.getStartKeyframe().getValue(TimestampProperty.PROPERTY).orElseThrow(IllegalStateException::new);
            long endTimestamp = segment.getEndKeyframe().getValue(TimestampProperty.PROPERTY).orElseThrow(IllegalStateException::new);
            if (endTimestamp >= startTimestamp) {
                continue; // All is fine, time is not moving backwards
            }
            drawQuadOnSegment(renderer, visibleWidth, segment, BORDER_TOP + KEYFRAME_SIZE + 1, 0xFFFF0000);
        }

        super.drawTimelineCursor(renderer, size);
    }

    private void drawQuadOnSegment(GuiRenderer renderer, int visibleWidth, PathSegment segment, int y, int color) {
        int startTime = getOffset();
        int visibleTime = (int) (getZoom() * getLength());
        int endTime = getOffset() + visibleTime;

        long startFrameTime = segment.getStartKeyframe().getTime();
        long endFrameTime = segment.getEndKeyframe().getTime();
        if (startFrameTime >= endTime || endFrameTime <= startTime) {
            return; // Segment out of display range
        }

        double relativeStart = startFrameTime - startTime;
        double relativeEnd = endFrameTime - startTime;
        int startX = BORDER_LEFT + Math.max(0, (int) (relativeStart / visibleTime * visibleWidth) + KEYFRAME_SIZE / 2 + 1);
        int endX = BORDER_LEFT + Math.min(visibleWidth, (int) (relativeEnd / visibleTime * visibleWidth) - KEYFRAME_SIZE / 2);
        if (startX < endX) {
            renderer.drawRect(startX + 1, y, endX - startX - 2, KEYFRAME_SIZE - 2, color);
        }
    }

    /**
     * Returns the keyframe at the specified position.
     * @param position The raw position
     * @return Pair of path id and keyframe or null when no keyframe was clicked
     */
    private Pair<SPPath, Long> getKeyframe(ReadablePoint position) {
        int time = getTimeAt(position.getX(), position.getY());
        if (time != -1) {
            Point mouse = new Point(position);
            getContainer().convertFor(this, mouse);
            int mouseY = mouse.getY();
            if (mouseY > BORDER_TOP && mouseY < BORDER_TOP + 2 * KEYFRAME_SIZE) {
                SPPath path;
                if (mouseY <= BORDER_TOP + KEYFRAME_SIZE) {
                    // Position keyframe
                    path = SPPath.POSITION;
                } else {
                    // Time keyframe
                    path = SPPath.TIME;
                }
                int visibleTime = (int) (getZoom() * getLength());
                int tolerance = visibleTime * KEYFRAME_SIZE / (getLastSize().getWidth() - BORDER_LEFT - BORDER_RIGHT) / 2;
                Optional<Keyframe> keyframe = gui.getMod().getCurrentTimeline().getPath(path).getKeyframes().stream()
                        .filter(k -> Math.abs(k.getTime() - time) <= tolerance)
                        .sorted(Comparator.comparing(k -> Math.abs(k.getTime() - time)))
                        .findFirst();
                return Pair.of(path, keyframe.map(Keyframe::getTime).orElse(null));
            }
        }
        return Pair.of(null, null);
    }

    @Override
    public boolean mouseClick(ReadablePoint position, int button) {
        int time = getTimeAt(position.getX(), position.getY());
        Pair<SPPath, Long> pathKeyframePair = getKeyframe(position);
        if (pathKeyframePair.getRight() != null) {
            SPPath path = pathKeyframePair.getLeft();
            // Clicked on keyframe
            long keyframeTime = pathKeyframePair.getRight();
            if (button == 0) { // Left click
                long now = MCVer.milliTime();
                if (lastClickedKeyframe == keyframeTime) {
                    // Clicked the same keyframe again, potentially a double click
                    if (now - lastClickedTime < DOUBLE_CLICK_INTERVAL) {
                        // Yup, double click, open the edit keyframe gui
                        gui.openEditKeyframePopup(path, keyframeTime);
                        return true;
                    }
                }
                // Not a double click, just update the click time and selection
                lastClickedTime = now;
                lastClickedKeyframe = keyframeTime;
                lastClickedPath = path;
                gui.getMod().setSelected(lastClickedPath, lastClickedKeyframe);
                // We might be dragging
                draggingStartX = position.getX();
                dragging = true;
            } else if (button == 1) { // Right click
                Keyframe keyframe = gui.getMod().getCurrentTimeline().getKeyframe(path, keyframeTime);
                for (Property property : keyframe.getProperties()) {
                    applyPropertyToGame(property, keyframe);
                }
            }
            return true;
        } else if (time != -1) {
            // Clicked on timeline but not on any keyframe
            if (button == 0) { // Left click
                setCursorPosition(time);
                gui.getMod().setSelected(null, 0);
            } else if (button == 1) { // Right click
                if (pathKeyframePair.getLeft() != null) {
                    // Apply the value of the clicked path at the clicked position
                    Path path = gui.getMod().getCurrentTimeline().getPath(pathKeyframePair.getLeft());
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
            if (!gui.loadEntityTracker(() -> mouseDrag(position, button, timeSinceLastCall))) return true;
            // Threshold passed
            SPTimeline timeline = gui.getMod().getCurrentTimeline();
            Point mouse = new Point(position);
            getContainer().convertFor(this, mouse);
            int mouseX = mouse.getX();
            int width = getLastSize().getWidth();
            int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;
            double segmentLength = getLength() * getZoom();
            double segmentTime =  segmentLength * (mouseX - BORDER_LEFT) / bodyWidth;
            int newTime = Math.min(Math.max((int) Math.round(getOffset() + segmentTime), 0), getLength());
            if (newTime < 0) {
                return true;
            }

            // If there already is a keyframe at the target time, then increase the time by one until there is none
            while (timeline.getKeyframe(lastClickedPath, newTime) != null) {
                newTime++;
            }

            // First undo any previous changes
            if (draggingChange != null) {
                draggingChange.undo(timeline.getTimeline());
            }

            // Move keyframe to new position and
            // store change for later undoing / pushing to history
            draggingChange = timeline.moveKeyframe(lastClickedPath, lastClickedKeyframe, newTime);

            // Selected keyframe has been replaced
            gui.getMod().setSelected(lastClickedPath, newTime);
        }
        return true;
    }

    @Override
    public boolean mouseRelease(ReadablePoint position, int button) {
        if (dragging) {
            if (actuallyDragging) {
                gui.getMod().getCurrentTimeline().getTimeline().pushChange(draggingChange);
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
