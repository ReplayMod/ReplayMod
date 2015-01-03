package eu.crushedpixel.replaymod.events;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldSettings.GameType;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.RenderWorldEvent;
import net.minecraftforge.event.entity.player.AchievementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerFlyableFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class ReplayTickHandler {

	private Minecraft mc = Minecraft.getMinecraft();

	@SubscribeEvent
	public void onPlayer(PlayerEvent event) {
		if(ReplayHandler.replayActive() && event.isCancelable()) {
			event.setCanceled(true);
		}
	}

	
	@SubscribeEvent
	public void onFovChange(FOVUpdateEvent event) {
		if(ReplayHandler.replayActive()) {
			event.newfov = 1f;
		}
	}

}
