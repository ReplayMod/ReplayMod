package eu.crushedpixel.replaymod.chat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import eu.crushedpixel.replaymod.ReplayMod;

@SideOnly(Side.CLIENT)
public class ChatMessageRequests {

	public enum ChatMessageType {
		INFORMATION, WARNING;
	}

	private static boolean active = true;
	private static boolean alive = true;

	private static Queue<IChatComponent> requests = new ConcurrentLinkedQueue<IChatComponent>();
	private static String prefix = "§8[§6Replay Mod§8]§r ";

	private static EntityPlayerSP player = null;

	public static Thread t = new Thread(new Runnable() {

		@Override
		public void run() {
			while(alive) {
				while(active) {
					try {
						while(player == null) {
							if(!alive) {
								break;
							}
							try {
								Thread.sleep(100);
								player = Minecraft.getMinecraft().thePlayer;
							} catch(Exception e) {}
						}

						player.addChatComponentMessage(requests.poll());
						Thread.sleep(100);
					} catch(Exception e) {}
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	});

	static {
		t.start();
	}

	public static void addChatMessage(String message, ChatMessageType type) {
		if(ReplayMod.replaySettings.isShowNotifications()) {
			message = prefix+toColor(message, type);
			ChatComponentText cct = new ChatComponentText(message);
			requests.add(cct);
		}
	}

	private static String toColor(String message, ChatMessageType type) {
		if(type == ChatMessageType.INFORMATION) {
			return "§2"+message;
		} else if(type == ChatMessageType.WARNING) {
			return "§c"+message;
		}

		return message;
	}

	public static void stop() {
		active = false;
	}

	public static void initialize() {
		active = true;
		requests.clear();
		if(ReplayMod.replaySettings.isShowNotifications()) {
		} else {
			System.out.println("Chat messages are disabled");
		}
	}
}
