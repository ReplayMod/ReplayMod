package com.replaymod.simplepathing.gui;

import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.change.CombinedChange;
import com.replaymod.replaystudio.pathing.change.UpdateKeyframeProperties;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiNumberField;
import de.johni0702.minecraft.gui.element.IGuiLabel;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import org.apache.commons.lang3.tuple.Triple;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.ReadablePoint;

import static de.johni0702.minecraft.gui.utils.Utils.link;

public abstract class GuiEditKeyframe<T extends GuiEditKeyframe<T>> extends AbstractGuiPopup<T> implements Typeable {
    private static GuiNumberField newGuiNumberField() {
        return new GuiNumberField().setPrecision(0).setValidateOnFocusChange(true);
    }

    protected final Keyframe keyframe;
    protected final Path path;

    public final GuiLabel title = new GuiLabel();

    public final GuiPanel inputs = new GuiPanel();

    public final GuiNumberField timeMinField = newGuiNumberField().setSize(30, 20).setMinValue(0);
    public final GuiNumberField timeSecField = newGuiNumberField().setSize(20, 20).setMinValue(0).setMaxValue(59);
    public final GuiNumberField timeMSecField = newGuiNumberField().setSize(30, 20).setMinValue(0).setMaxValue(999);

    public final GuiPanel timePanel = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(3))
            .addElements(new HorizontalLayout.Data(0.5),
                    new GuiLabel().setI18nText("replaymod.gui.editkeyframe.timelineposition"),
                    timeMinField, new GuiLabel().setI18nText("replaymod.gui.minutes"),
                    timeSecField, new GuiLabel().setI18nText("replaymod.gui.seconds"),
                    timeMSecField, new GuiLabel().setI18nText("replaymod.gui.milliseconds"));

    public final GuiButton saveButton = new GuiButton().setSize(150, 20).setI18nLabel("replaymod.gui.save");

    public final GuiButton cancelButton = new GuiButton()
            .onClick(this::close).setSize(150, 20).setI18nLabel("replaymod.gui.cancel");

    public final GuiPanel buttons = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(7))
            .addElements(new HorizontalLayout.Data(0.5), saveButton, cancelButton);

    {
        setBackgroundColor(Colors.DARK_TRANSPARENT);

        popup.setLayout(new VerticalLayout().setSpacing(10))
                .addElements(new VerticalLayout.Data(0.5), title, inputs, timePanel, buttons);
    }

    public GuiEditKeyframe(GuiPathing gui, Path path, Keyframe keyframe, String type) {
        super(ReplayModReplay.instance.getReplayHandler().getOverlay());
        this.keyframe = keyframe;
        this.path = path;

        long time = keyframe.getTime();
        Consumer<String> updateSaveButtonState = s -> saveButton.setEnabled(canSave());
        timeMinField.setValue(time / 1000 / 60).onTextChanged(updateSaveButtonState);
        timeSecField.setValue(time / 1000 % 60).onTextChanged(updateSaveButtonState);
        timeMSecField.setValue(time % 1000).onTextChanged(updateSaveButtonState);

        title.setI18nText("replaymod.gui.editkeyframe.title." + type);
        saveButton.onClick(() -> {
            Change change = save();
            long newTime = (timeMinField.getInteger() * 60 + timeSecField.getInteger()) * 1000 + timeMSecField.getInteger();
            if (newTime != keyframe.getTime()) {
                change = CombinedChange.createFromApplied(change, gui.moveKeyframe(path, keyframe, newTime));
            }
            path.getTimeline().pushChange(change);
            close();
        });
    }

    private boolean canSave() {
        long newTime = (timeMinField.getInteger() * 60 + timeSecField.getInteger()) * 1000 + timeMSecField.getInteger();
        if (newTime != keyframe.getTime() && path.getKeyframe(newTime) != null) {
            return false;
        }
        return true;
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            cancelButton.onClick();
            return true;
        }
        return false;
    }

    @Override
    public void open() {
        super.open();
    }

    protected abstract Change save();

    public static class Spectator extends GuiEditKeyframe<Spectator> {
        public Spectator(GuiPathing gui, Path path, Keyframe keyframe) {
            super(gui, path, keyframe, "spec");

            link(timeMinField, timeSecField, timeMSecField);

            popup.forEach(IGuiLabel.class).setColor(Colors.BLACK);
        }

        @Override
        protected Change save() {
            return CombinedChange.createFromApplied();
        }

        @Override
        protected Spectator getThis() {
            return this;
        }
    }

    public static class Time extends GuiEditKeyframe<Time> {
        public final GuiNumberField timestampMinField = newGuiNumberField().setSize(30, 20).setMinValue(0);
        public final GuiNumberField timestampSecField = newGuiNumberField().setSize(20, 20).setMinValue(0).setMaxValue(59);
        public final GuiNumberField timestampMSecField = newGuiNumberField().setSize(30, 20).setMinValue(0).setMaxValue(999);

        {
            inputs.setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(3))
                    .addElements(new HorizontalLayout.Data(0.5),
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.timestamp"),
                            timestampMinField, new GuiLabel().setI18nText("replaymod.gui.minutes"),
                            timestampSecField, new GuiLabel().setI18nText("replaymod.gui.seconds"),
                            timestampMSecField, new GuiLabel().setI18nText("replaymod.gui.milliseconds"));
        }

        public Time(GuiPathing gui, Path path, Keyframe keyframe) {
            super(gui, path, keyframe, "time");

            keyframe.getValue(TimestampProperty.PROPERTY).ifPresent(time -> {
                timestampMinField.setValue(time / 1000 / 60);
                timestampSecField.setValue(time / 1000 % 60);
                timestampMSecField.setValue(time % 1000);
            });

            link(timestampMinField, timestampSecField, timestampMSecField, timeMinField, timeSecField, timeMSecField);

            popup.forEach(IGuiLabel.class).setColor(Colors.BLACK);
        }

        @Override
        protected Change save() {
            int time = (timestampMinField.getInteger() * 60 + timestampSecField.getInteger()) * 1000
                    + timestampMSecField.getInteger();
            Change change = UpdateKeyframeProperties.create(path, keyframe)
                    .setValue(TimestampProperty.PROPERTY, time)
                    .done();
            change.apply(path.getTimeline());
            return change;
        }

        @Override
        protected Time getThis() {
            return this;
        }
    }

    public static class Position extends GuiEditKeyframe<Position> {
        public final GuiNumberField xField = newGuiNumberField().setSize(150, 20).setPrecision(10);
        public final GuiNumberField yField = newGuiNumberField().setSize(150, 20).setPrecision(10);
        public final GuiNumberField zField = newGuiNumberField().setSize(150, 20).setPrecision(10);

        public final GuiNumberField yawField = newGuiNumberField().setSize(150, 20).setPrecision(5);
        public final GuiNumberField pitchField = newGuiNumberField().setSize(150, 20).setPrecision(5);
        public final GuiNumberField rollField = newGuiNumberField().setSize(150, 20).setPrecision(5);

        {
            inputs.setLayout(new GridLayout().setCellsEqualSize(false).setColumns(4).setSpacingX(3).setSpacingY(5))
                    .addElements(new GridLayout.Data(1, 0.5),
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.xpos"), xField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camyaw"), yawField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.ypos"), yField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.campitch"), pitchField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.zpos"), zField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camroll"), rollField);
        }

        public Position(GuiPathing gui, Path path, Keyframe keyframe) {
            super(gui, path, keyframe, "pos");

            keyframe.getValue(CameraProperties.POSITION).ifPresent(pos -> {
                xField.setValue(pos.getLeft());
                yField.setValue(pos.getMiddle());
                zField.setValue(pos.getRight());
            });
            keyframe.getValue(CameraProperties.ROTATION).ifPresent(rot -> {
                yawField.setValue(rot.getLeft());
                pitchField.setValue(rot.getMiddle());
                rollField.setValue(rot.getRight());
            });

            link(xField, yField, zField, yawField, pitchField, rollField, timeMinField, timeSecField, timeMSecField);

            popup.forEach(IGuiLabel.class).setColor(Colors.BLACK);
        }

        @Override
        protected Change save() {
            Change change = UpdateKeyframeProperties.create(path, keyframe)
                    .setValue(CameraProperties.POSITION, Triple.of(xField.getDouble(), yField.getDouble(), zField.getDouble()))
                    .setValue(CameraProperties.ROTATION, Triple.of(yawField.getFloat(), pitchField.getFloat(), rollField.getFloat()))
                    .done();
            change.apply(path.getTimeline());
            return change;
        }

        @Override
        protected Position getThis() {
            return this;
        }
    }
}
