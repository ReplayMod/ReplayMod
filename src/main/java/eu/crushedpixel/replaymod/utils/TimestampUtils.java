package eu.crushedpixel.replaymod.utils;

public class TimestampUtils {

    public static int timestampToWholeMinutes(long timestamp) {
        return (int)(timestamp / (1000*60));
    }

    public static int getSecondsFromTimestamp(long timestamp) {
        return (int)(timestamp / 1000) % 60;
    }

    public static int getMillisecondsFromTimestamp(long timestamp) {
        return(int)(timestamp % 1000);
    }

    public static int calculateTimestamp(int min, int sec, int ms) {
        return (min * 60 * 1000) + (sec * 1000) + ms;
    }
}
