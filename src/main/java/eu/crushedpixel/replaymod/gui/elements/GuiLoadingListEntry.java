package eu.crushedpixel.replaymod.gui.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended.IGuiListEntry;
import net.minecraft.client.resources.I18n;

import java.awt.*;

public class GuiLoadingListEntry implements IGuiListEntry {

    boolean registered = false;
    private final Minecraft mc = Minecraft.getMinecraft();
    private GuiReplayListExtended parent;
    private final String message = I18n.format("replaymod.gui.loading")+"...";

    public GuiLoadingListEntry(GuiReplayListExtended parent) {
        this.parent = parent;
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
        try {
            int width = mc.fontRendererObj.getStringWidth(message);
            mc.fontRendererObj.drawString(message, x+(listWidth/2)-(width/2), y + (slotHeight/2)-(mc.fontRendererObj.FONT_HEIGHT/2) - 7
                    , Color.LIGHT_GRAY.getRGB());

            String bubbles = System.currentTimeMillis() % 500 >= 250 ? "oOo" : "OoO";

            width = mc.fontRendererObj.getStringWidth(bubbles);
            mc.fontRendererObj.drawString(bubbles, x+(listWidth/2)-(width/2), y + (slotHeight/2)-(mc.fontRendererObj.FONT_HEIGHT/2) + 8
                    , Color.LIGHT_GRAY.getRGB());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {}

    @Override
    public boolean mousePressed(int p_148278_1_, int p_148278_2_, int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_) {
        return false;
    }

    @Override
    public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {}
}
