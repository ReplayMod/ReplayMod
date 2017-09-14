package com.replaymod.replay.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.Sys;

// TODO: Marius is responsible for this. Please, someone clean it up.
public class ClassicCameraController implements CameraController {
    private static final double SPEED_CHANGE = 0.5;
    private static final double LOWER_SPEED = 2;
    private static final double UPPER_SPEED = 20;

    private final CameraEntity camera;

    private double MAX_SPEED = 10;
    private double THRESHOLD = MAX_SPEED / 20;
    private double DECAY = MAX_SPEED/3;

    private Vec3 direction;
    private Vec3 dirBefore;
    private double motion;
    private long lastCall = Sys.getTime();

    private boolean speedup = false;

    public ClassicCameraController(CameraEntity camera) {
        this.camera = camera;
    }

    @Override
    public void update(float partialTicksPassed) {
        boolean forward = false, backward = false, left = false, right = false, up = false, down = false;
        speedup = false;
        for(KeyBinding kb : Minecraft.getMinecraft().gameSettings.keyBindings) {
            if(!kb.getIsKeyPressed()) continue;
            if(kb.getKeyDescription().equals("key.forward")) {
                forward = true;
                speedup = true;
            }

            if(kb.getKeyDescription().equals("key.back")) {
                backward = true;
                speedup = true;
            }

            if(kb.getKeyDescription().equals("key.jump")) {
                up = true;
                speedup = true;
            }

            if(kb.getKeyDescription().equals("key.left")) {
                left = true;
                speedup = true;
            }

            if(kb.getKeyDescription().equals("key.right")) {
                right = true;
                speedup = true;
            }

            if(kb.getKeyDescription().equals("key.sneak")) {
                down = true;
                speedup = true;
            }
        }

        forwardCameraMovement(forward, backward, left, right, up, down);
        updateMovement();
    }

    @Override
    public void increaseSpeed() {
        setCameraMaximumSpeed(MAX_SPEED + SPEED_CHANGE);
    }

    @Override
    public void decreaseSpeed() {
        setCameraMaximumSpeed(MAX_SPEED - SPEED_CHANGE);
    }

    private void setCameraMaximumSpeed(double maxSpeed) {
        if(maxSpeed < LOWER_SPEED || maxSpeed > UPPER_SPEED) return;
        MAX_SPEED = maxSpeed;
        THRESHOLD = MAX_SPEED / 20;
        DECAY = 5;
    }

    private void forwardCameraMovement(boolean forward, boolean backward, boolean left, boolean right, boolean up, boolean down) {
        if(forward && !backward) {
            setMovement(MoveDirection.FORWARD);
        } else if(backward && !forward) {
            setMovement(MoveDirection.BACKWARD);
        }

        if(left && !right) {
            setMovement(MoveDirection.LEFT);
        } else if(right && !left) {
            setMovement(MoveDirection.RIGHT);
        }

        if(up && !down) {
            setMovement(MoveDirection.UP);
        } else if(down && !up) {
            setMovement(MoveDirection.DOWN);
        }
    }

    private void updateMovement() {
        long frac = Sys.getTime() - lastCall;

        if(frac == 0) return;

        double decFac = Math.max(0, 1 - (DECAY * (frac / 1000D)));

        if(speedup) {
            if(motion < THRESHOLD) motion = THRESHOLD;
            motion /= decFac;
        } else {
            motion *= decFac;
        }

        motion = Math.min(motion, MAX_SPEED);

        lastCall = Sys.getTime();

        if(direction == null || motion < THRESHOLD) {
            return;
        }

        Vec3 movement = direction.normalize();
        double factor = motion * (frac / 1000D);

        camera.moveCamera(movement.xCoord * factor, movement.yCoord * factor, movement.zCoord * factor);
    }

    private void setMovement(MoveDirection dir) {
        float rotationPitch = camera.rotationPitch, rotationYaw = camera.rotationYaw;
        switch(dir) {
            case BACKWARD:
                direction = this.getVectorForRotation(-rotationPitch, rotationYaw - 180);
                break;
            case DOWN:
                direction = this.getVectorForRotation(90, 0);
                break;
            case FORWARD:
                direction = this.getVectorForRotation(rotationPitch, rotationYaw);
                break;
            case LEFT:
                direction = this.getVectorForRotation(0, rotationYaw - 90);
                break;
            case RIGHT:
                direction = this.getVectorForRotation(0, rotationYaw + 90);
                break;
            case UP:
                direction = this.getVectorForRotation(-90, 0);
                break;
        }

        Vec3 dbf = direction;

        if(dirBefore != null) {
            direction = dirBefore.normalize().addVector(direction.xCoord, direction.yCoord, direction.zCoord);
        }

        dirBefore = dbf;

        updateMovement();
    }

    private Vec3 getVectorForRotation(float pitch, float yaw) {
        float f2 = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f3 = MathHelper.sin(-yaw * 0.017453292F - (float)Math.PI);
        float f4 = -MathHelper.cos(-pitch * 0.017453292F);
        float f5 = MathHelper.sin(-pitch * 0.017453292F);
        return Vec3.createVectorHelper((double)(f3 * f4), (double)f5, (double)(f2 * f4));
    }

    public enum MoveDirection {
        UP, DOWN, LEFT, RIGHT, FORWARD, BACKWARD
    }
}
