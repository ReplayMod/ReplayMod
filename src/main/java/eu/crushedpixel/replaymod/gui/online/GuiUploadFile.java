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
import eu.crushedpixel.replaymod.gui.replaymanager.ResourceHelper;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.utils.ImageUtils;

public class GuiUploadFile extends GuiScreen {

	private GuiTextField fileTitleInput;
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

		fileTitleInput = new GuiTextField(GuiConstants.UPLOAD_NAME_INPUT, fontRendererObj, 200, 21, 120, 20);
		String fname = FilenameUtils.getBaseName(replayFile.getAbsolutePath());
		fileTitleInput.setText(fname);


		categoryButton = new GuiButton(GuiConstants.UPLOAD_CATEGORY_BUTTON, 200, 80, "Category: "+category.toNiceString());
		categoryButton.width = 120;
		buttonList.add(categoryButton);

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
			b.xPosition = x;
			b.yPosition = height-30;
			b.width = w2-4;

			buttonList.add(b);

			i++;
		}

		startUploadButton.enabled = (!(fileTitleInput.getText().trim().length() < 5 || fileTitleInput.getText().trim().length() > 30 || 
				p.matcher(fileTitleInput.getText()).find()));
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
			mc.addScheduledTask(new Runnable() {
				@Override
				public void run() {
					try {
						uploader.uploadFile(AuthenticationHandler.getKey(), fileTitleInput.getText().trim(), replayFile, category);
					} catch (ApiException e) { //TODO: Error handling
						e.printStackTrace();
						mc.displayGuiScreen(new GuiMainMenu());
					} catch (RuntimeException e) {
						e.printStackTrace();
						mc.displayGuiScreen(new GuiMainMenu());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();

		drawString(fontRendererObj, metaData.getServerName(), 200, 50, Color.GRAY.getRGB());
		drawString(fontRendererObj, "Duration: "+String.format("%02dm%02ds",
				TimeUnit.MILLISECONDS.toMinutes(metaData.getDuration()),
				TimeUnit.MILLISECONDS.toSeconds(metaData.getDuration()) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(metaData.getDuration()))
				), 200, 65, Color.GRAY.getRGB());

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
			Gui.drawScaledCustomSizeModalRect(20, 20, 0, 0, 1280, 720, 57*3, 32*3, 1280, 720);
		}

		fileTitleInput.drawTextBox();

		super.drawScreen(mouseX, mouseY, partialTicks);

		this.drawRect(20, this.height-100, width-20, this.height-80, Color.BLACK.getRGB());
		this.drawRect(22, this.height-98, width-22, this.height-82, Color.WHITE.getRGB());

		int width = this.width-22 - 22;
		float w = width*uploader.getUploadProgress();

		this.drawRect(22, this.height-98, Math.round(22+w), this.height-82, Color.RED.getRGB());
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		fileTitleInput.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void updateScreen() {
		fileTitleInput.updateCursorCounter();
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
	}

	private static final Pattern p = Pattern.compile("[^a-z0-9 \\-_]", Pattern.CASE_INSENSITIVE);

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if(fileTitleInput.isFocused()) {
			fileTitleInput.textboxKeyTyped(typedChar, keyCode);
		}

		startUploadButton.enabled = (!(fileTitleInput.getText().trim().length() < 5 || fileTitleInput.getText().trim().length() > 30 || 
				p.matcher(fileTitleInput.getText()).find()));

	}
}
