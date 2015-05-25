package eu.crushedpixel.replaymod.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import java.util.UUID;

public class SkinProvider {

    public static ResourceLocation getResourceLocationForPlayerUUID(UUID uuid) {
        NetworkPlayerInfo info = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(uuid);
        ResourceLocation skinLocation;
        if(info.hasLocationSkin()) {
            skinLocation = info.getLocationSkin();
        } else {
            skinLocation = DefaultPlayerSkin.getDefaultSkin(uuid);
        }
        return skinLocation;
    }
}
