package eu.crushedpixel.replaymod;

import com.google.common.util.concurrent.ListenableFutureTask;
import eu.crushedpixel.replaymod.api.ApiClient;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.events.*;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.KeyframeSet;
import eu.crushedpixel.replaymod.localization.LocalizedResourcePack;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.registry.*;
import eu.crushedpixel.replaymod.renderer.InvisibilityRender;
import eu.crushedpixel.replaymod.renderer.SafeEntityRenderer;
import eu.crushedpixel.replaymod.renderer.SpectatorRenderer;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplayProcess;
import eu.crushedpixel.replaymod.replay.ReplaySender;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.settings.ReplaySettings;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import eu.crushedpixel.replaymod.utils.TooltipRenderer;
import eu.crushedpixel.replaymod.video.frame.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
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
import java.io.IOException;
import java.util.Queue;

@Mod(modid = ReplayMod.MODID, version = ReplayMod.VERSION)
public class ReplayMod {

    public static final String MODID = "replaymod";
    public static final String VERSION = "0.0.1";
    public static final ApiClient apiClient = new ApiClient();
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static GuiEventHandler guiEventHandler;
    public static GuiReplayOverlay overlay;
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
    public static DownloadedFileHandler downloadedFileHandler;
    public static FavoritedFileHandler favoritedFileHandler;
    public static RatedFileHandler ratedFileHandler;
    public static SpectatorRenderer spectatorRenderer;
    public static TooltipRenderer tooltipRenderer;

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

        downloadedFileHandler = new DownloadedFileHandler();
        favoritedFileHandler = new FavoritedFileHandler();
        ratedFileHandler = new RatedFileHandler();

        replayFileAppender = new ReplayFileAppender();
        replayFileAppender.start();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        overlay = new GuiReplayOverlay();

        FMLCommonHandler.instance().bus().register(new ConnectionEventHandler());
        MinecraftForge.EVENT_BUS.register(guiEventHandler = new GuiEventHandler());

        FMLCommonHandler.instance().bus().register(keyInputHandler);
        MinecraftForge.EVENT_BUS.register(new MouseInputHandler());

        recordingHandler = new RecordingHandler();
        FMLCommonHandler.instance().bus().register(recordingHandler);
        MinecraftForge.EVENT_BUS.register(recordingHandler);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) throws IOException {
        GameSettings.Options.RENDER_DISTANCE.setValueMax(64f);

        overlay.register();

        TickAndRenderListener tarl = new TickAndRenderListener();
        FMLCommonHandler.instance().bus().register(tarl);
        MinecraftForge.EVENT_BUS.register(tarl);

        spectatorRenderer = new SpectatorRenderer();

        KeybindRegistry.initialize();

        try {
            mc.entityRenderer = new SafeEntityRenderer(mc, mc.entityRenderer);
        } catch(Exception e) {
            e.printStackTrace();
        }

        tooltipRenderer = new TooltipRenderer();

        mc.getRenderManager().skinMap.put("default", new InvisibilityRender(mc.getRenderManager()));
        mc.getRenderManager().skinMap.put("slim", new InvisibilityRender(mc.getRenderManager(), true));

        //clean up replay_recordings folder
        removeTmcprFiles();

        try {
            mc.defaultResourcePacks.add(new LocalizedResourcePack());
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


        if (System.getProperty("replaymod.render.file") != null) {
            final File file = new File(System.getProperty("replaymod.render.file"));
            if (!file.exists()) {
                throw new IOException("File \"" + file.getPath() + "\" not found.");
            }

            String renderDistance = System.getProperty("mc.renderdistance");
            if (renderDistance != null) {
                mc.gameSettings.renderDistanceChunks = Integer.parseInt(renderDistance);
            }

            final String path = System.getProperty("replaymod.render.path");
            String type = System.getProperty("replaymod.render.type");
            String quality = System.getProperty("replaymod.render.quality");
            String fps = System.getProperty("replaymod.render.fps");
            String waitForChunks = System.getProperty("replaymod.render.waitforchunks");
            String linearMovement = System.getProperty("replaymod.render.linearmovement");
            FrameRenderer renderer;
            if (type != null) {
                String[] parts = type.split(":");
                type = parts[0].toUpperCase();
                if ("NORMAL".equals(type) || "DEFAULT".equals(type)) {
                    renderer = new DefaultFrameRenderer();
                } else if ("TILED".equals(type)) {
                    if (parts.length < 3) {
                        throw new IllegalArgumentException("Tiled renderer requires width and height.");
                    }
                    int width = Integer.parseInt(parts[1]);
                    int height = Integer.parseInt(parts[2]);
                    renderer = new TilingFrameRenderer(width, height);
                } else if ("STEREO".equals(type) || "STEREOSCOPIC".equals(type)) {
                    renderer = new StereoscopicFrameRenderer();
                } else if ("CUBIC".equals(type)) {
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("Cubic renderer requires boolean for whether it's stable.");
                    }
                    renderer = new CubicFrameRenderer(Boolean.parseBoolean(parts[1]));
                } else if ("EQUIRECTANGULAR".equals(type)) {
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("Equirectangular renderer requires boolean for whether it's stable.");
                    }
                    renderer = new EquirectangularFrameRenderer(Boolean.parseBoolean(parts[1]));
                } else {
                    throw new IllegalArgumentException("Unknown type: " + parts[0]);
                }
            } else {
                renderer = new DefaultFrameRenderer();
            }
            final RenderOptions options = new RenderOptions(renderer);
            if (quality != null) {
                options.setQuality(Float.parseFloat(quality));
            }
            if (fps != null) {
                options.setFps(Integer.parseInt(fps));
            }
            if (waitForChunks != null) {
                options.setWaitForChunks(Boolean.parseBoolean(waitForChunks));
            }
            if (linearMovement != null) {
                options.setLinearMovement(Boolean.parseBoolean(linearMovement));
            }

            @SuppressWarnings("unchecked")
            Queue<ListenableFutureTask> tasks = mc.scheduledTasks;
            synchronized (mc.scheduledTasks) {
                tasks.add(ListenableFutureTask.create(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("Loading replay...");
                        ReplayHandler.startReplay(file, false);

                        int index = 0;
                        if (path != null) {
                            for (KeyframeSet set : ReplayHandler.getKeyframeRepository()) {
                                if (set.getName().equals(path)) {
                                    break;
                                }
                                index++;
                            }
                            if (index >= ReplayHandler.getKeyframeRepository().length) {
                                throw new IllegalArgumentException("No path named \"" + path + "\".");
                            }
                        } else if (ReplayHandler.getKeyframeRepository().length == 0) {
                            throw new IllegalArgumentException("Replay file has no paths defined.");
                        }
                        ReplayHandler.useKeyframePresetFromRepository(index);

                        System.out.println("Rendering started...");
                        ReplayProcess.startReplayProcess(options);
                        System.out.println("Rendering done. Shutting down...");
                        mc.shutdown();
                    }
                }, null));
            }

            testIfMoeshAndExitMinecraft();
        }
    }

    private void testIfMoeshAndExitMinecraft() {
        if("Moesh".equals("the Player")) {
            "EXIT EVERYTHING".toString();
        }
    }

    private void removeTmcprFiles() {
        File folder = ReplayFileIO.getReplayFolder();

        for(File f : folder.listFiles()) {
            if(("." + FilenameUtils.getExtension(f.getAbsolutePath())).equals(ReplayFile.TEMP_FILE_EXTENSION)) {
                f.delete();
            }
        }
    }
}
