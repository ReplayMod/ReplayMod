package eu.crushedpixel.replaymod.gui.online;

import com.mojang.realmsclient.gui.ChatFormatting;
import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;
import eu.crushedpixel.replaymod.gui.elements.ComposedElement;
import eu.crushedpixel.replaymod.gui.elements.GuiAdvancedButton;
import eu.crushedpixel.replaymod.gui.elements.GuiDropdown;
import eu.crushedpixel.replaymod.gui.elements.GuiString;
import eu.crushedpixel.replaymod.holders.GuiEntryListValueEntry;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiReplayInstanceChooser extends GuiScreen {

    private boolean initialized = false;

    private GuiString dropdownTitle;
    private GuiDropdown<GuiEntryListValueEntry<File>> fileDropdown;
    private GuiAdvancedButton chooseButton, cancelButton;
    private ComposedElement composedElement;

    private List<File> filesToChooseFrom = new ArrayList<File>();

    private final String TITLE = I18n.format("replaymod.gui.viewer.chooser.title");
    private final String REPLAYFILE = I18n.format("replaymod.gui.editor.replayfile")+":";
    private final String MESSAGE;

    private final String ORIGINAL = ChatFormatting.GREEN+I18n.format("replaymod.gui.original")+ChatFormatting.RESET;
    private final String MODIFIED = ChatFormatting.RED+I18n.format("replaymod.gui.modified")+ChatFormatting.RESET;

    public GuiReplayInstanceChooser(final FileInfo fileInfo, File downloadedFile) {
        int id = fileInfo.getId();

        this.MESSAGE = I18n.format("replaymod.gui.viewer.chooser.message", ChatFormatting.UNDERLINE+fileInfo.getName()+ChatFormatting.RESET);

        //gather all applicable replay files
        try {
            File replayFolder = ReplayFileIO.getReplayFolder();

            List<File> chooseableFiles = new ArrayList<File>();

            File[] files = replayFolder.listFiles();
            if(files != null) {
                for(File file : files) {
                    try {
                        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
                        if(!("." + extension).equals(ReplayFile.ZIP_FILE_EXTENSION)) continue;

                        String filename = FilenameUtils.getBaseName(file.getAbsolutePath());
                        String[] split = filename.split("_");
                        String first = split[0];

                        if(StringUtils.isNumeric(first) && Integer.valueOf(first) == id) {
                            chooseableFiles.add(file);
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            //if no modified versions of the replay were found, start the downloaded one
            if(chooseableFiles.isEmpty()) {
                ReplayHandler.startReplay(downloadedFile);
                return;
            }

            chooseableFiles.add(0, downloadedFile);
            this.filesToChooseFrom = chooseableFiles;

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initGui() {
        if(!initialized) {
            dropdownTitle = new GuiString(0, 0, Color.WHITE, REPLAYFILE);

            fileDropdown = new GuiDropdown<GuiEntryListValueEntry<File>>(fontRendererObj, 0, 0, 0, 5);

            int i = 0;
            for(File file : filesToChooseFrom) {
                String displayName = FilenameUtils.getName(file.getAbsolutePath()) + " (" + (i == 0 ? ORIGINAL : MODIFIED) + ")";
                fileDropdown.addElement(new GuiEntryListValueEntry<File>(displayName, file));
                i++;
            }

            chooseButton = new GuiAdvancedButton(0, 0, 100, 20, I18n.format("replaymod.gui.load"), new Runnable() {
                @Override
                public void run() {
                    try {
                        File file = fileDropdown.getElement(fileDropdown.getSelectionIndex()).getValue();
                        ReplayHandler.startReplay(file);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }, null);

            cancelButton = new GuiAdvancedButton(0, 0, 100, 20, I18n.format("replaymod.gui.cancel"), new Runnable() {
                @Override
                public void run() {
                    mc.displayGuiScreen(new GuiReplayCenter());
                }
            }, null);

            composedElement = new ComposedElement(dropdownTitle, fileDropdown, chooseButton, cancelButton);
        }

        fileDropdown.width = 200;

        int strWidth = fontRendererObj.getStringWidth(REPLAYFILE);
        int totWidth = strWidth + fileDropdown.width + 5;

        dropdownTitle.positionX = (this.width-totWidth)/2;
        fileDropdown.xPosition = dropdownTitle.positionX + strWidth + 5;

        fileDropdown.yPosition = this.height/2 - 10;

        dropdownTitle.positionY = fileDropdown.yPosition + 6;

        cancelButton.xPosition = this.width - 100 - 5;
        chooseButton.xPosition = cancelButton.xPosition - 100 - 5;

        cancelButton.yPosition = chooseButton.yPosition = this.height - 5 - 20;

        this.initialized = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        drawCenteredString(fontRendererObj, ChatFormatting.UNDERLINE+TITLE, this.width / 2, 15, Color.WHITE.getRGB());

        String[] lines = eu.crushedpixel.replaymod.utils.StringUtils.splitStringInMultipleRows(MESSAGE, this.width-40);
        int i = 0;
        for(String line : lines) {
            drawString(fontRendererObj, line, 20, 40+(15*i), Color.WHITE.getRGB());
            i++;
        }

        composedElement.draw(mc, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        composedElement.mouseClick(mc, mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        composedElement.mouseRelease(mc, mouseX, mouseY, state);
    }
}
