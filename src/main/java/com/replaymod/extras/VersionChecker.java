package com.replaymod.extras;

import com.replaymod.core.ReplayMod;
import com.replaymod.online.ReplayModOnline;
import eu.crushedpixel.replaymod.utils.StringUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;

public class VersionChecker implements Extra {
    @Mod.Instance(ReplayModOnline.MOD_ID)
    private static ReplayModOnline module;

    @Override
    public void register(ReplayMod mod) throws Exception {
        final String currentVersion = mod.getVersion();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    boolean upToDate = module.getApiClient().isVersionUpToDate(currentVersion);
                    if (!upToDate) {
                        MinecraftForge.EVENT_BUS.register(VersionChecker.this);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }, "ReplayMod-VersionChecker").start();
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiMainMenu)) {
            return;
        }

        int width = Math.max(100, event.gui.width / 2 - 100 - 10);

        String[] lines = StringUtils.splitStringInMultipleRows(I18n.format("replaymod.gui.outdated"), width);

        int maxLineWidth = 0;
        for(String line : lines) {
            int lineWidth = event.gui.mc.fontRendererObj.getStringWidth(line);
            if(lineWidth > maxLineWidth) {
                maxLineWidth = lineWidth;
            }
        }

        Gui.drawRect(2, 77, 5 + maxLineWidth + 3, 80 + (lines.length * 10), 0x80FF0000);

        int i = 0;
        for(String line : lines) {
            event.gui.mc.fontRendererObj.drawStringWithShadow(line, 5, 80 + (i * 10), Color.WHITE.getRGB());
            i++;
        }
    }
}
