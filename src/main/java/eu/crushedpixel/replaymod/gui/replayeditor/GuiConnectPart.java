package eu.crushedpixel.replaymod.gui.replayeditor;

import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.GuiArrowButton;
import eu.crushedpixel.replaymod.gui.elements.GuiDropdown;
import eu.crushedpixel.replaymod.gui.elements.GuiEntryList;
import eu.crushedpixel.replaymod.gui.elements.listeners.SelectionListener;
import eu.crushedpixel.replaymod.holders.GuiEntryListStringEntry;
import eu.crushedpixel.replaymod.studio.StudioImplementation;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiConnectPart extends GuiStudioPart {

    private final String DESCRIPTION = I18n.format("replaymod.gui.editor.connect.description");
    private final String TITLE = I18n.format("replaymod.gui.editor.connect.title");

    private boolean initialized = false;

    private GuiEntryList<GuiEntryListStringEntry> concatList;
    private GuiDropdown<GuiEntryListStringEntry> replayDropdown;

    private GuiButton removeButton, addButton;
    private GuiArrowButton upButton, downButton;

    private List<File> replayFiles;
    private List<GuiEntryListStringEntry> filesToConcat;

    private Thread filterThread;
    private File outputFile;

    public GuiConnectPart(int yPos) {
        super(yPos);
        this.mc = Minecraft.getMinecraft();
        fontRendererObj = mc.fontRendererObj;
    }

    @Override
    public void applyFilters(File replayFile, final File outputFile) {
        this.outputFile = outputFile;
        filterThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    List<File> inputFiles = new ArrayList<File>();
                    OUTER:
                    for (GuiEntryListStringEntry fileName : filesToConcat) {
                        for (File file : replayFiles) {
                            if (fileName.equals(FilenameUtils.getBaseName(file.getAbsolutePath()))) {
                                inputFiles.add(file);
                                continue OUTER;
                            }
                        }
                        throw new RuntimeException(fileName.getDisplayString());
                    }
                    StudioImplementation.connectReplayFiles(inputFiles, outputFile, GuiConnectPart.this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "replay-editor-connect");

        filterThread.start();

        mc.displayGuiScreen(new GuiReplayEditingProcess(this));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void cancelFilters() {
        filterThread.stop();
        FileUtils.deleteQuietly(outputFile);
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void initGui() {
        if(!initialized) {
            concatList = new GuiEntryList<GuiEntryListStringEntry>(fontRendererObj, 30, yPos, 150, 0);
            filesToConcat = new ArrayList<GuiEntryListStringEntry>();
            String selectedName = FilenameUtils.getBaseName(GuiReplayEditor.instance.getSelectedFile().getAbsolutePath());
            filesToConcat.add(new GuiEntryListStringEntry(selectedName));
            concatList.setElements(filesToConcat);

            concatList.setSelectionIndex(0);

            replayDropdown = new GuiDropdown<GuiEntryListStringEntry>(1, fontRendererObj, 250, yPos + 5, 0, 4);

            replayDropdown.clearElements();
            replayFiles = ReplayFileIO.getAllReplayFiles();
            int index = -1;
            int i = 0;
            for(File file : replayFiles) {
                String name = FilenameUtils.getBaseName(file.getAbsolutePath());
                replayDropdown.addElement(new GuiEntryListStringEntry(name));
                if(name.equals(selectedName)) {
                    index = i;
                }
                i++;
            }

            replayDropdown.setSelectionPos(index);

            replayDropdown.addSelectionListener(new SelectionListener() {

                @Override
                public void onSelectionChanged(int selectionIndex) {
                    try {
                        filesToConcat.set(concatList.getSelectionIndex(), replayDropdown.getElement(selectionIndex));
                        concatList.setElements(filesToConcat);
                    } catch(Exception e) {
                        // TODO Prevent exception
                    }
                }
            });

            concatList.addSelectionListener(new SelectionListener() {
                @Override
                public void onSelectionChanged(int selectionIndex) {
                    String selName = concatList.getElement(selectionIndex).getDisplayString();
                    int i = 0;
                    for(Object s : replayDropdown.getAllElements()) {
                        String str = ((GuiEntryListStringEntry)s).getDisplayString();
                        if(str.equals(selName)) {
                            replayDropdown.setSelectionIndex(i);
                            break;
                        }
                        i++;
                    }
                    removeButton.enabled = upButton.enabled = downButton.enabled = !(selectionIndex < 0 || selectionIndex >= filesToConcat.size());
                    if(upButton.enabled && selectionIndex == 0) upButton.enabled = false;
                    if(downButton.enabled && selectionIndex == filesToConcat.size() - 1) downButton.enabled = false;
                }
            });

            @SuppressWarnings("unchecked")
            List<GuiButton> buttonList = this.buttonList;

            upButton = new GuiArrowButton(GuiConstants.REPLAY_EDITOR_UP_BUTTON, 195, yPos + 40, "", GuiArrowButton.Direction.UP);
            buttonList.add(upButton);

            downButton = new GuiArrowButton(GuiConstants.REPLAY_EDITOR_DOWN_BUTTON, 219, yPos + 40, "", GuiArrowButton.Direction.DOWN);
            buttonList.add(downButton);

            removeButton = new GuiButton(GuiConstants.REPLAY_EDITOR_REMOVE_BUTTON, 249, yPos + 40, I18n.format("replaymod.gui.remove"));
            buttonList.add(removeButton);

            addButton = new GuiButton(GuiConstants.REPLAY_EDITOR_ADD_BUTTON, 0, yPos + 40, I18n.format("replaymod.gui.add"));
            buttonList.add(addButton);

            concatList.setSelectionIndex(0);
        }

        int w = GuiReplayEditor.instance.width - 249 - 20 - 4;
        addButton.xPosition = 249 + 6 + (w / 2);

        addButton.width = w / 2 + 2;
        removeButton.width = w / 2 + 2;

        replayDropdown.width = GuiReplayEditor.instance.width - 250 - 18;

        int h = GuiReplayEditor.instance.height - yPos - 20;
        int rows = (int) (h / (float) GuiEntryList.elementHeight);
        concatList.setVisibleElements(rows);

        initialized = true;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton); //call this first to ensure the dropdown is still open
        concatList.mouseClicked(mouseX, mouseY, mouseButton);
        replayDropdown.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        concatList.drawTextBox();
        replayDropdown.drawTextBox();

        drawString(fontRendererObj, I18n.format("replaymod.gui.replay")+":", 200, yPos + 5 + 7, Color.WHITE.getRGB());
    }

    @Override
    public void updateScreen() {
        if(!initialized) initGui();
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {

    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if(!button.enabled || replayDropdown.isExpanded()) {
            return;
        }

        if(button.id == GuiConstants.REPLAY_EDITOR_ADD_BUTTON) {
            filesToConcat.add(new GuiEntryListStringEntry(FilenameUtils.getBaseName(replayFiles.get(0).getAbsolutePath())));
            concatList.setElements(filesToConcat);
            concatList.setSelectionIndex(filesToConcat.size() - 1);
        } else if(button.id == GuiConstants.REPLAY_EDITOR_REMOVE_BUTTON) {
            int indexBefore = concatList.getSelectionIndex();
            if(indexBefore >= 0 && indexBefore < filesToConcat.size()) {
                filesToConcat.remove(indexBefore);
                concatList.setElements(filesToConcat);
                if(filesToConcat.size() <= indexBefore) {
                    concatList.setSelectionIndex(filesToConcat.size() - 1);
                } else {
                    concatList.setSelectionIndex(indexBefore);
                }
            }
        }
    }
}
