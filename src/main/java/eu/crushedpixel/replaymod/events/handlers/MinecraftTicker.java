package eu.crushedpixel.replaymod.events.handlers;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraftforge.client.event.MouseEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class MinecraftTicker {

    public static void runMouseKeyboardTick(Minecraft mc) {
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

                if(mc.currentScreen == null) {
                    mc.dispatchKeypresses();
                }

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

                            if (mc.entityRenderer != null) {
                                if (mc.gameSettings.thirdPersonView == 0) {
                                    mc.entityRenderer.loadEntityShader(mc.getRenderViewEntity());
                                } else if (mc.gameSettings.thirdPersonView == 1) {
                                    mc.entityRenderer.loadEntityShader(null);
                                }
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

            mc.systemTime = Minecraft.getSystemTime();
        } catch (ReportedException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
