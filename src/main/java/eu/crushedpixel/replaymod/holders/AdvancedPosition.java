package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.Interpolate;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class AdvancedPosition extends Position {

    @Interpolate
    public double pitch, yaw, roll;

    private Integer spectatedEntityID;

    public AdvancedPosition(double x, double y, double z, double pitch, double yaw, double roll, Integer spectatedEntityID) {
        super(x, y, z);
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
        this.spectatedEntityID = spectatedEntityID;
    }

    public AdvancedPosition(int entityID, boolean spectate) {
        this(Minecraft.getMinecraft().theWorld.getEntityByID(entityID), spectate);
    }

    public AdvancedPosition(Entity e, boolean spectate) {
        this(e.posX, e.posY, e.posZ, e.rotationPitch, e.rotationYaw, spectate ? e.getEntityId() : null);
    }

    public AdvancedPosition(double x, double y, double z, float pitch, float yaw, Integer spectatedEntityID) {
        this(x, y, z, pitch, yaw, 0, spectatedEntityID);
    }

    public AdvancedPosition(double x, double y, double z, float pitch, float yaw) {
        this(x, y, z, pitch, yaw, 0, null);
    }

    public double distanceSquared(double x, double y, double z) {
        double dx = this.x -  x;
        double dy = this.y -  y;
        double dz = this.z -  z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public AdvancedPosition newInstance() {
        return new AdvancedPosition();
    }
}
