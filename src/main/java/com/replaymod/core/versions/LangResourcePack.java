//#if MC>=11400
package com.replaymod.core.versions;

import com.google.gson.Gson;
import com.replaymod.core.ReplayMod;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static de.johni0702.minecraft.gui.versions.MCVer.identifier;

//#if FABRIC>=1
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
//#else
//#endif

//#if MC>=12006
//$$ import net.minecraft.resource.ResourcePackInfo;
//$$ import net.minecraft.resource.ResourcePackSource;
//$$ import net.minecraft.text.Text;
//$$ import java.util.Optional;
//#endif

//#if MC>=11903
//$$ import java.util.Objects;
//$$ import net.minecraft.resource.InputSupplier;
//#endif

//#if MC>=11400
//#else
//$$ import net.minecraft.resources.IPackFinder;
//$$ import net.minecraft.resources.ResourcePackInfo;
//$$ import net.minecraftforge.fml.loading.moddiscovery.ModFile;
//$$ import net.minecraftforge.fml.packs.ModFileResourcePack;
//$$ import net.minecraftforge.fml.packs.ResourcePackLoader;
//#endif

/**
 * Resource pack which on-the-fly converts pre-1.13 language files into 1.13 json format.
 * Also duplicates `replaymod.input.*` bindings to `key.replaymod.*` as convention on Fabric.
 */
public class LangResourcePack extends AbstractFileResourcePack {
    private static final Gson GSON = new Gson();
    public static final String NAME = "replaymod_lang";
    private static final Pattern JSON_FILE_PATTERN = Pattern.compile("^assets/" + ReplayMod.MOD_ID + "/lang/([a-z][a-z])_([a-z][a-z]).json$");
    private static final Pattern LANG_FILE_NAME_PATTERN = Pattern.compile("^([a-z][a-z])_([a-z][a-z]).lang$");

    //#if MC>=11400
    public static final String LEGACY_KEY_PREFIX = "replaymod.input.";
    private static final String FABRIC_KEY_FORMAT = "key." + ReplayMod.MOD_ID + ".%s";

    private final Path basePath;
    public LangResourcePack() {
        //#if MC>=12006
        //$$ super(new ResourcePackInfo(NAME, Text.literal("ReplayMod Translations"), ResourcePackSource.NONE, Optional.empty()));
        //#elseif MC>=11903
        //$$ super(NAME, true);
        //#else
        super(new File(NAME));
        //#endif

        //#if FABRIC>=1
        ModContainer container = FabricLoader.getInstance().getModContainer(ReplayMod.MOD_ID).orElseThrow(IllegalAccessError::new);
        this.basePath = container.getRootPath();
        //#else
        //$$ this.basePath = null; // stub
        //#endif
    }
    //#else
    //$$ public LangResourcePack() {
    //$$     super(new File(NAME));
    //$$ }
    //$$
    //$$ private ModFileResourcePack getParent() {
    //$$     return ResourcePackLoader.getResourcePackFor(ReplayMod.MOD_ID).orElseThrow(() -> new RuntimeException("Failed to get ReplayMod resource pack!"));
    //$$ }
    //#endif

    private String langName(String path) {
        Matcher matcher = JSON_FILE_PATTERN.matcher(path);
        if (!matcher.matches()) return null;
        return String.format("%s_%s.lang", matcher.group(1), matcher.group(2).toUpperCase());
    }

    //#if MC>=11400
    private Path baseLangPath() {
        return basePath.resolve("assets").resolve(ReplayMod.MOD_ID).resolve("lang");
    }
    //#else
    //$$ private Path baseLangPath() {
    //$$     ModFileResourcePack parent = getParent();
    //$$     if (parent == null) return null;
    //$$     ModFile modFile = parent.getModFile();
    //$$     return modFile.getLocator().findPath(modFile, "assets", ReplayMod.MOD_ID, "lang");
    //$$ }
    //#endif

    private Path langPath(String path) {
        String langName = langName(path);
        if (langName == null) return null;
        Path basePath = baseLangPath();
        //#if MC<11400
        //$$ if (basePath == null) return null;
        //#endif
        return basePath.resolve(langName);
    }

    private String convertValue(String value) {
        return value;
    }

    //#if MC>=11903
    //$$ @Override
    //$$ public InputSupplier<InputStream> openRoot(String... segments) {
    //$$     byte[] bytes;
    //$$     try {
    //$$         bytes = readFile(String.join("/", segments));
    //$$     } catch (IOException e) {
    //$$         throw new RuntimeException(e);
    //$$     }
    //$$     if (bytes == null) {
    //$$         return null;
    //$$     }
    //$$     return () -> new ByteArrayInputStream(bytes);
    //$$ }
    //#endif

    //#if MC>=11903
    //$$ @Override
    //$$ public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
    //$$     return openRoot(type.getDirectory(), id.getNamespace(), id.getPath());
    //$$ }
    //#else
    @Override
    protected InputStream openFile(String path) throws IOException {
        byte[] bytes = readFile(path);
        if (bytes == null) {
            throw new net.minecraft.resource.ResourceNotFoundException(this.base, path);
        }
        return new ByteArrayInputStream(bytes);
    }
    //#endif

    private byte[] readFile(String path) throws IOException {
        if ("pack.mcmeta".equals(path)) {
            return "{\"pack\": {\"description\": \"ReplayMod language files\", \"pack_format\": 4}}".getBytes(StandardCharsets.UTF_8);
        }

        Path langPath = langPath(path);
        if (langPath == null) return null;

        List<String> langFile;
        try (InputStream in = Files.newInputStream(langPath)) {
            langFile = IOUtils.readLines(in, StandardCharsets.UTF_8);
        }

        Map<String, String> properties = new HashMap<>();
        for (String line : langFile) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            int i = line.indexOf('=');
            String key = line.substring(0, i);
            String value = line.substring(i + 1);
            value = convertValue(value);
            //#if MC>=11400
            if (key.startsWith(LEGACY_KEY_PREFIX)) {
                // Duplicating instead of just remapping as some other part of the UI may still rely on the old key
                properties.put(key, value);
                key = String.format(FABRIC_KEY_FORMAT, key.substring(LEGACY_KEY_PREFIX.length()));
            }
            //#endif
            //#if MC>=12109
            if (key.equals("replaymod.title")) {
                properties.put("key.category.replaymod.general", value);
            }
            //#endif
            properties.put(key, value);
        }

        return GSON.toJson(properties).getBytes(StandardCharsets.UTF_8);
    }

    //#if MC>=11903
    //#else
    @Override
    protected boolean containsFile(String path) {
        Path langPath = langPath(path);
        return langPath != null && Files.exists(langPath);
    }
    //#endif


    //#if MC>=11903
    //$$ @Override
    //$$ public void findResources(ResourceType type, String namespace, String prefix, ResultConsumer consumer) {
    //$$     findResources(type, prefix, id -> consumer.accept(id, () -> new ByteArrayInputStream(Objects.requireNonNull(readFile(id.getPath())))));
    //$$ }
    //#else
    @Override
    public Collection<Identifier> findResources(
            ResourceType resourcePackType,
            //#if MC>=11500
            String namespace,
            //#endif
            String path,
            //#if MC>=11900
            //$$ Predicate<Identifier> filter
            //#else
            int maxDepth,
            Predicate<String> pathFilter
            //#endif
    ) {
        //#if MC<11900
        Predicate<Identifier> filter = id -> pathFilter.test(id.getPath());
        //#endif

        List<Identifier> result = new ArrayList<>();
        findResources(resourcePackType, path, id -> {
            if (filter.test(id)) {
                result.add(id);
            }
        });
        return result;
    }
    //#endif

    private void findResources(ResourceType type, String path, Consumer<Identifier> consumer) {
        if (type != ResourceType.CLIENT_RESOURCES) return;
        if (!"lang".equals(path)) return;
        Path base = baseLangPath();
        //#if MC<11400
        //$$ if (base == null) return;
        //#endif
        try (Stream<Path> stream = Files.walk(base, 1)) {
            stream
                    .skip(1)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName).map(Path::toString)
                    .map(LANG_FILE_NAME_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> String.format("%s_%s.json", matcher.group(1), matcher.group(1)))
                    .map(name -> identifier(ReplayMod.MOD_ID, "lang/" + name))
                    .forEach(consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getNamespaces(ResourceType resourcePackType) {
        if (resourcePackType == ResourceType.CLIENT_RESOURCES) {
            return Collections.singleton("replaymod");
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() {}

    // Not needed on fabric, using MixinModResourcePackUtil instead.
    //#if MC<11400
    //$$ public static class Finder implements IPackFinder {
    //$$     @Override
    //$$     public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> packList, ResourcePackInfo.IFactory<T> factory) {
    //$$         packList.put(NAME, ResourcePackInfo.func_195793_a(NAME, true, LangResourcePack::new, factory, ResourcePackInfo.Priority.BOTTOM));
    //$$     }
    //$$ }
    //#endif
}
//#endif
