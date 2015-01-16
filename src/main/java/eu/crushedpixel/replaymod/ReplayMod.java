package eu.crushedpixel.replaymod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import eu.crushedpixel.replaymod.events.GuiEventHandler;
import eu.crushedpixel.replaymod.events.GuiReplayOverlay;
import eu.crushedpixel.replaymod.events.RecordingHandler;
import eu.crushedpixel.replaymod.events.ReplayTickHandler;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.settings.ReplaySettings;

@Mod(modid = ReplayMod.MODID, version = ReplayMod.VERSION)
public class ReplayMod
{
	
	//TODO: Set ReplayHandler replaying to false when replay is exited
	
	//XXX
	//Known Bugs
	//
	//Keyframe removal doesn't seem to properly work
	//
	//
	//
	//
	
	
	public static final String MODID = "replaymod";
	public static final String VERSION = "0.0.1";
	
	public static GuiReplayOverlay overlay = new GuiReplayOverlay();
	
	public static ReplaySettings replaySettings = new ReplaySettings(0, true, true, true, false);
	public static Configuration config;
	
	public static RecordingHandler recordingHandler;
	
	public static int PLAYER_ID = -1;

	// The instance of your mod that Forge uses.
	@Instance(value = "ReplayModID")
	public static ReplayMod instance;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		
		Property recServer = config.get("settings", "enableRecordingServer", true, "Defines whether a recording should be started upon joining a server.");
		Property recSP = config.get("settings", "enableRecordingSingleplayer", true, "Defines whether a recording should be started upon joining a singleplayer world.");
		Property maxFileSize = config.get("settings", "maximumFileSize", 0, "The maximum File size (in MB) of a recording. 0 means unlimited.");
		Property showNot = ReplayMod.instance.config.get("settings", "showNotifications", true, "Defines whether notifications should be sent to the player.");
		Property linear = ReplayMod.instance.config.get("settings", "forceLinearPath", false, "Defines whether travelling paths should be linear instead of interpolated.");
		
		replaySettings = new ReplaySettings(maxFileSize.getInt(0), recServer.getBoolean(true), recSP.getBoolean(true), showNot.getBoolean(true), linear.getBoolean(false));
		
		config.save();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(new ConnectionEventHandler());
		MinecraftForge.EVENT_BUS.register(new GuiEventHandler());
		ReplayTickHandler tickHandler = new ReplayTickHandler();
		FMLCommonHandler.instance().bus().register(tickHandler);
		MinecraftForge.EVENT_BUS.register(tickHandler);
		
		recordingHandler = new RecordingHandler();
		FMLCommonHandler.instance().bus().register(recordingHandler);
		MinecraftForge.EVENT_BUS.register(recordingHandler);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		overlay = new GuiReplayOverlay();
		FMLCommonHandler.instance().bus().register(overlay);
		MinecraftForge.EVENT_BUS.register(overlay);
	}
}
