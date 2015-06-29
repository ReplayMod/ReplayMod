package eu.crushedpixel.replaymod.online.authentication;

import net.minecraft.client.Minecraft;

import java.util.Random;

public class AuthenticationHash {

    private static final Random random = new Random();

    public AuthenticationHash() {
        username = Minecraft.getMinecraft().getSession().getUsername();
        currentTime = System.currentTimeMillis();
        randomLong = random.nextLong();
        hash = getAuthenticationHash();
    }

    public final String username;
    public final long currentTime;
    public final long randomLong;
    public final String hash;

    private String getAuthenticationHash() {
        String md5 = username + currentTime + randomLong;

        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
