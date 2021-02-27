package com.replaymod.core.versions.forge;

import com.replaymod.gradle.remap.Pattern;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;

class Patterns {
    @Pattern
    private static GuiButton getButton(GuiScreenEvent.ActionPerformedEvent event) {
        //#if MC>=10904
        return event.getButton();
        //#else
        //$$ return event.button;
        //#endif
    }

    @Pattern
    private static GuiScreen getGui(GuiScreenEvent event) {
        //#if MC>=10904
        return event.getGui();
        //#else
        //$$ return event.gui;
        //#endif
    }

    @Pattern
    private static EntityLivingBase getEntity(RenderLivingEvent event) {
        //#if MC>=10904
        return event.getEntity();
        //#else
        //$$ return event.entity;
        //#endif
    }

    @Pattern
    private static RenderGameOverlayEvent.ElementType getType(RenderGameOverlayEvent event) {
        //#if MC>=10904
        return event.getType();
        //#else
        //$$ return event.type;
        //#endif
    }
}
