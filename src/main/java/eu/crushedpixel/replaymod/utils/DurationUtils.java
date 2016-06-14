package eu.crushedpixel.replaymod.utils;

public class DurationUtils {

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
