package eu.crushedpixel.replaymod.renderer;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.events.KeyframesModifyEvent;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import eu.crushedpixel.replaymod.interpolation.SplinePoint;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PathPreviewRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private SplinePoint spline = new SplinePoint();

    private DistanceComparator distanceComparator = new DistanceComparator();

    private List<PositionKeyframe> keyframes = new ArrayList<PositionKeyframe>();

    @SubscribeEvent
    public void renderCameraPath(RenderWorldLastEvent event) {
        if(!ReplayHandler.isInReplay() || ReplayHandler.isInPath() || !ReplayMod.replaySettings.showPathPreview() || mc.gameSettings.hideGUI) return;

        Entity entity = ReplayHandler.getCameraEntity();
        if(entity == null) return;

        double doubleX = entity.posX;
        double doubleY = entity.posY;
        double doubleZ = entity.posZ;

        GlStateManager.pushAttrib();
        GlStateManager.pushMatrix();

        GlStateManager.disableLighting();
        GlStateManager.enableBlend();

        GlStateManager.disableTexture2D();

        if(spline.getPoints().size() > 1) {

            Position prev = null;

            if(ReplayMod.replaySettings.isLinearMovement()) {

                for(int i = 0; i < spline.getPoints().size(); i++) {
                    Position point = spline.getPoints().get(i);

                    if(prev != null) {
                        drawConnection(doubleX, doubleY, doubleZ, prev, point, Color.RED.getRGB());
                    }

                    prev = point;
                }

            } else {

                float max = spline.getPoints().size() * 50;
                for(int i = 0; i < max; i++) {
                    Position point = spline.getPoint(i / max);

                    if(prev != null) {
                        drawConnection(doubleX, doubleY, doubleZ, prev, point, Color.RED.getRGB());
                    }

                    prev = point;
                }
            }
        }


        distanceComparator.setPlayerPos(doubleX, doubleY + 1.4, doubleZ);

        Collections.sort(keyframes, distanceComparator);

        for(PositionKeyframe kf : keyframes) {
            drawPoint(doubleX, doubleY, doubleZ, kf);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();

        GlStateManager.disableBlend();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    @SubscribeEvent
    public void recalcSpline(KeyframesModifyEvent event) {
        keyframes = new ArrayList<PositionKeyframe>();
        spline = new SplinePoint();
        for(Keyframe kf : event.keyframes) {
            if(kf instanceof PositionKeyframe) {
                PositionKeyframe pkf = (PositionKeyframe)kf;
                Position pos = pkf.getPosition();
                spline.addPoint(pos);
                keyframes.add(pkf);
            }
        }

        if(spline.getPoints().size() > 1) {
            spline.prepare();
        }
    }

    private class DistanceComparator implements Comparator<PositionKeyframe> {

        private double playerX, playerY, playerZ;

        public void setPlayerPos(double playerX, double playerY, double playerZ) {
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerZ = playerZ;
        }

        @Override
        public int compare(PositionKeyframe o1, PositionKeyframe o2) {
            return -(new Double(o1.getPosition().distanceSquared(playerX, playerY, playerZ)).compareTo(o2.getPosition().distanceSquared(playerX, playerY, playerZ)));
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    private void drawConnection(double playerX, double playerY, double playerZ, Position pos1, Position pos2, int color) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        GL11.glLineWidth(3.0F);

        double x = pos1.getX() - playerX;
        double y = pos1.getY() - playerY;
        double z = pos1.getZ() - playerZ;

        renderer.setTranslation(x, y+1.4, z);

        renderer.startDrawing(1);

        renderer.setColorRGBA_I(color, 50);

        renderer.addVertex(pos2.getX() - pos1.getX(), pos2.getY() - pos1.getY(), pos2.getZ() - pos1.getZ());
        renderer.addVertex(0, 0, 0);

        tessellator.draw();
        renderer.setTranslation(0, 0, 0);
    }

    private void drawPoint(double playerX, double playerY, double playerZ, PositionKeyframe kf) {
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();

        GlStateManager.enableBlend();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        mc.renderEngine.bindTexture(GuiReplayOverlay.replay_gui);

        Position pos1 = kf.getPosition();

        double x = pos1.getX() - playerX;
        double y = pos1.getY() - playerY;
        double z = pos1.getZ() - playerZ;

        GlStateManager.translate(x, y+1.4, z);

        float pitch = mc.getRenderManager().playerViewX;
        float yaw = mc.getRenderManager().playerViewY;

        GL11.glNormal3f(0, 1, 0);

        GlStateManager.rotate(-yaw, 0, 1, 0);
        GlStateManager.rotate(pitch, 1, 0, 0);

        renderer.setColorRGBA_F(1, 1, 1, 0.5f);

        renderer.startDrawingQuads();

        float posX = 80/128f;
        float posY = 0;
        float size = 10/128f;


        if(kf.equals(ReplayHandler.getSelectedKeyframe())) {
            posY += size;
        }

        if(kf.getSpectatedEntityID() != null) {
            posX += size;
        }

        renderer.addVertexWithUV(-0.5, 0.5, 0, posX+size, posY);
        renderer.addVertexWithUV(0.5, 0.5, 0, posX+size, posY+size);
        renderer.addVertexWithUV(-0.5, -0.5, 0, posX, posY);
        renderer.addVertexWithUV(0.5, -0.5, 0, posX, posY+size);

        renderer.addVertexWithUV(0.5, -0.5, 0, posX+size, posY);
        renderer.addVertexWithUV(-0.5, -0.5, 0, posX, posY);
        renderer.addVertexWithUV(0.5, 0.5, 0, posX+size, posY+size);
        renderer.addVertexWithUV(-0.5, 0.5, 0, posX, posY + size);

        tessellator.draw();
        renderer.setTranslation(0, 0, 0);

        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
