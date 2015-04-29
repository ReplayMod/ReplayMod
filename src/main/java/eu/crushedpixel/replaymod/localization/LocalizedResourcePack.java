package eu.crushedpixel.replaymod.localization;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.StringEscapeUtils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LocalizedResourcePack implements IResourcePack {

    private Map<String, String> availableLanguages = new HashMap<String, String>();

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
        boolean downloaded = true;
        try {
            String lang = ReplayMod.apiClient.getTranslation(langcode);
            String prop = StringEscapeUtils.unescapeHtml4(lang);
            availableLanguages.put(langcode, prop);
        } catch(Exception e) {
            e.printStackTrace();
            downloaded = false;
        }

        return downloaded;
    }

    @Override
    public Set getResourceDomains() {
        return ImmutableSet.of("minecraft", "replaymod");
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
