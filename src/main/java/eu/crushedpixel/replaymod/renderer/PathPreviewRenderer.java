package eu.crushedpixel.replaymod.renderer;

import eu.crushedpixel.replaymod.events.KeyframesModifyEvent;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.SpectatorData;
import eu.crushedpixel.replaymod.interpolation.AdvancedPositionKeyframeList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Comparator;

import static com.replaymod.core.ReplayMod.TEXTURE;

public class PathPreviewRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int DEFAULT_PATH_COLOR = Color.RED.getRGB();
    private static final int SPECTATOR_PATH_COLOR = Color.BLUE.getRGB();

    private DistanceComparator distanceComparator = new DistanceComparator();

    private AdvancedPositionKeyframeList keyframes;

    private final ResourceLocation cameraHeadResource = new ResourceLocation("replaymod", "camera_head.png");

    @SubscribeEvent
    public void renderCameraPath(RenderWorldLastEvent event) {
        // TODO
//        if(!ReplayHandler.isInReplay() || ReplayHandler.isInPath() || !ReplayMod.replaySettings.showPathPreview() || mc.gameSettings.hideGUI) return;
//
//        GlStateManager.pushAttrib();
//
//        int renderDistanceSquared = mc.gameSettings.renderDistanceChunks*16 * mc.gameSettings.renderDistanceChunks*16;
//
//        Entity entity = ReplayHandler.getCameraEntity();
//        if(entity == null) return;
//
//        double doubleX = entity.posX;
//        double doubleY = entity.posY;
//        double doubleZ = entity.posZ;
//
//        GlStateManager.disableLighting();
//        GlStateManager.disableTexture2D();
//        GlStateManager.enableBlend();
//
//        if(keyframes.size() > 1) {
//            AdvancedPosition previousPosition = null;
//
//            int i = 0;
//            for(Keyframe<AdvancedPosition> property : keyframes) {
//                int timestamp = property.getRealTimestamp();
//                int nextTimestamp = timestamp;
//
//                if(i+1 < keyframes.size()) {
//                    nextTimestamp = keyframes.get(i+1).getRealTimestamp();
//                }
//
//                int diff = nextTimestamp-timestamp;
//                int step = diff/100;
//
//                for(int currentTimestamp = timestamp; currentTimestamp < nextTimestamp; currentTimestamp += step) {
//                    AdvancedPosition position = keyframes.getInterpolatedValueForTimestamp(currentTimestamp, ReplayMod.replaySettings.isLinearMovement());
//                    if(previousPosition != null) {
//                        drawConnection(doubleX, doubleY, doubleZ, previousPosition, position, DEFAULT_PATH_COLOR, renderDistanceSquared);
//                    }
//                    previousPosition = position;
//                }
//
//                i++;
//            }
//        }
//
//        distanceComparator.setPlayerPos(doubleX, doubleY + 1.4, doubleZ);
//
//        List<Keyframe<AdvancedPosition>> distanceSorted = new ArrayList<Keyframe<AdvancedPosition>>(keyframes);
//        Collections.sort(distanceSorted, distanceComparator);
//
//        GlStateManager.enableTexture2D();
//        GlStateManager.blendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);
//
//        GlStateManager.disableDepth();
//
//        for(Keyframe<AdvancedPosition> kf : distanceSorted) {
//            if(kf.getValue().distanceSquared(entity.posX, entity.posY, entity.posZ) < renderDistanceSquared) {
//                drawPoint(doubleX, doubleY, doubleZ, kf);
//            }
//        }
//
//        if(ReplayHandler.getPositionKeyframes().size() > 1) {
//            AdvancedPosition cameraPosition = ReplayHandler.getPositionKeyframes().getInterpolatedValueForTimestamp(ReplayHandler.getRealTimelineCursor(),
//                    ReplayMod.replaySettings.isLinearMovement());
//            if(cameraPosition != null) {
//                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//                drawCamera(doubleX, doubleY, doubleZ, cameraPosition);
//            }
//        }
//
//        GlStateManager.disableBlend();
//
//        GlStateManager.popAttrib();
    }

    @SubscribeEvent
    public void recalcSpline(KeyframesModifyEvent event) {
        // TODO
//        keyframes = ReplayHandler.getPositionKeyframes();
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

    private void drawConnection(double playerX, double playerY, double playerZ, AdvancedPosition pos1, AdvancedPosition pos2, int color, int renderDistanceSquared) {
        if(pos2.distanceSquared(playerX, playerY, playerZ) > renderDistanceSquared) return;
        GlStateManager.pushMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        GL11.glLineWidth(3.0F);

        double x = pos1.getX() - playerX;
        double y = pos1.getY() - playerY;
        double z = pos1.getZ() - playerZ;

        renderer.setTranslation(x, y+1.4, z);

        renderer.startDrawing(1);

        renderer.setColorRGBA_I(color, 255);

        renderer.addVertex(pos2.getX() - pos1.getX(), pos2.getY() - pos1.getY(), pos2.getZ() - pos1.getZ());
        renderer.addVertex(0, 0, 0);

        tessellator.draw();
        renderer.setTranslation(0, 0, 0);

        GlStateManager.popMatrix();
    }

    private void drawPoint(double playerX, double playerY, double playerZ, Keyframe<AdvancedPosition> kf) {
        GlStateManager.pushMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        mc.renderEngine.bindTexture(TEXTURE);

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

        renderer.startDrawingQuads();

        float posX = 80/128f;
        float posY = 0;
        float size = 10/128f;


        // TODO
//        if(kf.equals(ReplayHandler.getSelectedKeyframe())) {
//            posY += size;
//        }

        if(pos1 instanceof SpectatorData) {
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

        GlStateManager.popMatrix();
    }

    private void drawCamera(double playerX, double playerY, double playerZ, AdvancedPosition pos) {
        GlStateManager.pushMatrix();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        mc.renderEngine.bindTexture(cameraHeadResource);

        double x = pos.getX() - playerX;
        double y = pos.getY() - playerY;
        double z = pos.getZ() - playerZ;

        GlStateManager.translate(x, y + 1.4, z);

        GL11.glNormal3f(0, 1, 0);

        //draw the position line
        AdvancedPosition pos2 = pos.getDestination(2);

        GlStateManager.enableDepth();
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        renderer.startDrawing(1);

        renderer.setColorRGBA_I(Color.GREEN.getRGB(), 170);

        renderer.addVertex(pos2.getX() - pos.getX(), pos2.getY() - pos.getY(), pos2.getZ() - pos.getZ());
        renderer.addVertex(0, 0, 0);

        Tessellator.getInstance().draw();

        GlStateManager.enableTexture2D();

        GlStateManager.rotate((float) -pos.getYaw(), 0, 1, 0);
        GlStateManager.rotate((float) pos.getPitch(), 1, 0, 0);
        GlStateManager.rotate((float) pos.getRoll(), 0, 0, 1);

        float cubeSize = 0.5f;

        x = y = z = -cubeSize/2;

        renderer.startDrawingQuads();

        renderer.setColorRGBA_I(Color.WHITE.getRGB(), 200);

        //back
        renderer.addVertexWithUV(x, y + cubeSize, z, 3 * 8 / 64f, 8 / 64f);
        renderer.addVertexWithUV(x + cubeSize, y + cubeSize, z, 4*8/64f, 8/64f);
        renderer.addVertexWithUV(x + cubeSize, y, z, 4*8/64f, 2*8/64f);
        renderer.addVertexWithUV(x, y, z, 3*8/64f, 2*8/64f);

        //front
        renderer.addVertexWithUV(x + cubeSize, y, z + cubeSize, 2 * 8 / 64f, 2*8/64f);
        renderer.addVertexWithUV(x + cubeSize, y + cubeSize, z + cubeSize, 2 * 8 / 64f, 8/64f);
        renderer.addVertexWithUV(x, y + cubeSize, z + cubeSize, 8 / 64f, 8 / 64f);
        renderer.addVertexWithUV(x, y, z + cubeSize, 8 / 64f, 2*8/64f);

        //left
        renderer.addVertexWithUV(x + cubeSize, y + cubeSize, z, 0, 8/64f);
        renderer.addVertexWithUV(x + cubeSize, y + cubeSize, z + cubeSize, 8/64f, 8/64f);
        renderer.addVertexWithUV(x + cubeSize, y, z + cubeSize, 8/64f, 2*8/64f);
        renderer.addVertexWithUV(x+cubeSize,y,z, 0, 2*8/64f);

        //right
        renderer.addVertexWithUV(x, y + cubeSize, z + cubeSize, 2*8/64f, 8/64f);
        renderer.addVertexWithUV(x, y + cubeSize, z, 3*8/64f, 8/64f);
        renderer.addVertexWithUV(x, y, z, 3*8/64f, 2*8/64f);
        renderer.addVertexWithUV(x, y, z + cubeSize, 2 * 8 / 64f, 2 * 8 / 64f);

        //bottom
        renderer.addVertexWithUV(x + cubeSize, y, z, 3*8/64f, 0);
        renderer.addVertexWithUV(x + cubeSize, y, z + cubeSize, 3*8/64f, 8/64f);
        renderer.addVertexWithUV(x, y, z + cubeSize, 2*8/64f, 8/64f);
        renderer.addVertexWithUV(x, y, z, 2 * 8 / 64f, 0);

        //top
        renderer.addVertexWithUV(x, y + cubeSize, z, 8/64f, 0);
        renderer.addVertexWithUV(x, y + cubeSize, z + cubeSize, 8/64f, 8/64f);
        renderer.addVertexWithUV(x + cubeSize, y + cubeSize, z + cubeSize, 2*8/64f, 8/64f);
        renderer.addVertexWithUV(x + cubeSize, y + cubeSize, z, 2 * 8 / 64f, 0);

        Tessellator.getInstance().draw();
        renderer.setTranslation(0, 0, 0);

        GlStateManager.popMatrix();
    }
}
