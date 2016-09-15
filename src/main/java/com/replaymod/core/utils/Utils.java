package com.replaymod.core.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.UUID;

public class Utils {

    public static final BufferedImage DEFAULT_THUMBNAIL;

    static {
        BufferedImage thumbnail;
        try {
            thumbnail = ImageIO.read(Utils.class.getClassLoader().getResourceAsStream("default_thumb.jpg"));
        } catch (Exception e) {
            thumbnail = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
            e.printStackTrace();
        }
        DEFAULT_THUMBNAIL = thumbnail;
    }

    public static String convertSecondsToShortString(int seconds) {
        int hours = seconds/(60*60);
        int min = seconds/60 - hours*60;
        int sec = seconds - ((min*60) + (hours*60*60));

        StringBuilder builder = new StringBuilder();
        if(hours > 0) builder.append(String.format("%02d", hours)).append(":");
        builder.append(String.format("%02d", min)).append(":");
        builder.append(String.format("%02d", sec));

        return builder.toString();
    }

    public static Dimension fitIntoBounds(ReadableDimension toFit, ReadableDimension bounds) {
        int width = toFit.getWidth();
        int height = toFit.getHeight();

        float w = (float) width / bounds.getWidth();
        float h = (float) height / bounds.getHeight();

        if (w > h) {
            height = (int) (height / w);
            width = (int) (width / w);
        } else {
            height = (int) (height / h);
            width = (int) (width / h);
        }

        return new Dimension(width, height);
    }

    public static boolean isValidEmailAddress(String mail) {
        return mail.matches("^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$");
    }

    public static ResourceLocation getResourceLocationForPlayerUUID(UUID uuid) {
        NetworkPlayerInfo info = Minecraft.getMinecraft().getConnection().getPlayerInfo(uuid);
        ResourceLocation skinLocation;
        if (info != null && info.hasLocationSkin()) {
            skinLocation = info.getLocationSkin();
        } else {
            skinLocation = DefaultPlayerSkin.getDefaultSkin(uuid);
        }
        return skinLocation;
    }

    public static boolean isCtrlDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
    }
}
