package eu.crushedpixel.replaymod.gui.online;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.replay.FileUploader;
import eu.crushedpixel.replaymod.api.replay.holders.Category;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.elements.listeners.ProgressUpdateListener;
import eu.crushedpixel.replaymod.gui.replayviewer.GuiReplayViewer;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.registry.KeybindRegistry;
import eu.crushedpixel.replaymod.registry.ResourceHelper;
import eu.crushedpixel.replaymod.utils.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

public class GuiUploadFile extends GuiScreen implements ProgressUpdateListener {

    private final Minecraft mc = Minecraft.getMinecraft();

    private final ResourceLocation textureResource;
    private DynamicTexture dynTex = null;

    private boolean initialized;

    private int columnWidth;
    private int columnRight;

    private GuiAdvancedTextField name, tags;
    private GuiToggleButton category;
    private GuiString serverIP, duration;
    private GuiAdvancedCheckBox hideServerIP;
    private GuiTextArea description;

    private ComposedElement content;

    private GuiTextField messageTextField;
    private GuiAdvancedButton startUploadButton, cancelUploadButton, backButton;
    private GuiProgressBar progressBar;

    private File replayFile;

    private ReplayMetaData metaData;
    private BufferedImage thumb;
    private boolean hasThumbnail;

    private FileUploader uploader = new FileUploader();

    private GuiReplayViewer parent;

    private boolean lockUploadButton = false;

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
                    hasThumbnail = true;
                }

                archive.close();
                correctFile = true;
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (archive != null) {
                        archive.close();
                    }
                } catch (IOException ignored) {}
            }
        }

        if(!correctFile) {
            Logger logger = LogManager.getLogger();
            logger.error("Invalid file provided to upload");
            mc.displayGuiScreen(parent);
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
        if(replayFile == null) {
            mc.displayGuiScreen(parent);
            return;
        }

        if(!AuthenticationHandler.isAuthenticated()) {
            mc.displayGuiScreen(new GuiLoginPrompt(parent, this, true).toMinecraft());
            return;
        }

        if (!initialized) {
            initialized = true;

            int y = 20;

            name = new GuiAdvancedTextField(fontRendererObj, 0, y, 0, 20);
            name.hint = I18n.format("replaymod.gui.upload.namehint");
            name.text = FilenameUtils.getBaseName(replayFile.getAbsolutePath());
            name.setMaxStringLength(30);
            y+=25;

            int secs = metaData.getDuration() / 1000;
            String durationString = I18n.format("replaymod.gui.duration") + String.format(": %02dm%02ds", secs / 60, secs % 60);
            duration = new GuiString(0, y, Color.WHITE, durationString);
            y+=15;

            hideServerIP = new GuiAdvancedCheckBox(0, y, I18n.format("replaymod.gui.upload.hideip"), false);
            hideServerIP.enabled = !metaData.isSingleplayer();
            y+=15;

            serverIP = new GuiString(0, y, Color.GRAY, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    if (hideServerIP.isChecked()){
                        return I18n.format("replaymod.gui.iphidden");
                    } else {
                        return metaData.getServerName();
                    }
                }
            });
            y+=15;

            category = new GuiToggleButton(0, 0, y, I18n.format("replaymod.category") + ": ", Category.stringValues());
            y+=25;

            tags = new GuiAdvancedTextField(fontRendererObj, 0, y, 0, 20);
            tags.setMaxStringLength(30);
            tags.hint = I18n.format("replaymod.gui.upload.tagshint");
            y+=20;

            description = new GuiTextArea(fontRendererObj, 0, name.yPosition, 0, y - name.yPosition, 1000, 100, 1000);
        }

        columnWidth = Math.min(200, (width - 60) / 3);
        int columnLeft = width / 2 - columnWidth / 2 * 3 - 10;
        int columnMiddle = width / 2 - columnWidth / 2;
        columnRight = width / 2 + columnWidth / 2 + 10;

        name.xPosition = columnLeft;
        name.width = columnWidth;

        duration.positionX = columnLeft;
        hideServerIP.xPosition = columnLeft - 1;
        serverIP.positionX = columnLeft;

        category.xPosition = columnLeft - 1;
        category.width = columnWidth + 2;

        tags.xPosition = columnLeft;
        tags.width = columnWidth;

        description.positionX = columnMiddle;
        description.setWidth(columnWidth);

        List<GuiElement> elements = new ArrayList<GuiElement>(Arrays.asList(
                name, category, tags, description, serverIP, duration
        ));
        if (!metaData.isSingleplayer()) {
            elements.add(hideServerIP);
        }
        content = new ComposedElement(elements.toArray(new GuiElement[elements.size()]));

        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = this.buttonList;
        if(startUploadButton == null) {
            List<GuiButton> bottomBar = new ArrayList<GuiButton>();
            startUploadButton = new GuiAdvancedButton(GuiConstants.UPLOAD_START_BUTTON, 0, 0, I18n.format("replaymod.gui.upload.start"));
            bottomBar.add(startUploadButton);

            cancelUploadButton = new GuiAdvancedButton(GuiConstants.UPLOAD_CANCEL_BUTTON, 0, 0, I18n.format("replaymod.gui.upload.cancel"));
            cancelUploadButton.enabled = false;
            bottomBar.add(cancelUploadButton);

            backButton = new GuiAdvancedButton(GuiConstants.UPLOAD_BACK_BUTTON, 0, 0, I18n.format("replaymod.gui.back"));
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
        if(button.id == GuiConstants.UPLOAD_BACK_BUTTON) {
            mc.displayGuiScreen(parent);
        } else if(button.id == GuiConstants.UPLOAD_START_BUTTON) {
            final String name = this.name.getText().trim();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String tagsRaw = tags.getText();
                        String[] split = tagsRaw.split(",");
                        List<String> tags = new ArrayList<String>();
                        for(String str : split) {
                            if(!tags.contains(str) && str.length() > 0) {
                                tags.add(str);
                            }
                        }

                        Category category = Category.values()[GuiUploadFile.this.category.getValue()];

                        String desc = StringUtils.join(description.getText(), '\n');

                        if(hideServerIP.isChecked()) {
                            File tmp = File.createTempFile("replay_hidden_ip", "mcpr");
                            File tmpMeta = File.createTempFile("metadata", "json");

                            ReplayMetaData newMetaData = metaData.copy();
                            newMetaData.removeServer();
                            ReplayFileIO.write(newMetaData, tmpMeta);

                            HashMap<String, File> toAdd = new HashMap<String, File>();
                            toAdd.put(ReplayFile.ENTRY_METADATA, tmpMeta);

                            FileUtils.copyFile(replayFile, tmp);
                            ReplayFileIO.addFilesToZip(tmp, toAdd);

                            uploader.uploadFile(GuiUploadFile.this, AuthenticationHandler.getKey(), name, tags, tmp, category, desc);

                            FileUtils.deleteQuietly(tmpMeta);
                            FileUtils.deleteQuietly(tmp);
                        } else {
                            uploader.uploadFile(GuiUploadFile.this, AuthenticationHandler.getKey(), name, tags, replayFile, category, desc);
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
            int height = columnWidth * 720 / 1280;
            Gui.drawScaledCustomSizeModalRect(columnRight, 20, 0, 0, 1280, 720, columnWidth, height, 1280, 720);

            if (!hasThumbnail) {
                KeyBinding keyBinding = KeybindRegistry.getKeyBinding(KeybindRegistry.KEY_THUMBNAIL);
                String str = I18n.format("replaymod.gui.upload.nothumbnail",
                        keyBinding == null ? "???" : GameSettings.getKeyDisplayString(keyBinding.getKeyCode()));
                int y = 20 + height + 10;
                fontRendererObj.drawSplitString(str, columnRight, y, columnWidth, Color.RED.getRGB());
            }
        }

        messageTextField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);

        progressBar.drawProgressBar();

        startUploadButton.drawOverlay(mc, mouseX, mouseY);

        content.draw(mc, mouseX, mouseY);
        content.drawOverlay(mc, mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        content.mouseClick(mc, mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        content.tick(mc);
    }

    @Override
    public void onGuiClosed() {
        ResourceHelper.freeAllResources();
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        org.lwjgl.util.Point mouse = MouseUtils.getMousePos();
        content.buttonPressed(mc, mouse.getX(), mouse.getY(), typedChar, keyCode);

        if(uploader.isUploading()) {
            startUploadButton.enabled = false;
        } else {
            validateStartButton();
        }
    }

    private void validateStartButton() {
        boolean enabled = true;
        if(name.getText().trim().length() < 5 || name.getText().trim().length() > 30) {
            enabled = false;
            name.setTextColor(Color.RED.getRGB());
            startUploadButton.hoverText = I18n.format("replaymod.gui.upload.error.name.length");
        } else if(!RegexUtils.isValid(RegexUtils.ALPHANUMERIC_SPACE_HYPHEN_UNDERSCORE, name.getText())) {
            enabled = false;
            name.setTextColor(Color.RED.getRGB());
            startUploadButton.hoverText = I18n.format("replaymod.gui.upload.error.name");
        } else if(!RegexUtils.isValid(RegexUtils.ALPHANUMERIC_COMMA, tags.getText())) {
            enabled = false;
            tags.setTextColor(Color.RED.getRGB());
            startUploadButton.hoverText = I18n.format("replaymod.gui.upload.error.tags");
        } else {
            name.setTextColor(Color.WHITE.getRGB());
            tags.setTextColor(Color.WHITE.getRGB());
            startUploadButton.hoverText = null;
        }

        if(lockUploadButton) enabled = false;

        startUploadButton.enabled = enabled;
    }

    public void onStartUploading() {
        startUploadButton.enabled = false;
        cancelUploadButton.enabled = true;
        backButton.enabled = false;
        category.enabled = false;
        name.setElementEnabled(false);
        description.enabled = false;
        messageTextField.setText(I18n.format("replaymod.gui.upload.uploading"));
        messageTextField.setTextColor(Color.WHITE.getRGB());
    }

    public void onFinishUploading(boolean success, String info) {
        validateStartButton();
        cancelUploadButton.enabled = false;
        backButton.enabled = true;
        category.enabled = true;
        name.setElementEnabled(true);
        description.enabled = true;
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

    @Override
    public void onProgressChanged(float progress) {
        if(progressBar != null) progressBar.setProgress(progress);
    }

    @Override
    public void onProgressChanged(float progress, String progressString) {}
}
