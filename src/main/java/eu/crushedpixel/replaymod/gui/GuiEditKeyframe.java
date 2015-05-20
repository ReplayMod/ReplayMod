package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.gui.elements.GuiNumberInput;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.PositionKeyframe;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiEditKeyframe extends GuiScreen {

    private boolean initialized = false;

    private GuiButton saveButton, cancelButton;

    private GuiNumberInput xCoord, yCoord, zCoord, pitch, yaw;
    private List<GuiNumberInput> inputs = new ArrayList<GuiNumberInput>();

    private int virtualHeight = 200;
    private int virtualY;

    private Keyframe keyframe;
    private Keyframe keyframeBackup;
    private boolean save;
    private boolean posKeyframe;

    private int w, w2;
    private int totalWidth;
    private int left;

    public GuiEditKeyframe(Keyframe keyframe) {
        this.keyframe = keyframe;
        this.keyframeBackup = (Keyframe)keyframe.clone();
        this.posKeyframe = keyframe instanceof PositionKeyframe;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        virtualY = this.height - virtualHeight - 10;

        if(!initialized) {
            saveButton = new GuiButton(GuiConstants.KEYFRAME_EDITOR_SAVE_BUTTON, 0, 0, 100, 20, I18n.format("replaymod.gui.save"));
            cancelButton = new GuiButton(GuiConstants.KEYFRAME_EDITOR_CANCEL_BUTTON, 0, 0, 100, 20, I18n.format("replaymod.gui.cancel"));

            if(posKeyframe) {
                Position pos = ((PositionKeyframe)keyframe).getPosition();
                xCoord = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_X_INPUT, fontRendererObj, 0, 0, 100, null, null, round(pos.getX()), true);
                yCoord = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_Y_INPUT, fontRendererObj, 0, 0, 100, null, null, round(pos.getY()), true);
                zCoord = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_Z_INPUT, fontRendererObj, 0, 0, 100, null, null, round(pos.getZ()), true);
                yaw = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_YAW_INPUT, fontRendererObj, 0, 0, 100, -90d, 90d, round(pos.getYaw()), true);
                pitch = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_PITCH_INPUT, fontRendererObj, 0, 0, 100, -180d, 180d, round(pos.getPitch()), true);

                inputs.add(xCoord);
                inputs.add(yCoord);
                inputs.add(zCoord);
                inputs.add(yaw);
                inputs.add(pitch);
            }
        }

        if(posKeyframe) {
            w = Math.max(fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.xpos")),
                    Math.max(fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.ypos")),
                            fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.zpos"))));
            w2 = Math.max(fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.camyaw")),
                    fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.campitch")));

            totalWidth = w+100+w2+100+5+5+10;
            left = (this.width - totalWidth)/2;

            int x = w + left + 5;
            int i=0;
            for(GuiNumberInput input : inputs) {
                input.xPosition = i < 3 ? x : left+totalWidth-100;
                input.yPosition = i < 3 ? virtualY + 20 + i*30 : virtualY + 20 + (i-3)*30;
                i++;
            }
        }

        saveButton.yPosition = cancelButton.yPosition = virtualY + virtualHeight - 20 - 5;
        saveButton.xPosition = this.width - 100 - 5 - 10;
        cancelButton.xPosition = saveButton.xPosition - 100 - 5;

        buttonList.add(saveButton);
        buttonList.add(cancelButton);

        initialized = true;
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for(GuiNumberInput input : inputs) {
            if(input != null) input.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for(GuiNumberInput input : inputs) {
            if(input != null) input.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        for(GuiNumberInput input : inputs) {
            if(input != null) input.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(10, virtualY, width - 10, virtualY + virtualHeight, -1072689136, -804253680);
        drawCenteredString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.title"), this.width / 2, virtualY + 5, Color.WHITE.getRGB());

        if(posKeyframe) {
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.xpos"), left, virtualY + 27, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.ypos"), left, virtualY + 57, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.zpos"), left, virtualY + 87, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.camyaw"), left+totalWidth-100-5-w2, virtualY + 27, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.campitch"), left+totalWidth-100-5-w2, virtualY + 57, Color.WHITE.getRGB());
        }

        for(GuiNumberInput input : inputs) {
            if(input != null) input.drawTextBox();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private double round(double val) {
        return Math.round(val*100.0) / 100.0;
    }
}
