package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.GuiArrowButton;
import eu.crushedpixel.replaymod.gui.elements.GuiNumberInput;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Position;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.RoundUtils;
import eu.crushedpixel.replaymod.utils.TimestampUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiEditKeyframe extends GuiScreen {

    private boolean initialized = false;

    private GuiButton saveButton, cancelButton;
    private GuiArrowButton leftButton, rightButton;

    private GuiNumberInput xCoord, yCoord, zCoord, pitch, yaw, roll;
    private GuiNumberInput min, sec, ms;

    private List<GuiTextField> inputs = new ArrayList<GuiTextField>();
    private List<GuiTextField> posInputs = new ArrayList<GuiTextField>();

    private int virtualHeight = 200;
    private int virtualY;

    private Keyframe keyframe;
    private Keyframe keyframeBackup;
    private boolean save;
    private boolean posKeyframe;

    private Keyframe previous, next;

    private int w2;
    private int w3;
    private int totalWidth;
    private int left;

    private String screenTitle;

    public GuiEditKeyframe(Keyframe keyframe) {
        this.keyframe = keyframe;
        this.keyframeBackup = keyframe.copy();
        this.posKeyframe = keyframe.getValue() instanceof Position;
        boolean timeKeyframe = keyframe.getValue() instanceof TimestampValue;

        ReplayHandler.selectKeyframe(null);

        KeyframeList<Keyframe<Position>> positionKeyframes = ReplayHandler.getPositionKeyframes();
        KeyframeList<Keyframe<TimestampValue>> timeKeyframes = ReplayHandler.getTimeKeyframes();

        if(posKeyframe) {
            previous = positionKeyframes.getPreviousKeyframe(keyframe.getRealTimestamp() - 1);
            next = positionKeyframes.getNextKeyframe(keyframe.getRealTimestamp() + 1);

            screenTitle = I18n.format("replaymod.gui.editkeyframe.title.pos");
        } else if(timeKeyframe) {
            previous = timeKeyframes.getPreviousKeyframe(keyframe.getRealTimestamp() - 1);
            next = timeKeyframes.getNextKeyframe(keyframe.getRealTimestamp() + 1);

            screenTitle = I18n.format("replaymod.gui.editkeyframe.title.time");
        }

        ReplayHandler.selectKeyframe(keyframe);

        ReplayMod.replaySender.setReplaySpeed(0);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        virtualY = this.height - virtualHeight - 10;

        if(!initialized) {
            saveButton = new GuiButton(GuiConstants.KEYFRAME_EDITOR_SAVE_BUTTON, 0, 0, 100, 20, I18n.format("replaymod.gui.save"));
            cancelButton = new GuiButton(GuiConstants.KEYFRAME_EDITOR_CANCEL_BUTTON, 0, 0, 100, 20, I18n.format("replaymod.gui.cancel"));

            leftButton = new GuiArrowButton(GuiConstants.KEYFRAME_EDITOR_LEFT_BUTTON, 0, 0, "", GuiArrowButton.Direction.LEFT);
            rightButton = new GuiArrowButton(GuiConstants.KEYFRAME_EDITOR_RIGHT_BUTTON, 0, 0, "", GuiArrowButton.Direction.RIGHT);

            leftButton.enabled = previous != null;
            rightButton.enabled = next != null;

            //Real Time Input
            int timestamp = keyframe.getRealTimestamp();
            min = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_REAL_MIN_INPUT, fontRendererObj, 0, 0, 30, 0, 9, TimestampUtils.getMinutesFromTimestamp(timestamp), false);
            sec = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_REAL_SEC_INPUT, fontRendererObj, 0, 0, 25, 0, 59, TimestampUtils.getSecondsFromTimestamp(timestamp), false);
            ms = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_REAL_SEC_INPUT, fontRendererObj, 0, 0, 35, 0, 999, TimestampUtils.getMillisecondsFromTimestamp(timestamp), false);

            inputs.add(min);
            inputs.add(sec);
            inputs.add(ms);

            //Position/Virtual Time Input
            if(posKeyframe) {
                Position pos = ((Keyframe<Position>)keyframe).getValue();
                xCoord = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_X_INPUT, fontRendererObj, 0, 0, 100, null, null, RoundUtils.round(pos.getX()), true);
                yCoord = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_Y_INPUT, fontRendererObj, 0, 0, 100, null, null, RoundUtils.round(pos.getY()), true);
                zCoord = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_Z_INPUT, fontRendererObj, 0, 0, 100, null, null, RoundUtils.round(pos.getZ()), true);
                yaw = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_YAW_INPUT, fontRendererObj, 0, 0, 100, -90d, 90d, RoundUtils.round(pos.getYaw()), true);
                pitch = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_PITCH_INPUT, fontRendererObj, 0, 0, 100, -180d, 180d, RoundUtils.round(pos.getPitch()), true);
                roll = new GuiNumberInput(GuiConstants.KEYFRAME_EDITOR_ROLL_INPUT, fontRendererObj, 0, 0, 100, null, null, RoundUtils.round(pos.getRoll()), true);

                posInputs.add(xCoord);
                posInputs.add(yCoord);
                posInputs.add(zCoord);
                posInputs.add(yaw);
                posInputs.add(pitch);
                posInputs.add(roll);

                inputs.addAll(posInputs);
            }
        }

        w3 =    fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.timelineposition")+":")+10+
                fontRendererObj.getStringWidth(I18n.format("replaymod.gui.minutes"))+5+min.width+5+
                fontRendererObj.getStringWidth(I18n.format("replaymod.gui.seconds"))+5+sec.width+5+
                fontRendererObj.getStringWidth(I18n.format("replaymod.gui.milliseconds"))+5+ms.width;

        int l = (this.width-w3)/2;
        min.xPosition = fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.timelineposition")+":")+10 + l;
        sec.xPosition = fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.timelineposition")+":")+10 + l +
                min.width + 5 + fontRendererObj.getStringWidth(I18n.format("replaymod.gui.minutes")) + 5;
        ms.xPosition = fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.timelineposition")+":")+10 + l +
                min.width + 5 + fontRendererObj.getStringWidth(I18n.format("replaymod.gui.minutes")) + 5
                + sec.width + 5 + fontRendererObj.getStringWidth(I18n.format("replaymod.gui.seconds")) + 5;

        min.yPosition = sec.yPosition = ms.yPosition = virtualY+virtualHeight-65;

        if(posKeyframe) {
            int w = Math.max(fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.xpos")),
                    Math.max(fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.ypos")),
                            fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.zpos"))));
            w2 = Math.max(fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.camyaw")),
                    Math.max(fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.campitch")),
                            fontRendererObj.getStringWidth(I18n.format("replaymod.gui.editkeyframe.camroll"))));

            totalWidth = w +100+w2+100+5+5+10;
            left = (this.width - totalWidth)/2;

            int x = w + left + 5;
            int i = 0;
            for(GuiTextField input : posInputs) {
                input.xPosition = i < 3 ? x : left+totalWidth-100;
                input.yPosition = i < 3 ? virtualY + 20 + i*30 : virtualY + 20 + (i-3)*30;
                i++;
            }
        }

        saveButton.yPosition = cancelButton.yPosition = virtualY + virtualHeight - 20 - 5;
        saveButton.xPosition = this.width - 100 - 5 - 10;
        cancelButton.xPosition = saveButton.xPosition - 100 - 5;

        @SuppressWarnings("unchecked")
        List<GuiButton> buttonList = this.buttonList;

        buttonList.add(saveButton);
        buttonList.add(cancelButton);

        leftButton.xPosition = 15;
        rightButton.xPosition = width-35;
        leftButton.yPosition = rightButton.yPosition = virtualY + ((virtualHeight - leftButton.height)/2);

        buttonList.add(leftButton);
        buttonList.add(rightButton);

        initialized = true;
    }

    @Override
    public void onGuiClosed() {
        if(!save) {
            ReplayHandler.removeKeyframe(keyframe);
            ReplayHandler.addKeyframe(keyframeBackup);
            ReplayHandler.selectKeyframe(keyframeBackup);
        } else {
            keyframe.setRealTimestamp(TimestampUtils.calculateTimestamp(min.getIntValue(), sec.getIntValue(), ms.getIntValue()));
            if(posKeyframe) {
                ((Keyframe<Position>)keyframe).setValue(new Position(xCoord.getPreciseValue(), yCoord.getPreciseValue(),
                        zCoord.getPreciseValue(), new Float(pitch.getPreciseValue()), (float) yaw.getPreciseValue(),
                        (float) roll.getPreciseValue(), null));
            }
        }
        ReplayHandler.fireKeyframesModifyEvent();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        for(GuiTextField input : inputs) {
            if(input != null) input.textboxKeyTyped(typedChar, keyCode);
        }

        if(keyCode == Keyboard.KEY_ESCAPE) {
            save = false;
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        for(GuiTextField input : inputs) {
            if(input != null) input.mouseClicked(mouseX, mouseY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        for(GuiTextField input : inputs) {
            if(input != null) input.updateCursorCounter();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(10, virtualY, width - 10, virtualY + virtualHeight, -1072689136, -804253680);
        drawCenteredString(fontRendererObj, screenTitle, this.width / 2, virtualY + 5, Color.WHITE.getRGB());

        drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.timelineposition")+":", (width-w3)/2, min.yPosition + 7, Color.WHITE.getRGB());
        drawString(fontRendererObj, I18n.format("replaymod.gui.minutes"), min.xPosition + min.width + 5, min.yPosition + 7, Color.WHITE.getRGB());
        drawString(fontRendererObj, I18n.format("replaymod.gui.seconds"), sec.xPosition+sec.width+5, sec.yPosition+7, Color.WHITE.getRGB());
        drawString(fontRendererObj, I18n.format("replaymod.gui.milliseconds"), ms.xPosition+ms.width+5, ms.yPosition+7, Color.WHITE.getRGB());

        if(posKeyframe) {
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.xpos"), left, virtualY + 27, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.ypos"), left, virtualY + 57, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.zpos"), left, virtualY + 87, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.camyaw"), left+totalWidth-100-5-w2, virtualY + 27, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.campitch"), left+totalWidth-100-5-w2, virtualY + 57, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.camroll"), left+totalWidth-100-5-w2, virtualY + 87, Color.WHITE.getRGB());
        }

        for(GuiTextField input : inputs) {
            if(input != null) input.drawTextBox();
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(!button.enabled) return;
        if(button.id == GuiConstants.KEYFRAME_EDITOR_SAVE_BUTTON) {
            save = true;
            mc.displayGuiScreen(null);
        } else if(button.id == GuiConstants.KEYFRAME_EDITOR_CANCEL_BUTTON) {
            save = false;
            mc.displayGuiScreen(null);
        } else if(button.id == GuiConstants.KEYFRAME_EDITOR_LEFT_BUTTON) {
            save = false;
            mc.displayGuiScreen(new GuiEditKeyframe(previous));
        } else if(button.id == GuiConstants.KEYFRAME_EDITOR_RIGHT_BUTTON) {
            save = false;
            mc.displayGuiScreen(new GuiEditKeyframe(next));
        }
    }
}
