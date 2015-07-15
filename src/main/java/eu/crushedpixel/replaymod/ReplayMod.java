package eu.crushedpixel.replaymod;

import com.google.common.util.concurrent.ListenableFutureTask;
import eu.crushedpixel.replaymod.api.ApiClient;
import eu.crushedpixel.replaymod.api.replay.holders.FileInfo;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.events.handlers.*;
import eu.crushedpixel.replaymod.events.handlers.keyboard.KeyInputHandler;
import eu.crushedpixel.replaymod.gui.online.GuiReplayDownloading;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.KeyframeSet;
import eu.crushedpixel.replaymod.localization.LocalizedResourcePack;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.online.urischeme.UriScheme;
import eu.crushedpixel.replaymod.recording.ConnectionEventHandler;
import eu.crushedpixel.replaymod.registry.*;
import eu.crushedpixel.replaymod.renderer.*;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplayProcess;
import eu.crushedpixel.replaymod.replay.ReplaySender;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.settings.ReplaySettings;
import eu.crushedpixel.replaymod.sound.SoundHandler;
import eu.crushedpixel.replaymod.timer.ReplayTimer;
import eu.crushedpixel.replaymod.utils.OpenGLUtils;
import eu.crushedpixel.replaymod.utils.ReplayFile;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import eu.crushedpixel.replaymod.utils.TooltipRenderer;
import lombok.Getter;
import eu.crushedpixel.replaymod.video.rendering.Pipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.settings.GameSettings;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.apache.commons.io.FileUtils.forceDelete;
import static org.apache.commons.io.FileUtils.listFiles;

@Mod(modid = ReplayMod.MODID, useMetadata = true)
public class ReplayMod {

    public static ModContainer getContainer() {
        return Loader.instance().getIndexedModList().get(MODID);
    }

    public static final String MODID = "replaymod";
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
    public static MouseInputHandler mouseInputHandler = new MouseInputHandler();
    public static ReplaySender replaySender;
    public static int TP_DISTANCE_LIMIT = 128;
    public static ReplayFileAppender replayFileAppender;
    public static UploadedFileHandler uploadedFileHandler;
    public static DownloadedFileHandler downloadedFileHandler;
    public static FavoritedFileHandler favoritedFileHandler;
    public static RatedFileHandler ratedFileHandler;
    public static SpectatorRenderer spectatorRenderer;
    public static TooltipRenderer tooltipRenderer;
    public static PathPreviewRenderer pathPreviewRenderer;
    public static CustomObjectRenderer customObjectRenderer;
    public static SoundHandler soundHandler = new SoundHandler();
    public static CrosshairRenderHandler crosshairRenderHandler;

    @Getter
    private static boolean latestModVersion = false;

    // The instance of your mod that Forge uses.
    @Instance(value = "ReplayModID")
    public static ReplayMod instance;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        try {
            UriScheme uriScheme = UriScheme.create();
            if (uriScheme == null) {
                throw new UnsupportedOperationException("OS not supported.");
            }
            uriScheme.install();
        } catch (Exception e) {
            System.err.println("Failed to install UriScheme handler:");
            e.printStackTrace();
        }

        // Initialize the static OpenGL info field from the minecraft main thread
        // Unfortunately lwjgl uses static methods so we have to make use of magic init calls as well
        OpenGLUtils.init();

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
        FMLCommonHandler.instance().bus().register(replayFileAppender);

        //check if latest mod version
        try {
            latestModVersion = apiClient.isVersionUpToDate(getContainer().getVersion());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        mc.timer = new ReplayTimer();
        overlay = new GuiReplayOverlay();

        FMLCommonHandler.instance().bus().register(new ConnectionEventHandler());
        MinecraftForge.EVENT_BUS.register(guiEventHandler = new GuiEventHandler());

        FMLCommonHandler.instance().bus().register(keyInputHandler);
        FMLCommonHandler.instance().bus().register(mouseInputHandler);
        MinecraftForge.EVENT_BUS.register(mouseInputHandler);

        recordingHandler = new RecordingHandler();
        FMLCommonHandler.instance().bus().register(recordingHandler);
        MinecraftForge.EVENT_BUS.register(recordingHandler);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) throws IOException {
        if(!FMLClientHandler.instance().hasOptifine())
            GameSettings.Options.RENDER_DISTANCE.setValueMax(64f);

        overlay.register();

        TickAndRenderListener tarl = new TickAndRenderListener();
        FMLCommonHandler.instance().bus().register(tarl);
        MinecraftForge.EVENT_BUS.register(tarl);

        spectatorRenderer = new SpectatorRenderer();

        customObjectRenderer = new CustomObjectRenderer();
        FMLCommonHandler.instance().bus().register(customObjectRenderer);
        MinecraftForge.EVENT_BUS.register(customObjectRenderer);

        pathPreviewRenderer = new PathPreviewRenderer();
        FMLCommonHandler.instance().bus().register(pathPreviewRenderer);
        MinecraftForge.EVENT_BUS.register(pathPreviewRenderer);

        crosshairRenderHandler = new CrosshairRenderHandler();
        FMLCommonHandler.instance().bus().register(crosshairRenderHandler);
        MinecraftForge.EVENT_BUS.register(crosshairRenderHandler);

        KeybindRegistry.initialize();

        try {
            mc.entityRenderer = new SafeEntityRenderer(mc, mc.entityRenderer);
        } catch(Exception e) {
            e.printStackTrace();
        }

        tooltipRenderer = new TooltipRenderer();

        @SuppressWarnings("unchecked")
        Map<String, RenderPlayer> skinMap = mc.getRenderManager().skinMap;
        skinMap.put("default", new InvisibilityRender(mc.getRenderManager()));
        skinMap.put("slim", new InvisibilityRender(mc.getRenderManager(), true));

        //clean up replay_recordings folder
        removeTmcprFiles();

        Thread localizedResourcePackLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    @SuppressWarnings("unchecked")
                    List<IResourcePack> defaultResourcePacks = mc.defaultResourcePacks;
                    defaultResourcePacks.add(new LocalizedResourcePack());
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            mc.refreshResources();
                        }
                    });
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }, "localizedResourcePackLoader");
        localizedResourcePackLoader.start();

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

            final String path = System.getProperty("replaymod.render.path");
            String type = System.getProperty("replaymod.render.type");
            String bitrate = System.getProperty("replaymod.render.bitrate");
            String fps = System.getProperty("replaymod.render.fps");
            String waitForChunks = System.getProperty("replaymod.render.waitforchunks");
            String linearMovement = System.getProperty("replaymod.render.linearmovement");
            String skyColor = System.getProperty("replaymod.render.skycolor");
            String width = System.getProperty("replaymod.render.width");
            String height = System.getProperty("replaymod.render.height");
            String exportCommand = System.getProperty("replaymod.render.exportcommand");
            String exportCommandArgs = System.getProperty("replaymod.render.exportcommandargs");
            final RenderOptions options = new RenderOptions();
            if (bitrate != null) {
                options.setBitrate(bitrate);
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
            if (skyColor != null) {
                if (skyColor.startsWith("0x")) {
                    options.setSkyColor(Integer.parseInt(skyColor.substring(2), 16));
                } else {
                    options.setSkyColor(Integer.parseInt(skyColor));
                }
            }
            if (width != null) {
                options.setWidth(Integer.parseInt(width));
            }
            if (height != null) {
                options.setHeight(Integer.parseInt(height));
            }

            if (exportCommand != null) {
                options.setExportCommand(exportCommand);
            }
            if (exportCommandArgs != null) {
                options.setExportCommandArgs(exportCommandArgs);
            }

            Pipelines.Preset pipelinePreset = Pipelines.Preset.DEFAULT;
            if (type != null) {
                String[] parts = type.split(":");
                type = parts[0].toUpperCase();
                if ("NORMAL".equals(type) || "DEFAULT".equals(type)) {
                    pipelinePreset = Pipelines.Preset.DEFAULT;
                } else if ("STEREO".equals(type) || "STEREOSCOPIC".equals(type)) {
                    pipelinePreset = Pipelines.Preset.STEREOSCOPIC;
                } else if ("CUBIC".equals(type)) {
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("Cubic renderer requires boolean for whether it's stable.");
                    }
                    pipelinePreset = Pipelines.Preset.CUBIC;
                } else if ("EQUIRECTANGULAR".equals(type)) {
                    if (parts.length < 2) {
                        throw new IllegalArgumentException("Equirectangular renderer requires boolean for whether it's stable.");
                    }
                    pipelinePreset = Pipelines.Preset.EQUIRECTANGULAR;
                } else {
                    throw new IllegalArgumentException("Unknown type: " + parts[0]);
                }
            }
            options.setMode(pipelinePreset);

            @SuppressWarnings("unchecked")
            Queue<ListenableFutureTask> tasks = mc.scheduledTasks;
            synchronized (mc.scheduledTasks) {
                tasks.add(ListenableFutureTask.create(new Runnable() {
                    @Override
                    public void run() {
                        String renderDistance = System.getProperty("replaymod.render.mc.renderdistance");
                        String clouds = System.getProperty("replaymod.render.mc.clouds");
                        if (renderDistance != null) {
                            mc.gameSettings.renderDistanceChunks = Integer.parseInt(renderDistance);
                        }
                        if (clouds != null) {
                            mc.gameSettings.clouds = Boolean.parseBoolean(clouds);
                        }

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

        new Thread(new Runnable() {
            @Override
            public void run() {
                ServerSocket serverSocket = null;
                try {
                    serverSocket = new ServerSocket(UriScheme.PROCESS_PORT);
                    while (!Thread.interrupted()) {
                        Socket clientSocket = serverSocket.accept();
                        try {
                            InputStream inputStream = clientSocket.getInputStream();
                            String replayId = IOUtils.toString(inputStream);
                            final int id = Integer.parseInt(replayId);
                            mc.addScheduledTask(new Runnable() {
                                @Override
                                public void run() {
                                    loadOnlineReplay(id);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            IOUtils.closeQuietly(clientSocket);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(serverSocket);
                }
            }
        }, "UriSchemeHandler").start();

        String replayId = System.getenv("replaymod.uri.replayid");
        if (replayId != null) {
            final int id = Integer.parseInt(replayId);
            @SuppressWarnings("unchecked")
            Queue<ListenableFutureTask> tasks = mc.scheduledTasks;
            synchronized (mc.scheduledTasks) {
                tasks.add(ListenableFutureTask.create(new Runnable() {
                    @Override
                    public void run() {
                        loadOnlineReplay(id);
                    }
                }, null));
            }
        }
    }

    private void loadOnlineReplay(int id) {
        File file = ReplayMod.downloadedFileHandler.getFileForID(id);
        if (file == null) {
            FileInfo info = new FileInfo(id, null, null, null, 0, 0, 0, String.valueOf(id), false, 0);
            new GuiReplayDownloading(info).display();
        } else {
            try {
                ReplayHandler.startReplay(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void testIfMoeshAndExitMinecraft() {
        if("currentPlayer".equals("Moesh")) {
            System.exit(-1);
        }
    }

    private void removeTmcprFiles() {
        try {
        File folder = ReplayFileIO.getReplayFolder();

        for (File file : listFiles(folder, new String[]{ReplayFile.TEMP_FILE_EXTENSION.substring(1)}, false)) {
            forceDelete(file);
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
