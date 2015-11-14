package eu.crushedpixel.replaymod.events.handlers;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.gui.GuiReplaySettings;
import eu.crushedpixel.replaymod.gui.GuiConstants;
import eu.crushedpixel.replaymod.gui.replayeditor.GuiReplayEditor;
import eu.crushedpixel.replaymod.studio.VersionValidator;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.util.Point;

import java.awt.*;
import java.util.List;

public class GuiEventHandler {

    private static final Color DARK_RED = Color.decode("#DF0101");
    private static final Color DARK_GREEN = Color.decode("#01DF01");
    private final Minecraft mc = Minecraft.getMinecraft();

    public int replayCount = 0;
    private GuiButton editorButton;

    @SubscribeEvent
    public void onDraw(DrawScreenEvent e) {
        if(e.gui instanceof GuiMainMenu) {
            // TODO Do we need this?
//            e.gui.drawString(mc.fontRendererObj, I18n.format("replaymod.title")+":", 5, 5, Color.WHITE.getRGB());
//            if(ReplayMod.apiClient.isLoggedIn()) {
//                e.gui.drawString(mc.fontRendererObj, I18n.format("replaymod.gui.loggedin").toUpperCase(), 5, 15, DARK_GREEN.getRGB());
//            } else {
//                e.gui.drawString(mc.fontRendererObj, I18n.format("replaymod.gui.loggedout").toUpperCase(), 5, 15, DARK_RED.getRGB());
//            }

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
        if(event.gui instanceof GuiMainMenu) {
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

        } else if(event.gui instanceof GuiOptions) {
            buttonList.add(new GuiButton(GuiConstants.REPLAY_OPTIONS_BUTTON_ID,
                    event.gui.width / 2 - 155, event.gui.height / 6 + 48 - 6 - 24, 310, 20, I18n.format("replaymod.gui.settings.title")));
        }
    }

    @SubscribeEvent
    public void onButton(ActionPerformedEvent event) {
        if(!event.button.enabled) return;
        if(event.gui instanceof GuiMainMenu) {
            if(event.button.id == GuiConstants.REPLAY_EDITOR_BUTTON_ID) {
                mc.displayGuiScreen(new GuiReplayEditor());
            }
        } else if(event.gui instanceof GuiOptions && event.button.id == GuiConstants.REPLAY_OPTIONS_BUTTON_ID) {
            new GuiReplaySettings(event.gui, ReplayMod.instance.getSettingsRegistry()).display();
        }
    }

}
