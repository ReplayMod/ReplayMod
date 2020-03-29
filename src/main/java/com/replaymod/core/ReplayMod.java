package com.replaymod.core;

import com.google.common.io.Files;
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
import com.replaymod.replaystudio.studio.ReplayStudio;
import com.replaymod.replaystudio.us.myles.ViaVersion.api.protocol.ProtocolVersion;
import com.replaymod.replaystudio.util.I18n;
import com.replaymod.replaystudio.viaversion.ViaVersionPacketConverter;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import de.johni0702.minecraft.gui.container.GuiScreen;
import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.apache.commons.io.FileUtils;

//#if MC>=11400
import net.minecraft.client.options.Option;
import net.minecraft.resource.ResourcePackCreator;
import net.minecraft.resource.ResourcePackContainer;
import net.minecraft.util.NonBlockingThreadExecutor;
//#endif

//#if FABRIC>=1
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
//#else
//$$ import com.google.common.util.concurrent.ListenableFutureTask;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import java.util.Queue;
//$$ import java.util.concurrent.FutureTask;
//$$
//#if MC>=11300
//$$ import com.replaymod.core.versions.LangResourcePack;
//$$ import net.minecraft.resources.IPackFinder;
//$$ import net.minecraft.resources.ResourcePackInfo;
//$$ import net.minecraftforge.fml.DeferredWorkQueue;
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//$$ import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
//$$ import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//$$ import net.minecraftforge.versions.mcp.MCPVersion;
//#else
//$$ import net.minecraft.client.resources.IResourcePack;
//$$ import net.minecraftforge.common.config.Configuration;
//#endif
//$$
//#if MC>=10800
//$$ import net.minecraft.client.GameSettings;
//#endif
//$$
//#if MC>=11300
//$$ import net.minecraftforge.fml.ModList;
//#else
//$$ import net.minecraftforge.fml.common.Loader;
//$$ import net.minecraftforge.fml.common.Mod.EventHandler;
//$$ import net.minecraftforge.fml.common.Mod.Instance;
//$$ import net.minecraftforge.fml.common.event.FMLInitializationEvent;
//$$ import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
//$$ import static com.replaymod.core.versions.MCVer.FORGE_BUS;
//#if MC>=10800
//$$ import net.minecraftforge.fml.client.FMLClientHandler;
//#else
//$$ import com.replaymod.replay.InputReplayTimer;
//$$
//$$ import java.util.ArrayDeque;
//#endif
//#endif
//$$ import net.minecraftforge.fml.common.Mod;
//#if MC<11400
//$$ import net.minecraftforge.fml.common.gameevent.TickEvent;
//#endif
//#endif

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//#if FABRIC<=0
//#if MC>=11300
//$$ @Mod(ReplayMod.MOD_ID)
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
        //#if FABRIC>=1
        ClientModInitializer,
        //#endif
        Module
{

    @Getter(lazy = true)
    //#if MC>=11400
    private static final String minecraftVersion = MinecraftClient.getInstance().getGame().getVersion().getName();
    //#else
    //#if MC>=11300
    //$$ private static final String minecraftVersion = MCPVersion.getMCVersion();
    //#else
    //$$ private static final String minecraftVersion = Loader.MC_VERSION;
    //#endif
    //#endif

    public static final String MOD_ID = "replaymod";

    public static final Identifier TEXTURE = new Identifier("replaymod", "replay_gui.png");
    public static final int TEXTURE_SIZE = 256;

    private static final MinecraftClient mc = MCVer.getMinecraft();

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

    /**
     * Whether the current MC version is supported by the embedded ReplayStudio version.
     * If this is not the case (i.e. if this is variable true), any feature of the RM which depends on the ReplayStudio
     * lib will be disabled.
     *
     * Only supported on Fabric builds, i.e. will always be false / crash the game with Forge/pre-1.14 builds.
     * (specifically the code below and MCVer#getProtocolVersion make this assumption)
     */
    private boolean minimalMode;

    public ReplayMod() {
        I18n.setI18n(net.minecraft.client.resource.language.I18n::translate);

        //#if MC>=11400
        // Check Minecraft protocol version for compatibility
        if (!ProtocolVersion.isRegistered(MCVer.getProtocolVersion()) && !Boolean.parseBoolean(System.getProperty("replaymod.skipversioncheck", "false"))) {
            minimalMode = true;
        }
        //#endif

        //#if MC>=11400
        // Not needed on fabric, using MixinModResourcePackUtil instead. Could in theory also use it on 1.13 but it already works as is.
        //#else
        //#if MC>=11300
        //$$ DeferredWorkQueue.runLater(() -> MCVer.getMinecraft().getResourcePackList().addPackFinder(new LangResourcePack.Finder()));
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
        File folder = new File(path.startsWith("./") ? getMinecraft().runDirectory : null, path);
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
        DirectoryResourcePack jGuiResourcePack = new DirectoryResourcePack(new File("../jGui/src/main/resources")) {
            @Override
            protected InputStream openFile(String resourceName) throws IOException {
                try {
                    return super.openFile(resourceName);
                } catch (IOException e) {
                    if ("pack.mcmeta".equals(resourceName)) {
                        //#if MC>=11400
                        int version = 4;
                        //#else
                        //$$ int version = 1;
                        //#endif
                        return new ByteArrayInputStream(("{\"pack\": {\"description\": \"dummy pack for jGui resources in dev-env\", \"pack_format\": "
                                + version + "}}").getBytes(StandardCharsets.UTF_8));
                    }
                    throw e;
                }
            }
        };
        //#if MC>=11300
        mc.getResourcePackContainerManager().addCreator(new ResourcePackCreator() {
            @Override
            public <T extends ResourcePackContainer> void registerContainer(Map<String, T> map, ResourcePackContainer.Factory<T> factory) {
                map.put("jgui", ResourcePackContainer.of("jgui", true, () -> jGuiResourcePack, factory, ResourcePackContainer.InsertionPosition.BOTTOM));
            }
        });
        //#else
        //$$ @SuppressWarnings("unchecked")
        //$$ List<IResourcePack> defaultResourcePacks = ((MinecraftAccessor) mc).getDefaultResourcePacks();
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

    //#if FABRIC>=1
    @Override
    public void onInitializeClient() {
        modules.forEach(Module::initCommon);
        modules.forEach(Module::initClient);
        modules.forEach(m -> m.registerKeyBindings(keyBindingRegistry));
    }
    //#else
    //#if MC>=11300
    //$$ {
    //$$     FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLCommonSetupEvent event) -> modules.forEach(Module::initCommon));
    //$$     FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLClientSetupEvent event) -> modules.forEach(Module::initClient));
    //$$     FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLClientSetupEvent event) -> modules.forEach(m -> m.registerKeyBindings(keyBindingRegistry)));
    //$$ }
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
            Option.RENDER_DISTANCE.setMax(64f);
            //#else
            //$$ GameSettings.Options.RENDER_DISTANCE.setValueMax(64f);
            //#endif
        }
        //#endif

        testIfMoeshAndExitMinecraft();

        runPostStartup(() -> {
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
     * Execute the given runnable on the main client thread, returning only after it has been run (or after 30 seconds).
     */
    public void runSync(Runnable runnable) throws InterruptedException, ExecutionException, TimeoutException {
        //#if MC>=11400
        if (mc.isOnThread()) {
            runnable.run();
        } else {
            executor.executeFuture(() -> {
                runnable.run();
                return null;
            }).get(30, TimeUnit.SECONDS);
        }
        //#else
        //$$ if (mc.isCallingFromMinecraftThread()) {
        //$$     runnable.run();
        //$$ } else {
        //$$     FutureTask<Void> future = new FutureTask<>(runnable, null);
        //$$     runLater(future);
        //$$     future.get(30, TimeUnit.SECONDS);
        //$$ }
        //#endif
    }

    /**
     * Execute the given runnable after game has started (once the overlay has been closed).
     * Most importantly, it will run after resources (including language keys!) have been loaded.
     * Below 1.14, this is equivalent to {@link #runLater(Runnable)}.
     */
    public void runPostStartup(Runnable runnable) {
        runLater(new Runnable() {
            @Override
            public void run() {
                //#if MC>=11400
                if (getMinecraft().overlay != null) {
                    // delay until after resources have been loaded
                    runLater(this);
                    return;
                }
                //#endif
                runnable.run();
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
    private boolean inRenderTaskQueue = false;
    // Starting 1.14 MC clears the queue of scheduled tasks on disconnect.
    // This works fine for MC since it uses the queue only for packet handling but breaks our assumption that
    // stuff submitted via runLater is actually always run (e.g. recording might not be fully stopped because parts
    // of that are run via runLater and stopping the recording happens right around the time MC clears the queue).
    // Luckily, that's also the version where MC pulled out the executor implementation, so we can just spin up our own.
    public static class ReplayModExecutor extends NonBlockingThreadExecutor<Runnable> {
        private final Thread mcThread = Thread.currentThread();
        // Fail-fast in case we ever switch to async loading and forget to change this
        // (except for fabric 1.15+ because it loads the mod before the client thread is set)
        //#if FABRIC<1 || MC<11500
        { if (!MinecraftClient.getInstance().isOnThread()) throw new RuntimeException(); }
        //#endif

        private ReplayModExecutor(String string_1) {
            super(string_1);
        }

        @Override
        protected Runnable prepareRunnable(Runnable runnable) {
            return runnable;
        }

        @Override
        protected boolean canRun(Runnable runnable) {
            return true;
        }

        @Override
        protected Thread getThread() {
            return mcThread;
        }

        //#if FABRIC>=1 && MC<11500
        @Override
        public void send(Runnable runnable) {
            method_18858(runnable);
        }
        //#endif

        @Override
        public void executeTaskQueue() {
            super.executeTaskQueue();
        }
    }
    public final ReplayModExecutor executor = new ReplayModExecutor("Client/ReplayMod");
    //#endif

    public void runLater(Runnable runnable) {
        //#if MC>=11400
        if (mc.isOnThread() && inRunLater && !inRenderTaskQueue) {
            ((MinecraftAccessor) mc).getRenderTaskQueue().offer(() -> {
                inRenderTaskQueue = true;
                try {
                    runLater(runnable);
                } finally {
                    inRenderTaskQueue = false;
                }
            });
        } else {
            executor.method_18858(() -> {
                inRunLater = true;
                try {
                    runnable.run();
                } finally {
                    inRunLater = false;
                }
            });
        }
        //#else
        //$$ if (mc.isCallingFromMinecraftThread() && inRunLater) {
            //#if MC>=10800
            //$$ FORGE_BUS.register(new Object() {
            //$$     @SubscribeEvent
            //$$     public void onRenderTick(TickEvent.RenderTickEvent event) {
            //$$         if (event.phase == TickEvent.Phase.START) {
            //$$             runLater(runnable);
            //$$             FORGE_BUS.unregister(this);
            //$$         }
            //$$     }
            //$$ });
            //#else
            //$$ FML_BUS.register(new RunLaterHelper(runnable));
            //#endif
        //$$     return;
        //$$ }
        //#if MC>=10800
        //$$ Queue<FutureTask<?>> tasks = ((MinecraftAccessor) mc).getScheduledTasks();
        //$$ //noinspection SynchronizationOnLocalVariableOrMethodParameter
        //$$ synchronized (tasks) {
        //#else
        //$$ Queue<ListenableFutureTask<?>> tasks = scheduledTasks;
        //$$ synchronized (scheduledTasks) {
        //#endif
        //$$     tasks.add(ListenableFutureTask.create(() -> {
        //$$         inRunLater = true;
        //$$         try {
        //$$             runnable.run();
        //$$         } finally {
        //$$             inRunLater = false;
        //$$         }
        //$$     }, null));
        //$$ }
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
        //#if FABRIC>=1
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .orElseThrow(IllegalStateException::new)
                .getMetadata().getVersion().toString();
        //#else
        //#if MC>=11300
        //$$ return ModList.get().getModContainerById(MOD_ID).get().getModInfo().getVersion().toString();
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

    public MinecraftClient getMinecraft() {
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
            Style coloredDarkGray = new Style().setColor(Formatting.DARK_GRAY);
            Style coloredGold = new Style().setColor(Formatting.GOLD);
            Text text = new LiteralText("[").setStyle(coloredDarkGray)
                    .append(new TranslatableText("replaymod.title").setStyle(coloredGold))
                    .append(new LiteralText("] "))
                    .append(new TranslatableText(message, args).setStyle(new Style()
                            .setColor(warning ? Formatting.RED : Formatting.DARK_GREEN)));
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
            mc.inGameHud.getChatHud().addMessage(text);
        }
    }

    public GuiBackgroundProcesses getBackgroundProcesses() {
        return backgroundProcesses;
    }

    // This method is static because it depends solely on the environment, not on the actual RM instance.
    public static boolean isMinimalMode() {
        return ReplayMod.instance.minimalMode;
    }

    public static boolean isCompatible(int fileFormatVersion, int protocolVersion) {
        if (isMinimalMode()) {
            return protocolVersion == MCVer.getProtocolVersion();
        } else {
            return new ReplayStudio().isCompatible(fileFormatVersion, protocolVersion, MCVer.getProtocolVersion());
        }
    }
}
