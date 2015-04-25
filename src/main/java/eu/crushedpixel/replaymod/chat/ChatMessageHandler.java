package eu.crushedpixel.replaymod.chat;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@SideOnly(Side.CLIENT)
public class ChatMessageHandler {

    private boolean active = true;
    private boolean alive = true;
    private Queue<IChatComponent> requests = new ConcurrentLinkedQueue<IChatComponent>();
    private String prefix = "§8[§6Replay Mod§8]§r ";
    private EntityPlayerSP player = null;
    public Thread t = new Thread(new Runnable() {

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
                            } catch(Exception e) {
                            }
                        }

                        player.addChatComponentMessage(requests.poll());
                        Thread.sleep(100);
                    } catch(Exception e) {
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    });

    public ChatMessageHandler() {
        t.start();
    }

    public void addLocalizedChatMessage(String message, ChatMessageType type, Object... options) {
        message = I18n.format(message, options);
        if(ReplayMod.replaySettings.isShowNotifications()) {
            message = prefix + toColor(message, type);
            ChatComponentText cct = new ChatComponentText(message);
            requests.add(cct);
        }
    }

    private String toColor(String message, ChatMessageType type) {
        if(type == ChatMessageType.INFORMATION) {
            return "§2" + message;
        } else if(type == ChatMessageType.WARNING) {
            return "§c" + message;
        }

        return message;
    }

    public void stop() {
        active = false;
    }

    public void initialize() {
        active = true;
        requests.clear();
        if(!ReplayMod.replaySettings.isShowNotifications()) {
            System.out.println("Chat messages are disabled");
        }
    }

    public enum ChatMessageType {
        INFORMATION, WARNING;
    }
}
