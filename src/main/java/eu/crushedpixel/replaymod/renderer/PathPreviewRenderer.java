package eu.crushedpixel.replaymod.renderer;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.events.KeyframesModifyEvent;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
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

    private DistanceComparator distanceComparator = new DistanceComparator();

    private KeyframeList<AdvancedPosition> keyframes;

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

        if(keyframes.size() > 1) {

            AdvancedPosition prev = null;

            if(ReplayMod.replaySettings.isLinearMovement()) {

                for(Keyframe<AdvancedPosition> point : keyframes) {
                    if(prev != null) {
                        drawConnection(doubleX, doubleY, doubleZ, prev, point.getValue(), Color.RED.getRGB());
                    }

                    prev = point.getValue();
                }

            } else {

                float max = keyframes.size() * 50;
                for(int i = 0; i < max; i++) {
                    AdvancedPosition point = keyframes.getInterpolatedValueForPathPosition(i/max, false);

                    if(prev != null) {
                        drawConnection(doubleX, doubleY, doubleZ, prev, point, Color.RED.getRGB());
                    }

                    prev = point;
                }
            }
        }

        distanceComparator.setPlayerPos(doubleX, doubleY + 1.4, doubleZ);

        List<Keyframe<AdvancedPosition>> distanceSorted = new ArrayList<Keyframe<AdvancedPosition>>(keyframes);
        Collections.sort(distanceSorted, distanceComparator);


        for(Keyframe<AdvancedPosition> kf : distanceSorted) {
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
        keyframes = event.getPositionKeyframes();
    }

    private class DistanceComparator implements Comparator<Keyframe<AdvancedPosition>> {

        private double playerX, playerY, playerZ;

        public void setPlayerPos(double playerX, double playerY, double playerZ) {
            this.playerX = playerX;
            this.playerY = playerY;
            this.playerZ = playerZ;
        }

        @Override
        public int compare(Keyframe<AdvancedPosition> o1, Keyframe<AdvancedPosition> o2) {
            return -(new Double(o1.getValue().distanceSquared(playerX, playerY, playerZ)).compareTo(o2.getValue().distanceSquared(playerX, playerY, playerZ)));
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }
    }

    private void drawConnection(double playerX, double playerY, double playerZ, AdvancedPosition pos1, AdvancedPosition pos2, int color) {
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

    private void drawPoint(double playerX, double playerY, double playerZ, Keyframe<AdvancedPosition> kf) {
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();

        GlStateManager.enableBlend();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        mc.renderEngine.bindTexture(GuiReplayOverlay.replay_gui);

        AdvancedPosition pos1 = kf.getValue();

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

        if(pos1.getSpectatedEntityID() != null) {
            posX += size;
        }

        float minX = -0.5f;
        float minY = -0.5f;
        float maxX = 0.5f;
        float maxY = 0.5f;

        renderer.addVertexWithUV(minX, minY, 0, posX + size, posY + size);
        renderer.addVertexWithUV(minX, maxY, 0, posX + size, posY);
        renderer.addVertexWithUV(maxX, maxY, 0, posX, posY);
        renderer.addVertexWithUV(maxX, minY, 0, posX, posY + size);

        tessellator.draw();
        renderer.setTranslation(0, 0, 0);

        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
