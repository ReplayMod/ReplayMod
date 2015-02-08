package eu.crushedpixel.replaymod.events;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import eu.crushedpixel.replaymod.entities.CameraEntity;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.replay.ReplayHandler;

public class MinecraftTicker {

	private static Field debugCrashKeyPressTime, rightClickDelayTimer, systemTime, leftClickCounter;
	private static Method getSystemTime, updateDebugProfilerName,
	clickMouse, middleClickMouse, rightClickMouse, sendClickBlockToController;

	private static Minecraft mc = Minecraft.getMinecraft();

	private static float camPitch, camYaw, smoothCamPartialTicks, 
	smoothCamFilterX, smoothCamFilterY;

	static {
		try {
			debugCrashKeyPressTime = Minecraft.class.getDeclaredField(MCPNames.field("field_83002_am"));
			debugCrashKeyPressTime.setAccessible(true);

			rightClickDelayTimer = Minecraft.class.getDeclaredField(MCPNames.field("field_71467_ac"));
			rightClickDelayTimer.setAccessible(true);

			systemTime = Minecraft.class.getDeclaredField(MCPNames.field("field_71423_H"));
			systemTime.setAccessible(true);

			leftClickCounter = Minecraft.class.getDeclaredField(MCPNames.field("field_71429_W"));
			leftClickCounter.setAccessible(true);

			getSystemTime = Minecraft.class.getDeclaredMethod(MCPNames.method("func_71386_F"));
			getSystemTime.setAccessible(true);

			updateDebugProfilerName = Minecraft.class.getDeclaredMethod(MCPNames.method("func_71383_b"), int.class);
			updateDebugProfilerName.setAccessible(true);

			clickMouse = Minecraft.class.getDeclaredMethod(MCPNames.method("func_147116_af"));
			clickMouse.setAccessible(true);

			rightClickMouse = Minecraft.class.getDeclaredMethod(MCPNames.method("func_147121_ag"));
			rightClickMouse.setAccessible(true);

			middleClickMouse = Minecraft.class.getDeclaredMethod(MCPNames.method("func_147112_ai"));
			middleClickMouse.setAccessible(true);

			sendClickBlockToController = Minecraft.class.getDeclaredMethod(MCPNames.method("func_147115_a"), boolean.class);
			sendClickBlockToController.setAccessible(true);

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void runMouseKeyboardTick(Minecraft mc) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {

		if(mc.thePlayer == null) return;
		try {
			mc.mcProfiler.endStartSection("mouse");
			int i;
			while(Mouse.next()) {
				i = Mouse.getEventButton();
				KeyBinding.setKeyBindState(i - 100, Mouse.getEventButtonState());

				if (Mouse.getEventButtonState())
				{
					if (mc.thePlayer.isSpectator() && i == 2)
					{
						mc.ingameGUI.func_175187_g().func_175261_b();
					}
					else
					{
						KeyBinding.onTick(i - 100);
					}
				}

				long k = (Long)getSystemTime.invoke(mc) - (Long)systemTime.get(mc);

				if (k <= 200L)
				{
					int j = Mouse.getEventDWheel();

					if (j != 0)
					{
						if (mc.thePlayer.isSpectator())
						{
							j = j < 0 ? -1 : 1;

							if (mc.ingameGUI.func_175187_g().func_175262_a())
							{
								mc.ingameGUI.func_175187_g().func_175259_b(-j);
							}
							else
							{
								float f = MathHelper.clamp_float(mc.thePlayer.capabilities.getFlySpeed() + (float)j * 0.005F, 0.0F, 0.2F);
								mc.thePlayer.capabilities.setFlySpeed(f);
							}
						}
						else
						{
							mc.thePlayer.inventory.changeCurrentItem(j);
						}
					}

					if (mc.currentScreen == null)
					{
						if (!mc.inGameHasFocus && Mouse.getEventButtonState())
						{
							mc.setIngameFocus();
						}
					}
					else
					{
						mc.currentScreen.handleMouseInput();
					}
				}
				net.minecraftforge.fml.common.FMLCommonHandler.instance().fireMouseInput();
			}

			if ((Integer)leftClickCounter.get(mc) > 0)
			{
				leftClickCounter.set(mc, (Integer)leftClickCounter.get(mc) - 1);
			}
			mc.mcProfiler.endStartSection("keyboard");

			while (Keyboard.next())
			{
				i = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
				KeyBinding.setKeyBindState(i, Keyboard.getEventKeyState());

				if (Keyboard.getEventKeyState())
				{
					KeyBinding.onTick(i);
				}

				if ((Long)debugCrashKeyPressTime.get(mc) > 0L)
				{
					if ((Long)getSystemTime.invoke(mc) - (Long)debugCrashKeyPressTime.get(mc) >= 6000L)
					{
						throw new ReportedException(new CrashReport("Manually triggered debug crash", new Throwable()));
					}

					if (!Keyboard.isKeyDown(46) || !Keyboard.isKeyDown(61))
					{
						debugCrashKeyPressTime.set(mc, -1);
					}
				}
				else if (Keyboard.isKeyDown(46) && Keyboard.isKeyDown(61))
				{
					debugCrashKeyPressTime.set(mc, getSystemTime.invoke(mc));
				}

				mc.dispatchKeypresses();

				if (Keyboard.getEventKeyState())
				{
					if (i == 62 && mc.entityRenderer != null)
					{
						mc.entityRenderer.switchUseShader();
					}

					if (mc.currentScreen != null)
					{
						mc.currentScreen.handleKeyboardInput();
					}
					else
					{
						if (i == 1)
						{
							mc.displayInGameMenu();
						}

						if (i == 32 && Keyboard.isKeyDown(61) && mc.ingameGUI != null)
						{
							mc.ingameGUI.getChatGUI().clearChatMessages();
						}

						if (i == 31 && Keyboard.isKeyDown(61))
						{
							mc.refreshResources();
						}

						if (i == 17 && Keyboard.isKeyDown(61))
						{
							;
						}

						if (i == 18 && Keyboard.isKeyDown(61))
						{
							;
						}

						if (i == 47 && Keyboard.isKeyDown(61))
						{
							;
						}

						if (i == 38 && Keyboard.isKeyDown(61))
						{
							;
						}

						if (i == 22 && Keyboard.isKeyDown(61))
						{
							;
						}

						if (i == 20 && Keyboard.isKeyDown(61))
						{
							mc.refreshResources();
						}

						if (i == 33 && Keyboard.isKeyDown(61))
						{
							boolean flag1 = Keyboard.isKeyDown(42) | Keyboard.isKeyDown(54);
							mc.gameSettings.setOptionValue(GameSettings.Options.RENDER_DISTANCE, flag1 ? -1 : 1);
						}

						if (i == 30 && Keyboard.isKeyDown(61))
						{
							mc.renderGlobal.loadRenderers();
						}

						if (i == 35 && Keyboard.isKeyDown(61))
						{
							mc.gameSettings.advancedItemTooltips = !mc.gameSettings.advancedItemTooltips;
							mc.gameSettings.saveOptions();
						}

						if (i == 48 && Keyboard.isKeyDown(61))
						{
							mc.getRenderManager().setDebugBoundingBox(!mc.getRenderManager().isDebugBoundingBox());
						}

						if (i == 25 && Keyboard.isKeyDown(61))
						{
							mc.gameSettings.pauseOnLostFocus = !mc.gameSettings.pauseOnLostFocus;
							mc.gameSettings.saveOptions();
						}

						if (i == 59)
						{
							mc.gameSettings.hideGUI = !mc.gameSettings.hideGUI;
						}

						if (i == 61)
						{
							mc.gameSettings.showDebugInfo = !mc.gameSettings.showDebugInfo;
							mc.gameSettings.showDebugProfilerChart = GuiScreen.isShiftKeyDown();
						}

						if (mc.gameSettings.keyBindTogglePerspective.isPressed())
						{
							++mc.gameSettings.thirdPersonView;

							if (mc.gameSettings.thirdPersonView > 2)
							{
								mc.gameSettings.thirdPersonView = 0;
							}

							if (mc.gameSettings.thirdPersonView == 0)
							{
								mc.entityRenderer.loadEntityShader(mc.getRenderViewEntity());
							}
							else if (mc.gameSettings.thirdPersonView == 1)
							{
								mc.entityRenderer.loadEntityShader((Entity)null);
							}
						}

						if (mc.gameSettings.keyBindSmoothCamera.isPressed())
						{
							mc.gameSettings.smoothCamera = !mc.gameSettings.smoothCamera;
						}
					}

					if (mc.gameSettings.showDebugInfo && mc.gameSettings.showDebugProfilerChart)
					{
						if (i == 11)
						{
							updateDebugProfilerName.invoke(mc, 0);
						}

						for (int l = 0; l < 9; ++l)
						{
							if (i == 2 + l)
							{
								updateDebugProfilerName.invoke(mc, l+1);
							}
						}
					}
				}
				net.minecraftforge.fml.common.FMLCommonHandler.instance().fireKeyInput();
			}

			for (i = 0; i < 9; ++i)
			{
				if (mc.gameSettings.keyBindsHotbar[i].isPressed())
				{
					if (mc.thePlayer.isSpectator())
					{
						mc.ingameGUI.func_175187_g().func_175260_a(i);
					}
					else
					{
						mc.thePlayer.inventory.currentItem = i;
					}
				}
			}

			boolean flag = mc.gameSettings.chatVisibility != EntityPlayer.EnumChatVisibility.HIDDEN;

			while (mc.gameSettings.keyBindInventory.isPressed())
			{
				if (mc.playerController.isRidingHorse())
				{
					mc.thePlayer.sendHorseInventory();
				}
				else
				{
					mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
					mc.displayGuiScreen(new GuiInventory(mc.thePlayer));
				}
			}

			while (mc.gameSettings.keyBindDrop.isPressed())
			{
				if (!mc.thePlayer.isSpectator())
				{
					mc.thePlayer.dropOneItem(GuiScreen.isCtrlKeyDown());
				}
			}

			while (mc.gameSettings.keyBindChat.isPressed() && flag)
			{
				mc.displayGuiScreen(new GuiChat());
			}

			if (mc.currentScreen == null && mc.gameSettings.keyBindCommand.isPressed() && flag)
			{
				mc.displayGuiScreen(new GuiChat("/"));
			}

			if (mc.thePlayer != null && mc.thePlayer.isUsingItem())
			{
				if (!mc.gameSettings.keyBindUseItem.isKeyDown())
				{
					mc.playerController.onStoppedUsingItem(mc.thePlayer);
				}

				label435:

					while (true)
					{
						if (!mc.gameSettings.keyBindAttack.isPressed())
						{
							while (mc.gameSettings.keyBindUseItem.isPressed())
							{
								;
							}

							while (true)
							{
								if (mc.gameSettings.keyBindPickBlock.isPressed())
								{
									continue;
								}

								break label435;
							}
						}
					}
			}
			else
			{
				while (mc.gameSettings.keyBindAttack.isPressed())
				{
					if(mc != null)
						try {
							clickMouse.invoke(mc);
						} catch(Exception e) {}

				}

				while (mc.gameSettings.keyBindUseItem.isPressed())
				{
					if(mc != null)
						try {
							rightClickMouse.invoke(mc);
						} catch(Exception e) {}
				}

				while (mc.gameSettings.keyBindPickBlock.isPressed())
				{
					if(mc != null)
						try {
							middleClickMouse.invoke(mc);
						} catch(Exception e) {}
				}
			}

			if (mc.gameSettings.keyBindUseItem.isKeyDown() && (Integer)rightClickDelayTimer.get(mc) == 0 && !mc.thePlayer.isUsingItem())
			{
				if(mc != null)
					try {
						rightClickMouse.invoke(mc);
					} catch(Exception e) {}
			}

			if(mc != null)
				try {
					sendClickBlockToController.invoke(mc, mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown() && mc.inGameHasFocus);
				} catch(Exception e) {}

			if(mc != null)
				systemTime.set(mc, getSystemTime.invoke(mc));
		} catch(Exception e) {}
	}
}
