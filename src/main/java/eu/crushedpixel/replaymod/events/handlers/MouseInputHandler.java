package eu.crushedpixel.replaymod.events.handlers;

public class MouseInputHandler {

    // TODO
//    private final Minecraft mc = Minecraft.getMinecraft();
//    private boolean rightDown = false;
//    private boolean leftDown = false;
//
//    @SubscribeEvent
//    public void mouseEvent(MouseEvent event) {
//        if(!ReplayHandler.isInReplay()) {
//            return;
//        }
//
//        if(event.dwheel != 0 && mc.currentScreen == null) {
//            boolean increase = event.dwheel > 0;
//            CameraEntity.modifyCameraSpeed(increase);
//        }
//
//        if(Mouse.isButtonDown(0)) {
//            if(!leftDown) {
//                leftDown = true;
//                if(mc.pointedEntity != null && ReplayHandler.isCameraView() && mc.currentScreen == null) {
//                    if(SpectatingHandler.canSpectate(mc.pointedEntity))
//                        ReplayHandler.spectateEntity(mc.pointedEntity);
//                }
//            }
//        } else {
//            leftDown = false;
//        }
//
//        if(Mouse.isButtonDown(1)) {
//            if(!rightDown) {
//                rightDown = true;
//                if(mc.pointedEntity != null && ReplayHandler.isCameraView() && mc.currentScreen == null) {
//                    if(SpectatingHandler.canSpectate(mc.pointedEntity))
//                        ReplayHandler.spectateEntity(mc.pointedEntity);
//                }
//            }
//        } else {
//            rightDown = false;
//        }
//    }
}
