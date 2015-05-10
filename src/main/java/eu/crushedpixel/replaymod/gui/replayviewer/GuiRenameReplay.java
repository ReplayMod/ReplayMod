package eu.crushedpixel.replaymod.gui.replayviewer;

import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.IOException;

public class GuiRenameReplay extends GuiScreen {
    private GuiScreen field_146585_a;
    private GuiTextField field_146583_f;
    private File file;

    public GuiRenameReplay(GuiScreen parent, File file) {
        this.field_146585_a = parent;
        this.file = file;
    }

    public void updateScreen() {
        this.field_146583_f.updateCursorCounter();
    }

    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 96 + 12, I18n.format("replaymod.gui.rename")));
        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 4 + 120 + 12, I18n.format("replaymod.gui.cancel")));
        String s = FilenameUtils.getBaseName(file.getAbsolutePath());
        this.field_146583_f = new GuiTextField(2, this.fontRendererObj, this.width / 2 - 100, 60, 200, 20);
        this.field_146583_f.setFocused(true);
        this.field_146583_f.setText(s);
    }

    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    protected void actionPerformed(GuiButton button) throws IOException {
        if(button.enabled) {
            if(button.id == 1) {
                this.mc.displayGuiScreen(this.field_146585_a);
            } else if(button.id == 0) {
                File folder = ReplayFileIO.getReplayFolder();

                File initRenamed = new File(folder, (this.field_146583_f.getText().trim() + ReplayFile.ZIP_FILE_EXTENSION).replaceAll("[^a-zA-Z0-9\\.\\-]", "_"));
                File renamed = initRenamed;
                int i = 1;
                while(renamed.isFile()) {
                    renamed = new File(initRenamed.getAbsolutePath() + "_" + i);
                    i++;
                }
                file.renameTo(renamed);
                this.mc.displayGuiScreen(this.field_146585_a);
            }
        }
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.field_146583_f.textboxKeyTyped(typedChar, keyCode);
        ((GuiButton) this.buttonList.get(0)).enabled = this.field_146583_f.getText().trim().length() > 0;

        if(keyCode == 28 || keyCode == 156) {
            this.actionPerformed((GuiButton) this.buttonList.get(0));
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.field_146583_f.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, I18n.format("replaymod.gui.viewer.rename.title"), this.width / 2, 20, 16777215);
        this.drawString(this.fontRendererObj, I18n.format("replaymod.gui.viewer.rename.name"), this.width / 2 - 100, 47, 10526880);
        this.field_146583_f.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
