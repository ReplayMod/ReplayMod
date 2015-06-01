package eu.crushedpixel.replaymod.utils;

public class DurationUtils {

    public static String convertSecondsToString(int seconds) {
        int hours = seconds/(60*60);
        int min = seconds/60 - hours*60;
        int sec = seconds - ((min*60) + (hours*60*60));

        StringBuilder builder = new StringBuilder();
        if(hours > 0) builder.append(hours).append("h ");
        if(min > 0 || hours > 0) builder.append(min).append("min ");
        builder.append(sec).append("sec");

        return builder.toString();
    }
}
