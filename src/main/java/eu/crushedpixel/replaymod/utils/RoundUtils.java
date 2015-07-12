package eu.crushedpixel.replaymod.utils;

import java.text.DecimalFormat;

public class RoundUtils {

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

}
