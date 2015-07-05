package eu.crushedpixel.replaymod.renderer;

import eu.crushedpixel.replaymod.holders.CustomImageObject;
import eu.crushedpixel.replaymod.holders.ExtendedPosition;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Allows users to render custom images in the World.
 */
public class CustomObjectRenderer {

    private static final Minecraft mc = Minecraft.getMinecraft();

    @SubscribeEvent
    public void renderCustomObjects(RenderWorldLastEvent event) {
        if(!ReplayHandler.isInReplay()) return;

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

        //do stuff here
        for(CustomImageObject object : ReplayHandler.getCustomImageObjects()) {
            drawCustomImageObject(doubleX, doubleY, doubleZ, object);
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();

        GlStateManager.disableBlend();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }

    private void drawCustomImageObject(double playerX, double playerY, double playerZ, CustomImageObject customImageObject) {
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();

        GlStateManager.disableBlend();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();

        mc.renderEngine.bindTexture(customImageObject.getResourceLocation());

        ExtendedPosition objectPosition = customImageObject.getPosition();

        double x = objectPosition.getX() - playerX;
        double y = objectPosition.getY() - playerY;
        double z = objectPosition.getZ() - playerZ;

        GL11.glNormal3f(0, 1, 0);

        GlStateManager.translate(x, y + 1.4, z);

        GlStateManager.rotate(-objectPosition.getYaw(), 0, 1, 0);
        GlStateManager.rotate(objectPosition.getRoll(), 0, 0, 1);
        GlStateManager.rotate(objectPosition.getPitch(), 1, 0, 0);

        renderer.setColorRGBA_F(1, 1, 1, 0.5f);

        float width = objectPosition.getWidth() * objectPosition.getScale();
        float height = objectPosition.getHeight() * objectPosition.getScale();

        float minX = -width/2 + objectPosition.getAnchorX();
        float maxX = width/2 + objectPosition.getAnchorX();
        float minY = -height/2 + objectPosition.getAnchorY();
        float maxY = height/2 + objectPosition.getAnchorY();

        renderer.startDrawingQuads();

        renderer.addVertexWithUV(minX, minY, 0, 1, 1);
        renderer.addVertexWithUV(minX, maxY, 0, 1, 0);
        renderer.addVertexWithUV(maxX, maxY, 0, 0, 0);
        renderer.addVertexWithUV(maxX, minY, 0, 0, 1);

        renderer.addVertexWithUV(maxX, maxY, 0, 0, 0);
        renderer.addVertexWithUV(minX, maxY, 0, 1, 0);
        renderer.addVertexWithUV(minX, minY, 0, 1, 1);
        renderer.addVertexWithUV(maxX, minY, 0, 0, 1);

        tessellator.draw();
        renderer.setTranslation(0, 0, 0);

        GlStateManager.disableAlpha();
        GlStateManager.disableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
