package eu.crushedpixel.replaymod.holders;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@Data
@AllArgsConstructor
public class Position {

    private double x, y, z;
    private float pitch, yaw, roll;

    public Position(int entityID) {
        this(Minecraft.getMinecraft().theWorld.getEntityByID(entityID));
    }

    public Position(Entity e) {
        this(e.posX, e.posY, e.posZ, e.rotationPitch, e.rotationYaw);
    }

    public Position(double x, double y, double z, float pitch, float yaw) {
        this(x, y, z, pitch, yaw, 0);
    }

    public double distanceSquared(double x, double y, double z) {
        double dx = this.x -  x;
        double dy = this.y -  y;
        double dz = this.z -  z;
        return dx * dx + dy * dy + dz * dz;
    }
}
