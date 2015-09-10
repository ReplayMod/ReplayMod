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

    public AdvancedPosition(double x, double y, double z, double pitch, double yaw, double roll) {
        super(x, y, z);
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public AdvancedPosition(int entityID) {
        this(Minecraft.getMinecraft().theWorld.getEntityByID(entityID));
    }

    public AdvancedPosition(Entity e) {
        this(e.posX, e.posY, e.posZ, e.rotationPitch, e.rotationYaw);
    }

    public AdvancedPosition(double x, double y, double z, double pitch, double yaw) {
        this(x, y, z, pitch, yaw, 0);
    }

    public SpectatorData asSpectatorData(int entityID) {
        return new SpectatorData(x, y, z, pitch, yaw, roll, entityID);
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
        return new AdvancedPosition(position.x, position.y, position.z, pitch, yaw, roll);
    }

    public void apply(AdvancedPosition toApply) {
        this.x = toApply.getX();
        this.y = toApply.getY();
        this.z = toApply.getZ();
        this.pitch = toApply.getPitch();
        this.yaw = toApply.getYaw();
        this.roll = toApply.getRoll();
    }

    public AdvancedPosition copy() {
        return new AdvancedPosition(x, y, z, pitch, yaw, roll);
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
