package eu.crushedpixel.replaymod.gui.replayviewer;

import com.mojang.realmsclient.util.Pair;
import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;
import eu.crushedpixel.replaymod.gui.GuiReplaySettings;
import eu.crushedpixel.replaymod.gui.elements.GuiLoadingListEntry;
import eu.crushedpixel.replaymod.gui.elements.GuiReplayListEntry;
import eu.crushedpixel.replaymod.gui.elements.GuiReplayListExtended;
import eu.crushedpixel.replaymod.gui.online.GuiUploadFile;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.registry.ResourceHelper;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.ImageUtils;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GuiReplayViewer extends GuiScreen implements GuiYesNoCallback {

    private static final int LOAD_BUTTON_ID = 9001;
    private static final int UPLOAD_BUTTON_ID = 9002;
    private static final int FOLDER_BUTTON_ID = 9003;
    private static final int RENAME_BUTTON_ID = 9004;
    private static final int DELETE_BUTTON_ID = 9005;
    private static final int SETTINGS_BUTTON_ID = 9006;
    private static final int CANCEL_BUTTON_ID = 9007;
    private boolean initialized;
    private GuiReplayListExtended replayGuiList;
    private List<Pair<Pair<File, ReplayMetaData>, File>> replayFileList = new ArrayList<Pair<Pair<File, ReplayMetaData>, File>>();
    private GuiButton loadButton;
    private GuiButton uploadButton;
    private GuiButton renameButton;
    private GuiButton deleteButton;
    private boolean delete_file = false;

    private Thread fileReloader;

    private class FileReloaderThread extends Thread {
        private final GuiLoadingListEntry loadingListEntry = new GuiLoadingListEntry();

        @Override
        public synchronized void start() {
            replayFileList = new ArrayList<Pair<Pair<File, ReplayMetaData>, File>>();
            replayGuiList.clearEntries();
            replayGuiList.addEntry(loadingListEntry);
            super.start();
        }

        @Override
        public void run() {
            for(final File file : ReplayFileIO.getAllReplayFiles()) {
                if(interrupted()) break;
                try {
                    ReplayFile replayFile = new ReplayFile(file);
                    final ReplayMetaData metaData = replayFile.metadata().get();
                    BufferedImage img = replayFile.thumb().get();

                    replayFile.close();

                    File tmp = null;
                    if(img != null) {
                        img = ImageUtils.scaleImage(img, new Dimension(1280, 720));
                        tmp = File.createTempFile(FilenameUtils.getBaseName(file.getAbsolutePath())+"_THUMBNAIL", "jpg");
                        tmp.deleteOnExit();

                        ImageIO.write(img, "jpg", tmp);
                    }

                    final File thumb = tmp;
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            addEntry(file, metaData, thumb);
                        }
                    });
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            mc.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    replayGuiList.removeEntry(loadingListEntry);
                }
            });
        }
    }

    public static GuiYesNo getYesNoGui(GuiYesNoCallback p_152129_0_, String file, int p_152129_2_) {
        String s1 = I18n.format("replaymod.gui.viewer.delete.linea");
        String s2 = "\'" + file + "\' " + I18n.format("replaymod.gui.viewer.delete.lineb");
        String s3 = I18n.format("replaymod.gui.delete");
        String s4 = I18n.format("replaymod.gui.cancel");
        return new GuiYesNo(p_152129_0_, s1, s2, s3, s4, p_152129_2_);
    }

    @Override
    public void onGuiClosed() {
        ResourceHelper.freeAllResources();
        super.onGuiClosed();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        if(!this.initialized) {
            replayGuiList = new ReplayList(this, this.mc, this.width, this.height, 32, this.height - 64, 36);
            this.initialized = true;
        } else {
            this.replayGuiList.setDimensions(this.width, this.height, 32, this.height - 64);
        }

        try {
            if(fileReloader != null) {
                fileReloader.interrupt();
                fileReloader.join();
            }

            fileReloader = new FileReloaderThread();
            fileReloader.start();
        } catch(Exception e) {
            e.printStackTrace();
        }

        this.createButtons();
    }

    private void createButtons() {
        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = this.buttonList;
        buttonList.add(loadButton = new GuiButton(LOAD_BUTTON_ID, this.width / 2 - 154, this.height - 52, 73, 20, I18n.format("replaymod.gui.load")));
        buttonList.add(uploadButton = new GuiButton(UPLOAD_BUTTON_ID, this.width / 2 - 154 + 78, this.height - 52, 73, 20, I18n.format("replaymod.gui.upload")));
        buttonList.add(new GuiButton(FOLDER_BUTTON_ID, this.width / 2 + 4, this.height - 52, 150, 20, I18n.format("replaymod.gui.viewer.replayfolder")));
        buttonList.add(renameButton = new GuiButton(RENAME_BUTTON_ID, this.width / 2 - 154, this.height - 28, 72, 20, I18n.format("replaymod.gui.rename")));
        buttonList.add(deleteButton = new GuiButton(DELETE_BUTTON_ID, this.width / 2 - 76, this.height - 28, 72, 20, I18n.format("replaymod.gui.delete")));
        buttonList.add(new GuiButton(SETTINGS_BUTTON_ID, this.width / 2 + 4, this.height - 28, 72, 20, I18n.format("replaymod.gui.settings")));
        buttonList.add(new GuiButton(CANCEL_BUTTON_ID, this.width / 2 + 4 + 78, this.height - 28, 72, 20, I18n.format("replaymod.gui.cancel")));
        setButtonsEnabled(false);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        this.replayGuiList.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.replayGuiList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        this.replayGuiList.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        this.replayGuiList.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRendererObj, I18n.format("replaymod.gui.replayviewer"), this.width / 2, 20, 16777215);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if(uploadButton.isMouseOver() && !uploadButton.enabled && loadButton.enabled) {
            if(currentFileUploaded) {
                ReplayMod.tooltipRenderer.drawTooltip(mouseX, mouseY, I18n.format("replaymod.gui.viewer.alreadyuploaded"), this, Color.RED);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(button.enabled) {
            if(button.id == LOAD_BUTTON_ID) {
                loadReplay(replayGuiList.selected);
            } else if(button.id == CANCEL_BUTTON_ID) {
                mc.displayGuiScreen(null);
            } else if(button.id == DELETE_BUTTON_ID) {
                String s = ((GuiReplayListEntry)replayGuiList.getListEntry(replayGuiList.selected)).getFileInfo().getName();

                if(s != null) {
                    delete_file = true;
                    GuiYesNo guiyesno = getYesNoGui(this, s, 1);
                    this.mc.displayGuiScreen(guiyesno);
                }
            } else if(button.id == SETTINGS_BUTTON_ID) {
                this.mc.displayGuiScreen(new GuiReplaySettings(this));
            } else if(button.id == RENAME_BUTTON_ID) {
                File file = replayFileList.get(replayGuiList.selected).first().first();
                this.mc.displayGuiScreen(new GuiRenameReplay(this, file));
            } else if(button.id == UPLOAD_BUTTON_ID) {
                File file = replayFileList.get(replayGuiList.selected).first().first();
                this.mc.displayGuiScreen(new GuiUploadFile(file, this));
            } else if(button.id == FOLDER_BUTTON_ID) {
                File file1 = ReplayFileIO.getReplayFolder();

                String s = file1.getAbsolutePath();

                if(Util.getOSType() == Util.EnumOS.OSX) {
                    try {
                        Runtime.getRuntime().exec(new String[]{"/usr/bin/open", s});
                        return;
                    } catch (IOException e) {
                        LogManager.getLogger().error("Cannot open file", e);
                    }
                } else if(Util.getOSType() == Util.EnumOS.WINDOWS) {
                    String s1 = String.format("cmd.exe /C start \"Open file\" \"%s\"", s);

                    try {
                        Runtime.getRuntime().exec(s1);
                        return;
                    } catch(IOException e) {
                        LogManager.getLogger().error("Cannot open file", e);
                    }
                }

                boolean flag = false;

                try {
                    Desktop.getDesktop().browse(file1.toURI());
                } catch(Throwable throwable) {
                    flag = true;
                }

                if(flag) {
                    Sys.openURL("file://" + s);
                }
            }
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if(this.delete_file) {
            this.delete_file = false;

            if(result) {
                try {
                    FileUtils.forceDelete(replayFileList.get(replayGuiList.selected).first().first());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                replayFileList.remove(replayGuiList.selected);
                replayGuiList.selected = -1;
            }

            this.mc.displayGuiScreen(this);
        }
    }

    private boolean currentFileUploaded = false;

    public void setButtonsEnabled(boolean b) {
        loadButton.enabled = b;

        if(b) {
            currentFileUploaded = ReplayMod.uploadedFileHandler.isUploaded(replayFileList.get(replayGuiList.selected).first().first());
            uploadButton.enabled = !currentFileUploaded;
        } else {
            uploadButton.enabled = false;
        }


        renameButton.enabled = b;
        deleteButton.enabled = b;
    }

    public void loadReplay(int id) {
        mc.displayGuiScreen(null);

        try {
            ReplayHandler.startReplay(replayFileList.get(id).first().first());
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    private void addEntry(File file, ReplayMetaData metaData, File thumb) {
        final Pair<Pair<File, ReplayMetaData>, File> p = Pair.of(Pair.of(file, metaData), thumb);
        final int index = getInsertionIndex(p, replayFileList);

        replayFileList.add(index, p);

        final FileInfo fileInfo = new FileInfo(-1, p.first().second(), null, null,
                -1, -1, -1, FilenameUtils.getBaseName(p.first().first().getName()), true, -1);

        replayGuiList.addEntry(index, new GuiReplayListEntry(replayGuiList, fileInfo, p.second()));
    }

    private static FileAgeComparator fileAgeComparator = new FileAgeComparator();

    public static class FileAgeComparator implements Comparator<Pair<Pair<File, ReplayMetaData>, File>> {

        @Override
        public int compare(Pair<Pair<File, ReplayMetaData>, File> o1, Pair<Pair<File, ReplayMetaData>, File> o2) {
            try {
                return new Date(o2.first().second().getDate()).compareTo(new Date(o1.first().second().getDate()));
            } catch(Exception e) {
                return 0;
            }
        }
    }

    private int getInsertionIndex(Pair<Pair<File, ReplayMetaData>, File> p, List<Pair<Pair<File, ReplayMetaData>, File>> list) {
        List<Pair<Pair<File, ReplayMetaData>, File>> nl = new ArrayList<Pair<Pair<File, ReplayMetaData>, File>>(list);

        nl.add(p);
        Collections.sort(nl, fileAgeComparator);

        return nl.indexOf(p);
    }

}
