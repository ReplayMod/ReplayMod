package com.replaymod.online.gui;

import com.replaymod.core.ReplayMod;
import com.replaymod.online.ReplayModOnline;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.ApiException;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import net.minecraft.util.Formatting;

import java.io.File;
import java.io.IOException;

public class GuiReplayDownloading extends AbstractGuiScreen<GuiReplayDownloading> {
    private final GuiScreen cancelScreen;
    private final ApiClient apiClient;

    private GuiProgressBar progressBar = new GuiProgressBar(this).setHeight(20);
    private GuiButton cancelButton = new GuiButton(this).setI18nLabel("gui.cancel").setSize(150, 20).onClick(new Runnable() {
                @Override
                public void run() {
                    apiClient.cancelDownload();
                    cancelScreen.display();
                }
            });

    public GuiReplayDownloading(GuiScreen cancelScreen, final ReplayModOnline mod,
                                final int replayId, String name) {
        this.cancelScreen = cancelScreen;
        this.apiClient = mod.getApiClient();
        setTitle(new GuiLabel().setI18nText("replaymod.gui.viewer.download.title"));
        final GuiLabel subTitle = new GuiLabel(this).setI18nText("replaymod.gui.viewer.download.message",
                Formatting.UNDERLINE + name + Formatting.RESET);
        setLayout(new CustomLayout<GuiReplayDownloading>() {
            @Override
            protected void layout(GuiReplayDownloading container, int width, int height) {
                width(progressBar, width - 20);
                pos(progressBar, 10, height - 50);

                pos(cancelButton, width - 5 - 150, height - 5 - 20);

                pos(subTitle, width / 2 - subTitle.getMinSize().getWidth() / 2, 40);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                final File replayFile = mod.getDownloadedFile(replayId);
                try {
                    apiClient.downloadFile(replayId, replayFile, progressBar::setProgress);
                    if (replayFile.exists()) {
                        ReplayMod.instance.runLater(() -> {
                            try {
                                mod.startReplay(replayId, null, null);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (IOException | ApiException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    protected GuiReplayDownloading getThis() {
        return this;
    }
}
