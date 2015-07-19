package eu.crushedpixel.replaymod.utils;

import java.text.DecimalFormat;

public class RoundUtils {

    public static int roundToMultiple(int value, int snap) {
        return Math.max(snap, (int)Math.round((float)value/snap) * snap);
    }

    public static double round2Decimals(double val) {
        return round(val, 2);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        StringBuilder format = new StringBuilder("#.");
        for(int i=0; i<places; i++) {
            format.append("#");
        }

        DecimalFormat df = new DecimalFormat(format.toString());
        return Double.valueOf(df.format(value));
    }

    public static int getClosestInt(int base, int[] snap) {
        int min = Integer.MAX_VALUE;
        int closest = base;

        for (int v : snap) {
            final int diff = Math.abs(v - base);

            if (diff < min) {
                min = diff;
                closest = v;
            }
        }

        return closest;
    }

}
