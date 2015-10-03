package eu.crushedpixel.replaymod.gui.replayeditor;

import com.replaymod.core.ReplayMod;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.GuiProgressBar;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GuiReplayEditingProcess extends GuiScreen {

    private String title;
    private String pleaseWait;
    private String cancelCallback;

    private boolean finished = false;

    private boolean callback = false;

    private boolean initialized = false;

    private GuiProgressBar progressBar;
    private GuiButton cancelButton;

    private GuiStudioPart studioPart;

    public GuiReplayEditingProcess(GuiStudioPart studioPart) {
        this.studioPart = studioPart;
    }

    @Override
    public void initGui() {
        if(!initialized) {
            title = I18n.format("replaymod.gui.editor.progress.title");
            pleaseWait = I18n.format("replaymod.gui.editor.progress.pleasewait");
            cancelCallback = I18n.format("replaymod.gui.rendering.cancel.callback");

            progressBar = new GuiProgressBar();
            cancelButton = new GuiButton(GuiConstants.REPLAY_EDITING_CANCEL_BUTTON, 0, 0, I18n.format("gui.cancel"));
        }

        progressBar.setBounds(10, this.height-30-20, this.width-20, 20);

        cancelButton.xPosition = this.width - 5 - 150;
        cancelButton.width = 150;
        cancelButton.yPosition = this.height - 5 - 20;

        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = this.buttonList;
        buttonList.add(cancelButton);

        initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(mc.fontRendererObj, title, this.width / 2, 20, Color.WHITE.getRGB());
        this.drawCenteredString(mc.fontRendererObj, pleaseWait, this.width / 2, 40, Color.WHITE.getRGB());

        progressBar.setProgress(studioPart.getProgress());
        progressBar.setProgressString(studioPart.getFilterProgressString());
        progressBar.drawProgressBar();

        if(studioPart.getProgress() >= 1f && !finished) {
            finished = true;
            cancelButton.displayString = I18n.format("gui.done");
            ReplayMod.soundHandler.playRenderSuccessSound();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(!button.enabled) return;
        if(button.id == GuiConstants.REPLAY_EDITING_CANCEL_BUTTON) {
            if(finished) {
                mc.displayGuiScreen(null);
            } else {
                if(!callback) {
                    callback = true;
                    button.displayString = cancelCallback;
                } else {
                    studioPart.cancelFilters();
                    mc.displayGuiScreen(null);
                }
            }
        }
    }
}
