package com.replaymod.core;

import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.replaymod.compat.ReplayModCompat;
import com.replaymod.core.gui.GuiBackgroundProcesses;
import com.replaymod.core.gui.GuiReplaySettings;
import com.replaymod.core.gui.RestoreReplayGui;
import com.replaymod.core.handler.MainMenuHandler;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.versions.MCVer;
import com.replaymod.editor.ReplayModEditor;
import com.replaymod.extras.ReplayModExtras;
import com.replaymod.online.ReplayModOnline;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.render.ReplayModRender;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.util.I18n;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import de.johni0702.minecraft.gui.container.GuiScreen;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.io.FileUtils;

//#if MC>=11400
//$$ import com.github.steveice10.mc.protocol.MinecraftConstants;
//$$ import com.replaymod.core.versions.LangResourcePack;
//$$ import net.fabricmc.api.ClientModInitializer;
//$$ import net.fabricmc.loader.api.FabricLoader;
//$$ import net.minecraft.SharedConstants;
//$$ import net.minecraft.client.options.GameOption;
//$$ import net.minecraft.resource.DirectoryResourcePack;
//$$ import net.minecraft.resource.ResourcePackCreator;
//$$ import net.minecraft.resource.ResourcePackContainer;
//#else
import net.minecraftforge.eventbus.api.SubscribeEvent;

//#if MC>=11300
import com.replaymod.core.versions.LangResourcePack;
import net.minecraft.resources.FolderPack;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.versions.mcp.MCPVersion;
//#else
//$$ import net.minecraft.client.resources.FolderResourcePack;
//$$ import net.minecraft.client.resources.IResourcePack;
//$$ import net.minecraftforge.common.config.Configuration;
//#endif

//#if MC>=10800
import net.minecraft.client.GameSettings;
//#endif

//#if MC>=11300
import net.minecraftforge.fml.ModList;
//#else
//$$ import net.minecraftforge.fml.common.Loader;
//$$ import net.minecraftforge.fml.common.Mod.EventHandler;
//$$ import net.minecraftforge.fml.common.Mod.Instance;
//$$ import net.minecraftforge.fml.common.event.FMLInitializationEvent;
//$$ import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
//#if MC>=10800
//$$ import net.minecraftforge.fml.client.FMLClientHandler;
//#else
//$$ import com.replaymod.replay.InputReplayTimer;
//$$
//$$ import java.util.ArrayDeque;
//#endif
//#endif
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.gameevent.TickEvent;
//#endif

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.FutureTask;

import static com.replaymod.core.versions.MCVer.*;

//#if MC<11400
//#if MC>=11300
@Mod(ReplayMod.MOD_ID)
//#else
//$$ @Mod(modid = ReplayMod.MOD_ID,
//$$         useMetadata = true,
//$$         version = "@MOD_VERSION@",
//$$         acceptedMinecraftVersions = "@MC_VERSION@",
//$$         acceptableRemoteVersions = "*",
        //#if MC>=10800
        //$$ clientSideOnly = true,
        //$$ updateJSON = "https://raw.githubusercontent.com/ReplayMod/ReplayMod/master/versions.json",
        //#endif
//$$         guiFactory = "com.replaymod.core.gui.GuiFactory")
//#endif
//#endif
public class ReplayMod implements
        //#if MC>=11400
        //$$ ClientModInitializer,
        //#endif
        Module
{

    @Getter(lazy = true)
    //#if MC>=11400
    //$$ private static final String minecraftVersion = MinecraftClient.getInstance().getGame().getVersion().getName();
    //#else
    //#if MC>=11300
    private static final String minecraftVersion = MCPVersion.getMCVersion();
    //#else
    //$$ private static final String minecraftVersion = Loader.MC_VERSION;
    //#endif
    //#endif

    public static final String MOD_ID = "replaymod";

    public static final ResourceLocation TEXTURE = new ResourceLocation("replaymod", "replay_gui.png");
    public static final int TEXTURE_SIZE = 256;

    private static final Minecraft mc = MCVer.getMinecraft();

    //#if MC<11300
    //$$ @Deprecated
    //$$ public static Configuration config;
    //#endif

    private final KeyBindingRegistry keyBindingRegistry = new KeyBindingRegistry();
    private final SettingsRegistry settingsRegistry = new SettingsRegistry();
    {
        settingsRegistry.register(Setting.class);
    }

    // The instance of your mod that Forge uses.
    //#if MC>=11300
    { instance = this; }
    //#else
    //$$ @Instance(MOD_ID)
    //#endif
    public static ReplayMod instance;

    private final List<Module> modules = new ArrayList<>();

    private final GuiBackgroundProcesses backgroundProcesses = new GuiBackgroundProcesses();

    public ReplayMod() {
        I18n.setI18n(net.minecraft.client.resources.I18n::format);

        //#if MC>=11400
        //$$ // Check Minecraft protocol version for compatibility
        //$$ int supportedProtocol = MinecraftConstants.PROTOCOL_VERSION;
        //$$ int actualProtocol = SharedConstants.getGameVersion().getProtocolVersion();
        //$$ if (supportedProtocol != actualProtocol) {
        //$$     throw new UnsupportedOperationException(String.format(
        //$$             "Unsupported Minecraft version, supporting protocol version %s (%s) but actual version is %s (%s).",
        //$$             supportedProtocol, MinecraftConstants.GAME_VERSION,
        //$$             actualProtocol, SharedConstants.getGameVersion().getName()
        //$$     ));
        //$$ }
        //#endif

        //#if MC>=11400
        //$$ // Not needed on fabric, using MixinModResourcePackUtil instead. Could in theory also use it on 1.13 but it already works as is.
        //#else
        //#if MC>=11300
        DeferredWorkQueue.runLater(() -> MCVer.getMinecraft().getResourcePackList().addPackFinder(new LangResourcePack.Finder()));
        //#endif
        //#endif

        // Register all RM modules
        modules.add(this);
        modules.add(new ReplayModRecording(this));
        ReplayModReplay replayModule = new ReplayModReplay(this);
        modules.add(replayModule);
        modules.add(new ReplayModOnline(this, replayModule));
        modules.add(new ReplayModRender(this));
        modules.add(new ReplayModSimplePathing(this));
        modules.add(new ReplayModEditor(this));
        modules.add(new ReplayModExtras(this));
        modules.add(new ReplayModCompat());

        //#if MC>=11300
        settingsRegistry.register();
        //#endif
    }

    //#if MC<=11300
    //$$ @EventHandler
    //$$ public void init(FMLPreInitializationEvent event) {
    //$$     config = new Configuration(event.getSuggestedConfigurationFile());
    //$$     config.load();
    //$$     settingsRegistry.setConfiguration(config);
    //$$     settingsRegistry.save(); // Save default values to disk
    //$$ }
    //#endif

    public KeyBindingRegistry getKeyBindingRegistry() {
        return keyBindingRegistry;
    }

    public SettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    public File getReplayFolder() throws IOException {
        String path = getSettingsRegistry().get(Setting.RECORDING_PATH);
        File folder = new File(path.startsWith("./") ? getMinecraft().gameDir : null, path);
        FileUtils.forceMkdir(folder);
        return folder;
    }

    static { // Note: even preInit is too late and we'd have to issue another resource reload
        //#ifdef DEV_ENV
        //noinspection ConstantConditions
        if (true) {
        //#else
        //$$ //noinspection ConstantConditions
        //$$ if (false) {
        //#endif
        @SuppressWarnings("unchecked")
        //#if MC>=11300
        FolderPack jGuiResourcePack = new FolderPack(new File("../jGui/src/main/resources")) {
            @Override
            protected InputStream getInputStream(String resourceName) throws IOException {
                try {
                    return super.getInputStream(resourceName);
        //#else
        //$$ List<IResourcePack> defaultResourcePacks = mc.defaultResourcePacks;
        //$$ FolderResourcePack jGuiResourcePack = new FolderResourcePack(new File("../jGui/src/main/resources")) {
        //$$     @Override
        //$$     protected InputStream getInputStreamByName(String resourceName) throws IOException {
        //$$         try {
        //$$             return super.getInputStreamByName(resourceName);
        //#endif
                } catch (IOException e) {
                    if ("pack.mcmeta".equals(resourceName)) {
                        //#if MC>=11400
                        //$$ int version = 4;
                        //#else
                        int version = 1;
                        //#endif
                        return new ByteArrayInputStream(("{\"pack\": {\"description\": \"dummy pack for jGui resources in dev-env\", \"pack_format\": "
                                + version + "}}").getBytes(StandardCharsets.UTF_8));
                    }
                    throw e;
                }
            }
        };
        //#if MC>=11300
        mc.getResourcePackList().addPackFinder(new IPackFinder() {
            @Override
            public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> map, ResourcePackInfo.IFactory<T> factory) {
                map.put("jgui", ResourcePackInfo.func_195793_a("jgui", true, () -> jGuiResourcePack, factory, ResourcePackInfo.Priority.BOTTOM));
            }
        });
        //#else
        //$$ defaultResourcePacks.add(jGuiResourcePack);
        //#endif
        //#if MC<=10710
        //$$ FolderResourcePack mainResourcePack = new FolderResourcePack(new File("../src/main/resources")) {
        //$$     @Override
        //$$     protected InputStream getInputStreamByName(String resourceName) throws IOException {
        //$$         try {
        //$$             return super.getInputStreamByName(resourceName);
        //$$         } catch (IOException e) {
        //$$             if ("pack.mcmeta".equals(resourceName)) {
        //$$                 return new ByteArrayInputStream(("{\"pack\": {\"description\": \"dummy pack for mod resources in dev-env\", \"pack_format\": 1}}").getBytes(StandardCharsets.UTF_8));
        //$$             }
        //$$             throw e;
        //$$         }
        //$$     }
        //$$ };
        //$$ defaultResourcePacks.add(mainResourcePack);
        //#endif
    }}

    //#if MC>=11400
    //$$ @Override
    //$$ public void onInitializeClient() {
    //$$     modules.forEach(Module::initCommon);
    //$$     modules.forEach(Module::initClient);
    //$$     modules.forEach(m -> m.registerKeyBindings(keyBindingRegistry));
    //$$ }
    //#else
    //#if MC>=11300
    {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLCommonSetupEvent event) -> modules.forEach(Module::initCommon));
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLClientSetupEvent event) -> modules.forEach(Module::initClient));
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLClientSetupEvent event) -> modules.forEach(m -> m.registerKeyBindings(keyBindingRegistry)));
    }
    //#else
    //$$ @EventHandler
    //$$ public void init(FMLInitializationEvent event) {
    //$$    modules.forEach(Module::initCommon);
    //$$    modules.forEach(Module::initClient);
    //$$    modules.forEach(m -> m.registerKeyBindings(keyBindingRegistry));
    //$$ }
    //#endif
    //#endif

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.settings", 0, () -> {
            new GuiReplaySettings(null, settingsRegistry).display();
        });
    }

    @Override
    public void initClient() {
        new MainMenuHandler().register();

        //#if MC<=10710
        //$$ FML_BUS.register(this); // For runLater(Runnable)
        //#endif

        backgroundProcesses.register();
        keyBindingRegistry.register();

        // 1.7.10 crashes when render distance > 16
        //#if MC>=10800
        if (!MCVer.hasOptifine()) {
            //#if MC>=11400
            //$$ GameOption.RENDER_DISTANCE.setMax(64f);
            //#else
            GameSettings.Options.RENDER_DISTANCE.setValueMax(64f);
            //#endif
        }
        //#endif

        testIfMoeshAndExitMinecraft();

        runLater(() -> {
            // Cleanup deleted corrupted replays
            try {
                File[] files = getReplayFolder().listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getName().endsWith(".mcpr.del")) {
                            if (file.lastModified() + 2 * 24 * 60 * 60 * 1000 < System.currentTimeMillis()) {
                                FileUtils.deleteDirectory(file);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Restore corrupted replays
            try {
                File[] files = getReplayFolder().listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isDirectory() && file.getName().endsWith(".mcpr.tmp")) {
                            File origFile = new File(file.getParentFile(), Files.getNameWithoutExtension(file.getName()));
                            new RestoreReplayGui(GuiScreen.wrap(mc.currentScreen), origFile).display();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Set when the currently running code has been scheduled by runLater.
     * If this is the case, subsequent calls to runLater have to be delayed until all scheduled tasks have been
     * processed, otherwise a livelock may occur.
     */
    private boolean inRunLater = false;
    //#if MC>=11400
    //$$ private boolean inRenderTaskQueue = false;
    //#endif

    public void runLater(Runnable runnable) {
        //#if MC>=11400
        //$$ if (mc.isOnThread() && inRunLater && !inRenderTaskQueue) {
        //$$     ((MinecraftAccessor) mc).getRenderTaskQueue().offer(() -> {
        //$$         inRenderTaskQueue = true;
        //$$         try {
        //$$             runLater(runnable);
        //$$         } finally {
        //$$             inRenderTaskQueue = false;
        //$$         }
        //$$     });
        //$$ } else {
        //$$     mc.method_18858(() -> {
        //$$         inRunLater = true;
        //$$         try {
        //$$             runnable.run();
        //$$         } finally {
        //$$             inRunLater = false;
        //$$         }
        //$$     });
        //$$ }
        //#else
        if (mc.isCallingFromMinecraftThread() && inRunLater) {
            //#if MC>=10800
            FORGE_BUS.register(new Object() {
                @SubscribeEvent
                public void onRenderTick(TickEvent.RenderTickEvent event) {
                    if (event.phase == TickEvent.Phase.START) {
                        runLater(runnable);
                        FORGE_BUS.unregister(this);
                    }
                }
            });
            //#else
            //$$ FORGE_BUS.register(new RunLaterHelper(runnable));
            //#endif
            return;
        }
        //#if MC>=10800
        Queue<FutureTask<?>> tasks = ((MinecraftAccessor) mc).getScheduledTasks();
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (tasks) {
        //#else
        //$$ Queue<ListenableFutureTask<?>> tasks = scheduledTasks;
        //$$ synchronized (scheduledTasks) {
        //#endif
            tasks.add(ListenableFutureTask.create(() -> {
                inRunLater = true;
                try {
                    runnable.run();
                } finally {
                    inRunLater = false;
                }
            }, null));
        }
        //#endif
    }

    //#if MC<=10710
    //$$ // 1.7.10: Cannot use MC's because it is processed only during ticks (so not at all when replay is paused)
    //$$ private final Queue<ListenableFutureTask<?>> scheduledTasks = new ArrayDeque<>();
    //$$
    //$$ // in 1.7.10 apparently events can't be delivered to anonymous classes
    //$$ public class RunLaterHelper {
    //$$     private final Runnable runnable;
    //$$
    //$$     private RunLaterHelper(Runnable runnable) {
    //$$         this.runnable = runnable;
    //$$     }
    //$$
    //$$     @SubscribeEvent
    //$$     public void onRenderTick(TickEvent.RenderTickEvent event) {
    //$$         if (event.phase == TickEvent.Phase.START) {
    //$$             runLater(runnable);
    //$$             FML_BUS.unregister(this);
    //$$         }
    //$$     }
    //$$ }
    //$$
    //$$ @SubscribeEvent
    //$$ public void runScheduledTasks(InputReplayTimer.RunScheduledTasks event) {
    //$$     synchronized (scheduledTasks) {
    //$$         while (!scheduledTasks.isEmpty()) {
    //$$             scheduledTasks.poll().run();
    //$$         }
    //$$     }
    //$$ }
    //#endif

    public String getVersion() {
        //#if MC>=11400
        //$$ return FabricLoader.getInstance().getModContainer(MOD_ID)
        //$$         .orElseThrow(IllegalStateException::new)
        //$$         .getMetadata().getVersion().toString();
        //#else
        //#if MC>=11300
        return ModList.get().getModContainerById(MOD_ID).get().getModInfo().getVersion().toString();
        //#else
        //$$ return Loader.instance().getIndexedModList().get(MOD_ID).getVersion();
        //#endif
        //#endif
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
        if (getSettingsRegistry().get(Setting.NOTIFICATIONS)) {
            // Some nostalgia: "§8[§6Replay Mod§8]§r Your message goes here"
            //#if MC>=10904
            Style coloredDarkGray = new Style().setColor(TextFormatting.DARK_GRAY);
            Style coloredGold = new Style().setColor(TextFormatting.GOLD);
            ITextComponent text = new TextComponentString("[").setStyle(coloredDarkGray)
                    .appendSibling(new TextComponentTranslation("replaymod.title").setStyle(coloredGold))
                    .appendSibling(new TextComponentString("] "))
                    .appendSibling(new TextComponentTranslation(message, args).setStyle(new Style()
                            .setColor(warning ? TextFormatting.RED : TextFormatting.DARK_GREEN)));
            //#else
            //$$ ChatStyle coloredDarkGray = new ChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
            //$$ ChatStyle coloredGold = new ChatStyle().setColor(EnumChatFormatting.GOLD);
            //$$ IChatComponent text = new ChatComponentText("[").setChatStyle(coloredDarkGray)
            //$$         .appendSibling(new ChatComponentTranslation("replaymod.title").setChatStyle(coloredGold))
            //$$         .appendSibling(new ChatComponentText("] "))
            //$$         .appendSibling(new ChatComponentTranslation(message, args).setChatStyle(new ChatStyle()
            //$$                 .setColor(warning ? EnumChatFormatting.RED : EnumChatFormatting.DARK_GREEN)));
            //#endif
            // Send message to chat GUI
            // The ingame GUI is initialized at startup, therefore this is possible before the client is connected
            mc.ingameGUI.getChatGUI().printChatMessage(text);
        }
    }

    public GuiBackgroundProcesses getBackgroundProcesses() {
        return backgroundProcesses;
    }
}
