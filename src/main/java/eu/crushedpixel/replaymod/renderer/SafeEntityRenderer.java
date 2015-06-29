package eu.crushedpixel.replaymod.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

public class SafeEntityRenderer extends EntityRenderer {

    public SafeEntityRenderer(Minecraft mcIn, EntityRenderer renderer) {
        super(mcIn, renderer.resourceManager);
    }

    @Override
    public void updateCameraAndRender(float partialTicks) {
        try {
            super.updateCameraAndRender(partialTicks);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateRenderer() {
        try {
            super.updateRenderer();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
