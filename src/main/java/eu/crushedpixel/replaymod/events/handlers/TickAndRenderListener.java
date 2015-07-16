package eu.crushedpixel.replaymod.events.handlers;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.chat.ChatMessageHandler;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplayProcess;
import eu.crushedpixel.replaymod.video.ReplayScreenshot;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public class TickAndRenderListener {

    private static Minecraft mc = Minecraft.getMinecraft();

    private static int requestScreenshot = 0;

    public static void requestScreenshot() {
        if(requestScreenshot == 0) requestScreenshot = 1;
    }

    public static void finishScreenshot() {
        requestScreenshot = 0;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) throws Exception {
        if(!ReplayHandler.isInReplay()) return; //If not in Replay, cancel
        if (ReplayProcess.isVideoRecording()) return; // If recording, cancel

        if(requestScreenshot == 1) {
            mc.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    ReplayMod.chatMessageHandler.addLocalizedChatMessage("replaymod.chat.savingthumb", ChatMessageHandler.ChatMessageType.INFORMATION);
                    ReplayScreenshot.prepareScreenshot();
                    requestScreenshot = 2;
                }
            });
        } else if(requestScreenshot == 2) {
            mc.addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    ReplayScreenshot.saveScreenshot();
                }
            });
        }

        if(ReplayHandler.isInPath()) ReplayProcess.unblockAndTick(false);
        if(ReplayHandler.isCamera()) mc.setRenderViewEntity(ReplayHandler.getCameraEntity());
        if(mc.getRenderViewEntity() != null && (mc.getRenderViewEntity() == mc.thePlayer || !mc.getRenderViewEntity().isEntityAlive())
                && ReplayHandler.getCameraEntity() != null && !ReplayHandler.isInPath()) {
            ReplayHandler.spectateCamera();
        }

        if(mc.isGamePaused() && ReplayHandler.isInPath()) {
            mc.isGamePaused = false;
        }
    }

    @SubscribeEvent
    public void tick(TickEvent event) {
        if(!ReplayHandler.isInReplay() || ReplayProcess.isVideoRecording()) return;

        if(ReplayHandler.getCameraEntity() != null)
            ReplayHandler.getCameraEntity().updateMovement();
        if(ReplayHandler.isInPath()) {
            ReplayProcess.unblockAndTick(true);
        } else onMouseMove(new MouseEvent());

        FMLCommonHandler.instance().bus().post(new InputEvent.KeyInputEvent());
    }

    @SubscribeEvent
    public void onMouseMove(MouseEvent event) {
        if(!ReplayHandler.isInReplay()) return;

        mc.mcProfiler.startSection("mouse");

        if(Minecraft.isRunningOnMac && mc.inGameHasFocus && !Mouse.isInsideWindow()) {
            Mouse.setGrabbed(false);
            Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
            Mouse.setGrabbed(true);
        }

        if(mc.inGameHasFocus && !(ReplayHandler.isInPath())) {
            mc.mouseHelper.mouseXYChange();
            float f1 = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float f2 = f1 * f1 * f1 * 8.0F;
            float f3 = (float) mc.mouseHelper.deltaX * f2;
            float f4 = (float) mc.mouseHelper.deltaY * f2;
            byte b0 = 1;

            if(mc.gameSettings.invertMouse) {
                b0 = -1;
            }

            if(ReplayHandler.getCameraEntity() != null) {
                ReplayHandler.getCameraEntity().setAngles(f3, f4 * (float) b0);
            }
        }
    }
}
