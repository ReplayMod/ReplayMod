package eu.crushedpixel.replaymod.video.entity;

import eu.crushedpixel.replaymod.video.frame.TilingFrameRenderer;

import static net.minecraft.client.renderer.GlStateManager.scale;
import static net.minecraft.client.renderer.GlStateManager.translate;
import static org.lwjgl.opengl.GL11.glFrustum;

public class TilingEntityRenderer extends CustomEntityRenderer {
    private final TilingFrameRenderer renderer;
    private int tileX, tileY;

    public TilingEntityRenderer(TilingFrameRenderer renderer) {
        this.renderer = renderer;
    }

    public void setTile(int tileX, int tileY) {
        this.tileX = tileX;
        this.tileY = tileY;
    }

    @Override
    public void gluPerspective(float fovY, float aspect, float zNear, float zFar) {
        double height = Math.tan(fovY / 360 * Math.PI) * zNear;
        double width = height * aspect;

        int videoWidth = renderer.getVideoWidth();
        int videoHeight = renderer.getVideoHeight();
        int tileWidth = renderer.getTileWidth();
        int tileHeight = renderer.getTileHeight();
        double tilesX = (double) videoWidth / tileWidth;
        double tilesY = (double) videoHeight / tileHeight;
        double tilesMin = Math.min(tilesX, tilesY);
        scale(tilesMin, tilesMin, 1);
        double xOffset = (2 * tileX - tilesX + 1) / tilesMin;
        double yOffset = (2 * tileY - tilesY + 1) / tilesMin;
        translate(-xOffset, yOffset, 0);

        glFrustum(-width, width, -height, height, zNear, zFar);
    }
}
