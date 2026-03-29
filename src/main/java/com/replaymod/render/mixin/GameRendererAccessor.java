package com.replaymod.render.mixin;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC>=12106
//$$ import net.minecraft.client.gui.render.GuiRenderer;
//$$ import net.minecraft.client.render.fog.FogRenderer;
//$$ import net.minecraft.client.util.Pool;
//#endif

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    //#if MC<12106
    @Accessor
    boolean getRenderHand();
    @Accessor
    void setRenderHand(boolean value);
    //#endif

    //#if MC>=12106
    //$$ @Accessor
    //$$ Pool getPool();
    //$$ @Accessor
    //$$ GuiRenderer getGuiRenderer();
    //#if MC >= 26.1
    //$$ @Accessor
    //$$ net.minecraft.client.renderer.state.GameRenderState getGameRenderState();
    //#else
    //$$ @Accessor
    //$$ net.minecraft.client.gui.render.state.GuiRenderState getGuiState();
    //#endif
    //$$ @Accessor
    //$$ FogRenderer getFogRenderer();
    //#endif
}
