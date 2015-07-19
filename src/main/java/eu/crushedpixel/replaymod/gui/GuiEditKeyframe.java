package eu.crushedpixel.replaymod.gui;

import eu.crushedpixel.replaymod.ReplayMod;
import eu.crushedpixel.replaymod.gui.elements.*;
import eu.crushedpixel.replaymod.gui.overlay.GuiReplayOverlay;
import eu.crushedpixel.replaymod.holders.AdvancedPosition;
import eu.crushedpixel.replaymod.holders.Keyframe;
import eu.crushedpixel.replaymod.holders.Marker;
import eu.crushedpixel.replaymod.holders.TimestampValue;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.interpolation.KeyframeValue;
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

public abstract class GuiEditKeyframe<T extends KeyframeValue> extends GuiScreen implements GuiReplayOverlay.NoOverlay {

    @SuppressWarnings("unchecked")
    public static GuiEditKeyframe create(Keyframe kf) {
        if(kf.getValue() instanceof Marker) return new GuiEditKeyframeMarker(kf);
        if(kf.getValue() instanceof TimestampValue) return new GuiEditKeyframeTime(kf);
        if(kf.getValue() instanceof AdvancedPosition) return new GuiEditKeyframePosition(kf);
        throw new UnsupportedOperationException("Keyframe type unknown: " + kf);
    }

    protected boolean initialized = false;

    private GuiAdvancedButton saveButton, cancelButton;
    private GuiArrowButton leftButton, rightButton;

    protected GuiNumberInput min, sec, ms;

    protected ComposedElement inputs;

    private int virtualHeight = 200;
    protected int virtualY;

    protected Keyframe<T> keyframe;
    protected Keyframe<T> keyframeBackup;
    protected boolean save;

    private Keyframe<?> previous, next;

    protected int w2;
    protected int w3;
    protected int totalWidth;
    protected int left;

    protected String screenTitle;

    public GuiEditKeyframe(Keyframe<T> keyframe, KeyframeList<T> keyframes) {
        this.keyframe = keyframe;
        this.keyframeBackup = keyframe.copy();

        previous = keyframes.getPreviousKeyframe(keyframe.getRealTimestamp(), false);
        next = keyframes.getNextKeyframe(keyframe.getRealTimestamp(), false);

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
                    mc.displayGuiScreen(create(previous));
                }
            };

            rightButton = new GuiArrowButton(GuiConstants.KEYFRAME_EDITOR_RIGHT_BUTTON, 0, 0, "", GuiArrowButton.Direction.RIGHT){
                @Override
                public void performAction() {
                    save = true;
                    mc.displayGuiScreen(create(next));
                }
            };

            saveButton.width = cancelButton.width = 100;

            leftButton.enabled = previous != null;
            rightButton.enabled = next != null;

            //Real Time Input
            int timestamp = keyframe.getRealTimestamp();
            min = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 30, 0d, 29d, (double)TimestampUtils.timestampToWholeMinutes(timestamp), false, "", 1);
            sec = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 25, 0d, 59d, (double)TimestampUtils.getSecondsFromTimestamp(timestamp), false, "", 1);
            ms = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 35, 0d, 999d, (double)TimestampUtils.getMillisecondsFromTimestamp(timestamp), false, "", 1);

            inputs = new ComposedElement(min, sec, ms, saveButton, cancelButton, leftButton, rightButton);
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
    }

    @Override
    public void onGuiClosed() {
        if(!save) {
            ReplayHandler.selectKeyframe(keyframeBackup);
        } else {
            keyframe.setRealTimestamp(TimestampUtils.calculateTimestamp(min.getIntValue(), sec.getIntValue(), ms.getIntValue()));
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

        drawScreen0();

        inputs.draw(mc, mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    protected void drawScreen0() {}

    private static class GuiEditKeyframeMarker extends GuiEditKeyframe<Marker> {
        private GuiAdvancedTextField markerNameInput;

        public GuiEditKeyframeMarker(Keyframe<Marker> keyframe) {
            super(keyframe, ReplayHandler.getMarkerKeyframes());
        }

        @Override
        public void initGui() {
            super.initGui();

            if (!initialized) {
                String name = keyframe.getValue().getName();
                if (name == null) name = "";
                markerNameInput = new GuiAdvancedTextField(fontRendererObj, 0, 0, 200, 20);
                markerNameInput.hint = I18n.format("replaymod.gui.editkeyframe.markername");
                markerNameInput.setText(name);

                inputs.addPart(markerNameInput);
            }

            markerNameInput.xPosition = width/2 - 100;
            markerNameInput.yPosition = height/2-10;

            initialized = true;
        }

        @Override
        public void onGuiClosed() {
            if (!save) {
                ReplayHandler.getMarkerKeyframes().remove(keyframe);
                ReplayHandler.getMarkerKeyframes().add(keyframeBackup);
            } else {
                keyframe.getValue().setName(markerNameInput.getText().trim());
            }
            super.onGuiClosed();
        }

    }

    private static class GuiEditKeyframeTime extends GuiEditKeyframe<TimestampValue> {
        private GuiNumberInput kfMin, kfSec, kfMs;

        public GuiEditKeyframeTime(Keyframe<TimestampValue> keyframe) {
            super(keyframe, ReplayHandler.getTimeKeyframes());
            screenTitle = I18n.format("replaymod.gui.editkeyframe.title.time");
        }

        @Override
        public void initGui() {
            super.initGui();

            if (!initialized) {
                int time = keyframe.getValue().asInt();

                kfMin = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 30, 0d, null, (double)TimestampUtils.timestampToWholeMinutes(time), false, "", 1);
                kfSec = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 25, 0d, 59d, (double)TimestampUtils.getSecondsFromTimestamp(time), false, "", 1);
                kfMs = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 35, 0d, 999d, (double)TimestampUtils.getMillisecondsFromTimestamp(time), false, "", 1);

                inputs.addPart(kfMin);
                inputs.addPart(kfSec);
                inputs.addPart(kfMs);
            }

            kfMin.xPosition = min.xPosition;
            kfSec.xPosition = sec.xPosition;
            kfMs.xPosition = ms.xPosition;

            kfMin.yPosition = kfSec.yPosition = kfMs.yPosition = min.yPosition - 10 - 20;

            initialized = true;
        }

        @Override
        protected void drawScreen0() {
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.timestamp")+":", (width-w3)/2, kfMin.yPosition + 7, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.minutes"), kfMin.xPosition + kfMin.width + 5, kfMin.yPosition + 7, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.seconds"), kfSec.xPosition + kfSec.width + 5, kfSec.yPosition + 7, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.milliseconds"), kfMs.xPosition + kfMs.width + 5, kfMs.yPosition + 7, Color.WHITE.getRGB());
        }

        @Override
        public void onGuiClosed() {
            if (!save) {
                ReplayHandler.removeKeyframe(keyframe);
                ReplayHandler.addKeyframe(keyframeBackup);
            } else {
                keyframe.setValue(new TimestampValue(TimestampUtils.calculateTimestamp(
                        kfMin.getIntValue(), kfSec.getIntValue(), kfMs.getIntValue())));
            }
            super.onGuiClosed();
        }

    }

    private static class GuiEditKeyframePosition extends GuiEditKeyframe<AdvancedPosition> {
        private GuiNumberInput xCoord, yCoord, zCoord, pitch, yaw, roll;
        private ComposedElement posInputs;

        public GuiEditKeyframePosition(Keyframe<AdvancedPosition> keyframe) {
            super(keyframe, ReplayHandler.getPositionKeyframes());
            screenTitle = I18n.format("replaymod.gui.editkeyframe.title.pos");
        }

        @Override
        public void initGui() {
            if (keyframe.getValue().getSpectatedEntityID() != null) {
                super.initGui();
                return;
            }

            super.initGui();

            if (!initialized) {
                AdvancedPosition pos = keyframe.getValue();
                xCoord = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, null, null, RoundUtils.round2Decimals(pos.getX()), true);
                yCoord = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, null, null, RoundUtils.round2Decimals(pos.getY()), true);
                zCoord = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, null, null, RoundUtils.round2Decimals(pos.getZ()), true);
                yaw = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, -180d, 180d, RoundUtils.round2Decimals(pos.getYaw()), true);
                pitch = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, -90d, 90d, RoundUtils.round2Decimals(pos.getPitch()), true);
                roll = new GuiDraggingNumberInput(fontRendererObj, 0, 0, 100, null, null, RoundUtils.round2Decimals(pos.getRoll()), true);

                posInputs = new ComposedElement(xCoord, yCoord, zCoord, yaw, pitch, roll);
                inputs.addPart(posInputs);
            }

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

            initialized = true;
        }

        @Override
        public void onGuiClosed() {
            if (!save) {
                ReplayHandler.removeKeyframe(keyframe);
                ReplayHandler.addKeyframe(keyframeBackup);
            } else {
                keyframe.setValue(new AdvancedPosition(xCoord.getPreciseValue(), yCoord.getPreciseValue(),
                        zCoord.getPreciseValue(), new Float(pitch.getPreciseValue()), (float) yaw.getPreciseValue(),
                        (float) roll.getPreciseValue(), null));
            }
            super.onGuiClosed();
        }

        @Override
        protected void drawScreen0() {
            if (keyframe.getValue().getSpectatedEntityID() != null) return;
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.xpos"), left, virtualY + 27, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.ypos"), left, virtualY + 57, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.zpos"), left, virtualY + 87, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.camyaw"), left + totalWidth - 100 - 5 - w2, virtualY + 27, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.campitch"), left + totalWidth - 100 - 5 - w2, virtualY + 57, Color.WHITE.getRGB());
            drawString(fontRendererObj, I18n.format("replaymod.gui.editkeyframe.camroll"), left + totalWidth - 100 - 5 - w2, virtualY + 87, Color.WHITE.getRGB());
        }

    }
}
