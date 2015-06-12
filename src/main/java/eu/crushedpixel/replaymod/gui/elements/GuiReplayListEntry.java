package eu.crushedpixel.replaymod.gui.elements;

import com.mojang.realmsclient.gui.ChatFormatting;
import eu.crushedpixel.replaymod.api.replay.holders.Category;
import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;
import eu.crushedpixel.replaymod.registry.ResourceHelper;
import eu.crushedpixel.replaymod.utils.DurationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GuiReplayListEntry implements IGuiListEntry {

    private final DateFormat dateFormat = new SimpleDateFormat();
    boolean registered = false;
    private Minecraft mc = Minecraft.getMinecraft();
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
        boolean online = fileInfo.getDownloads() >= 0;

        try {
            if(fileInfo.getName() == null || fileInfo.getName().length() < 1) {
                fileInfo.setName("No Name");
            }
            mc.fontRendererObj.drawString(ChatFormatting.UNDERLINE.toString()+fileInfo.getName(), x + 3, y + 1, 16777215);

            int thumbnailOffset = -5;

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
                    mc.getTextureManager().loadTexture(textureResource, dynTex);
                    dynTex.updateDynamicTexture();
                    ResourceHelper.registerResource(textureResource);
                    registered = true;
                }

                mc.getTextureManager().bindTexture(textureResource);
                Gui.drawScaledCustomSizeModalRect(x - (int) (slotHeight * (16 / 9f)) + thumbnailOffset, y, 0, 0, 1280, 720, (int) (slotHeight * (16 / 9f)), slotHeight, 1280, 720);
            }

            if(online) {
                String downloads = fileInfo.getDownloads() + " ⬇";
                int downloadsWidth = mc.fontRendererObj.getStringWidth(downloads);

                Gui.drawRect(x + thumbnailOffset - 2 - downloadsWidth - 1, y + slotHeight - (mc.fontRendererObj.FONT_HEIGHT + 2) * 2,
                        x + thumbnailOffset, y + slotHeight - (mc.fontRendererObj.FONT_HEIGHT + 2), 0x80000000);

                mc.fontRendererObj.drawStringWithShadow(downloads, x + thumbnailOffset - 1 - downloadsWidth,
                        y + slotHeight - (mc.fontRendererObj.FONT_HEIGHT) * 2 - 2, Color.WHITE.getRGB());
            }

            if(fileInfo != null && fileInfo.getMetadata() != null) {
                String duration = DurationUtils.convertSecondsToShortString(fileInfo.getMetadata().getDuration() / 1000);
                int durationWidth = mc.fontRendererObj.getStringWidth(duration);

                Gui.drawRect(x + thumbnailOffset - 2 - durationWidth - 1, y + slotHeight - mc.fontRendererObj.FONT_HEIGHT - 2, x + thumbnailOffset, y + slotHeight,
                        0x80000000);

                mc.fontRendererObj.drawStringWithShadow(duration, x + thumbnailOffset - 1 - durationWidth,
                        y + slotHeight - mc.fontRendererObj.FONT_HEIGHT, Color.WHITE.getRGB());

                String serverName = fileInfo.getMetadata().getServerName();
                if(serverName == null) serverName = ChatFormatting.DARK_RED.toString()+I18n.format("replaymod.gui.iphidden");

                mc.fontRendererObj.drawStringWithShadow(serverName, x + 3, y + (online ? 25 : 13), Color.LIGHT_GRAY.getRGB());

                String dateRecorded = dateFormat.format(new Date(fileInfo.getMetadata().getDate()));
                int dateWidth = mc.fontRendererObj.getStringWidth(dateRecorded);
                mc.fontRendererObj.drawStringWithShadow(dateRecorded, x + (online ? listWidth - 9 - dateWidth : 3), y + (online ? 13 : 23), Color.LIGHT_GRAY.getRGB());


                if(online) {
                    String owner = I18n.format("replaymod.gui.center.author", ChatFormatting.GRAY.toString()+ChatFormatting.ITALIC, fileInfo.getOwner());

                    mc.fontRendererObj.drawStringWithShadow(ChatFormatting.RESET.toString()+owner, x + 3, y + 13, Color.WHITE.getRGB());
                }

                if(online) {
                    Category category = Category.fromId(fileInfo.getCategory());

                    mc.fontRendererObj.drawStringWithShadow(ChatFormatting.ITALIC.toString()+category.toNiceString(), x + 3, y + slotHeight - mc.fontRendererObj.FONT_HEIGHT, Color.GRAY.getRGB());
                }

                if(fileInfo.getRatings() != null) {
                    String thumbsString = ChatFormatting.GOLD.toString()+"⭑"+fileInfo.getFavorites()+ChatFormatting.GREEN.toString()+" ⬆"
                            + fileInfo.getRatings().getPositive() + ChatFormatting.RED.toString()+" ⬇" + fileInfo.getRatings().getNegative();
                    int stringWidth = mc.fontRendererObj.getStringWidth(thumbsString);

                    mc.fontRendererObj.drawString(thumbsString, x + listWidth - stringWidth - 5, y + slotHeight - mc.fontRendererObj.FONT_HEIGHT, Color.GREEN.getRGB());
                }
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
