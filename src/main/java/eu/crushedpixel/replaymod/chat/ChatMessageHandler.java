package eu.crushedpixel.replaymod.chat;

import eu.crushedpixel.replaymod.ReplayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@SideOnly(Side.CLIENT)
public class ChatMessageHandler {

    private static final IChatComponent prefix;

    static {
        //results in "§8[§6Replay Mod§8]§r "

        ChatStyle dark_gray = new ChatStyle().setColor(EnumChatFormatting.DARK_GRAY);
        ChatStyle gold = new ChatStyle().setColor(EnumChatFormatting.GOLD);

        prefix = new ChatComponentText("[").setChatStyle(dark_gray)
                .appendSibling(new ChatComponentTranslation("replaymod.title").setChatStyle(gold))
                .appendSibling(new ChatComponentText("] "));
    }


    private boolean active = true;
    private boolean alive = true;
    private Queue<IChatComponent> requests = new ConcurrentLinkedQueue<IChatComponent>();
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
                            Thread.sleep(100);
                            player = Minecraft.getMinecraft().thePlayer;
                        }

                        IChatComponent message = requests.poll();
                        if (message != null) {
                            player.addChatComponentMessage(message);
                        }
                        Thread.sleep(100);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }, "replaymod-chat-message-handler");

    public ChatMessageHandler() {
        t.start();
    }

    public void addLocalizedChatMessage(String message, ChatMessageType type, Object... options) {
        message = I18n.format(message, options);
        if(ReplayMod.replaySettings.isShowNotifications()) {

            ChatComponentText cct = new ChatComponentText(message);
            cct.setChatStyle(type.getChatStyle());

            requests.add(prefix.createCopy().appendSibling(cct));
        }
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
        INFORMATION(new ChatStyle().setColor(EnumChatFormatting.DARK_GREEN)), WARNING(new ChatStyle().setColor(EnumChatFormatting.RED));

        private ChatStyle chatStyle;

        public ChatStyle getChatStyle() {
            return chatStyle;
        }

        ChatMessageType(ChatStyle chatStyle) {
            this.chatStyle = chatStyle;
        }
    }
}
