package com.replaymod.extras;

import com.google.common.collect.ImmutableSet;
import com.replaymod.core.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.replaymod.extras.ReplayModExtras.LOGGER;

public class LocalizationExtra implements Extra {
    private static final String ZIP_FILE_URL = "https://github.com/ReplayMod/Translations/archive/master.zip";
    private static final String LANG_PREFIX = "Translations-master/";

    @Override
    public void register(ReplayMod mod) throws Exception {
        final Minecraft mc = mod.getMinecraft();
        if (Boolean.parseBoolean(System.getProperty("replaymod.offline", "false"))) {
            return;
        }
        Thread localizedResourcePackLoader = new Thread(() -> {
            try {
                // Download zip of lang files
                LOGGER.debug("Downloading languages from {}", ZIP_FILE_URL);
                Map<String, byte[]> languages = new HashMap<>();
                try (InputStream urlIn = new URL(ZIP_FILE_URL).openStream();
                     ZipInputStream in = new ZipInputStream(urlIn)) {
                    ZipEntry entry;
                    while ((entry = in.getNextEntry()) != null) {
                        String name = entry.getName();
                        if (!name.startsWith(LANG_PREFIX) || !name.endsWith(".lang")) {
                           continue;
                        }
                        name = name.substring(LANG_PREFIX.length());
                        languages.put(name, IOUtils.toByteArray(in));
                        LOGGER.debug("Added language file {}", name);
                    }
                }
                LOGGER.debug("Downloaded {} languages", languages.size());

                // Add lang files as resource pack
                mc.addScheduledTask(() -> {
                    @SuppressWarnings("unchecked")
                    List<IResourcePack> defaultResourcePacks = mc.defaultResourcePacks;
                    defaultResourcePacks.add(new LocalizedResourcePack(languages));
                    mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
                    LOGGER.debug("Added language files to resource packs and reloaded LanguageManager");
                });
            } catch (Throwable t) {
                LOGGER.error("Loading localized resource pack:", t);
            }
        }, "localizedResourcePackLoader");
        localizedResourcePackLoader.setDaemon(true);
        localizedResourcePackLoader.start();
    }

    public static class LocalizedResourcePack implements IResourcePack {
        private final Pattern LANG_PATTERN = Pattern.compile("^lang/([.+].lang)$");
        private final Map<String, byte[]> languages;

        public LocalizedResourcePack(Map<String, byte[]> languages) {
            this.languages = languages;
        }

        @Override
        public InputStream getInputStream(ResourceLocation loc) {
            if (!"replaymod".equals(loc.getResourceDomain())) return null;
            Matcher matcher = LANG_PATTERN.matcher(loc.getResourcePath());
            if (matcher.matches()) {
                byte[] bytes = languages.get(matcher.group());
                if (bytes != null) {
                    return new ByteArrayInputStream(bytes);
                }
            }
            return null;
        }

        @Override
        public boolean resourceExists(ResourceLocation loc) {
            // Assumes that getInputStream returns a ByteArrayInputStream that doesn't need to be closed
            return getInputStream(loc) != null;
        }

        @Override
        public Set getResourceDomains() {
            return ImmutableSet.of("replaymod");
        }

        @Override
        public IMetadataSection getPackMetadata(IMetadataSerializer p_135058_1_, String p_135058_2_) throws IOException {
            return null;
        }

        @Override
        public BufferedImage getPackImage() throws IOException {
            return null;
        }

        @Override
        public String getPackName() {
            return "ReplayModLocalizationResourcePack";
        }
    }
}
