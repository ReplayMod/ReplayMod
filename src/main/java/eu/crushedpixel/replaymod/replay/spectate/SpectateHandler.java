package eu.crushedpixel.replaymod.replay.spectate;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

import com.google.common.base.Predicate;

import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.gui.GuiSpectateSelection;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class SpectateHandler {

	private static Minecraft mc = Minecraft.getMinecraft();
	
	private static Predicate<EntityPlayer> playerPredicate = new Predicate<EntityPlayer>() {
		@Override
		public boolean apply(EntityPlayer input) {
			if(input instanceof CameraEntity || input == mc.thePlayer) return false;
			return true;
		}
	};
	
	public static void openSpectateSelection() {
		if(!ReplayHandler.isInReplay()) {
			return;
		}
		
		List<EntityPlayer> players = mc.theWorld.getEntities(EntityPlayer.class, playerPredicate);
		mc.displayGuiScreen(new GuiSpectateSelection(players));
	}
}
