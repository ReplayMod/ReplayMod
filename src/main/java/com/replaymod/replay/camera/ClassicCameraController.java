package com.replaymod.replay.camera;

import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.options.KeyBinding;

import static com.replaymod.core.versions.MCVer.*;

// TODO: Marius is responsible for this. Please, someone clean it up.
public class ClassicCameraController implements CameraController {
    private static final double SPEED_CHANGE = 0.5;
    private static final double LOWER_SPEED = 2;
    private static final double UPPER_SPEED = 20;

    private final CameraEntity camera;

    private double MAX_SPEED = 10;
    private double THRESHOLD = MAX_SPEED / 20;
    private double DECAY = MAX_SPEED/3;

    private Vector3f direction;
    private Vector3f dirBefore;
    private double motion;
    private long lastCall = System.currentTimeMillis();

    private boolean speedup = false;

    public ClassicCameraController(CameraEntity camera) {
        this.camera = camera;
    }

    @Override
    public void update(float partialTicksPassed) {
        boolean forward = false, backward = false, left = false, right = false, up = false, down = false;
        speedup = false;
        for(KeyBinding kb : getMinecraft().options.keysAll) {
            if(!kb.isPressed()) continue;
            if(kb.getId().equals("key.forward")) {
                forward = true;
                speedup = true;
            }

            if(kb.getId().equals("key.back")) {
                backward = true;
                speedup = true;
            }

            if(kb.getId().equals("key.jump")) {
                up = true;
                speedup = true;
            }

            if(kb.getId().equals("key.left")) {
                left = true;
                speedup = true;
            }

            if(kb.getId().equals("key.right")) {
                right = true;
                speedup = true;
            }

            if(kb.getId().equals("key.sneak")) {
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
        long frac = System.currentTimeMillis() - lastCall;

        if(frac == 0) return;

        double decFac = Math.max(0, 1 - (DECAY * (frac / 1000D)));

        if(speedup) {
            if(motion < THRESHOLD) motion = THRESHOLD;
            motion /= decFac;
        } else {
            motion *= decFac;
        }

        motion = Math.min(motion, MAX_SPEED);

        lastCall = System.currentTimeMillis();

        if(direction == null || direction.lengthSquared() == 0 || motion < THRESHOLD) {
            return;
        }

        Vector3f movement = direction.normalise(null);
        double factor = motion * (frac / 1000D);

        camera.moveCamera(movement.x * factor, movement.y * factor, movement.z * factor);
    }

    private void setMovement(MoveDirection dir) {
        float rotationPitch = camera.pitch, rotationYaw = camera.yaw;
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

        Vector3f dbf = direction;

        if(dirBefore != null) {
            dirBefore.normalise(dirBefore);
            Vector3f.add(direction, dirBefore, dirBefore);
            direction = dirBefore;
        }

        dirBefore = dbf;

        updateMovement();
    }

    private Vector3f getVectorForRotation(float pitch, float yaw) {
        float f2 = cos(-yaw * 0.017453292F - (float) Math.PI);
        float f3 = sin(-yaw * 0.017453292F - (float)Math.PI);
        float f4 = -cos(-pitch * 0.017453292F);
        float f5 = sin(-pitch * 0.017453292F);
        return new Vector3f(f3 * f4, f5, f2 * f4);
    }

    public enum MoveDirection {
        UP, DOWN, LEFT, RIGHT, FORWARD, BACKWARD
    }
}
