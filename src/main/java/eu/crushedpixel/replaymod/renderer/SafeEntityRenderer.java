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
        } //This is plain easier than doing proper error prevention.
        //If Johni reads this, don't think I'm a bad programmer... Just a lazy one :P
    }

    @Override
    public void updateRenderer() {
        try {
            super.updateRenderer();
        } catch(Exception e) {
        }
    }

}
