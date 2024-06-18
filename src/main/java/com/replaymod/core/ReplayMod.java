package com.replaymod.core;

import com.replaymod.compat.ReplayModCompat;
import com.replaymod.core.files.ReplayFilesService;
import com.replaymod.core.files.ReplayFoldersService;
import com.replaymod.core.gui.GuiBackgroundProcesses;
import com.replaymod.core.gui.GuiReplaySettings;
import com.replaymod.core.versions.MCVer;
import com.replaymod.core.versions.scheduler.Scheduler;
import com.replaymod.core.versions.scheduler.SchedulerImpl;
import com.replaymod.editor.ReplayModEditor;
import com.replaymod.extras.ReplayModExtras;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.render.ReplayModRender;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.studio.ReplayStudio;
import com.replaymod.replaystudio.util.I18n;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

//#if MC>=12006
//$$ import net.minecraft.resource.ResourcePackInfo;
//$$ import net.minecraft.resource.ResourcePackSource;
//$$ import net.minecraft.text.Text;
//$$ import java.util.Optional;
//#endif

//#if MC>=11900
//#else
import net.minecraft.client.options.Option;
//#endif

public class ReplayMod implements Module, Scheduler {

    public static final String MOD_ID = "replaymod";

    public static final Identifier TEXTURE = new Identifier("replaymod", "replay_gui.png");
    public static final int TEXTURE_SIZE = 256;
    public static final Identifier LOGO_FAVICON = new Identifier("replaymod", "favicon_logo.png");

    private static final MinecraftClient mc = MCVer.getMinecraft();

    private final ReplayModBackend backend;
    private final SchedulerImpl scheduler = new SchedulerImpl();
    private final KeyBindingRegistry keyBindingRegistry = new KeyBindingRegistry();
    private final SettingsRegistry settingsRegistry = new SettingsRegistry();
    {
        settingsRegistry.register(Setting.class);
    }

    { instance = this; }
    public static ReplayMod instance;

    private final List<Module> modules = new ArrayList<>();

    private final GuiBackgroundProcesses backgroundProcesses = new GuiBackgroundProcesses();
    public final ReplayFoldersService folders = new ReplayFoldersService(settingsRegistry);
    public final ReplayFilesService files = new ReplayFilesService(folders);

    /**
     * Whether the current MC version is supported by the embedded ReplayStudio version.
     * If this is not the case (i.e. if this is variable true), any feature of the RM which depends on the ReplayStudio
     * lib will be disabled.
     *
     * Only supported on Fabric builds, i.e. will always be false / crash the game with Forge/pre-1.14 builds.
     * (specifically the code below and MCVer#getProtocolVersion make this assumption)
     */
    private boolean minimalMode;

    public ReplayMod(ReplayModBackend backend) {
        this.backend = backend;

        I18n.setI18n(net.minecraft.client.resource.language.I18n::translate);

        // Check Minecraft protocol version for compatibility
        if (!ProtocolVersion.isRegistered(MCVer.getProtocolVersion()) && !Boolean.parseBoolean(System.getProperty("replaymod.skipversioncheck", "false"))) {
            minimalMode = true;
        }

        // Register all RM modules
        modules.add(this);
        modules.add(new ReplayModRecording(this));
        ReplayModReplay replayModule = new ReplayModReplay(this);
        modules.add(replayModule);
        modules.add(new ReplayModRender(this));
        modules.add(new ReplayModSimplePathing(this));
        modules.add(new ReplayModEditor(this));
        modules.add(new ReplayModExtras(this));
        modules.add(new ReplayModCompat());

        settingsRegistry.register();
    }

    public KeyBindingRegistry getKeyBindingRegistry() {
        return keyBindingRegistry;
    }

    public SettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    public static final DirectoryResourcePack jGuiResourcePack = createJGuiResourcePack();
    public static final String JGUI_RESOURCE_PACK_NAME = "replaymod_jgui";
    private static DirectoryResourcePack createJGuiResourcePack() {
        File folder = new File("../jGui/src/main/resources");
        if (!folder.exists()) {
            folder = new File("../../../jGui/src/main/resources");
            if (!folder.exists()) {
                return null;
            }
        }
        //#if MC>=12006
        //$$ return new DirectoryResourcePack(new ResourcePackInfo(JGUI_RESOURCE_PACK_NAME, Text.literal("jGui"), ResourcePackSource.NONE, Optional.empty()), folder.toPath()) {
        //#elseif MC>=11903
        //$$ return new DirectoryResourcePack(JGUI_RESOURCE_PACK_NAME, folder.toPath(), true) {
        //#else
        return new DirectoryResourcePack(folder) {
        //#endif
            @Override
            //#if MC>=11400
            public String getName() {
            //#else
            //$$ public String getPackName() {
            //#endif
                return JGUI_RESOURCE_PACK_NAME;
            }

            //#if MC>=11903
            //$$ @Override
            //$$ public net.minecraft.resource.InputSupplier<InputStream> openRoot(String... segments) {
            //$$     if (segments.length == 1 && segments[0].equals("pack.mcmeta")) {
            //$$         return () -> new ByteArrayInputStream(generatePackMeta());
            //$$     }
            //$$     return super.openRoot(segments);
            //$$ }
            //#else
            @Override
            protected InputStream openFile(String resourceName) throws IOException {
                try {
                    return super.openFile(resourceName);
                } catch (IOException e) {
                    if ("pack.mcmeta".equals(resourceName)) {
                        return new ByteArrayInputStream(generatePackMeta());
                    }
                    throw e;
                }
            }
            //#endif

            private byte[] generatePackMeta() {
                //#if MC>=11400
                int version = 4;
                //#else
                //$$ int version = 1;
                //#endif
                return ("{\"pack\": {\"description\": \"dummy pack for jGui resources in dev-env\", \"pack_format\": "
                        + version + "}}").getBytes(StandardCharsets.UTF_8);
            }
        };
    }

    void initModules() {
        modules.forEach(Module::initCommon);
        modules.forEach(Module::initClient);
        modules.forEach(m -> m.registerKeyBindings(keyBindingRegistry));
    }

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("replaymod.input.settings", 0, () -> {
            new GuiReplaySettings(null, settingsRegistry).display();
        }, false);
    }

    @Override
    public void initClient() {
        backgroundProcesses.register();
        keyBindingRegistry.register();

        // 1.7.10 crashes when render distance > 16
        // Post 1.19 this has become non-trivial to do, install Sodium+Bobby or OptiFine if you need it
        //#if MC>=10800 && MC<11900
        if (!MCVer.hasOptifine()) {
            Option.RENDER_DISTANCE.setMax(64f);
        }
        //#endif

        runPostStartup(() -> files.initialScan(this));
    }

    @Override
    public void runSync(Runnable runnable) throws InterruptedException, ExecutionException, TimeoutException {
        scheduler.runSync(runnable);
    }

    @Override
    public void runPostStartup(Runnable runnable) {
        scheduler.runPostStartup(runnable);
    }

    @Override
    public void runLaterWithoutLock(Runnable runnable) {
        scheduler.runLaterWithoutLock(runnable);
    }

    @Override
    public void runLater(Runnable runnable) {
        scheduler.runLater(runnable);
    }

    @Override
    public void runTasks() {
        scheduler.runTasks();
    }

    public String getVersion() {
        return backend.getVersion();
    }

    public String getMinecraftVersion() {
        return backend.getMinecraftVersion();
    }

    public boolean isModLoaded(String id) {
        return backend.isModLoaded(id);
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
            //#if MC>=11600
            Style coloredDarkGray = Style.EMPTY.withColor(Formatting.DARK_GRAY);
            Style coloredGold = Style.EMPTY.withColor(Formatting.GOLD);
            Style alert = Style.EMPTY.withColor(warning ? Formatting.RED : Formatting.DARK_GREEN);
            //#else
            //$$ Style coloredDarkGray = new Style().setColor(Formatting.DARK_GRAY);
            //$$ Style coloredGold = new Style().setColor(Formatting.GOLD);
            //$$ Style alert = new Style().setColor(warning ? Formatting.RED : Formatting.DARK_GREEN);
            //#endif
            Text text = new LiteralText("[").setStyle(coloredDarkGray)
                    .append(new TranslatableText("replaymod.title").setStyle(coloredGold))
                    .append(new LiteralText("] "))
                    .append(new TranslatableText(message, args).setStyle(alert));
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
