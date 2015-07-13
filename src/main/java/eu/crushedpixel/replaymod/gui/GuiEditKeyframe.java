package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Marker;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.MouseUtils;
import eu.crushedpixel.replaymod.utils.RoundUtils;
import eu.crushedpixel.replaymod.utils.TimestampUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.Point;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GuiEditKeyframe extends GuiScreen {

    private enum KeyframeType {
        MARKER, POSITION, TIME, SPECTATOR;

        public static KeyframeType fromKeyframe(Keyframe kf) {
            if(kf.getValue() instanceof Marker) return MARKER;
            if(kf.getValue() instanceof TimestampValue) return TIME;
            if(kf.getValue() instanceof AdvancedPosition) {
                AdvancedPosition pos = (AdvancedPosition)kf.getValue();
                if(pos.getSpectatedEntityID() == null) return POSITION;
                else return SPECTATOR;
            }

            return null;
        }
    }

    private boolean initialized = false;

    private GuiAdvancedButton saveButton, cancelButton;
    private GuiArrowButton leftButton, rightButton;


    private GuiNumberInput kfMin, kfSec, kfMs;
    private GuiNumberInput xCoord, yCoord, zCoord, pitch, yaw, roll;
    private GuiNumberInput min, sec, ms;

    private GuiAdvancedTextField markerNameInput;

    private ComposedElement inputs;
    private ComposedElement posInputs;

    private int virtualHeight = 200;
    private int virtualY;

    private Keyframe keyframe;
    private Keyframe keyframeBackup;
    private boolean save;

    private KeyframeType keyframeType;

    private Keyframe previous, next;

    private int w2;
    private int w3;
    private int totalWidth;
    private int left;

    private String screenTitle;

    public GuiEditKeyframe(Keyframe keyframe) {
        this.keyframe = keyframe;
        this.keyframeBackup = keyframe.copy();
        this.keyframeType = KeyframeType.fromKeyframe(keyframe);

        KeyframeList<AdvancedPosition> positionKeyframes = ReplayHandler.getPositionKeyframes();
        KeyframeList<TimestampValue> timeKeyframes = ReplayHandler.getTimeKeyframes();
        KeyframeList<Marker> markerKeyframes = ReplayHandler.getMarkerKeyframes();

        if(keyframeType == KeyframeType.POSITION) {
            previous = positionKeyframes.getPreviousKeyframe(keyframe.getRealTimestamp() - 1);
            next = positionKeyframes.getNextKeyframe(keyframe.getRealTimestamp() + 1);

            screenTitle = I18n.format("replaymod.gui.editkeyframe.title.pos");
        } else if(keyframeType == KeyframeType.TIME) {
            previous = timeKeyframes.getPreviousKeyframe(keyframe.getRealTimestamp() - 1);
            next = timeKeyframes.getNextKeyframe(keyframe.getRealTimestamp() + 1);

            screenTitle = I18n.format("replaymod.gui.editkeyframe.title.time");
        } else if(keyframeType == KeyframeType.MARKER) {
            previous = markerKeyframes.getPreviousKeyframe(keyframe.getRealTimestamp() - 1);
            next = markerKeyframes.getNextKeyframe(keyframe.getRealTimestamp() + 1);
        }

        ReplayMod.replaySender.setReplaySpeed(0);
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        virtualY = this.height - virtualHeight - 10;

        if(!initialized) {
            saveButton = new GuiAdvancedButton(GuiConstants.KEYFRAME_EDITOR_SAVE_BUTTON, 0, 0, I18n.format("replaymod.gui.save")) {
                @Override
                public void performAction() {
                    save = true;
                    mc.displayGuiScreen(null);
                }
            };

            cancelButton = new GuiAdvancedButton(GuiConstants.KEYFRAME_EDITOR_CANCEL_BUTTON, 0, 0, I18n.format("replaymod.gui.cancel")) {
                @Override
                public void performAction() {
                    save = false;
                    mc.displayGuiScreen(null);
                }
            };

            leftButton = new GuiArrowButton(GuiConstants.KEYFRAME_EDITOR_LEFT_BUTTON, 0, 0, "", GuiArrowButton.Direction.LEFT) {
                @Override
                public void performAction() {
                    save = true;
                    mc.displayGuiScreen(new GuiEditKeyframe(previous));
                }
            };

            rightButton = new GuiArrowButton(GuiConstants.KEYFRAME_EDITOR_RIGHT_BUTTON, 0, 0, "", GuiArrowButton.Direction.RIGHT){
                @Override
                public void performAction() {
                    save = true;
                    mc.displayGuiScreen(new GuiEditKeyframe(next));
                }
            };

            saveButton.width = cancelButton.width = 100;

            leftButton.enabled = previous != null;
            rightButton.enabled = next != null;

            //Real Time Input
            int timestamp = keyframe.getRealTimestamp();
            min = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 30, 0d, 9d, (double)TimestampUtils.getMinutesFromTimestamp(timestamp), false, "", 1);
            sec = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 25, 0d, 59d, (double)TimestampUtils.getSecondsFromTimestamp(timestamp), false, "", 1);
            ms = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 35, 0d, 999d, (double)TimestampUtils.getMillisecondsFromTimestamp(timestamp), false, "", 1);

            inputs = new ComposedElement(min, sec, ms, saveButton, cancelButton, leftButton, rightButton);

            //Position/Virtual Time Input
            if(keyframeType == KeyframeType.POSITION) {
                AdvancedPosition pos = ((Keyframe<AdvancedPosition>) keyframe).getValue();
                xCoord = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, null, null, RoundUtils.round2Decimals(pos.getX()), true);
                yCoord = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, null, null, RoundUtils.round2Decimals(pos.getY()), true);
                zCoord = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, null, null, RoundUtils.round2Decimals(pos.getZ()), true);
                yaw = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, -90d, 90d, RoundUtils.round2Decimals(pos.getYaw()), true);
                pitch = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, -180d, 180d, RoundUtils.round2Decimals(pos.getPitch()), true);
                roll = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, null, null, RoundUtils.round2Decimals(pos.getRoll()), true);

                posInputs = new ComposedElement(xCoord, yCoord, zCoord, yaw, pitch, roll);
                inputs.addPart(posInputs);
            } else if(keyframeType == KeyframeType.TIME) {
                int time = ((Keyframe<TimestampValue>) keyframe).getValue().asInt();

                kfMin = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 30, 0d, null, (double)TimestampUtils.getMinutesFromTimestamp(time), false, "", 1);
                kfSec = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 25, 0d, 59d, (double)TimestampUtils.getSecondsFromTimestamp(time), false, "", 1);
                kfMs = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 35, 0d, 999d, (double)TimestampUtils.getMillisecondsFromTimestamp(time), false, "", 1);

                inputs.addPart(kfMin);
                inputs.addPart(kfSec);
                inputs.addPart(kfMs);
            } else if(keyframeType == KeyframeType.MARKER) {
                String name = ((Keyframe<Marker>)keyframe).getValue().getName();
                if(name == null) name = "";
                markerNameInput = new GuiAdvancedTextField(fontRendererObj, 0, 0, 200, 20);
                markerNameInput.hint = I18n.format("replaymod.gui.editkeyframe.markername");
                markerNameInput.setText(name);

                inputs.addPart(markerNameInput);
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

        if(keyframeType == KeyframeType.POSITION) {
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
            for(GuiElement input : posInputs.getParts()) {
                if(input instanceof GuiTextField) {
                    GuiTextField textField = (GuiTextField)input;
                    textField.xPosition = i < 3 ? x : left+totalWidth-100;
                    textField.yPosition = i < 3 ? virtualY + 20 + i*30 : virtualY + 20 + (i-3)*30;
                    i++;
                }
            }
        } else if(keyframeType == KeyframeType.TIME) {
            kfMin.xPosition = min.xPosition;
            kfSec.xPosition = sec.xPosition;
            kfMs.xPosition = ms.xPosition;

            kfMin.yPosition = kfSec.yPosition = kfMs.yPosition = min.yPosition - 10 - 20;
        } else if(keyframeType == KeyframeType.MARKER) {
            markerNameInput.xPosition = width/2 - 100;
            markerNameInput.yPosition = height/2-10;
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
            if(keyframeType != KeyframeType.MARKER) {
                ReplayHandler.removeKeyframe(keyframe);
                ReplayHandler.addKeyframe(keyframeBackup);
            } else {
                ReplayHandler.getMarkerKeyframes().remove(keyframe);
                ReplayHandler.getMarkerKeyframes().add(keyframeBackup);
            }

            ReplayHandler.selectKeyframe(keyframeBackup);
        } else {
            keyframe.setRealTimestamp(TimestampUtils.calculateTimestamp(min.getIntValue(), sec.getIntValue(), ms.getIntValue()));
            if(keyframeType == KeyframeType.POSITION) {
                ((Keyframe<AdvancedPosition>) keyframe).setValue(new AdvancedPosition(xCoord.getPreciseValue(), yCoord.getPreciseValue(),
                        zCoord.getPreciseValue(), new Float(pitch.getPreciseValue()), (float) yaw.getPreciseValue(),
                        (float) roll.getPreciseValue(), null));
            } else if(keyframeType == KeyframeType.TIME) {
                ((Keyframe<TimestampValue>)keyframe).setValue(new TimestampValue(TimestampUtils.calculateTimestamp(kfMin.getIntValue(), kfSec.getIntValue(), kfMs.getIntValue())));
            } else if(keyframeType == KeyframeType.MARKER) {
                ((Keyframe<Marker>)keyframe).getValue().setName(markerNameInput.getText().trim());
            }
        }
        ReplayHandler.fireKeyframesModifyEvent();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        Point mousePos = MouseUtils.getMousePos();
        inputs.buttonPressed(mc, mousePos.getX(), mousePos.getY(), typedChar, keyCode);

        if(keyCode == Keyboard.KEY_ESCAPE) {
            save = false;
            mc.displayGuiScreen(null);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        inputs.mouseClick(mc, mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        inputs.mouseDrag(mc, mouseX, mouseY, clickedMouseButton);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        inputs.mouseRelease(mc, mouseX, mouseY, state);
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void updateScreen() {
        inputs.tick(mc);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawGradientRect(10, virtualY, width - 10, virtualY + virtualHeight, -1072689136, -804253680);
        drawCenteredString(fontRendererObj, screenTitle, this.width / 2, virtualY + 5, Color.WHITE.getRGB());

        drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.timelineposition") + ":", (width - w3) / 2, min.yPosition + 7, Color.WHITE.getRGB());
        drawString(fontRendererObj, I18n.format("replaymod.gui.minutes"), min.xPosition + min.width + 5, min.yPosition + 7, Color.WHITE.getRGB());
        drawString(fontRendererObj, I18n.format("replaymod.gui.seconds"), sec.xPosition+sec.width+5, sec.yPosition+7, Color.WHITE.getRGB());
        drawString(fontRendererObj, I18n.format("replaymod.gui.milliseconds"), ms.xPosition + ms.width + 5, ms.yPosition + 7, Color.WHITE.getRGB());

        if(keyframeType == KeyframeType.POSITION) {
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.xpos"), left, virtualY + 27, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.ypos"), left, virtualY + 57, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.zpos"), left, virtualY + 87, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.camyaw"), left+totalWidth-100-5-w2, virtualY + 27, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.campitch"), left+totalWidth-100-5-w2, virtualY + 57, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.camroll"), left+totalWidth-100-5-w2, virtualY + 87, Color.WHITE.getRGB());
        } else if(keyframeType == KeyframeType.TIME) {
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.timestamp")+":", (width-w3)/2, kfMin.yPosition + 7, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.minutes"), kfMin.xPosition + kfMin.width + 5, kfMin.yPosition + 7, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.seconds"), kfSec.xPosition+kfSec.width+5, kfSec.yPosition+7, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.milliseconds"), kfMs.xPosition+kfMs.width+5, kfMs.yPosition+7, Color.WHITE.getRGB());
        }

        inputs.draw(mc, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
