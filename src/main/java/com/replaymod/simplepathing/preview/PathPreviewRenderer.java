package com.replaymod.simplepathing.preview;

import com.mojang.blaze3d.platform.GlStateManager;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.events.PostRenderWorldCallback;
import com.replaymod.core.versions.MCVer;
import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.SpectatorProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.replaystudio.util.EntityPositionTracker;
import com.replaymod.replaystudio.util.Location;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import com.replaymod.simplepathing.SPTimeline;
import com.replaymod.simplepathing.gui.GuiPathing;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.opengl.GL11;

//#if MC>=12105
//$$ import net.minecraft.client.render.RenderLayer;
//$$ import net.minecraft.client.render.VertexConsumer;
//$$ import net.minecraft.client.render.VertexConsumerProvider;
//#else
import static com.replaymod.core.versions.MCVer.bindTexture;
//#endif

//#if MC>=12102
//#if MC<12105
//$$ import net.minecraft.client.gl.ShaderProgramKeys;
//#endif
//#endif

//#if MC>=11700
//$$ import net.minecraft.client.render.GameRenderer;
//#endif

//#if MC>=11500
import com.mojang.blaze3d.systems.RenderSystem;
//#endif

import java.util.Comparator;
import java.util.Optional;

import static com.replaymod.core.ReplayMod.TEXTURE;
import static com.replaymod.core.versions.MCVer.emitLine;
import static com.replaymod.core.versions.MCVer.popMatrix;
import static com.replaymod.core.versions.MCVer.pushMatrix;
import static de.johni0702.minecraft.gui.versions.MCVer.identifier;

public class PathPreviewRenderer extends EventRegistrations {
    private static final Identifier CAMERA_HEAD = identifier("replaymod", "camera_head.png");
    private static final MinecraftClient mc = MCVer.getMinecraft();

    private static final int SLOW_PATH_COLOR = 0xffcccc;
    private static final int FAST_PATH_COLOR = 0x660000;
    private static final double FASTEST_PATH_SPEED = 0.01;

    private final ReplayModSimplePathing mod;
    private final ReplayHandler replayHandler;

    public PathPreviewRenderer(ReplayModSimplePathing mod, ReplayHandler replayHandler) {
        this.mod = mod;
        this.replayHandler = replayHandler;
    }

    { on(PostRenderWorldCallback.EVENT, this::renderCameraPath); }
    private void renderCameraPath(MatrixStack matrixStack) {
        if (!replayHandler.getReplaySender().isAsyncMode() || mc.options.hudHidden) return;

        Entity view = mc.getCameraEntity();
        if (view == null) return;

        GuiPathing guiPathing = mod.getGuiPathing();
        if (guiPathing == null) return;
        EntityPositionTracker entityTracker = guiPathing.getEntityTracker();

        SPTimeline timeline = mod.getCurrentTimeline();
        if (timeline == null) return;
        Path path = timeline.getPositionPath();
        if (path.getKeyframes().isEmpty()) return;
        Path timePath = timeline.getTimePath();

        path.update();

        int renderDistance = mc.options.viewDistance * 16;
        int renderDistanceSquared = renderDistance * renderDistance;

        Vector3f viewPos = new Vector3f(
                (float) view.getX(),
                (float) view.getY()
                //#if MC>=10800 && MC<11500
                //$$ // Eye height is subtracted to make path appear higher (at eye height) than it actually is (at foot height)
                //$$ - view.getStandingEyeHeight(),
                //#else
                ,
                //#endif
                (float) view.getZ()
        );

        //#if MC<11700
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        //#endif
        pushMatrix();
        try {
            //#if MC<11700
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            //#endif

            //#if MC>=11700
            //#if MC>=12006
            //$$ RenderSystem.getModelViewStack().mul(matrixStack.peek().getPositionMatrix());
            //#else
            //$$ RenderSystem.getModelViewStack().method_34425(matrixStack.peek().getModel());
            //#endif
            //#if MC<12102
            //$$ RenderSystem.applyModelViewMatrix();
            //#endif
            //#elseif MC>=11500
            RenderSystem.multMatrix(matrixStack.peek().getModel());
            //#endif

            for (PathSegment segment : path.getSegments()) {
                Interpolator interpolator = segment.getInterpolator();
                Keyframe start = segment.getStartKeyframe();
                Keyframe end = segment.getEndKeyframe();
                long diff = (int) (end.getTime() - start.getTime());

                boolean spectator = interpolator.getKeyframeProperties().contains(SpectatorProperty.PROPERTY);
                if (spectator && entityTracker == null) {
                    continue; // Cannot render spectator positions when entity tracker is not yet loaded
                }
                // Spectator segments have 20 lines per second (at least 10) whereas normal segments have a fixed 100
                long steps = spectator ? Math.max(diff / 50, 10) : 100;
                Vector3f prevPos = null;
                for (int i = 0; i <= steps; i++) {
                    long time = start.getTime() + diff * i / steps;
                    if (spectator) {
                        Optional<Integer> entityId = path.getValue(SpectatorProperty.PROPERTY, time);
                        Optional<Integer> replayTime = timePath.getValue(TimestampProperty.PROPERTY, time);
                        if (entityId.isPresent() && replayTime.isPresent()) {
                            Location loc = entityTracker.getEntityPositionAtTimestamp(entityId.get(), replayTime.get());
                            if (loc != null) {
                                Vector3f pos = loc2Vec(loc);
                                if (prevPos != null) {
                                    drawConnection(viewPos, prevPos, pos, 0x0000ffff, renderDistanceSquared);
                                }
                                prevPos = pos;
                                continue;
                            }
                        }
                    } else {
                        Optional<Vector3f> optPos = path.getValue(CameraProperties.POSITION, time).map(this::tripleD2Vec);
                        if (optPos.isPresent()) {
                            Vector3f pos = optPos.get();
                            if (prevPos != null) {
                                double distance = Math.sqrt(distanceSquared(prevPos, pos));
                                double speed = Math.min(distance / (diff / steps), FASTEST_PATH_SPEED);
                                double speedFraction = speed / FASTEST_PATH_SPEED;
                                int color = interpolateColor(SLOW_PATH_COLOR, FAST_PATH_COLOR, speedFraction);
                                drawConnection(viewPos, prevPos, pos, (color << 8) | 0xff, renderDistanceSquared);
                            }
                            prevPos = pos;
                            continue;
                        }
                    }
                    prevPos = null;
                }
            }

            //#if MC<12105
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            //#endif
            //#if MC<11700
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            //#endif

            path.getKeyframes().stream()
                    .map(k -> Pair.of(k, k.getValue(CameraProperties.POSITION).map(this::tripleD2Vec)))
                    .filter(p -> p.getRight().isPresent())
                    .map(p -> Pair.of(p.getLeft(), p.getRight().get()))
                    .filter(p -> distanceSquared(p.getRight(), viewPos) < renderDistanceSquared)
                    .sorted(new KeyframeComparator(viewPos)) // Need to render the furthest first
                    .forEachOrdered(p -> drawPoint(viewPos, p.getRight(), p.getLeft()));

            //#if MC<12105
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            //#endif

            int time = guiPathing.timeline.getCursorPosition();
            Optional<Integer> entityId = path.getValue(SpectatorProperty.PROPERTY, time);
            if (entityId.isPresent()) {
                // Spectating an entity
                if (entityTracker != null) {
                    Optional<Integer> replayTime = timePath.getValue(TimestampProperty.PROPERTY, time);
                    if (replayTime.isPresent()) {
                        Location loc = entityTracker.getEntityPositionAtTimestamp(entityId.get(), replayTime.get());
                        if (loc != null) {
                            drawCamera(viewPos, loc2Vec(loc), new Vector3f(loc.getYaw(), loc.getPitch(), 0f));
                        }
                    }
                }
            } else {
                // Normal camera path
                Optional<Vector3f> cameraPos = path.getValue(CameraProperties.POSITION, time).map(this::tripleD2Vec);
                Optional<Vector3f> cameraRot = path.getValue(CameraProperties.ROTATION, time).map(this::tripleF2Vec);
                if (cameraPos.isPresent() && cameraRot.isPresent()) {
                    drawCamera(viewPos, cameraPos.get(), cameraRot.get());
                }
            }
        } finally {
            popMatrix();
            //#if MC<12105
            //#if MC>=11700
            //$$ GL11.glDisable(GL11.GL_BLEND);
            //#else
            GL11.glPopAttrib();
            //#endif
            //#endif
        }
    }

    private Vector3f loc2Vec(Location loc) {
        return new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ());
    }

    private Vector3f tripleD2Vec(Triple<Double, Double, Double> loc) {
        return new Vector3f(loc.getLeft().floatValue(), loc.getMiddle().floatValue(), loc.getRight().floatValue());
    }

    private Vector3f tripleF2Vec(Triple<Float, Float, Float> loc) {
        return new Vector3f(loc.getLeft(), loc.getMiddle(), loc.getRight());
    }

    private static int interpolateColor(int c1, int c2, double weight) {
        return (interpolateColorComponent((c1 >> 16) & 0xff, (c2 >> 16) & 0xff, weight) << 16)
                | (interpolateColorComponent((c1 >> 8) & 0xff, (c2 >> 8) & 0xff, weight) << 8)
                | interpolateColorComponent(c1 & 0xff, c2 & 0xff, weight);
    }

    private static int interpolateColorComponent(int c1, int c2, double weight) {
        return (int) (c1 + (1 - Math.pow(Math.E, -4 * weight)) * (c2 - c1)) & 0xff;
    }

    private static double distanceSquared(Vector3f p1, Vector3f p2) {
        return Vector3f.sub(p1, p2, null).lengthSquared();
    }

    private void drawConnection(Vector3f view, Vector3f pos1, Vector3f pos2, int color, int renderDistanceSquared) {
        if (distanceSquared(view, pos1) > renderDistanceSquared) return;
        if (distanceSquared(view, pos2) > renderDistanceSquared) return;

        //#if MC>=12105
        //$$ VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        //$$ immediate.draw();
        //$$ VertexConsumer buffer = immediate.getBuffer(RenderLayer.LINES);
        //#else
        Tessellator tessellator = Tessellator.getInstance();
        //#if MC>=12100
        //$$ BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        //#else
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        //#endif
        //#endif

        emitLine(new MatrixStack(), buffer, Vector3f.sub(pos1, view, null), Vector3f.sub(pos2, view, null), color);

        GL11.glLineWidth(3);

        //#if MC>=12105
        //$$ immediate.draw();
        //#else
        //#if MC>=11700
        //#if MC>=12102
        //$$ RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        //#else
        //$$ RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
        //#endif
        //$$ RenderSystem.disableCull();
        //#endif
        //#if MC>=12100
        //$$ try (var builtBuffer = buffer.end()) {
        //$$     net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(builtBuffer);
        //$$ }
        //#else
        tessellator.draw();
        //#endif
        //#if MC>=11700
        //$$ RenderSystem.enableCull();
        //#endif
        //#endif
    }

    private void drawPoint(Vector3f view, Vector3f pos, Keyframe keyframe) {

        //#if MC<12105
        bindTexture(TEXTURE);
        //#endif

        float posX = 80f / ReplayMod.TEXTURE_SIZE;
        float posY = 0f;
        float size = 10f / ReplayMod.TEXTURE_SIZE;

        if (mod.isSelected(keyframe)) {
            posY += size;
        }

        if (keyframe.getValue(SpectatorProperty.PROPERTY).isPresent()) {
            posX += size;
        }

        float minX = -0.5f;
        float minY = -0.5f;
        float maxX = 0.5f;
        float maxY = 0.5f;

        //#if MC>=12105
        //$$ VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        //$$ immediate.draw();
        //#if MC>=12106
        //$$ VertexConsumer buffer = immediate.getBuffer(RenderLayer.getTextSeeThrough(TEXTURE));
        //#else
        //$$ VertexConsumer buffer = immediate.getBuffer(RenderLayer.getGuiTexturedOverlay(TEXTURE));
        //#endif
        //#else
        Tessellator tessellator = Tessellator.getInstance();
        //#if MC>=12100
        //$$ BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        //#else
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        //#endif
        //#endif

        vertex(buffer, minX, minY, 0, posX + size, posY + size, 255);
        vertex(buffer, minX, maxY, 0, posX + size, posY, 255);
        vertex(buffer, maxX, maxY, 0, posX, posY, 255);
        vertex(buffer, maxX, minY, 0, posX, posY + size, 255);

        pushMatrix();

        Vector3f t = Vector3f.sub(pos, view, null);
        GL11.glTranslatef(t.x, t.y, t.z);
        GL11.glRotatef(-mc.getEntityRenderDispatcher().camera.getYaw(), 0, 1, 0);
        GL11.glRotatef(mc.getEntityRenderDispatcher().camera.getPitch(), 1, 0, 0);

        //#if MC>=12105
        //$$ immediate.draw();
        //#else
        //#if MC>=12102
        //$$ RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        //#elseif MC>=11700
        //$$ RenderSystem.applyModelViewMatrix();
        //$$ RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        //#endif
        //#if MC>=12100
        //$$ try (var builtBuffer = buffer.end()) {
        //$$     net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(builtBuffer);
        //$$ }
        //#else
        tessellator.draw();
        //#endif
        //#endif

        popMatrix();
    }

    private void drawCamera(Vector3f view, Vector3f pos, Vector3f rot) {

        //#if MC<12105
        bindTexture(CAMERA_HEAD);
        //#endif

        pushMatrix();

        Vector3f t = Vector3f.sub(pos, view, null);
        GL11.glTranslatef(t.x, t.y, t.z);
        GL11.glRotatef(-rot.x, 0, 1, 0); // Yaw
        GL11.glRotatef(rot.y, 1, 0, 0); // Pitch
        GL11.glRotatef(rot.z, 0, 0, 1); // Roll

        //draw the position line
        //#if MC>=12105
        //$$ VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        //$$ immediate.draw();
        //$$ VertexConsumer buffer = immediate.getBuffer(RenderLayer.LINES);
        //#else
        Tessellator tessellator = Tessellator.getInstance();
        //#if MC>=12100
        //$$ BufferBuilder buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.LINES, VertexFormats.LINES);
        //#else
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        //#endif
        //#endif

        emitLine(new MatrixStack(), buffer, new Vector3f(0, 0, 0), new Vector3f(0, 0, 2), 0x00ff00aa);

        //#if MC>=12105
        //$$ immediate.draw();
        //#else
        //#if MC>=12102
        //$$ RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
        //#elseif MC>=11700
        //$$ RenderSystem.applyModelViewMatrix();
        //$$ RenderSystem.setShader(GameRenderer::getRenderTypeLinesShader);
        //#else
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        //#endif

        //#if MC>=12100
        //$$ try (var builtBuffer = buffer.end()) {
        //$$     net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(builtBuffer);
        //$$ }
        //#else
        tessellator.draw();
        //#endif

        //#if MC<11700
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        //#endif
        //#endif

        // draw camera cube

        float cubeSize = 0.5f;

        float r = -cubeSize/2;

        //#if MC>=12106
        //$$ buffer = immediate.getBuffer(RenderLayer.getText(CAMERA_HEAD));
        //#elseif MC>=12105
        //$$ buffer = immediate.getBuffer(RenderLayer.getGuiTextured(CAMERA_HEAD));
        //#elseif MC>=12100
        //$$ buffer = tessellator.begin(net.minecraft.client.render.VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        //#else
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        //#endif

        //back
        vertex(buffer, r, r + cubeSize, r, 3 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r, 4*8/64f, 8/64f, 200);
        vertex(buffer, r + cubeSize, r, r, 4*8/64f, 2*8/64f, 200);
        vertex(buffer, r, r, r, 3*8/64f, 2*8/64f, 200);

        //front
        vertex(buffer, r + cubeSize, r, r + cubeSize, 2 * 8 / 64f, 2*8/64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r + cubeSize, 2 * 8 / 64f, 8/64f, 200);
        vertex(buffer, r, r + cubeSize, r + cubeSize, 8 / 64f, 8 / 64f, 200);
        vertex(buffer, r, r, r + cubeSize, 8 / 64f, 2*8/64f, 200);

        //left
        vertex(buffer, r + cubeSize, r + cubeSize, r, 0, 8/64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r + cubeSize, 8/64f, 8/64f, 200);
        vertex(buffer, r + cubeSize, r, r + cubeSize, 8/64f, 2*8/64f, 200);
        vertex(buffer, r+cubeSize, r, r, 0, 2*8/64f, 200);

        //right
        vertex(buffer, r, r + cubeSize, r + cubeSize, 2*8/64f, 8/64f, 200);
        vertex(buffer, r, r + cubeSize, r, 3*8/64f, 8/64f, 200);
        vertex(buffer, r, r, r, 3*8/64f, 2*8/64f, 200);
        vertex(buffer, r, r, r + cubeSize, 2 * 8 / 64f, 2 * 8 / 64f, 200);

        //bottom
        vertex(buffer, r + cubeSize, r, r, 3*8/64f, 0, 200);
        vertex(buffer, r + cubeSize, r, r + cubeSize, 3*8/64f, 8/64f, 200);
        vertex(buffer, r, r, r + cubeSize, 2*8/64f, 8/64f, 200);
        vertex(buffer, r, r, r, 2 * 8 / 64f, 0, 200);

        //top
        vertex(buffer, r, r + cubeSize, r, 8/64f, 0, 200);
        vertex(buffer, r, r + cubeSize, r + cubeSize, 8/64f, 8/64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r + cubeSize, 2*8/64f, 8/64f, 200);
        vertex(buffer, r + cubeSize, r + cubeSize, r, 2 * 8 / 64f, 0, 200);

        //#if MC>=12105
        //$$ immediate.draw();
        //#else
        //#if MC>=12102
        //$$ RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        //#elseif MC>=11700
        //$$ RenderSystem.applyModelViewMatrix();
        //$$ RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        //#endif
        //#if MC>=12100
        //$$ try (var builtBuffer = buffer.end()) {
        //$$     net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(builtBuffer);
        //$$ }
        //#else
        tessellator.draw();
        //#endif
        //#endif

        popMatrix();
    }

    //#if MC>=12105
    //$$ private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v, int alpha) {
    //#else
    private void vertex(BufferBuilder buffer, float x, float y, float z, float u, float v, int alpha) {
    //#endif
        //#if MC>=12106
        //$$ buffer.vertex(x, y, z).color(255, 255, 255, alpha).texture(u, v).light(240, 240);
        //#else
        buffer.vertex(x, y, z).texture(u, v).color(255, 255, 255, alpha).next();
        //#endif
    }

    private class KeyframeComparator implements Comparator<Pair<Keyframe, Vector3f>> {
        private final Vector3f viewPos;

        public KeyframeComparator(Vector3f viewPos) {
            this.viewPos = viewPos;
        }

        @Override
        public int compare(Pair<Keyframe, Vector3f> o1, Pair<Keyframe, Vector3f> o2) {
            return -Double.compare(distanceSquared(o1.getRight(), viewPos), distanceSquared(o2.getRight(), viewPos));
        }
    }
}
