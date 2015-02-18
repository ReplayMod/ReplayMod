package eu.crushedpixel.replaymod.gui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;

import com.mojang.realmsclient.util.Pair;

import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class GuiSpectateSelection extends GuiScreen {

	private List<Pair<EntityPlayer, ResourceLocation>> players;
	private int playerCount;
	private int upperPlayer = 0;

	private int upperBound = 30;
	private int lowerBound;
	
	private double prevSpeed;

	private class PlayerComparator implements Comparator<EntityPlayer> {

		@Override
		public int compare(EntityPlayer o1, EntityPlayer o2) {
			if(isSpectator(o1) && !isSpectator(o2)) {
				return 1;
			} else if(isSpectator(o2) && !isSpectator(o1)) {
				return -1;
			} else {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		}
		
	}
	
	private boolean isSpectator(EntityPlayer e) {
		return e.isInvisible() && e.getActivePotionEffect(Potion.invisibility) == null;
	}
	
	public GuiSpectateSelection(List<EntityPlayer> players) {
		this.prevSpeed = ReplayHandler.getSpeed();
		
		Collections.sort(players, new PlayerComparator());
		
		this.players = new ArrayList<Pair<EntityPlayer, ResourceLocation>>();

		for(EntityPlayer p : players) {
			ResourceLocation loc = new ResourceLocation("/temp-skins/"+p.getGameProfile().getName());
			AbstractClientPlayer.getDownloadImageSkin(loc, p.getName());
			this.players.add(Pair.of(p, loc));
		}

		playerCount = players.size();
		
		ReplayHandler.setSpeed(0);
	}

	private boolean drag = false;

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
			throws IOException {

		if(fitting < playerCount) {
			float visiblePerc = (float)fitting/(float)playerCount;

			int h = this.height-32-32;
			int offset = Math.round((upperPlayer/(fitting))*visiblePerc*h);

			int lower = Math.round(32+offset+(h*visiblePerc))-2;

			int k2 = (int)(this.width*0.4);

			if(mouseX >= k2-16 && mouseX <= k2-12 && mouseY >= 32-2+offset && mouseY <= lower) {
				lastY = mouseY;
				drag = true;
				return;
			}
		}
		int k2 = (int)(this.width*0.4);
		int l2 = 30;
		
		if(mouseX >= k2 && mouseX <= (this.width*0.6) && mouseY >= 30 && mouseY <= lowerBound) {
			int off = mouseY-30;
			int p = (off/21) + upperPlayer;
			ReplayHandler.spectateEntity(players.get(p).first());
			ReplayHandler.setSpeed(prevSpeed);
			mc.displayGuiScreen(null);
		}
	}

	private int lastY = 0;

	@Override
	protected void mouseClickMove(int mouseX, int mouseY,
			int clickedMouseButton, long timeSinceLastClick) {

		if(drag) {
			float step = 1f/(float)playerCount;

			int diff = mouseY-lastY;
			int h = this.height-32-32;

			float percDiff = (float)diff/(float)h;
			if(Math.abs(percDiff) > Math.abs(step)) {
				int s = (int)(percDiff/step);
				lastY = mouseY;
				upperPlayer += s;
				if(upperPlayer > playerCount-fitting) {
					upperPlayer = playerCount-fitting;
				} else if(upperPlayer < 0) {
					upperPlayer = 0;
				}
			}
		}

		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
	}
	
	@Override
	public void onGuiClosed() {
		ReplayHandler.setSpeed(prevSpeed);
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		drag = false;

		super.mouseReleased(mouseX, mouseY, state);
	}

	@Override
	public void initGui() {
		upperPlayer = 0;
		lowerBound = this.height-10;
	}

	private int fitting = 0;

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawCenteredString(fontRendererObj, "Spectate Player", this.width/2, 5, Color.WHITE.getRGB());
		int k2 = (int)(this.width*0.4);
		int l2 = 30;

		drawGradientRect(k2-10, l2-10, (int)(this.width*0.6)+20, this.height-30-2+10, -1072689136, -804253680);

		fitting = 0;

		int sk = 0;
		for(Pair<EntityPlayer, ResourceLocation> p : players) {
			if(sk < upperPlayer) {
				sk++;
				continue;
			}
			boolean spec = isSpectator(p.first());
			
			this.drawString(fontRendererObj, p.first().getName(), k2+16+5, l2+8-(fontRendererObj.FONT_HEIGHT/2), 
					spec ? Color.DARK_GRAY.getRGB() : Color.WHITE.getRGB());
			
			mc.getTextureManager().bindTexture(p.second());
			
			this.drawScaledCustomSizeModalRect(k2, l2, 8.0F, 8.0F, 8, 8, 16, 16, 64.0F, 64.0F);
			if(p.first().func_175148_a(EnumPlayerModelParts.HAT))
				Gui.drawScaledCustomSizeModalRect(k2, l2, 40.0F, 8.0F, 8, 8, 16, 16, 64.0F, 64.0F);

			GlStateManager.resetColor();
			
			l2 += 16+5;
			fitting++;
			if(l2+32 > lowerBound) {
				break;
			}
		}

		if(fitting < playerCount) {
			float visiblePerc = (float)fitting/(float)playerCount;

			int h = this.height-32-32;
			int offset = Math.round((upperPlayer/(fitting))*visiblePerc*h);

			int lower = Math.round(32+offset+(h*visiblePerc));

			this.drawRect(k2-18, 30-2, k2-10, this.height-30-2, Color.BLACK.getRGB());
			this.drawRect(k2-16, 32-2+offset, k2-12, lower-2, Color.LIGHT_GRAY.getRGB());
		} else {

		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
