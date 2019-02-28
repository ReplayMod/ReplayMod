package com.replaymod.core;

import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.replaymod.core.gui.GuiReplaySettings;
import com.replaymod.core.gui.RestoreReplayGui;
import com.replaymod.core.handler.MainMenuHandler;
import com.replaymod.core.utils.OpenGLUtils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.util.I18n;
import de.johni0702.minecraft.gui.container.GuiScreen;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

//#if MC>=11300
import com.replaymod.core.versions.LangResourcePack;
import net.minecraft.resources.FolderPack;
import net.minecraft.resources.IResourcePack;
import net.minecraftforge.fml.javafmlmod.FMLModLoadingContext;
//#else
//$$ import net.minecraft.client.resources.FolderResourcePack;
//$$ import net.minecraft.client.resources.IResourcePack;
//#endif

//#if MC>=10904
import net.minecraft.util.text.*;
//#else
//$$ import net.minecraft.util.ChatComponentText;
//$$ import net.minecraft.util.ChatComponentTranslation;
//$$ import net.minecraft.util.ChatStyle;
//$$ import net.minecraft.util.EnumChatFormatting;
//$$ import net.minecraft.util.IChatComponent;
//#endif

//#if MC>=10800
//#if MC>=11300
import net.minecraft.client.GameSettings;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
//#else
//$$ import net.minecraft.client.settings.GameSettings;
//$$ import net.minecraftforge.fml.client.FMLClientHandler;
//$$ import net.minecraftforge.fml.common.Loader;
//$$ import net.minecraftforge.fml.common.Mod.EventHandler;
//$$ import net.minecraftforge.fml.common.Mod.Instance;
//$$ import net.minecraftforge.fml.common.ModContainer;
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//#endif
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
//#else
//$$ import cpw.mods.fml.common.Loader;
//$$ import cpw.mods.fml.common.Mod;
//$$ import cpw.mods.fml.common.Mod.EventHandler;
//$$ import cpw.mods.fml.common.Mod.Instance;
//$$ import cpw.mods.fml.common.ModContainer;
//$$ import cpw.mods.fml.common.event.FMLInitializationEvent;
//$$ import cpw.mods.fml.common.event.FMLPostInitializationEvent;
//$$ import cpw.mods.fml.common.event.FMLPreInitializationEvent;
//$$ import cpw.mods.fml.common.eventhandler.SubscribeEvent;
//$$ import cpw.mods.fml.common.gameevent.TickEvent;
//$$ import com.replaymod.replay.InputReplayTimer;
//$$
//$$ import java.util.ArrayDeque;
//#endif

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.FutureTask;

import static com.replaymod.core.versions.MCVer.*;

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
public class ReplayMod {

    public static ModContainer getContainer() {
        //#if MC>=11300
        return ModList.get().getModContainerById(MOD_ID).get();
        //#else
        //$$ return Loader.instance().getIndexedModList().get(MOD_ID);
        //#endif
    }

    @Getter(lazy = true)
    //#if MC>=11300
    private static final String minecraftVersion = "1.13"; // FIXME
    //#else
    //$$ private static final String minecraftVersion = Loader.MC_VERSION;
    //#endif

    public static final String MOD_ID = "replaymod";

    public static final ResourceLocation TEXTURE = new ResourceLocation("replaymod", "replay_gui.png");
    public static final int TEXTURE_SIZE = 256;

    private static final Minecraft mc = MCVer.getMinecraft();

    @Deprecated
    public static Configuration config;

    private final KeyBindingRegistry keyBindingRegistry = new KeyBindingRegistry();
    private final SettingsRegistry settingsRegistry = new SettingsRegistry();

    // The instance of your mod that Forge uses.
    //#if MC>=11300
    { instance = this; }
    //#else
    //$$ @Instance(MOD_ID)
    //#endif
    public static ReplayMod instance;

    private final List<Module> modules = new ArrayList<>();
    {
        modules.add(new ReplayModRecording(this));
        modules.add(new ReplayModReplay(this));
    }

    public KeyBindingRegistry getKeyBindingRegistry() {
        return keyBindingRegistry;
    }

    public SettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    public File getReplayFolder() throws IOException {
        String path = getSettingsRegistry().get(Setting.RECORDING_PATH);
        File folder = new File(path.startsWith("./") ? mcDataDir(getMinecraft()) : null, path);
        FileUtils.forceMkdir(folder);
        return folder;
    }

    //#if MC>=11300
    { FMLModLoadingContext.get().getModEventBus().addListener(this::preInit); }
    //#else
    //$$ @EventHandler
    //#endif
    public void preInit(FMLPreInitializationEvent event) {
        // Initialize the static OpenGL info field from the minecraft main thread
        // Unfortunately lwjgl uses static methods so we have to make use of magic init calls as well
        OpenGLUtils.init();

        I18n.setI18n(net.minecraft.client.resources.I18n::format);
        //#if MC>=11300
        MCVer.getMinecraft().resourcePackRepository.addPackFinder(new LangResourcePack.Finder());
        //#endif

        //#if MC>=11300
        config = new Configuration(new File(mcDataDir(mc), "configs/replaymod.cfg")); // FIXME where'd the suggestion go?
        //#else
        //$$ config = new Configuration(event.getSuggestedConfigurationFile());
        //#endif
        // FIXME forge config api is currently broken config.load();
        settingsRegistry.setConfiguration(config);

        modules.forEach(m -> m.preInit(event));
    }

    //#ifdef DEV_ENV
    static { // Note: even preInit is too late and we'd have to issue another resource reload
        @SuppressWarnings("unchecked")
        //#if MC>=11300
        List<IResourcePack> defaultResourcePacks = new ArrayList<>(); // FIXME: probably replaced with DownloadingPackFinder
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
                        return new ByteArrayInputStream(("{\"pack\": {\"description\": \"dummy pack for jGui resources in dev-env\", \"pack_format\": 1}}").getBytes(Charsets.UTF_8));
                    }
                    throw e;
                }
            }
        };
        defaultResourcePacks.add(jGuiResourcePack);
        //#if MC<=10710
        //$$ FolderResourcePack mainResourcePack = new FolderResourcePack(new File("../src/main/resources")) {
        //$$     @Override
        //$$     protected InputStream getInputStreamByName(String resourceName) throws IOException {
        //$$         try {
        //$$             return super.getInputStreamByName(resourceName);
        //$$         } catch (IOException e) {
        //$$             if ("pack.mcmeta".equals(resourceName)) {
        //$$                 return new ByteArrayInputStream(("{\"pack\": {\"description\": \"dummy pack for mod resources in dev-env\", \"pack_format\": 1}}").getBytes(Charsets.UTF_8));
        //$$             }
        //$$             throw e;
        //$$         }
        //$$     }
        //$$ };
        //$$ defaultResourcePacks.add(mainResourcePack);
        //#endif
    }
    //#endif

    //#if MC>=11300
    { FMLModLoadingContext.get().getModEventBus().addListener(this::init); }
    //#else
    //$$ @EventHandler
    //#endif
    public void init(FMLInitializationEvent event) {
        getSettingsRegistry().register(Setting.class);

        new MainMenuHandler().register();

        //#if MC<=10710
        //$$ FML_BUS.register(this); // For runLater(Runnable)
        //#endif
        FML_BUS.register(keyBindingRegistry);

        getKeyBindingRegistry().registerKeyBinding("replaymod.input.settings", 0, () -> {
            new GuiReplaySettings(null, settingsRegistry).display();
        });

        modules.forEach(m -> m.init(event));
    }

    //#if MC>=11300
    { FMLModLoadingContext.get().getModEventBus().addListener(this::postInit); }
    //#else
    //$$ @EventHandler
    //#endif
    public void postInit(FMLPostInitializationEvent event) {
        settingsRegistry.save(); // Save default values to disk

        // 1.7.10 crashes when render distance > 16
        //#if MC>=10800
        //if(!FMLClientHandler.instance().hasOptifine()) FIXME 1.13 update
            GameSettings.Options.RENDER_DISTANCE.setValueMax(64f);
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

        modules.forEach(m -> m.postInit(event));
    }

    /**
     * Set when the currently running code has been scheduled by runLater.
     * If this is the case, subsequent calls to runLater have to be delayed until all scheduled tasks have been
     * processed, otherwise a livelock may occur.
     */
    private boolean inRunLater = false;

    public void runLater(Runnable runnable) {
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
        //#if MC<10904
        //$$ @SuppressWarnings("unchecked")
        //#endif
        Queue<FutureTask<?>> tasks = mc.scheduledTasks;
        synchronized (mc.scheduledTasks) {
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
        //#if MC>=11300
        return getContainer().getModInfo().getVersion().toString();
        //#else
        //$$ return getContainer().getVersion();
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
            /* FIXME: needs RuntimeInvisibleParameterAnnotations workaround
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
            */
            mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation(message, args));
        }
    }

    public static abstract class Module {
        public void preInit(FMLPreInitializationEvent event) {}
        public void init(FMLInitializationEvent event) {}
        public void postInit(FMLPostInitializationEvent event) {}
    }
}
