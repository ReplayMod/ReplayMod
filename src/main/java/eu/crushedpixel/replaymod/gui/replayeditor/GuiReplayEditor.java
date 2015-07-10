package eu.crushedpixel.replaymod.gui.replayeditor;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.GuiDropdown;
import eu.crushedpixel.replaymod.holders.GuiEntryListStringEntry;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import eu.crushedpixel.replaymod.utils.StringUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiReplayEditor extends GuiScreen {

    private static final int tabYPos = 110;
    public static GuiReplayEditor instance = null;
    private StudioTab currentTab = StudioTab.TRIM;
    private GuiDropdown<GuiEntryListStringEntry> replayDropdown;
    private GuiButton saveModeButton;
    private boolean overrideSave = false;
    private boolean initialized = false;
    private List<File> replayFiles = new ArrayList<File>();

    public GuiReplayEditor() {
        instance = this;
    }

    public File getSelectedFile() {
        try {
            return replayFiles.get(replayDropdown.getSelectionIndex());
        } catch(ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    private void refreshReplayDropdown() {
        replayDropdown.clearElements();
        replayFiles = ReplayFileIO.getAllReplayFiles();
        if(replayFiles.size() == 0) {
            mc.displayGuiScreen(null);
            ReplayMod.guiEventHandler.replayCount = 0;
            return;
        }
        for(File file : replayFiles) {
            String name = FilenameUtils.getBaseName(file.getAbsolutePath());
            replayDropdown.addElement(new GuiEntryListStringEntry(name));
        }
    }

    @Override
    public void initGui() {
        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = this.buttonList;
        List<GuiButton> tabButtons = new ArrayList<GuiButton>();

        tabButtons.add(new GuiButton(GuiConstants.REPLAY_EDITOR_TRIM_TAB, 0, 0, I18n.format("replaymod.gui.editor.trim.title")));
        tabButtons.add(new GuiButton(GuiConstants.REPLAY_EDITOR_CONNECT_TAB, 0, 0, I18n.format("replaymod.gui.editor.connect.title")));
        tabButtons.add(new GuiButton(GuiConstants.REPLAY_EDITOR_MODIFY_TAB, 0, 0, I18n.format("replaymod.gui.editor.modify.title")));

        int w = this.width - 30;
        int w2 = w / tabButtons.size();
        int i = 0;
        for(GuiButton b : tabButtons) {
            int x = 15 + (w2 * i);
            b.xPosition = x + 2;
            b.yPosition = 30;
            b.width = w2 - 4;

            buttonList.add(b);

            i++;
        }

        int modeWidth = tabButtons.get(0).width;

        if(!initialized) {
            replayDropdown = new GuiDropdown<GuiEntryListStringEntry>(fontRendererObj, 15 + 2 + 1 + 80, 60, this.width - 30 - 8 - 80 - modeWidth - 4, 5);
            refreshReplayDropdown();
        } else {
            replayDropdown.width = this.width - 30 - 8 - 80 - modeWidth - 4;
        }

        if(!initialized) {
            saveModeButton = new GuiButton(GuiConstants.REPLAY_EDITOR_SAVEMODE_BUTTON, width - 15 - modeWidth - 3, 60, getSaveModeLabel());
        } else {
            saveModeButton.xPosition = width - 15 - modeWidth - 3;
        }
        saveModeButton.width = modeWidth;
        buttonList.add(saveModeButton);


        GuiButton backButton = new GuiButton(GuiConstants.REPLAY_EDITOR_BACK_BUTTON, width - 70 - 18, height - 20 - 5, I18n.format("replaymod.gui.back"));
        backButton.width = 70;
        buttonList.add(backButton);

        GuiButton saveButton = new GuiButton(GuiConstants.REPLAY_EDITOR_SAVE_BUTTON, width - 70 - 18, height - (2 * 20) - 5 - 3, I18n.format("replaymod.gui.save"));
        saveButton.width = 70;
        buttonList.add(saveButton);

        for(StudioTab tab : StudioTab.values()) {
            tab.getStudioPart().initGui();
        }

        initialized = true;
    }

    private String getSaveModeLabel() {
        return overrideSave ? I18n.format("replaymod.gui.editor.savemode.override") : I18n.format("replaymod.gui.editor.savemode.newfile");
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(!button.enabled) return;
        if(button.id == GuiConstants.REPLAY_EDITOR_SAVEMODE_BUTTON) {
            overrideSave = !overrideSave;
            button.displayString = getSaveModeLabel();
        } else if(button.id == GuiConstants.REPLAY_EDITOR_BACK_BUTTON) {
            mc.displayGuiScreen(new GuiMainMenu());
        } else if(button.id == GuiConstants.REPLAY_EDITOR_TRIM_TAB) {
            currentTab = StudioTab.TRIM;
        } else if(button.id == GuiConstants.REPLAY_EDITOR_CONNECT_TAB) {
            currentTab = StudioTab.CONNECT;
        } else if(button.id == GuiConstants.REPLAY_EDITOR_MODIFY_TAB) {
            currentTab = StudioTab.MODIFY;
        } else if(button.id == GuiConstants.REPLAY_EDITOR_SAVE_BUTTON) {
            File outputFile = getSelectedFile();
            File folder = ReplayFileIO.getReplayFolder();
            if(!overrideSave) {
                String name = FilenameUtils.getBaseName(outputFile.getAbsolutePath()) + "_edited";
                File f = new File(folder, name + ".mcpr");
                int num = 0;
                while(f.exists()) {
                    num++;
                    String fileName = name + "_" + num;
                    f = new File(folder, fileName + ".mcpr");
                }
                outputFile = f;
            }
            currentTab.getStudioPart().applyFilters(getSelectedFile(), outputFile);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {
        replayDropdown.mouseClicked(mouseX, mouseY, mouseButton);
        currentTab.getStudioPart().mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        currentTab.getStudioPart().drawScreen(mouseX, mouseY, partialTicks);

        drawCenteredString(fontRendererObj, "§n" + currentTab.getStudioPart().getTitle(), width / 2, 92, Color.WHITE.getRGB());

        String[] rows = StringUtils.splitStringInMultipleRows(currentTab.getStudioPart().getDescription(), width - 30 - 70 - 20);

        int i = 0;
        for(String row : rows) {
            drawString(fontRendererObj, row, 30, height - (15 * (rows.length - i)), Color.WHITE.getRGB());
            i++;
        }

        drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.replayeditor"), this.width / 2, 10, 16777215);
        drawString(fontRendererObj,  I18n.format("replaymod.gui.editor.replayfile"), 30, 67, Color.WHITE.getRGB());

        replayDropdown.drawTextBox();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        currentTab.getStudioPart().keyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        currentTab.getStudioPart().updateScreen();
        super.updateScreen();
    }

    private enum StudioTab {
        TRIM(new GuiTrimPart(tabYPos)), CONNECT(new GuiConnectPart(tabYPos)), MODIFY(new GuiConnectPart(tabYPos));

        private GuiStudioPart studioPart;

        StudioTab(GuiStudioPart part) {
            this.studioPart = part;
        }

        public GuiStudioPart getStudioPart() {
            return studioPart;
        }
    }
}
