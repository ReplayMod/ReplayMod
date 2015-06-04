package eu.crushedpixel.replaymod.utils;

import net.minecraft.client.resources.I18n;

public class DurationUtils {

    public static String convertSecondsToString(int seconds) {
        int hours = seconds/(60*60);
        int min = seconds/60 - hours*60;
        int sec = seconds - ((min*60) + (hours*60*60));

        StringBuilder builder = new StringBuilder();
        if(hours > 0) builder.append(hours).append(I18n.format("replaymod.gui.hours"));
        if(min > 0 || hours > 0) builder.append(min).append(I18n.format("replaymod.gui.minutes"));
        builder.append(sec).append(I18n.format("replaymod.gui.seconds"));

        return builder.toString();
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
}
