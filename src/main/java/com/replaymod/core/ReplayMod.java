package com.replaymod.core;

import com.google.common.util.concurrent.ListenableFutureTask;
import com.replaymod.replay.ReplaySender;
import com.replaymod.replaystudio.util.I18n;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.events.handlers.CrosshairRenderHandler;
import eu.crushedpixel.replaymod.events.handlers.GuiEventHandler;
import eu.crushedpixel.replaymod.events.handlers.MouseInputHandler;
import eu.crushedpixel.replaymod.events.handlers.TickAndRenderListener;
import eu.crushedpixel.replaymod.events.handlers.keyboard.KeyInputHandler;
import eu.crushedpixel.replaymod.registry.KeybindRegistry;
import eu.crushedpixel.replaymod.registry.ReplayFileAppender;
import eu.crushedpixel.replaymod.registry.UploadedFileHandler;
import eu.crushedpixel.replaymod.renderer.CustomObjectRenderer;
import eu.crushedpixel.replaymod.renderer.PathPreviewRenderer;
import eu.crushedpixel.replaymod.renderer.SpectatorRenderer;
import eu.crushedpixel.replaymod.settings.ReplaySettings;
import eu.crushedpixel.replaymod.sound.SoundHandler;
import eu.crushedpixel.replaymod.utils.OpenGLUtils;
import eu.crushedpixel.replaymod.utils.TooltipRenderer;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.*;
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
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Queue;

@Mod(modid = ReplayMod.MOD_ID, useMetadata = true)
public class ReplayMod {

    public static ModContainer getContainer() {
        return Loader.instance().getIndexedModList().get(MOD_ID);
    }

    @Getter(lazy = true)
    private static final String minecraftVersion = parseMinecraftVersion();
    private static String parseMinecraftVersion() {
        CrashReport crashReport = new CrashReport("", new Throwable());
        @SuppressWarnings("unchecked")
        List<CrashReportCategory.Entry> list = crashReport.getCategory().children;
        for (CrashReportCategory.Entry entry : list) {
            if ("Minecraft Version".equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Unknown";
    }

    public static final String MOD_ID = "replaymod";

    public static final ResourceLocation TEXTURE = new ResourceLocation("replaymod", "replay_gui.png");
    public static final int TEXTURE_SIZE = 256;

    private static final Minecraft mc = Minecraft.getMinecraft();

    @Deprecated
    public static GuiEventHandler guiEventHandler;
    @Deprecated
    public static ReplaySettings replaySettings;
    @Deprecated
    public static Configuration config;
    @Deprecated
    public static boolean firstMainMenu = true;
    @Deprecated
    public static ChatMessageHandler chatMessageHandler = new ChatMessageHandler();
    @Deprecated
    public static KeyInputHandler keyInputHandler = new KeyInputHandler();
    @Deprecated
    public static MouseInputHandler mouseInputHandler = new MouseInputHandler();
    @Deprecated
    public static ReplaySender replaySender;
    @Deprecated
    public static int TP_DISTANCE_LIMIT = 128;
    @Deprecated
    public static ReplayFileAppender replayFileAppender;
    @Deprecated
    public static UploadedFileHandler uploadedFileHandler;
    @Deprecated
    public static SpectatorRenderer spectatorRenderer;
    @Deprecated
    public static TooltipRenderer tooltipRenderer;
    @Deprecated
    public static PathPreviewRenderer pathPreviewRenderer;
    @Deprecated
    public static CustomObjectRenderer customObjectRenderer;
    @Deprecated
    public static SoundHandler soundHandler = new SoundHandler();
    @Deprecated
    public static CrosshairRenderHandler crosshairRenderHandler;

    private final KeyBindingRegistry keyBindingRegistry = new KeyBindingRegistry();
    private final SettingsRegistry settingsRegistry = new SettingsRegistry();

    // The instance of your mod that Forge uses.
    @Instance(MOD_ID)
    public static ReplayMod instance;

    public KeyBindingRegistry getKeyBindingRegistry() {
        return keyBindingRegistry;
    }

    public SettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    public File getReplayFolder() throws IOException {
        File folder = new File(replaySettings.getRecordingPath());
        FileUtils.forceMkdir(folder);
        return folder;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // Initialize the static OpenGL info field from the minecraft main thread
        // Unfortunately lwjgl uses static methods so we have to make use of magic init calls as well
        OpenGLUtils.init();

        I18n.setI18n(net.minecraft.client.resources.I18n::format);

        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        settingsRegistry.setConfiguration(config);

        uploadedFileHandler = new UploadedFileHandler(event.getModConfigurationDirectory());

        replaySettings = new ReplaySettings();

        replayFileAppender = new ReplayFileAppender();
        FMLCommonHandler.instance().bus().register(replayFileAppender);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(guiEventHandler = new GuiEventHandler());

        FMLCommonHandler.instance().bus().register(keyInputHandler);
        FMLCommonHandler.instance().bus().register(keyBindingRegistry);
        FMLCommonHandler.instance().bus().register(mouseInputHandler);
        MinecraftForge.EVENT_BUS.register(mouseInputHandler);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) throws IOException {
        settingsRegistry.save(); // Save default values to disk

        if(!FMLClientHandler.instance().hasOptifine())
            GameSettings.Options.RENDER_DISTANCE.setValueMax(64f);

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

        tooltipRenderer = new TooltipRenderer();

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
            /*
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
            } else {
                options.setExportCommandArgs(EncodingPreset.MP4DEFAULT.getCommandLineArgs());
            }

            options.setOutputFile(new File(String.valueOf(System.currentTimeMillis())));

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
            */

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
                        // TODO
//                        try {
//                            ReplayHandler.startReplay(file, false);
//                        } catch (Throwable t) {
//                            t.printStackTrace();
//                            FMLCommonHandler.instance().exitJava(1, false);
//                        }
//
//                        int index = 0;
//                        if (path != null) {
//                            for (KeyframeSet set : ReplayHandler.getKeyframeRepository()) {
//                                if (set.getName().equals(path)) {
//                                    break;
//                                }
//                                index++;
//                            }
//                            if (index >= ReplayHandler.getKeyframeRepository().length) {
//                                throw new IllegalArgumentException("No path named \"" + path + "\".");
//                            }
//                        } else if (ReplayHandler.getKeyframeRepository().length == 0) {
//                            throw new IllegalArgumentException("Replay file has no paths defined.");
//                        }
//                        ReplayHandler.useKeyframePresetFromRepository(index);
//
//                        System.out.println("Rendering started...");
//                        try {
//                            ReplayProcess.startReplayProcess(options, true);
//                        } catch (Throwable t) {
//                            t.printStackTrace();
//                            FMLCommonHandler.instance().exitJava(1, false);
//                        }
//                        if (mc.hasCrashed) {
//                            System.out.println(mc.crashReporter.getCompleteReport());
//                        }
//                        System.out.println("Rendering done. Shutting down...");
//                        mc.shutdown();
                    }
                }, null));
            }

            testIfMoeshAndExitMinecraft();
        }
    }

    public void runLater(Runnable runnable) {
        @SuppressWarnings("unchecked")
        Queue<ListenableFutureTask> tasks = mc.scheduledTasks;
        synchronized (mc.scheduledTasks) {
            tasks.add(ListenableFutureTask.create(runnable, null));
        }
    }

    public String getVersion() {
        return getContainer().getVersion();
    }

    private void testIfMoeshAndExitMinecraft() {
        if("currentPlayer".equals("Moesh")) {
            System.exit(-1);
        }
    }

    public Minecraft getMinecraft() {
        return mc;
    }

    public void printInfoToChat(String message, Object... args) {
        printToChat(false, message, args);
    }

    public void printWarningToChat(String message, Object... args) {
        printToChat(true, message, args);
    }

    private void printToChat(boolean warning, String message, Object... args) {
        if (ReplayMod.replaySettings.isShowNotifications()) {
            // Some nostalgia: "§8[§6Replay Mod§8]§r Your message goes here"
            ChatStyle coloredDarkGray = new ChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
            ChatStyle coloredGold = new ChatStyle().setColor(EnumChatFormatting.GOLD);
            IChatComponent text = new ChatComponentText("[").setChatStyle(coloredDarkGray)
                    .appendSibling(new ChatComponentTranslation("replaymod.title").setChatStyle(coloredGold))
                    .appendSibling(new ChatComponentText("] "))
                    .appendSibling(new ChatComponentTranslation(message, args).setChatStyle(new ChatStyle()
                            .setColor(warning ? EnumChatFormatting.RED : EnumChatFormatting.DARK_GREEN)));
            // Send message to chat GUI
            // The ingame GUI is initialized at startup, therefore this is possible before the client is connected
            mc.ingameGUI.getChatGUI().printChatMessage(text);
        }
    }
}
