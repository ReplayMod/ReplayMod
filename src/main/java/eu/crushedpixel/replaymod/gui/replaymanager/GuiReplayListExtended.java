package eu.crushedpixel.replaymod.gui.replaymanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.MathHelper;

import org.lwjgl.input.Mouse;

import eu.crushedpixel.replaymod.recording.ReplayMetaData;

public class GuiReplayListExtended extends GuiListExtended {

	private GuiReplayManager parent;
	public int selected = -1;
	
	public GuiReplayListExtended(GuiReplayManager parent, Minecraft mcIn, int p_i45010_2_,
			int p_i45010_3_, int p_i45010_4_, int p_i45010_5_, int p_i45010_6_) {
		super(mcIn, p_i45010_2_, p_i45010_3_, p_i45010_4_, p_i45010_5_, p_i45010_6_);
		this.parent = parent;
	}

	
	@Override
	protected void elementClicked(int slotIndex, boolean isDoubleClick,
			int mouseX, int mouseY) {
		super.elementClicked(slotIndex, isDoubleClick, mouseX, mouseY);
		this.selected = slotIndex;
		parent.setButtonsEnabled(true);
		if(isDoubleClick) {
			parent.loadReplay(slotIndex);
		}
	}

	
	@Override
	protected void drawSelectionBox(int p_148120_1_, int p_148120_2_, int p_148120_3_, int p_148120_4_)
    {
        int i1 = this.getSize();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        for (int j1 = 0; j1 < i1; ++j1)
        {
        	
            int k1 = p_148120_2_ + j1 * this.slotHeight + this.headerPadding;
            int l1 = this.slotHeight - 4;

            if (k1 > this.bottom || k1 + l1 < this.top)
            {
                this.func_178040_a(j1, p_148120_1_, k1);
            }

            if (this.showSelectionBox && selected == j1)
            {
                int i2 = this.left + (this.width / 2 - this.getListWidth() / 2);
                int j2 = this.left + this.width / 2 + this.getListWidth() / 2;
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.disableTexture2D();
                worldrenderer.startDrawingQuads();
                worldrenderer.setColorOpaque_I(8421504);
                worldrenderer.addVertexWithUV((double)i2, (double)(k1 + l1 + 2), 0.0D, 0.0D, 1.0D);
                worldrenderer.addVertexWithUV((double)j2, (double)(k1 + l1 + 2), 0.0D, 1.0D, 1.0D);
                worldrenderer.addVertexWithUV((double)j2, (double)(k1 - 2), 0.0D, 1.0D, 0.0D);
                worldrenderer.addVertexWithUV((double)i2, (double)(k1 - 2), 0.0D, 0.0D, 0.0D);
                worldrenderer.setColorOpaque_I(0);
                worldrenderer.addVertexWithUV((double)(i2 + 1), (double)(k1 + l1 + 1), 0.0D, 0.0D, 1.0D);
                worldrenderer.addVertexWithUV((double)(j2 - 1), (double)(k1 + l1 + 1), 0.0D, 1.0D, 1.0D);
                worldrenderer.addVertexWithUV((double)(j2 - 1), (double)(k1 - 1), 0.0D, 1.0D, 0.0D);
                worldrenderer.addVertexWithUV((double)(i2 + 1), (double)(k1 - 1), 0.0D, 0.0D, 0.0D);
                tessellator.draw();
                GlStateManager.enableTexture2D();
            }
            
            this.drawSlot(j1, p_148120_1_, k1, l1, p_148120_3_, p_148120_4_);
        }
    }

	
	private List<GuiReplayListEntry> entries = new ArrayList<GuiReplayListEntry>();
	
	public void clearEntries() {
		entries = new ArrayList<GuiReplayListEntry>();
	}
	
	public void addEntry(String fileName, ReplayMetaData metaData, File image) {
		entries.add(new GuiReplayListEntry(this, fileName, metaData, image));
	}

	@Override
	public GuiReplayListEntry getListEntry(int index) {
		return entries.get(index);
	}

	@Override
	protected int getSize() {
		return entries.size();
	}


	@Override
    public void handleMouseInput()
    {
        if (this.isMouseYWithinSlotBounds(this.mouseY))
        {
            if (Mouse.isButtonDown(0))
            {
                if (this.initialClickY == -1.0F)
                {
                	int i2 = this.getScrollBarX();
                    int i1 = i2 + 6;
                    
                    boolean flag = true;

                    if (this.mouseY >= this.top && this.mouseY <= this.bottom && this.mouseX <= i1)
                    {
                        int i = this.width / 2 - this.getListWidth() / 2;
                        int j = this.width / 2 + this.getListWidth() / 2;
                        int k = this.mouseY - this.top - this.headerPadding + (int)this.amountScrolled - 4;
                        int l = k / this.slotHeight;

                        if (this.mouseX >= i && this.mouseX <= j && l >= 0 && k >= 0 && l < this.getSize())
                        {
                            boolean flag1 = l == this.selectedElement && Minecraft.getSystemTime() - this.lastClicked < 250L;
                            this.elementClicked(l, flag1, this.mouseX, this.mouseY);
                            this.selectedElement = l;
                            this.lastClicked = Minecraft.getSystemTime();
                        }
                        else if (this.mouseX >= i && this.mouseX <= j && k < 0)
                        {
                            this.func_148132_a(this.mouseX - i, this.mouseY - this.top + (int)this.amountScrolled - 4);
                            flag = false;
                        }

                        if (this.mouseX >= i2 && this.mouseX <= i1)
                        {
                            this.scrollMultiplier = -1.0F;
                            int j1 = this.func_148135_f();

                            if (j1 < 1)
                            {
                                j1 = 1;
                            }

                            int k1 = (int)((float)((this.bottom - this.top) * (this.bottom - this.top)) / (float)this.getContentHeight());
                            k1 = MathHelper.clamp_int(k1, 32, this.bottom - this.top - 8);
                            this.scrollMultiplier /= (float)(this.bottom - this.top - k1) / (float)j1;
                        }
                        else
                        {
                            this.scrollMultiplier = 1.0F;
                        }

                        if (flag)
                        {
                            this.initialClickY = (float)this.mouseY;
                        }
                        else
                        {
                            this.initialClickY = -2.0F;
                        }
                    }
                    else
                    {
                        this.initialClickY = -2.0F;
                    }
                }
                else if (this.initialClickY >= 0.0F)
                {
                    this.amountScrolled -= ((float)this.mouseY - this.initialClickY) * this.scrollMultiplier;
                    this.initialClickY = (float)this.mouseY;
                }
            }
            else
            {
                this.initialClickY = -1.0F;
            }

            int l1 = Mouse.getEventDWheel();

            if (l1 != 0)
            {
                if (l1 > 0)
                {
                    l1 = -1;
                }
                else if (l1 < 0)
                {
                    l1 = 1;
                }

                this.amountScrolled += (float)(l1 * this.slotHeight / 2);
            }
        }
    }

	
}
