package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.Interpolate;
import eu.crushedpixel.replaymod.interpolation.KeyframeValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class Position extends KeyframeValue {

    @Interpolate
    public double x, y, z;
    @Interpolate
    public double pitch, yaw, roll;

    private Integer spectatedEntityID;

    public Position(int entityID, boolean spectate) {
        this(Minecraft.getMinecraft().theWorld.getEntityByID(entityID), spectate);
    }

    public Position(Entity e, boolean spectate) {
        this(e.posX, e.posY, e.posZ, e.rotationPitch, e.rotationYaw, spectate ? e.getEntityId() : null);
    }

    public Position(double x, double y, double z, float pitch, float yaw, Integer spectatedEntityID) {
        this(x, y, z, pitch, yaw, 0, spectatedEntityID);
    }

    public Position(double x, double y, double z, float pitch, float yaw) {
        this(x, y, z, pitch, yaw, 0, null);
    }

    public double distanceSquared(double x, double y, double z) {
        double dx = this.x -  x;
        double dy = this.y -  y;
        double dz = this.z -  z;
        return dx * dx + dy * dy + dz * dz;
    }
}
