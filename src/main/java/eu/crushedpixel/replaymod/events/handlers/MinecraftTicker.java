package eu.crushedpixel.replaymod.events.handlers;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraftforge.client.event.MouseEvent;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class MinecraftTicker {

    public static void runMouseKeyboardTick(Minecraft mc) throws IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, IOException, LWJGLException {
        ReplayMod.mouseInputHandler.mouseEvent(new MouseEvent());
        if(mc.thePlayer == null) return;
        try {
            mc.mcProfiler.endStartSection("mouse");
            int i;
            while(Mouse.next()) {
                i = Mouse.getEventButton();
                KeyBinding.setKeyBindState(i - 100, Mouse.getEventButtonState());

                if(Mouse.getEventButtonState()) {
                    if(mc.thePlayer.isSpectator() && i == 2) {
                        mc.ingameGUI.func_175187_g().func_175261_b();
                    } else {
                        KeyBinding.onTick(i - 100);
                    }
                }

                long k = Minecraft.getSystemTime() - mc.systemTime;

                if(k <= 200L) {
                    int j = Mouse.getEventDWheel();

                    if(j != 0) {
                        if(mc.thePlayer.isSpectator()) {
                            j = j < 0 ? -1 : 1;

                            if(mc.ingameGUI.func_175187_g().func_175262_a()) {
                                mc.ingameGUI.func_175187_g().func_175259_b(-j);
                            } else {
                                float f = MathHelper.clamp_float(mc.thePlayer.capabilities.getFlySpeed() + (float) j * 0.005F, 0.0F, 0.2F);
                                mc.thePlayer.capabilities.setFlySpeed(f);
                            }
                        } else {
                            mc.thePlayer.inventory.changeCurrentItem(j);
                        }
                    }

                    if(mc.currentScreen == null) {
                        if(!mc.inGameHasFocus && Mouse.getEventButtonState()) {
                            mc.setIngameFocus();
                        }
                    } else {
                        mc.currentScreen.handleMouseInput();
                    }
                }
                net.minecraftforge.fml.common.FMLCommonHandler.instance().fireMouseInput();
            }

            if(mc.leftClickCounter > 0) {
                mc.leftClickCounter--;
            }
            mc.mcProfiler.endStartSection("keyboard");

            while(Keyboard.next()) {
                i = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
                KeyBinding.setKeyBindState(i, Keyboard.getEventKeyState());

                if(Keyboard.getEventKeyState()) {
                    KeyBinding.onTick(i);
                }

                if(mc.debugCrashKeyPressTime > 0L) {
                    if(Minecraft.getSystemTime() - mc.debugCrashKeyPressTime >= 6000L) {
                        throw new ReportedException(new CrashReport("Manually triggered debug crash", new Throwable()));
                    }

                    if(!Keyboard.isKeyDown(46) || !Keyboard.isKeyDown(61)) {
                        mc.debugCrashKeyPressTime = -1;
                    }
                } else if(Keyboard.isKeyDown(46) && Keyboard.isKeyDown(61)) {
                    mc.debugCrashKeyPressTime = Minecraft.getSystemTime();
                }

                mc.dispatchKeypresses();

                if(Keyboard.getEventKeyState()) {
                    if(i == 62 && mc.entityRenderer != null) {
                        mc.entityRenderer.switchUseShader();
                    }

                    if(mc.currentScreen != null) {
                        mc.currentScreen.handleKeyboardInput();
                    } else {
                        if(i == 1) {
                            mc.displayInGameMenu();
                        }

                        if(i == 32 && Keyboard.isKeyDown(61) && mc.ingameGUI != null) {
                            mc.ingameGUI.getChatGUI().clearChatMessages();
                        }

                        if(i == 31 && Keyboard.isKeyDown(61)) {
                            mc.refreshResources();
                        }

                        if(i == 17 && Keyboard.isKeyDown(61)) {
                            ;
                        }

                        if(i == 18 && Keyboard.isKeyDown(61)) {
                            ;
                        }

                        if(i == 47 && Keyboard.isKeyDown(61)) {
                            ;
                        }

                        if(i == 38 && Keyboard.isKeyDown(61)) {
                            ;
                        }

                        if(i == 22 && Keyboard.isKeyDown(61)) {
                            ;
                        }

                        if(i == 20 && Keyboard.isKeyDown(61)) {
                            mc.refreshResources();
                        }

                        if(i == 33 && Keyboard.isKeyDown(61)) {
                            boolean flag1 = Keyboard.isKeyDown(42) | Keyboard.isKeyDown(54);
                            mc.gameSettings.setOptionValue(GameSettings.Options.RENDER_DISTANCE, flag1 ? -1 : 1);
                        }

                        if(i == 30 && Keyboard.isKeyDown(61)) {
                            mc.renderGlobal.loadRenderers();
                        }

                        if(i == 35 && Keyboard.isKeyDown(61)) {
                            mc.gameSettings.advancedItemTooltips = !mc.gameSettings.advancedItemTooltips;
                            mc.gameSettings.saveOptions();
                        }

                        if(i == 48 && Keyboard.isKeyDown(61)) {
                            mc.getRenderManager().setDebugBoundingBox(!mc.getRenderManager().isDebugBoundingBox());
                        }

                        if(i == 25 && Keyboard.isKeyDown(61)) {
                            mc.gameSettings.pauseOnLostFocus = !mc.gameSettings.pauseOnLostFocus;
                            mc.gameSettings.saveOptions();
                        }

                        if(i == 59) {
                            mc.gameSettings.hideGUI = !mc.gameSettings.hideGUI;
                        }

                        if(i == 61) {
                            mc.gameSettings.showDebugInfo = !mc.gameSettings.showDebugInfo;
                            mc.gameSettings.showDebugProfilerChart = GuiScreen.isShiftKeyDown();
                        }

                        if(mc.gameSettings.keyBindTogglePerspective.isPressed()) {
                            ++mc.gameSettings.thirdPersonView;

                            if(mc.gameSettings.thirdPersonView > 2) {
                                mc.gameSettings.thirdPersonView = 0;
                            }

                            if(mc.gameSettings.thirdPersonView == 0) {
                                mc.entityRenderer.loadEntityShader(mc.getRenderViewEntity());
                            } else if(mc.gameSettings.thirdPersonView == 1) {
                                mc.entityRenderer.loadEntityShader((Entity) null);
                            }
                        }

                        if(mc.gameSettings.keyBindSmoothCamera.isPressed()) {
                            mc.gameSettings.smoothCamera = !mc.gameSettings.smoothCamera;
                        }
                    }

                    if(mc.gameSettings.showDebugInfo && mc.gameSettings.showDebugProfilerChart) {
                        if(i == 11) {
                            mc.updateDebugProfilerName(0);
                        }

                        for(int l = 0; l < 9; ++l) {
                            if(i == 2 + l) {
                                mc.updateDebugProfilerName(l + 1);
                            }
                        }
                    }
                }

                net.minecraftforge.fml.common.FMLCommonHandler.instance().fireKeyInput();
            }

            for(i = 0; i < 9; ++i) {
                if(mc.gameSettings.keyBindsHotbar[i].isPressed()) {
                    if(mc.thePlayer.isSpectator()) {
                        mc.ingameGUI.func_175187_g().func_175260_a(i);
                    } else {
                        mc.thePlayer.inventory.currentItem = i;
                    }
                }
            }

            if(mc.thePlayer != null && mc.thePlayer.isUsingItem()) {
                if(!mc.gameSettings.keyBindUseItem.isKeyDown()) {
                    mc.playerController.onStoppedUsingItem(mc.thePlayer);
                }

                label435:

                while(true) {
                    if(!mc.gameSettings.keyBindAttack.isPressed()) {
                        while(mc.gameSettings.keyBindUseItem.isPressed()) {
                            ;
                        }

                        while(true) {
                            if(mc.gameSettings.keyBindPickBlock.isPressed()) {
                                continue;
                            }

                            break label435;
                        }
                    }
                }
            }

            if(mc != null)
                mc.sendClickBlockToController(mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown() && mc.inGameHasFocus);

            if(mc != null)
                mc.systemTime = Minecraft.getSystemTime();
        } catch (ReportedException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
