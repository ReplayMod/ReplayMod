package com.replaymod.extras;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.replaymod.core.ReplayMod;
import com.replaymod.online.ReplayModOnline;
import com.replaymod.online.api.ApiClient;
import com.replaymod.online.api.ApiException;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringEscapeUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalizationExtra implements Extra {
    private ReplayModOnline module;

    @Override
    public void register(ReplayMod mod) throws Exception {
        this.module = ReplayModOnline.instance;

        final Minecraft mc = mod.getMinecraft();
        Thread localizedResourcePackLoader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    @SuppressWarnings("unchecked")
                    List<IResourcePack> defaultResourcePacks = mc.defaultResourcePacks;
                    defaultResourcePacks.add(new LocalizedResourcePack(module.getApiClient()));
                    mc.addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            mc.refreshResources();
                        }
                    });
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }, "localizedResourcePackLoader");
        localizedResourcePackLoader.start();
    }

    @RequiredArgsConstructor
    public static class LocalizedResourcePack implements IResourcePack {
        private final ApiClient apiClient;
        private Map<String, String> availableLanguages = new HashMap<>();
        private boolean websiteAvailable = true;

        @Override
        public InputStream getInputStream(ResourceLocation loc) {
            if(!loc.getResourcePath().endsWith(".lang")) return null;
            String langcode = loc.getResourcePath().split("/")[1].split("\\.")[0];
            if(availableLanguages.containsKey(langcode)) return new ByteArrayInputStream(availableLanguages.get(langcode).getBytes(Charsets.UTF_8));
            return null;
        }

        @Override
        public boolean resourceExists(ResourceLocation loc) {
            if(!(loc.getResourcePath().endsWith(".lang"))) return false;
            String langcode = loc.getResourcePath().split("/")[1].split("\\.")[0];
            if(availableLanguages.containsKey(langcode)) return true;
            if(!websiteAvailable) return false;
            try {
                if (Boolean.parseBoolean(System.getProperty("replaymod.offline", "false"))) {
                    return false;
                }
                String lang = apiClient.getTranslation(langcode);
                String prop = StringEscapeUtils.unescapeHtml4(lang);
                availableLanguages.put(langcode, prop);
                return true;
            } catch (ApiException e) {
                if (e.getError() == null || e.getError().getId() != 16) { // This language has not been translated
                    e.printStackTrace();
                }
            } catch(ConnectException ce) {
                websiteAvailable = false;
                ce.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public Set<String> getResourceDomains() {
            return ImmutableSet.of("minecraft", "replaymod");
        }

        @Override
        public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException {
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
