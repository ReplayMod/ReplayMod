package eu.crushedpixel.replaymod.gui.online;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.input.Keyboard;

import com.google.gson.Gson;

import eu.crushedpixel.replaymod.api.client.ApiException;
import eu.crushedpixel.replaymod.api.client.FileUploader;
import eu.crushedpixel.replaymod.api.client.holders.Category;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.utils.ImageUtils;
import eu.crushedpixel.replaymod.utils.ResourceHelper;

public class GuiUploadFile extends GuiScreen {

	private GuiTextField fileTitleInput, tagInput, messageTextField, tagPlaceholder;
	private GuiButton categoryButton, startUploadButton, cancelUploadButton, backButton;

	private Gson gson = new Gson();

	private File replayFile;
	private ReplayMetaData metaData;
	private BufferedImage thumb;

	private FileUploader uploader = new FileUploader();

	private Category category = Category.MINIGAME;

	private final ResourceLocation textureResource;
	private DynamicTexture dynTex = null;

	private Minecraft mc = Minecraft.getMinecraft();

	private static final Pattern p = Pattern.compile("[^a-z0-9 \\-_]", Pattern.CASE_INSENSITIVE);
	private static final Pattern pt = Pattern.compile("[^a-z0-9,]", Pattern.CASE_INSENSITIVE);

	public GuiUploadFile(File file) {
		this.textureResource = new ResourceLocation("upload_thumbs/"+FilenameUtils.getBaseName(file.getAbsolutePath()));
		dynTex = null;

		boolean correctFile = false;
		this.replayFile = file;

		if(("."+FilenameUtils.getExtension(file.getAbsolutePath())).equals(ConnectionEventHandler.ZIP_FILE_EXTENSION)) {
			ZipFile archive = null;
			try {
				archive = new ZipFile(file);
				ZipArchiveEntry recfile = archive.getEntry("recording"+ConnectionEventHandler.TEMP_FILE_EXTENSION);
				ZipArchiveEntry metadata = archive.getEntry("metaData"+ConnectionEventHandler.JSON_FILE_EXTENSION);

				ZipArchiveEntry image = archive.getEntry("thumb");
				BufferedImage img = null;
				if(image != null) {
					InputStream is = archive.getInputStream(image);
					is.skip(7);
					BufferedImage bimg = ImageIO.read(is);
					if(bimg != null) {
						thumb = ImageUtils.scaleImage(bimg, new Dimension(1280, 720));
					}
				}

				InputStream is = archive.getInputStream(metadata);
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String json = br.readLine();

				metaData = gson.fromJson(json, ReplayMetaData.class);

				archive.close();
				correctFile = true;
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				if(archive != null) {
					try {
						archive.close();
					} catch (IOException e) {}
				}
			}
		}

		if(!correctFile) {
			System.out.println("Invalid file provided to upload");
			mc.displayGuiScreen(new GuiMainMenu()); //TODO: Error message
			replayFile = null;
			return;
		}

		//If thumb is null, set image to placeholder
		if(thumb == null) {
			try {
				thumb = ImageIO.read(MCPNames.class.getClassLoader().getResourceAsStream("default_thumb.jpg"));
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void initGui() {
		if(replayFile == null) return;

		if(fileTitleInput == null) {
			fileTitleInput = new GuiTextField(GuiConstants.UPLOAD_NAME_INPUT, fontRendererObj, (this.width/2)+20+10, 21, Math.min(200, this.width-20-260), 20);
			String fname = FilenameUtils.getBaseName(replayFile.getAbsolutePath());
			fileTitleInput.setText(fname);
			fileTitleInput.setMaxStringLength(30);
		} else {
			fileTitleInput.xPosition = (this.width/2)+20+10;
			//fileTitleInput.yPosition = 21;
			fileTitleInput.width = Math.min(200, this.width-20-260);
			//fileTitleInput.height = 20;
		}

		if(categoryButton == null) {
			categoryButton = new GuiButton(GuiConstants.UPLOAD_CATEGORY_BUTTON, (this.width/2)+20+10-1, 80, "Category: "+category.toNiceString());
			categoryButton.width = Math.min(202, this.width-20-260+2);
			buttonList.add(categoryButton);
		} else {
			categoryButton.xPosition = (this.width/2)+20+10-1;
		}

		if(startUploadButton == null) {
			List<GuiButton> bottomBar = new ArrayList<GuiButton>();
			startUploadButton = new GuiButton(GuiConstants.UPLOAD_START_BUTTON, 0, 0, "Start Upload");
			bottomBar.add(startUploadButton);

			cancelUploadButton = new GuiButton(GuiConstants.UPLOAD_CANCEL_BUTTON, 0, 0, "Cancel Upload");
			cancelUploadButton.enabled = false;
			bottomBar.add(cancelUploadButton);

			backButton = new GuiButton(GuiConstants.UPLOAD_BACK_BUTTON, 0, 0, "Back");
			bottomBar.add(backButton);

			int i = 0;
			for(GuiButton b : bottomBar) {
				int w = this.width - 30;
				int w2 = w/bottomBar.size();

				int x = 15+(w2*i);
				b.xPosition = x+2;
				b.yPosition = height-30;
				b.width = w2-4;

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
				int w2 = w/bottomBar.size();

				int x = 15+(w2*i);
				b.xPosition = x+2;
				b.yPosition = height-30;
				b.width = w2-4;

				buttonList.add(b);

				i++;
			}
		}

		if(messageTextField == null) {
			messageTextField = new GuiTextField(GuiConstants.UPLOAD_INFO_FIELD, fontRendererObj, 20, height-80, width-40, 20);
			messageTextField.setEnabled(true);
			messageTextField.setFocused(false);
			messageTextField.setMaxStringLength(Integer.MAX_VALUE);
		} else {
			messageTextField.yPosition = height-80;
			messageTextField.width = width-40;
		}

		if(tagInput == null) {
			tagInput = new GuiTextField(GuiConstants.UPLOAD_TAG_INPUT, fontRendererObj, (this.width/2)+20+10, 110, Math.min(200, this.width-20-260), 20);
			tagInput.setMaxStringLength(30);
		} else {
			tagInput.xPosition = (this.width/2)+20+10;
			tagInput.width = Math.min(200, this.width-20-260);
		}

		if(tagPlaceholder == null) {
			tagPlaceholder = new GuiTextField(GuiConstants.UPLOAD_TAG_PLACEHOLDER, fontRendererObj, (this.width/2)+20+10, 110, Math.min(200, this.width-20-260), 20);
			tagPlaceholder.setTextColor(Color.DARK_GRAY.getRGB());
			tagPlaceholder.setText("Tags separated by comma");
		} else {
			tagPlaceholder.xPosition = (this.width/2)+20+10;
			tagPlaceholder.width = Math.min(200, this.width-20-260);
		}

		validateStartButton();
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if(!button.enabled) return;
		if(button.id == GuiConstants.UPLOAD_CATEGORY_BUTTON) {
			category = category.next();
			categoryButton.displayString = "Category: "+category.toNiceString();
		} else if(button.id == GuiConstants.UPLOAD_BACK_BUTTON) {
			mc.displayGuiScreen(new GuiMainMenu());
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
						uploader.uploadFile(GuiUploadFile.this, AuthenticationHandler.getKey(), name, tags, replayFile, category);
					} catch (ApiException e) { //TODO: Error handling
						e.printStackTrace();
						//mc.displayGuiScreen(new GuiMainMenu());
					} catch (RuntimeException e) {
						e.printStackTrace();
						//mc.displayGuiScreen(new GuiMainMenu());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		} else if(button.id == GuiConstants.UPLOAD_CANCEL_BUTTON) {
			uploader.cancelUploading();
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();

		drawString(fontRendererObj, metaData.getServerName(), (this.width/2)+20+10, 50, Color.GRAY.getRGB());
		drawString(fontRendererObj, "Duration: "+String.format("%02dm%02ds",
				TimeUnit.MILLISECONDS.toMinutes(metaData.getDuration()),
				TimeUnit.MILLISECONDS.toSeconds(metaData.getDuration()) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(metaData.getDuration()))
				), (this.width/2)+20+10, 65, Color.GRAY.getRGB());

		drawCenteredString(fontRendererObj, "Upload File", this.width/2, 5, Color.WHITE.getRGB());

		//Draw thumbnail
		if(thumb != null) {
			if(dynTex == null) {
				dynTex = new DynamicTexture(thumb);
				mc.getTextureManager().loadTexture(textureResource, dynTex);
				dynTex.updateDynamicTexture();
				ResourceHelper.registerResource(textureResource);
			} 

			mc.getTextureManager().bindTexture(textureResource); //Will be freed by the ResourceHelper
			int wid = (this.width)/2;
			int hei = Math.round(wid*(720f/1280f));
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

		this.drawRect(19, this.height-52, width-19, this.height-37, Color.BLACK.getRGB());
		this.drawRect(21, this.height-50, width-21, this.height-39, Color.WHITE.getRGB());

		int width = this.width-21 - 21;
		float prog = uploader.getUploadProgress();
		float w = width*prog;

		this.drawRect(21, this.height-50, Math.round(21+w), this.height-39, Color.RED.getRGB());

		String perc = (int)Math.floor(prog*100)+"%";
		fontRendererObj.drawString(perc, this.width/2 - fontRendererObj.getStringWidth(perc)/2, this.height-48, Color.BLACK.getRGB());
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
		Keyboard.enableRepeatEvents(false);
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
		} else if(p.matcher(fileTitleInput.getText()).find()) {
			enabled = false;
			fileTitleInput.setTextColor(Color.RED.getRGB());
		} else if(pt.matcher(tagInput.getText()).find()) {
			enabled = false;
			tagInput.setTextColor(Color.RED.getRGB());
		} else {
			fileTitleInput.setTextColor(Color.WHITE.getRGB());
			tagInput.setTextColor(Color.WHITE.getRGB());
		}
		startUploadButton.enabled = enabled;
	}

	public void onStartUploading() {
		startUploadButton.enabled = false;
		cancelUploadButton.enabled = true;
		backButton.enabled = false;
		categoryButton.enabled = false;
		fileTitleInput.setEnabled(false);
		messageTextField.setText("Uploading...");
		messageTextField.setTextColor(Color.WHITE.getRGB());
	}

	public void onFinishUploading(boolean success, String info) {
		validateStartButton();
		cancelUploadButton.enabled = false;
		backButton.enabled = true;
		categoryButton.enabled = true;
		fileTitleInput.setEnabled(true);
		if(success) {
			messageTextField.setText("File has been successfully uploaded");
			messageTextField.setTextColor(Color.GREEN.getRGB());
		} else {
			messageTextField.setText(info);
			messageTextField.setTextColor(Color.RED.getRGB());
		}
	}
}
