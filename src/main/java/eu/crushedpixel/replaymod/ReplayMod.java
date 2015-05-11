package eu.crushedpixel.replaymod;

import eu.crushedpixel.replaymod.api.client.ApiClient;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.events.*;
import eu.crushedpixel.replaymod.localization.LocalizedResourcePack;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.registry.KeybindRegistry;
import eu.crushedpixel.replaymod.registry.ReplayFileAppender;
import eu.crushedpixel.replaymod.registry.UploadedFileHandler;
import eu.crushedpixel.replaymod.renderer.SafeEntityRenderer;
import eu.crushedpixel.replaymod.replay.ReplaySender;
import eu.crushedpixel.replaymod.settings.ReplaySettings;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

@Mod(modid = ReplayMod.MODID, version = ReplayMod.VERSION)
public class ReplayMod {

    //TODO: Set ReplayHandler replaying to false when replay is exited
    //TODO: Hide Titles upon hurrying

    //TODO: Show the player whether he has already uploaded a replay

    //TODO: help page

    //TODO: Add "Miscellaneous" Replay Category

    //XXX
    //Known Bugs
    //
    //Keyframes have problems with Linear Paths
    //Rain isn't working
    //Incompatible with Shaders Mod
    //

    public static final String MODID = "replaymod";
    public static final String VERSION = "0.0.1";
    public static final ApiClient apiClient = new ApiClient();
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static GuiReplayOverlay overlay = new GuiReplayOverlay();
    public static ReplaySettings replaySettings;
    public static Configuration config;
    public static boolean firstMainMenu = true;
    public static RecordingHandler recordingHandler;
    public static ChatMessageHandler chatMessageHandler = new ChatMessageHandler();
    public static KeyInputHandler keyInputHandler = new KeyInputHandler();
    public static ReplaySender replaySender;
    public static int TP_DISTANCE_LIMIT = 128;
    public static ReplayFileAppender replayFileAppender;
    public static UploadedFileHandler uploadedFileHandler;

    private static Field defaultResourcePacksField;
    static {
        try {
            defaultResourcePacksField = Minecraft.class.getDeclaredField(MCPNames.field("field_110449_ao"));
            defaultResourcePacksField.setAccessible(true);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // The instance of your mod that Forge uses.
    @Instance(value = "ReplayModID")
    public static ReplayMod instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        AuthenticationHandler.loadAuthkeyFromConfig();

        uploadedFileHandler = new UploadedFileHandler(event.getModConfigurationDirectory());

        replaySettings = new ReplaySettings();
        replaySettings.readValues();

        replayFileAppender = new ReplayFileAppender();
        replayFileAppender.start();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(new ConnectionEventHandler());
        MinecraftForge.EVENT_BUS.register(new GuiEventHandler());

        FMLCommonHandler.instance().bus().register(keyInputHandler);
        MinecraftForge.EVENT_BUS.register(new MouseInputHandler());

        recordingHandler = new RecordingHandler();
        FMLCommonHandler.instance().bus().register(recordingHandler);
        MinecraftForge.EVENT_BUS.register(recordingHandler);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        overlay = new GuiReplayOverlay();
        FMLCommonHandler.instance().bus().register(overlay);
        MinecraftForge.EVENT_BUS.register(overlay);

        TickAndRenderListener tarl = new TickAndRenderListener();
        FMLCommonHandler.instance().bus().register(tarl);
        MinecraftForge.EVENT_BUS.register(tarl);

        KeybindRegistry.initialize();

        try {
            mc.entityRenderer = new SafeEntityRenderer(mc, mc.entityRenderer);
        } catch(Exception e) {
            e.printStackTrace();
        }

        //clean up replay_recordings folder
        removeTmcprFiles();

        try {
            List rps = (List) defaultResourcePacksField.get(mc);
            rps.add(new LocalizedResourcePack());
            defaultResourcePacksField.set(mc, rps);
            mc.refreshResources();
        } catch(Exception e) {
            e.printStackTrace();
        }

        /*
        boolean auth = false;
        try {
            auth = AuthenticationHandler.hasDonated(Minecraft.getMinecraft().getSession().getPlayerID());
        } catch(Exception e) {
            JOptionPane.showMessageDialog(null, "Couldn't connect to the Replay Mod Server to verify whether you donated!");
            FMLCommonHandler.instance().exitJava(0, false);
        }

        if(!auth) {
            JOptionPane.showMessageDialog(null, "It seems like you didn't donate, so you can't use the Replay Mod yet.");
            FMLCommonHandler.instance().exitJava(0, false);
        }
        */
    }

    private void removeTmcprFiles() {
        File folder = ReplayFileIO.getReplayFolder();

        for(File f : folder.listFiles()) {
            if(("." + FilenameUtils.getExtension(f.getAbsolutePath())).equals(ConnectionEventHandler.TEMP_FILE_EXTENSION)) {
                f.delete();
            }
        }
    }
}
