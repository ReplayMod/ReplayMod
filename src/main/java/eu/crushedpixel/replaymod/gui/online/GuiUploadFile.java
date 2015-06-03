package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.replay.FileUploader;
import eu.crushedpixel.replaymod.api.replay.holders.Category;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.GuiProgressBar;
import eu.crushedpixel.replaymod.gui.replayviewer.GuiReplayViewer;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.registry.ResourceHelper;
import eu.crushedpixel.replaymod.utils.ImageUtils;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiCheckBox;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class GuiUploadFile extends GuiScreen {

    private final Minecraft mc = Minecraft.getMinecraft();

    private static final Pattern titlePattern = Pattern.compile("[^a-z0-9 \\-_]", Pattern.CASE_INSENSITIVE);
    private static final Pattern tagsPattern = Pattern.compile("[^a-z0-9,]", Pattern.CASE_INSENSITIVE);

    private final ResourceLocation textureResource;
    private DynamicTexture dynTex = null;

    private GuiTextField fileTitleInput, tagInput, messageTextField, tagPlaceholder;
    private GuiCheckBox hideServerIP;
    private GuiButton categoryButton, startUploadButton, cancelUploadButton, backButton;
    private GuiProgressBar progressBar;

    private File replayFile;

    private ReplayMetaData metaData;
    private BufferedImage thumb;

    private FileUploader uploader = new FileUploader();

    private Category category = Category.MINIGAME;

    private GuiReplayViewer parent;

    private boolean lockUploadButton = false;

    private final Logger logger = LogManager.getLogger();

    public GuiUploadFile(File file, GuiReplayViewer parent) {
        this.parent = parent;

        this.textureResource = new ResourceLocation("upload_thumbs/" + FilenameUtils.getBaseName(file.getAbsolutePath()));
        dynTex = null;

        boolean correctFile = false;
        this.replayFile = file;

        if(("." + FilenameUtils.getExtension(file.getAbsolutePath())).equals(ReplayFile.ZIP_FILE_EXTENSION)) {
            ReplayFile archive = null;
            try {
                archive = new ReplayFile(file);

                metaData = archive.metadata().get();
                BufferedImage img = archive.thumb().get();
                if(img != null) {
                    thumb = ImageUtils.scaleImage(img, new Dimension(1280, 720));
                }

                archive.close();
                correctFile = true;
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                if(archive != null) {
                    try {
                        archive.close();
                    } catch(IOException e) {
                    }
                }
            }
        }

        if(!correctFile) {
            logger.error("Invalid file provided to upload");
            mc.displayGuiScreen(parent); //TODO: Error message
            replayFile = null;
            return;
        }

        //If thumb is null, set image to placeholder
        if(thumb == null) {
            try {
                thumb = ImageIO.read(GuiUploadFile.class.getClassLoader().getResourceAsStream("default_thumb.jpg"));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void initGui() {
        if(replayFile == null) return;

        if(fileTitleInput == null) {
            fileTitleInput = new GuiTextField(GuiConstants.UPLOAD_NAME_INPUT, fontRendererObj, (this.width / 2) + 20 + 10, 21, Math.min(200, this.width - 20 - 260), 20);
            String fname = FilenameUtils.getBaseName(replayFile.getAbsolutePath());
            fileTitleInput.setText(fname);
            fileTitleInput.setMaxStringLength(30);
        } else {
            fileTitleInput.xPosition = (this.width / 2) + 20 + 10;
            fileTitleInput.width = Math.min(200, this.width - 20 - 260);
        }

        if(hideServerIP == null) {
            hideServerIP = new GuiCheckBox(GuiConstants.UPLOAD_HIDE_SERVER_IP, 0, 80, I18n.format("replaymod.gui.upload.hideip"), false);
            hideServerIP.enabled = !metaData.isSingleplayer();
        }

        if(hideServerIP.enabled) {
            hideServerIP.xPosition = (this.width / 2) + 20 + 9;
            buttonList.add(hideServerIP);
        }

        if(categoryButton == null) {
            categoryButton = new GuiButton(GuiConstants.UPLOAD_CATEGORY_BUTTON, (this.width / 2) + 20 + 10 - 1, 95, I18n.format("replaymod.category")+": " + category.toNiceString());
            categoryButton.width = Math.min(202, this.width - 20 - 260 + 2);
        } else {
            categoryButton.xPosition = (this.width / 2) + 20 + 10 - 1;
        }

        buttonList.add(categoryButton);

        if(startUploadButton == null) {
            List<GuiButton> bottomBar = new ArrayList<GuiButton>();
            startUploadButton = new GuiButton(GuiConstants.UPLOAD_START_BUTTON, 0, 0, I18n.format("replaymod.gui.upload.start"));
            bottomBar.add(startUploadButton);

            cancelUploadButton = new GuiButton(GuiConstants.UPLOAD_CANCEL_BUTTON, 0, 0, I18n.format("replaymod.gui.upload.cancel"));
            cancelUploadButton.enabled = false;
            bottomBar.add(cancelUploadButton);

            backButton = new GuiButton(GuiConstants.UPLOAD_BACK_BUTTON, 0, 0, I18n.format("replaymod.gui.back"));
            bottomBar.add(backButton);

            int i = 0;
            for(GuiButton b : bottomBar) {
                int w = this.width - 30;
                int w2 = w / bottomBar.size();

                int x = 15 + (w2 * i);
                b.xPosition = x + 2;
                b.yPosition = height - 30;
                b.width = w2 - 4;

                buttonList.add(b);

                i++;
            }
        } else {
            List<GuiButton> bottomBar = new ArrayList<GuiButton>();

            bottomBar.add(startUploadButton);
            bottomBar.add(cancelUploadButton);
            bottomBar.add(backButton);

            int i = 0;
            for(GuiButton b : bottomBar) {
                int w = this.width - 30;
                int w2 = w / bottomBar.size();

                int x = 15 + (w2 * i);
                b.xPosition = x + 2;
                b.yPosition = height - 30;
                b.width = w2 - 4;

                buttonList.add(b);

                i++;
            }
        }

        if(messageTextField == null) {
            messageTextField = new GuiTextField(GuiConstants.UPLOAD_INFO_FIELD, fontRendererObj, 20, height - 80, width - 40, 20);
            messageTextField.setEnabled(true);
            messageTextField.setFocused(false);
            messageTextField.setMaxStringLength(Integer.MAX_VALUE);
        } else {
            messageTextField.yPosition = height - 80;
            messageTextField.width = width - 40;
        }

        if(tagInput == null) {
            tagInput = new GuiTextField(GuiConstants.UPLOAD_TAG_INPUT, fontRendererObj, (this.width / 2) + 20 + 10, categoryButton.yPosition + 20 + 5, Math.min(200, this.width - 20 - 260), 20);
            tagInput.setMaxStringLength(30);
        } else {
            tagInput.xPosition = (this.width / 2) + 20 + 10;
            tagInput.width = Math.min(200, this.width - 20 - 260);
        }

        if(tagPlaceholder == null) {
            tagPlaceholder = new GuiTextField(GuiConstants.UPLOAD_TAG_PLACEHOLDER, fontRendererObj, (this.width / 2) + 20 + 10, tagInput.yPosition, Math.min(200, this.width - 20 - 260), 20);
            tagPlaceholder.setTextColor(Color.DARK_GRAY.getRGB());
            tagPlaceholder.setText(I18n.format("replaymod.gui.upload.tagshint"));
        } else {
            tagPlaceholder.xPosition = (this.width / 2) + 20 + 10;
            tagPlaceholder.width = Math.min(200, this.width - 20 - 260);
        }

        if(progressBar == null) {
            progressBar = new GuiProgressBar(19, height - 52, width - (2*19), 15);
        } else {
            progressBar.setBounds(19, height - 52, width - (2*19), 15);
        }

        validateStartButton();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(!button.enabled) return;
        if(button.id == GuiConstants.UPLOAD_CATEGORY_BUTTON) {
            category = category.next();
            categoryButton.displayString = I18n.format("replaymod.category")+": " + category.toNiceString();
        } else if(button.id == GuiConstants.UPLOAD_BACK_BUTTON) {
            mc.displayGuiScreen(parent);
        } else if(button.id == GuiConstants.UPLOAD_START_BUTTON) {
            final String name = fileTitleInput.getText().trim();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String tagsRaw = tagInput.getText();
                        String[] split = tagsRaw.split(",");
                        List<String> tags = new ArrayList<String>();
                        for(String str : split) {
                            if(!tags.contains(str) && str.length() > 0) {
                                tags.add(str);
                            }
                        }

                        if(hideServerIP.isChecked()) {
                            File tmp = File.createTempFile("replay_hidden_ip", "mcpr");
                            File tmpMeta = File.createTempFile("metadata", "json");

                            ReplayMetaData newMetaData = metaData.copy();
                            newMetaData.removeServer();
                            ReplayFileIO.writeReplayMetaDataToFile(newMetaData, tmpMeta);

                            HashMap<String, File> toAdd = new HashMap<String, File>();
                            toAdd.put(ReplayFile.ENTRY_METADATA, tmpMeta);

                            FileUtils.copyFile(replayFile, tmp);
                            ReplayFileIO.addFilesToZip(tmp, toAdd);

                            uploader.uploadFile(GuiUploadFile.this, AuthenticationHandler.getKey(), name, tags, tmp, category);

                            tmpMeta.delete();
                            tmp.delete();
                        } else {
                            uploader.uploadFile(GuiUploadFile.this, AuthenticationHandler.getKey(), name, tags, replayFile, category);
                        }

                        ReplayMod.uploadedFileHandler.markAsUploaded(replayFile);
                    } catch(Exception e) {
                        messageTextField.setText(I18n.format("replaymod.gui.unknownerror"));
                        messageTextField.setTextColor(Color.RED.getRGB());
                        e.printStackTrace();
                    }
                }
            }, "replaymod-file-uploader").start();
        } else if(button.id == GuiConstants.UPLOAD_CANCEL_BUTTON) {
            uploader.cancelUploading();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        String durationString = I18n.format("replaymod.gui.duration") + ": " + String.format("%02dm%02ds",
                TimeUnit.MILLISECONDS.toMinutes(metaData.getDuration()),
                TimeUnit.MILLISECONDS.toSeconds(metaData.getDuration()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(metaData.getDuration()))
        );

        drawString(fontRendererObj, durationString, (this.width / 2) + 20 + 10, 50, Color.WHITE.getRGB());
        drawString(fontRendererObj, hideServerIP.isChecked() ? I18n.format("replaymod.gui.iphidden") : metaData.getServerName(),
                (this.width / 2) + 20 + 10, 65, Color.GRAY.getRGB());

        drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.upload.title"), this.width / 2, 5, Color.WHITE.getRGB());

        //Draw thumbnail
        if(thumb != null) {
            if(dynTex == null) {
                dynTex = new DynamicTexture(thumb);
                mc.getTextureManager().loadTexture(textureResource, dynTex);
                dynTex.updateDynamicTexture();
                ResourceHelper.registerResource(textureResource);
            }

            mc.getTextureManager().bindTexture(textureResource); //Will be freed by the ResourceHelper
            int wid = (this.width) / 2;
            int hei = Math.round(wid * (720f / 1280f));
            Gui.drawScaledCustomSizeModalRect(19, 20, 0, 0, 1280, 720, wid, hei, 1280, 720);
        }

        fileTitleInput.drawTextBox();
        messageTextField.drawTextBox();

        if(tagInput.getText().length() > 0 || tagInput.isFocused()) {
            tagInput.drawTextBox();
        } else {
            tagPlaceholder.drawTextBox();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        progressBar.setProgress(uploader.getUploadProgress());
        progressBar.drawProgressBar();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        fileTitleInput.mouseClicked(mouseX, mouseY, mouseButton);
        tagInput.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        fileTitleInput.updateCursorCounter();
        tagInput.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        ResourceHelper.freeAllResources();
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(fileTitleInput.isFocused()) {
            fileTitleInput.textboxKeyTyped(typedChar, keyCode);
        } else if(tagInput.isFocused()) {
            tagInput.textboxKeyTyped(typedChar, keyCode);
        }

        if(uploader.isUploading()) {
            startUploadButton.enabled = false;
        } else {
            validateStartButton();
        }
    }

    private void validateStartButton() {
        boolean enabled = true;
        if(fileTitleInput.getText().trim().length() < 5 || fileTitleInput.getText().trim().length() > 30) {
            enabled = false;
        } else if(titlePattern.matcher(fileTitleInput.getText()).find()) {
            enabled = false;
            fileTitleInput.setTextColor(Color.RED.getRGB());
        } else if(tagsPattern.matcher(tagInput.getText()).find()) {
            enabled = false;
            tagInput.setTextColor(Color.RED.getRGB());
        } else {
            fileTitleInput.setTextColor(Color.WHITE.getRGB());
            tagInput.setTextColor(Color.WHITE.getRGB());
        }

        if(lockUploadButton) enabled = false;

        startUploadButton.enabled = enabled;
    }

    public void onStartUploading() {
        startUploadButton.enabled = false;
        cancelUploadButton.enabled = true;
        backButton.enabled = false;
        categoryButton.enabled = false;
        fileTitleInput.setEnabled(false);
        messageTextField.setText(I18n.format("replaymod.gui.upload.uploading"));
        messageTextField.setTextColor(Color.WHITE.getRGB());
    }

    public void onFinishUploading(boolean success, String info) {
        validateStartButton();
        cancelUploadButton.enabled = false;
        backButton.enabled = true;
        categoryButton.enabled = true;
        fileTitleInput.setEnabled(true);
        if(success) {
            messageTextField.setText(I18n.format("replaymod.gui.upload.success"));
            messageTextField.setTextColor(Color.GREEN.getRGB());
            startUploadButton.enabled = false;
            lockUploadButton = true;
        } else {
            messageTextField.setText(info);
            messageTextField.setTextColor(Color.RED.getRGB());
        }
    }
}
