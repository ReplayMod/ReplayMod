package eu.crushedpixel.replaymod.gui.replaymanager;

import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import eu.crushedpixel.replaymod.recording.ReplayMetaData;

public class GuiReplayListEntry implements IGuiListEntry {

	private Minecraft minecraft = Minecraft.getMinecraft();
	private final DateFormat dateFormat = new SimpleDateFormat();

	private ReplayMetaData metaData;
	private String fileName;

	private final ResourceLocation textureResource;
	private DynamicTexture dynTex = null;

	private BufferedImage image = null;

	public ReplayMetaData getMetaData() {
		return metaData;
	}

	public void setMetaData(ReplayMetaData metaData) {
		this.metaData = metaData;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	private GuiReplayListExtended parent;

	public GuiReplayListEntry(GuiReplayListExtended parent, String fileName, ReplayMetaData metaData, BufferedImage image) {
		this.metaData = metaData;
		this.fileName = fileName;
		this.parent = parent;
		this.textureResource = new ResourceLocation("thumbs/"+fileName);
		dynTex = null;
		this.image = image;
	}

	@Override
	public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
		minecraft.fontRendererObj.drawString(fileName, x + 3, y + 1, 16777215);

		//Draw thumbnail
		if(image != null) {
			if(dynTex == null) {
				dynTex = new DynamicTexture(image);
				minecraft.getTextureManager().loadTexture(textureResource, dynTex);
				dynTex.updateDynamicTexture();
				ResourceHelper.registerResource(textureResource);
			} 
	
			minecraft.getTextureManager().bindTexture(textureResource); //Will be freed by the ResourceHelper
			Gui.drawScaledCustomSizeModalRect(x-60, y, 0, 0, 1280, 720, 57, 32, 1280, 720);
		}

		List<String> list = new ArrayList<String>();
		list.add(metaData.getServerName()+" ("+dateFormat.format(new Date(metaData.getDate()))+")");

		list.add(String.format("%02dm%02ds",
				TimeUnit.MILLISECONDS.toMinutes(metaData.getDuration()),
				TimeUnit.MILLISECONDS.toSeconds(metaData.getDuration()) - 
				TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(metaData.getDuration()))
				));

		for (int l1 = 0; l1 < Math.min(list.size(), 2); ++l1) {
			minecraft.fontRendererObj.drawString((String)list.get(l1), x + 3, y + 12 + minecraft.fontRendererObj.FONT_HEIGHT * l1, 8421504);
		}


	}

	@Override
	public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {}

	@Override
	public boolean mousePressed(int p_148278_1_, int p_148278_2_,
			int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_) {
		for(int slot = 0; slot<parent.getSize(); slot++) {
			if(parent.getListEntry(slot) == this) {
				parent.elementClicked(slot, false, p_148278_5_, p_148278_6_);
				break;
			}
		}

		return true;
	}

	@Override
	public void mouseReleased(int slotIndex, int x, int y, int mouseEvent,
			int relativeX, int relativeY) {}

}
