package com.replaymod.core;

import com.google.common.net.PercentEscaper;
import com.replaymod.compat.ReplayModCompat;
import com.replaymod.core.gui.GuiBackgroundProcesses;
import com.replaymod.core.gui.GuiReplaySettings;
import com.replaymod.core.gui.RestoreReplayGui;
import com.replaymod.core.versions.MCVer;
import com.replaymod.core.versions.scheduler.Scheduler;
import com.replaymod.core.versions.scheduler.SchedulerImpl;
import com.replaymod.editor.ReplayModEditor;
import com.replaymod.extras.ReplayModExtras;
import com.replaymod.recording.ReplayModRecording;
import com.replaymod.render.ReplayModRender;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.lib.viaversion.api.protocol.version.ProtocolVersion;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import com.replaymod.replaystudio.util.I18n;
import com.replaymod.simplepathing.ReplayModSimplePathing;
import de.johni0702.minecraft.gui.container.GuiScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.options.Option;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

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

    public Path getReplayFolder() throws IOException {
        String str = getSettingsRegistry().get(Setting.RECORDING_PATH);
        return Files.createDirectories(getMinecraft().runDirectory.toPath().resolve(str));
    }

    /**
     * Folder into which replay backups are saved before the MarkerProcessor is unleashed.
     */
    public Path getRawReplayFolder() throws IOException {
        return Files.createDirectories(getReplayFolder().resolve("raw"));
    }

    /**
     * Folder into which replays are recorded.
     * Distinct from the main folder, so they cannot be opened while they are still saving.
     */
    public Path getRecordingFolder() throws IOException {
        return Files.createDirectories(getReplayFolder().resolve("recording"));
    }

    /**
     * Folder in which replay cache files are stored.
     * Distinct from the recording folder cause people kept confusing them with recordings.
     */
    public Path getCacheFolder() throws IOException {
        String str = getSettingsRegistry().get(Setting.CACHE_PATH);
        Path path = getMinecraft().runDirectory.toPath().resolve(str);
        Files.createDirectories(path);
        try {
            Files.setAttribute(path, "dos:hidden", true);
        } catch (UnsupportedOperationException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    private static final PercentEscaper CACHE_FILE_NAME_ENCODER = new PercentEscaper("-_ ", false);

    public Path getCachePathForReplay(Path replay) throws IOException {
        Path replayFolder = getReplayFolder();
        Path cacheFolder = getCacheFolder();
        Path relative = replayFolder.toAbsolutePath().relativize(replay.toAbsolutePath());
        return cacheFolder.resolve(CACHE_FILE_NAME_ENCODER.escape(relative.toString()));
    }

    public Path getReplayPathForCache(Path cache) throws IOException {
        String relative = URLDecoder.decode(cache.getFileName().toString(), "UTF-8");
        Path replayFolder = getReplayFolder();
        return replayFolder.resolve(relative);
    }

    public static final DirectoryResourcePack jGuiResourcePack = createJGuiResourcePack();
    public static final String JGUI_RESOURCE_PACK_NAME = "replaymod_jgui";
    private static DirectoryResourcePack createJGuiResourcePack() {
        File folder = new File("../jGui/src/main/resources");
        if (!folder.exists()) {
            return null;
        }
        return new DirectoryResourcePack(folder) {
            @Override
            //#if MC>=11400
            public String getName() {
            //#else
            //$$ public String getPackName() {
            //#endif
                return JGUI_RESOURCE_PACK_NAME;
            }

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
        //#if MC>=10800
        if (!MCVer.hasOptifine()) {
            Option.RENDER_DISTANCE.setMax(64f);
        }
        //#endif

        runPostStartup(() -> {
            final long DAYS = 24 * 60 * 60 * 1000;

            // Cleanup any cache folders still remaining in the recording folder (we once used to put them there)
            try {
                Files.walkFileTree(getReplayFolder(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        String name = dir.getFileName().toString();
                        if (name.endsWith(".mcpr.cache")) {
                            FileUtils.deleteDirectory(dir.toFile());
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return super.preVisitDirectory(dir, attrs);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Cleanup raw folder content three weeks after creation (these are pretty valuable for debugging)
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(getRawReplayFolder())) {
                for (Path path : paths) {
                    if (Files.getLastModifiedTime(path).toMillis() + 21 * DAYS < System.currentTimeMillis()) {
                        Files.delete(path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Move anything which is still in the recording folder into the regular replay folder
            // so it can be opened and/or recovered
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(getRecordingFolder())) {
                for (Path path : paths) {
                    Path destination = getReplayFolder().resolve(path.getFileName());
                    if (Files.exists(destination)) {
                        continue; // better play it save
                    }
                    Files.move(path, destination);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Cleanup cache folders 7 days after last modification or when its replay is gone
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(getCacheFolder())) {
                for (Path path : paths) {
                    if (Files.isDirectory(path)) {
                        Path replay = getReplayPathForCache(path);
                        long lastModified = Files.getLastModifiedTime(path).toMillis();
                        if (lastModified + 7 * DAYS < System.currentTimeMillis() || !Files.exists(replay)) {
                            FileUtils.deleteDirectory(path.toFile());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Cleanup deleted corrupted replays
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(getReplayFolder())) {
                for (Path path : paths) {
                    String name = path.getFileName().toString();
                    if (name.endsWith(".mcpr.del") && Files.isDirectory(path)) {
                        long lastModified = Files.getLastModifiedTime(path).toMillis();
                        if (lastModified + 2 * DAYS < System.currentTimeMillis()) {
                            FileUtils.deleteDirectory(path.toFile());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Restore corrupted replays
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(getReplayFolder())) {
                for (Path path : paths) {
                    String name = path.getFileName().toString();
                    if (name.endsWith(".mcpr.tmp") && Files.isDirectory(path)) {
                        Path original = path.resolveSibling(FilenameUtils.getBaseName(name));
                        Path noRecoverMarker = original.resolveSibling(original.getFileName() + ".no_recover");
                        if (Files.exists(noRecoverMarker)) {
                            // This file, when its markers are processed, doesn't actually result in any replays.
                            // So we don't really need to recover it either, let's just get rid of it.
                            FileUtils.deleteDirectory(path.toFile());
                            Files.delete(noRecoverMarker);
                            continue;
                        }
                        new RestoreReplayGui(this, GuiScreen.wrap(mc.currentScreen), original.toFile()).display();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Cleanup leftover no_recover files
            try (DirectoryStream<Path> paths = Files.newDirectoryStream(getReplayFolder())) {
                for (Path path : paths) {
                    String name = path.getFileName().toString();
                    if (name.endsWith(".no_recover")) {
                        Files.delete(path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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

    public ReplayFile openReplay(Path path) throws IOException {
        return openReplay(path, path);
    }

    public ReplayFile openReplay(Path input, Path output) throws IOException {
        return new ZipReplayFile(
                new ReplayStudio(),
                input != null ? input.toFile() : null,
                output.toFile(),
                getCachePathForReplay(output).toFile()
        );
    }
}
