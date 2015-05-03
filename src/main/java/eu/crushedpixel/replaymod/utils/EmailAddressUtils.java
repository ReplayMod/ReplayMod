package eu.crushedpixel.replaymod.utils;

import java.net.URLEncoder;

public class EmailAddressUtils {
    public static boolean isValidEmailAddress(String mail) {
        try {
            String[] spl1 = mail.split("@");
            String[] spl2 = spl1[1].split("\\.");
            String suffix = spl2[1];

            return spl1[0].equals(URLEncoder.encode(spl1[0], "UTF-8")) && spl1[1].equals(URLEncoder.encode(spl1[1], "UTF-8"));
        } catch(Exception e) {
            return false;
        }
    }
}
