package eu.crushedpixel.replaymod.events.handlers;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.GuiReplaySettings;
import eu.crushedpixel.replaymod.gui.online.GuiLoginPrompt;
import eu.crushedpixel.replaymod.gui.online.GuiReplayCenter;
import eu.crushedpixel.replaymod.gui.replayeditor.GuiReplayEditor;
import eu.crushedpixel.replaymod.gui.replayviewer.GuiReplayViewer;
import eu.crushedpixel.replaymod.online.authentication.AuthenticationHandler;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplayProcess;
import eu.crushedpixel.replaymod.studio.VersionValidator;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import eu.crushedpixel.replaymod.utils.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.util.Point;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GuiEventHandler {

    private static final Color DARK_RED = Color.decode("#DF0101");
    private static final Color DARK_GREEN = Color.decode("#01DF01");
    private final Minecraft mc = Minecraft.getMinecraft();

    public int replayCount = 0;
    private GuiButton editorButton;

    @SubscribeEvent
    public void onGui(GuiOpenEvent event) {
        if(event.gui instanceof GuiMainMenu) {
            if(ReplayMod.firstMainMenu) {
                ReplayMod.firstMainMenu = false;
                if(!AuthenticationHandler.isAuthenticated()) {
                    event.gui = new GuiLoginPrompt(event.gui, event.gui, false).toMinecraft();
                    return;
                }
            } else {
                try {
                    mc.timer.timerSpeed = 1;
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            if(ReplayHandler.isInReplay()) ReplayHandler.setInReplay(false);
        }

        if(!AuthenticationHandler.isAuthenticated()) return;

        if(event.gui instanceof GuiChat || event.gui instanceof GuiInventory) {
            if(ReplayHandler.isInReplay()) {
                event.setCanceled(true);
            }
        } else if(event.gui instanceof GuiDisconnected) {
            if(!ReplayHandler.isInReplay() && System.currentTimeMillis() - ReplayHandler.lastExit < 5000) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onDraw(DrawScreenEvent e) {
        if(e.gui instanceof GuiMainMenu) {
            e.gui.drawString(mc.fontRendererObj, I18n.format("replaymod.title")+":", 5, 5, Color.WHITE.getRGB());
            if(AuthenticationHandler.isAuthenticated()) {
                e.gui.drawString(mc.fontRendererObj, I18n.format("replaymod.gui.loggedin").toUpperCase(), 5, 15, DARK_GREEN.getRGB());
            } else {
                e.gui.drawString(mc.fontRendererObj, I18n.format("replaymod.gui.loggedout").toUpperCase(), 5, 15, DARK_RED.getRGB());
            }

            //if version not up to date, display info string
            if(!ReplayMod.isLatestModVersion()) {
                int width = Math.max(100, e.gui.width / 2 - 100 - 10);

                String[] lines = StringUtils.splitStringInMultipleRows(I18n.format("replaymod.gui.outdated"), width);

                int maxLineWidth = 0;
                for(String line : lines) {
                    int lineWidth = mc.fontRendererObj.getStringWidth(line);
                    if(lineWidth > maxLineWidth) {
                        maxLineWidth = lineWidth;
                    }
                }

                Gui.drawRect(2, 77, 5+maxLineWidth+3, 80+(lines.length * 10), 0x80FF0000);

                int i = 0;
                for(String line : lines) {
                    mc.fontRendererObj.drawStringWithShadow(line, 5, 80 + (i * 10), Color.WHITE.getRGB());
                    i++;
                }
            }

            if(replayCount == 0) {
                if(editorButton.isMouseOver()) {
                    Point mouse = MouseUtils.getMousePos();
                    ReplayMod.tooltipRenderer.drawTooltip(mouse.getX(), mouse.getY(), I18n.format("replaymod.gui.morereplays"), e.gui, Color.RED);
                }
            } else if(!VersionValidator.isValid) {
                if(editorButton.isMouseOver()) {
                    Point mouse = MouseUtils.getMousePos();
                    ReplayMod.tooltipRenderer.drawTooltip(mouse.getX(), mouse.getY(), I18n.format("replaymod.gui.java"), e.gui, Color.RED);
                }
            }
        }
    }

    @SubscribeEvent
    public void onInit(InitGuiEvent event) {
        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = event.buttonList;
        if(event.gui instanceof GuiIngameMenu && ReplayHandler.isInReplay()) {
            ReplayMod.replaySender.setReplaySpeed(0);
            for(GuiButton b : new ArrayList<GuiButton>(buttonList)) {
                if(b.id == 1) {
                    b.displayString = I18n.format("replaymod.gui.exit");
                    b.yPosition -= 24 * 2;
                    b.id = GuiConstants.EXIT_REPLAY_BUTTON;
                } else if(b.id >= 5 && b.id <= 7) {
                    buttonList.remove(b);
                } else if(b.id != 4) {
                    b.yPosition -= 24 * 2;
                }
            }
        } else if(event.gui instanceof GuiMainMenu) {
            int i1 = event.gui.height / 4 + 24 + 10;

            for(GuiButton b : buttonList) {
                if(b.id != 0 && b.id != 4 && b.id != 5) {
                    b.yPosition = b.yPosition - 2 * 24 + 10;
                }
            }

            GuiButton rm = new GuiButton(GuiConstants.REPLAY_MANAGER_BUTTON_ID, event.gui.width / 2 - 100, i1 + 2 * 24, I18n.format("replaymod.gui.replayviewer"));
            rm.width = rm.width / 2 - 2;
            //rm.enabled = AuthenticationHandler.isAuthenticated();
            buttonList.add(rm);

            replayCount = ReplayFileIO.getAllReplayFiles().size();

            GuiButton re = new GuiButton(GuiConstants.REPLAY_EDITOR_BUTTON_ID, event.gui.width / 2 + 2, i1 + 2 * 24, I18n.format("replaymod.gui.replayeditor"));
            re.width = re.width / 2 - 2;
            re.enabled = VersionValidator.isValid && replayCount > 0;
            buttonList.add(re);

            editorButton = re;

            GuiButton rc = new GuiButton(GuiConstants.REPLAY_CENTER_BUTTON_ID, event.gui.width / 2 - 100, i1 + 3 * 24, I18n.format("replaymod.gui.replaycenter"));
            rc.enabled = true;
            buttonList.add(rc);

        } else if(event.gui instanceof GuiOptions) {
            buttonList.add(new GuiButton(GuiConstants.REPLAY_OPTIONS_BUTTON_ID,
                    event.gui.width / 2 - 155, event.gui.height / 6 + 48 - 6 - 24, 310, 20, I18n.format("replaymod.gui.settings.title")));
        }
    }

    @SubscribeEvent
    public void onButton(ActionPerformedEvent event) {
        if(!event.button.enabled) return;
        if(event.gui instanceof GuiMainMenu) {
            if(event.button.id == GuiConstants.REPLAY_MANAGER_BUTTON_ID) {
                mc.displayGuiScreen(new GuiReplayViewer());
            } else if(event.button.id == GuiConstants.REPLAY_CENTER_BUTTON_ID) {
                if(AuthenticationHandler.isAuthenticated()) {
                    mc.displayGuiScreen(new GuiReplayCenter());
                } else {
                    mc.displayGuiScreen(new GuiLoginPrompt(event.gui, new GuiReplayCenter(), true).toMinecraft());
                }
            } else if(event.button.id == GuiConstants.REPLAY_EDITOR_BUTTON_ID) {
                mc.displayGuiScreen(new GuiReplayEditor());
            }
        } else if(event.gui instanceof GuiOptions && event.button.id == GuiConstants.REPLAY_OPTIONS_BUTTON_ID) {
            mc.displayGuiScreen(new GuiReplaySettings(event.gui));
        }

        if(ReplayHandler.isInReplay() && event.gui instanceof GuiIngameMenu && event.button.id == GuiConstants.EXIT_REPLAY_BUTTON) {
            if(ReplayHandler.isInPath()) ReplayProcess.stopReplayProcess(false);

            event.button.enabled = false;

            mc.displayGuiScreen(new GuiMainMenu());

            ReplayHandler.endReplay();
        }
    }

}
