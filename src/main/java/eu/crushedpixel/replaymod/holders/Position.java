package eu.crushedpixel.replaymod.holders;

import net.minecraft.entity.Entity;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Position {

    private double x, y, z;
    private float pitch, yaw, roll;

    public Position(Entity e) {
        this.x = e.posX;
        this.y = e.posY;
        this.z = e.posZ;
        this.pitch = e.rotationPitch;
        this.yaw = e.rotationYaw;
    }

    public Position(double x, double y, double z, float pitch, float yaw, float roll) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public Position(double x, double y, double z, float pitch, float yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getRoll() { return roll; }

    public void setRoll(float roll) { this.roll = roll; }

    @Override
    public String toString() {
        return "X=" + x + ", Y=" + y + ", Z=" + z + ", Yaw=" + yaw + ", Pitch=" + pitch + ", Roll="+roll;
    }

    @Override
    public boolean equals(Object o2) {
        if(o2 == null) return false;
        if(!(o2 instanceof Position)) return false;
        Position pos2 = (Position)o2;
        return hashCode() == pos2.hashCode();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(x)
                .append(y)
                .append(z)
                .append(pitch)
                .append(yaw)
                .append(roll)
                .toHashCode();
    }
}
