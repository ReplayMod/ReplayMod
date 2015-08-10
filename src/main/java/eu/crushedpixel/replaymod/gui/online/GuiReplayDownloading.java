package eu.crushedpixel.replaymod.gui.online;

import com.mojang.realmsclient.gui.ChatFormatting;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.advanced.GuiProgressBar;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;
import eu.crushedpixel.replaymod.gui.elements.listeners.ProgressUpdateListener;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

import java.io.File;

public class GuiReplayDownloading extends AbstractGuiScreen<GuiReplayDownloading> implements ProgressUpdateListener {

    private GuiProgressBar progressBar = new GuiProgressBar(this).setHeight(20);
    private GuiButton cancelButton = new GuiButton(this).setI18nLabel("gui.cancel").setSize(150, 20).onClick(new Runnable() {
                @Override
                public void run() {
                    ReplayMod.apiClient.cancelDownload();
                    getMinecraft().displayGuiScreen(new GuiReplayCenter());
                }
            });

    public GuiReplayDownloading(final FileInfo fileInfo) {
        setTitle(new GuiLabel().setI18nText("replaymod.gui.viewer.download.title"));
        final GuiLabel subTitle = new GuiLabel(this).setI18nText("replaymod.gui.viewer.download.message",
                ChatFormatting.UNDERLINE + fileInfo.getName() + ChatFormatting.RESET);
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
                final File replayFile = ReplayMod.downloadedFileHandler.downloadFileForID(fileInfo.getId(), GuiReplayDownloading.this);
                if(replayFile.exists()) {
                    getMinecraft().addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ReplayHandler.startReplay(replayFile);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void onProgressChanged(float progress) {
        progressBar.setProgress(progress);
    }

    @Override
    public void onProgressChanged(float progress, String progressString) {
        onProgressChanged(progress);
    }

    @Override
    protected GuiReplayDownloading getThis() {
        return this;
    }
}
