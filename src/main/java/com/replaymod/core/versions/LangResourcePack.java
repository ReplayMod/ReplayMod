//#if MC>=11400
package com.replaymod.core.versions;

import com.google.gson.Gson;
import com.replaymod.core.ReplayMod;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.ResourceNotFoundException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private static final String NAME = "replaymod_lang";
    private static final Pattern JSON_FILE_PATTERN = Pattern.compile("^assets/" + ReplayMod.MOD_ID + "/lang/([a-z][a-z])_([a-z][a-z]).json$");
    private static final Pattern LANG_FILE_NAME_PATTERN = Pattern.compile("^([a-z][a-z])_([a-z][a-z]).lang$");

    //#if MC>=11400
    public static final String LEGACY_KEY_PREFIX = "replaymod.input.";
    private static final String FABRIC_KEY_FORMAT = "key." + ReplayMod.MOD_ID + ".%s";

    private final Path basePath;
    public LangResourcePack(Path basePath) {
        super(new File(NAME));
        this.basePath = basePath;
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

    @Override
    protected InputStream openFile(String path) throws IOException {
        if ("pack.mcmeta".equals(path)) {
            return new ByteArrayInputStream("{\"pack\": {\"description\": \"ReplayMod language files\", \"pack_format\": 4}}".getBytes(StandardCharsets.UTF_8));
        }

        Path langPath = langPath(path);
        if (langPath == null) throw new ResourceNotFoundException(this.base, path);

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
            properties.put(key, value);
        }

        return new ByteArrayInputStream(GSON.toJson(properties).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean containsFile(String path) {
        Path langPath = langPath(path);
        return langPath != null && Files.exists(langPath);
    }


    @Override
    public Collection<Identifier> findResources(
            ResourceType resourcePackType,
            //#if MC>=11500
            String namespace,
            //#endif
            String path,
            int maxDepth,
            Predicate<String> filter
    ) {
        if (resourcePackType == ResourceType.CLIENT_RESOURCES && "lang".equals(path)) {
            Path base = baseLangPath();
            //#if MC<11400
            //$$ if (base == null) return Collections.emptyList();
            //#endif
            try {
                return Files.walk(base, 1)
                        .skip(1)
                        .filter(Files::isRegularFile)
                        .map(Path::getFileName).map(Path::toString)
                        .map(LANG_FILE_NAME_PATTERN::matcher)
                        .filter(Matcher::matches)
                        .map(matcher -> String.format("%s_%s.json", matcher.group(1), matcher.group(1)))
                        .filter(filter::test)
                        .map(name -> new Identifier(ReplayMod.MOD_ID, "lang/" + name))
                        .collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
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
