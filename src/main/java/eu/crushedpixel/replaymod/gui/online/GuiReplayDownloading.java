package eu.crushedpixel.replaymod.gui.online;

import com.mojang.realmsclient.gui.ChatFormatting;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.GuiProgressBar;
import eu.crushedpixel.replaymod.gui.elements.listeners.ProgressUpdateListener;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class GuiReplayDownloading extends GuiScreen implements ProgressUpdateListener {

    private String title;
    private String pleaseWait;
    private String cancelCallback;

    private boolean callback = false;

    private boolean initialized = false;

    private GuiProgressBar progressBar;
    private GuiButton cancelButton;

    public GuiReplayDownloading(final FileInfo fileInfo) {
        pleaseWait = I18n.format("replaymod.gui.viewer.download.message", ChatFormatting.UNDERLINE+fileInfo.getName()+ChatFormatting.RESET);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File replayFile = ReplayMod.downloadedFileHandler.downloadFileForID(fileInfo.getId(), GuiReplayDownloading.this);
                if(replayFile.exists()) {
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ReplayHandler.startReplay(replayFile);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public void initGui() {
        if(!initialized) {
            title = I18n.format("replaymod.gui.viewer.download.title");
            cancelCallback = I18n.format("replaymod.gui.rendering.cancel.callback");

            progressBar = new GuiProgressBar();
            cancelButton = new GuiButton(GuiConstants.REPLAY_DOWNLOADING_CANCEL_BUTTON, 0, 0, I18n.format("gui.cancel"));
        }

        progressBar.setBounds(10, this.height-30-20, this.width-20, 20);

        cancelButton.xPosition = this.width - 5 - 150;
        cancelButton.width = 150;
        cancelButton.yPosition = this.height - 5 - 20;

        buttonList.add(cancelButton);

        initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(mc.fontRendererObj, title, this.width / 2, 20, Color.WHITE.getRGB());
        this.drawCenteredString(mc.fontRendererObj, pleaseWait, this.width / 2, 40, Color.WHITE.getRGB());

        progressBar.drawProgressBar();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(!button.enabled) return;
        if(button.id == GuiConstants.REPLAY_DOWNLOADING_CANCEL_BUTTON) {
            if(!callback) {
                callback = true;
                button.displayString = cancelCallback;
            } else {
                ReplayMod.apiClient.cancelDownload();
                mc.displayGuiScreen(new GuiReplayCenter());
            }
        }
    }

    @Override
    public void onProgressChanged(float progress) {
        if(progressBar != null) progressBar.setProgress(progress);
    }

    @Override
    public void onProgressChanged(float progress, String progressString) {
        onProgressChanged(progress);
    }
}
