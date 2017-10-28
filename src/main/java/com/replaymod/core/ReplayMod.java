package com.replaymod.core;

import com.google.common.io.Files;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.replaymod.core.gui.GuiReplaySettings;
import com.replaymod.core.gui.RestoreReplayGui;
import com.replaymod.core.handler.MainMenuHandler;
import com.replaymod.core.utils.OpenGLUtils;
import com.replaymod.replaystudio.util.I18n;
import de.johni0702.minecraft.gui.container.GuiScreen;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Mod(modid = ReplayMod.MOD_ID,
        useMetadata = true,
        version = "@MOD_VERSION@",
        acceptedMinecraftVersions = "@MC_VERSION@",
        acceptableRemoteVersions = "*",
        updateJSON = "https://raw.githubusercontent.com/ReplayMod/ReplayMod/master/versions.json",
        guiFactory = "com.replaymod.core.gui.GuiFactory")
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
    public static Configuration config;

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
        String path = getSettingsRegistry().get(Setting.RECORDING_PATH);
        File folder = new File(path.startsWith("./") ? getMinecraft().mcDataDir : null, path);
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
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        getSettingsRegistry().register(Setting.class);

        new MainMenuHandler().register();

        MinecraftForge.EVENT_BUS.register(keyBindingRegistry);

        getKeyBindingRegistry().registerKeyBinding("replaymod.input.settings", 0, () -> {
            new GuiReplaySettings(null, settingsRegistry).display();
        });
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) throws IOException {
        settingsRegistry.save(); // Save default values to disk

        if(!FMLClientHandler.instance().hasOptifine())
            GameSettings.Options.RENDER_DISTANCE.setValueMax(64f);

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

    public void runLater(Runnable runnable) {
        if (mc.isCallingFromMinecraftThread() && inRunLater) {
            EventBus bus = MinecraftForge.EVENT_BUS;
            bus.register(new Object() {
                @SubscribeEvent
                public void onRenderTick(TickEvent.RenderTickEvent event) {
                    if (event.phase == TickEvent.Phase.START) {
                        runLater(runnable);
                        bus.unregister(this);
                    }
                }
            });
            return;
        }
        synchronized (mc.scheduledTasks) {
            mc.scheduledTasks.add(ListenableFutureTask.create(() -> {
                inRunLater = true;
                try {
                    runnable.run();
                } finally {
                    inRunLater = false;
                }
            }, null));
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
        if (getSettingsRegistry().get(Setting.NOTIFICATIONS)) {
            // Some nostalgia: "§8[§6Replay Mod§8]§r Your message goes here"
            Style coloredDarkGray = new Style().setColor(TextFormatting.DARK_GRAY);
            Style coloredGold = new Style().setColor(TextFormatting.GOLD);
            ITextComponent text = new TextComponentString("[").setStyle(coloredDarkGray)
                    .appendSibling(new TextComponentTranslation("replaymod.title").setStyle(coloredGold))
                    .appendSibling(new TextComponentString("] "))
                    .appendSibling(new TextComponentTranslation(message, args).setStyle(new Style()
                            .setColor(warning ? TextFormatting.RED : TextFormatting.DARK_GREEN)));
            // Send message to chat GUI
            // The ingame GUI is initialized at startup, therefore this is possible before the client is connected
            mc.ingameGUI.getChatGUI().printChatMessage(text);
        }
    }
}
