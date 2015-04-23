package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.api.client.holders.FileInfo;
import eu.crushedpixel.replaymod.utils.ResourceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GuiReplayListEntry implements IGuiListEntry {

    private final DateFormat dateFormat = new SimpleDateFormat();
    boolean registered = false;
    private Minecraft minecraft = Minecraft.getMinecraft();
    private FileInfo fileInfo;
    private ResourceLocation textureResource;
    private DynamicTexture dynTex = null;
    private File imageFile;
    private BufferedImage image = null;
    private GuiReplayListExtended parent;

    public GuiReplayListEntry(GuiReplayListExtended parent, FileInfo fileInfo, File imageFile) {
        this.fileInfo = fileInfo;
        this.parent = parent;
        dynTex = null;
        this.imageFile = imageFile;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
        try {
            if(fileInfo.getName() == null || fileInfo.getName().length() < 1) {
                fileInfo.setName("No Name");
            }
            minecraft.fontRendererObj.drawString(fileInfo.getName(), x + 3, y + 1, 16777215);

            if(y < -slotHeight || y > parent.height) {
                if(registered) {
                    registered = false;
                    ResourceHelper.freeResource(textureResource);
                    textureResource = null;
                    image = null;
                    dynTex = null;
                }
                return;
            } else {
                if(!registered) {
                    textureResource = new ResourceLocation("thumbs/" + fileInfo.getName() + fileInfo.getId());
                    if(imageFile == null) {
                        image = ResourceHelper.getDefaultThumbnail();
                    } else {
                        image = ImageIO.read(imageFile);
                    }
                    dynTex = new DynamicTexture(image);
                    minecraft.getTextureManager().loadTexture(textureResource, dynTex);
                    dynTex.updateDynamicTexture();
                    ResourceHelper.registerResource(textureResource);
                    registered = true;
                }

                minecraft.getTextureManager().bindTexture(textureResource);
                Gui.drawScaledCustomSizeModalRect(x - 60, y, 0, 0, 1280, 720, 57, 32, 1280, 720);
            }

            List<String> list = new ArrayList<String>();
            list.add(fileInfo.getMetadata().getServerName() + " (" + dateFormat.format(new Date(fileInfo.getMetadata().getDate())) + ")");

            list.add(String.format("%02dm%02ds",
                    TimeUnit.MILLISECONDS.toMinutes(fileInfo.getMetadata().getDuration()),
                    TimeUnit.MILLISECONDS.toSeconds(fileInfo.getMetadata().getDuration()) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(fileInfo.getMetadata().getDuration()))
            ));

            for(int l1 = 0; l1 < Math.min(list.size(), 2); ++l1) {
                minecraft.fontRendererObj.drawString((String) list.get(l1), x + 3, y + 12 + minecraft.fontRendererObj.FONT_HEIGHT * l1, 8421504);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
    }

    @Override
    public boolean mousePressed(int p_148278_1_, int p_148278_2_,
                                int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_) {
        for(int slot = 0; slot < parent.getSize(); slot++) {
            if(parent.getListEntry(slot) == this) {
                parent.elementClicked(slot, false, p_148278_5_, p_148278_6_);
                break;
            }
        }

        return true;
    }

    @Override
    public void mouseReleased(int slotIndex, int x, int y, int mouseEvent,
                              int relativeX, int relativeY) {
    }

}
