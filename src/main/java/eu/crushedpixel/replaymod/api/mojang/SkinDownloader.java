package eu.crushedpixel.replaymod.api.mojang;


import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.api.ApiException;
import eu.crushedpixel.replaymod.api.mojang.holders.Profile;
import eu.crushedpixel.replaymod.api.mojang.holders.Properties;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SkinDownloader {

    public static ITextureObject getDownloadImageSkin(ResourceLocation resourceLocationIn, UUID uuid) throws IOException, ApiException {
        TextureManager texturemanager = Minecraft.getMinecraft().getTextureManager();
        ITextureObject object = texturemanager.getTexture(resourceLocationIn);

        try {
            if(object == null) {
                Profile profile = ReplayMod.apiClient.getProfileFromUUID(uuid);

                if(profile.getProperties() != null && profile.getProperties().length > 0) {
                    List<Properties> plist = Arrays.asList(profile.getProperties());
                    Collections.reverse(plist);

                    for(Properties p : plist) {
                        if(p.getName().equals("textures")) {
                            object = new ThreadDownloadImageData((File) null,
                                    p.getTextureValue().getTextures().getSKIN().getUrl(),
                                    DefaultPlayerSkin.getDefaultSkin(uuid), new ImageBufferDownload());
                        }
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            object = new ThreadDownloadImageData((File)null,
                    null,
                    DefaultPlayerSkin.getDefaultSkin(uuid), new ImageBufferDownload());
        }

        return object;
    }


}
