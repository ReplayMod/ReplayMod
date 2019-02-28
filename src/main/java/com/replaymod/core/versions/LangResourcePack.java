//#if MC>=11300
package com.replaymod.core.versions;

import com.google.gson.Gson;
import com.replaymod.core.ReplayMod;
import net.minecraft.resources.AbstractResourcePack;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.IResourcePack;
import net.minecraft.resources.ResourcePackFileNotFoundException;
import net.minecraft.resources.ResourcePackInfo;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.packs.ModFileResourcePack;
import net.minecraftforge.fml.packs.ResourcePackLoader;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Resource pack which on-the-fly converts pre-1.13 language files into 1.13 json format.
 */
public class LangResourcePack extends AbstractResourcePack {
    private static final Gson GSON = new Gson();
    private static final String NAME = "replaymod_lang";
    private static final Pattern JSON_FILE_PATTERN = Pattern.compile("^assets/" + ReplayMod.MOD_ID + "/lang/([a-z][a-z])_([a-z][a-z]).json$");
    private static final Pattern LANG_FILE_NAME_PATTERN = Pattern.compile("^([a-z][a-z])_([a-z][a-z]).lang$");

    LangResourcePack() {
        super(new File(NAME));
    }

    private ModFileResourcePack getParent() {
        return (ModFileResourcePack) ResourcePackLoader.getResourcePackFor(ReplayMod.MOD_ID);
    }

    private String langName(String path) {
        Matcher matcher = JSON_FILE_PATTERN.matcher(path);
        if (!matcher.matches()) return null;
        return String.format("%s_%s.lang", matcher.group(1), matcher.group(2).toUpperCase());
    }

    private Path langPath(String path) {
        ModFileResourcePack parent = getParent();
        if (parent == null) return null;
        ModFile modFile = parent.getModFile();

        String langName = langName(path);
        if (langName == null) return null;
        return modFile.getLocator().findPath(modFile, "assets", ReplayMod.MOD_ID, "lang", langName);
    }

    private String convertValue(String value) {
        return value;
    }

    @Override
    protected InputStream getInputStream(String path) throws IOException {
        if ("pack.mcmeta".equals(path)) {
            return new StringBufferInputStream("{\"pack\": {\"description\": \"ReplayMod language files\", \"pack_format\": 4}}");
        }

        Path langPath = langPath(path);
        if (langPath == null) throw new ResourcePackFileNotFoundException(file, path);

        String langFile;
        try (InputStream in = Files.newInputStream(langPath)) {
            langFile = IOUtils.toString(in);
        }

        Map<String, String> properties = new HashMap<>();
        for (String line : langFile.split("\n")) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            int i = line.indexOf('=');
            String key = line.substring(0, i);
            String value = line.substring(i + 1);
            value = convertValue(value);
            properties.put(key, value);
        }

        return new ByteArrayInputStream(GSON.toJson(properties).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected boolean resourceExists(String path) {
        Path langPath = langPath(path);
        return langPath != null && Files.exists(langPath);
    }

    @Override
    public Collection<ResourceLocation> getAllResourceLocations(ResourcePackType resourcePackType, String path, int maxDepth, Predicate<String> filter) {
        if (resourcePackType == ResourcePackType.CLIENT_RESOURCES && "lang".equals(path)) {
            IResourcePack parent = getParent();
            if (parent == null) return Collections.emptyList();
            return parent.getAllResourceLocations(resourcePackType, path, maxDepth, name -> {
                Matcher matcher = LANG_FILE_NAME_PATTERN.matcher(name);
                if (matcher.matches()) {
                    return filter.test(String.format("%s_%s.json", matcher.group(1), matcher.group(1)));
                } else {
                    return false;
                }
            }).stream().map(resourceLocation -> {
                String p = resourceLocation.getPath().substring(0, "assets/lang/xx_XX".length()) + ".json";
                return new ResourceLocation(resourceLocation.getNamespace(), p);
            }).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Set<String> getResourceNamespaces(ResourcePackType resourcePackType) {
        if (resourcePackType == ResourcePackType.CLIENT_RESOURCES) {
            return Collections.singleton("replaymod");
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public void close() {}

    public static class Finder implements IPackFinder {
        @Override
        public <T extends ResourcePackInfo> void addPackInfosToMap(Map<String, T> packList, ResourcePackInfo.IFactory<T> factory) {
            packList.put(NAME, ResourcePackInfo.func_195793_a(NAME, true, LangResourcePack::new, factory, ResourcePackInfo.Priority.BOTTOM));
        }
    }
}
//#endif
