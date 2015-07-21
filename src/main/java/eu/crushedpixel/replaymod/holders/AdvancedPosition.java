package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.interpolation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

import javax.vecmath.Vector3d;

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

    public AdvancedPosition getDestination(double stepSize) {
        float f2 = MathHelper.cos((float) (Math.toRadians(-yaw) - (float)Math.PI));
        float f3 = MathHelper.sin((float) (Math.toRadians(-yaw) - (float) Math.PI));
        float f4 = -MathHelper.cos((float) (Math.toRadians(-pitch)));
        float f5 = MathHelper.sin((float) (Math.toRadians(-pitch)));
        Vector3d direction = new Vector3d((double)(f3 * f4), (double)f5, (double)(f2 * f4));
        direction.normalize();
        direction.scale(stepSize);

        Vector3d position = new Vector3d(x, y, z);
        position.add(direction);
        return new AdvancedPosition(position.x, position.y, position.z, pitch, yaw, roll, null);
    }

    @Override
    public AdvancedPosition newInstance() {
        return new AdvancedPosition();
    }

    @Override
    public Interpolation getCubicInterpolator() {
        return new AdvancedPositionSplineInterpolation();
    }

    @Override
    public Interpolation getLinearInterpolator() {
        return new AdvancedPositionLinearInterpolation();
    }
}
