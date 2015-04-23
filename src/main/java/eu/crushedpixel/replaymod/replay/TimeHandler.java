package eu.crushedpixel.replaymod.replay;

import net.minecraft.network.play.server.S03PacketTimeUpdate;

public class TimeHandler {

    private static long actualDaytime;
    private static long desiredDaytime;

    private static boolean timeOverridden = false;

    public static boolean isTimeOverridden() {
        return timeOverridden;
    }

    public static void setTimeOverridden(boolean overridden) {
        timeOverridden = overridden;
    }

    public static void setDesiredDaytime(long ddt) {
        desiredDaytime = ddt;
    }

    public static S03PacketTimeUpdate getTimePacket(S03PacketTimeUpdate packet) {
        if(!timeOverridden) return packet;
        return new S03PacketTimeUpdate(packet.func_149366_c(), desiredDaytime, true);
    }
}
