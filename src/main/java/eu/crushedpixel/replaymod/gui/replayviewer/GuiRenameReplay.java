package eu.crushedpixel.replaymod.gui.replayviewer;

import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class GuiRenameReplay extends GuiScreen {
    private GuiScreen parent;
    private GuiTextField replayNameInput;
    private File file;

    public GuiRenameReplay(GuiScreen parent, File file) {
        this.parent = parent;
        this.file = file;
    }

    @Override
    public void updateScreen() {
        this.replayNameInput.updateCursorCounter();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = this.buttonList;
        buttonList.clear();
        buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 96 + 12, I18n.format("replaymod.gui.rename")));
        buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 120 + 12, I18n.format("replaymod.gui.cancel")));
        String s = FilenameUtils.getBaseName(file.getAbsolutePath());
        this.replayNameInput = new GuiTextField(2, this.fontRendererObj, this.width / 2 - 100, 60, 200, 20);
        this.replayNameInput.setFocused(true);
        this.replayNameInput.setText(s);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(button.enabled) {
            if(button.id == 1) {
                this.mc.displayGuiScreen(this.parent);
            } else if(button.id == 0) {
                File folder = ReplayFileIO.getReplayFolder();

                File initRenamed = new File(folder, (this.replayNameInput.getText().trim() + ReplayFile.ZIP_FILE_EXTENSION).replaceAll("[^a-zA-Z0-9\\.\\- ]", "_"));
                File renamed = ReplayFileIO.getNextFreeFile(initRenamed);
                try {
                    FileUtils.moveFile(file, renamed);
                } catch (IOException e) {
                    e.printStackTrace();
                    mc.displayGuiScreen(new GuiErrorScreen(
                            I18n.format("replaymod.gui.viewer.delete.failed1"),
                            I18n.format("replaymod.gui.viewer.delete.failed2")
                    ));
                    return;
                }
                this.mc.displayGuiScreen(this.parent);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.replayNameInput.textboxKeyTyped(typedChar, keyCode);
        ((GuiButton) this.buttonList.get(0)).enabled = this.replayNameInput.getText().trim().length() > 0;

        if(keyCode == 28 || keyCode == 156) {
            this.actionPerformed((GuiButton) this.buttonList.get(0));
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.replayNameInput.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("replaymod.gui.viewer.rename.title"), this.width / 2, 20, 16777215);
        this.drawString(this.fontRendererObj, I18n.format("replaymod.gui.viewer.rename.name"), this.width / 2 - 100, 47, 10526880);
        this.replayNameInput.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
