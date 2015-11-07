package com.replaymod.replay.camera;

public class ClassicCameraController implements CameraController {
    @Override
    public void update(float partialTicksPassed) {
        // TODO
    }

    @Override
    public void increaseSpeed() {

    }

    @Override
    public void decreaseSpeed() {

    }


//    public static final double SPEED_CHANGE = 0.5;
//    public static final double LOWER_SPEED = 2;
//    public static final double UPPER_SPEED = 20;
//
//    private static double MAX_SPEED = 10;
//    private static double THRESHOLD = MAX_SPEED / 20;
//    private static double DECAY = MAX_SPEED/3;
//
//    public static void modifyCameraSpeed(boolean increase) {
//        setCameraMaximumSpeed(getCameraMaximumSpeed() + (increase ? 1 : -1) * SPEED_CHANGE);
//    }
//
//    public static void setCameraMaximumSpeed(double maxSpeed) {
//        if(maxSpeed < LOWER_SPEED || maxSpeed > UPPER_SPEED) return;
//        MAX_SPEED = maxSpeed;
//        THRESHOLD = MAX_SPEED / 20;
//        DECAY = 5;
//    }
//
//    public static double getCameraMaximumSpeed() {
//        return MAX_SPEED;
//    }
//
//    private Vec3 direction;
//    private Vec3 dirBefore;
//    private double motion;
//    private long lastCall = 0;
//
//    private boolean speedup = false;
//
//    //frac = time since last tick
//    public void updateMovement() {
//
//        long frac = Sys.getTime() - lastCall;
//
//        if(frac == 0) return;
//
//        double decFac = Math.max(0, 1 - (DECAY * (frac / 1000D)));
//
//        if(speedup) {
//            if(motion < THRESHOLD) motion = THRESHOLD;
//            motion /= decFac;
//        } else {
//            motion *= decFac;
//        }
//
//        motion = Math.min(motion, MAX_SPEED);
//
//        lastCall = Sys.getTime();
//
//        if(direction == null || motion < THRESHOLD) {
//            return;
//        }
//
//        Vec3 movement = direction.normalize();
//        double factor = motion * (frac / 1000D);
//
//        moveCamera(movement.xCoord * factor, movement.yCoord * factor, movement.zCoord * factor);
//    }
//
//    public void setMovement(MoveDirection dir) {
//        switch(dir) {
//            case BACKWARD:
//                direction = this.getVectorForRotation(-rotationPitch, rotationYaw - 180);
//                break;
//            case DOWN:
//                direction = this.getVectorForRotation(90, 0);
//                break;
//            case FORWARD:
//                direction = this.getVectorForRotation(rotationPitch, rotationYaw);
//                break;
//            case LEFT:
//                direction = this.getVectorForRotation(0, rotationYaw - 90);
//                break;
//            case RIGHT:
//                direction = this.getVectorForRotation(0, rotationYaw + 90);
//                break;
//            case UP:
//                direction = this.getVectorForRotation(-90, 0);
//                break;
//        }
//
//        Vec3 dbf = direction;
//
//        if(dirBefore != null) {
//            direction = dirBefore.normalize().add(direction);
//        }
//
//        dirBefore = dbf;
//
//        updateMovement();
//    }
//
//    public enum MoveDirection {
//        UP, DOWN, LEFT, RIGHT, FORWARD, BACKWARD
//    }
}
