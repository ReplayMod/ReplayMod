package com.replaymod.simplepathing.gui;

import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.ExplicitInterpolationProperty;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.change.CombinedChange;
import com.replaymod.replaystudio.pathing.change.SetInterpolator;
import com.replaymod.replaystudio.pathing.change.UpdateKeyframeProperties;
import com.replaymod.replaystudio.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.simplepathing.Setting;
import com.replaymod.simplepathing.gui.GuiEditKeyframe.Position.InterpolationPanel.InterpolatorType;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.resources.I18n;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.ReadablePoint;

import java.util.Map;

import static com.replaymod.simplepathing.gui.GuiEditKeyframe.Position.InterpolationPanel.InterpolatorSettingsPanel.CubicInterpolatorSettingsPanel;
import static com.replaymod.simplepathing.gui.GuiEditKeyframe.Position.InterpolationPanel.InterpolatorSettingsPanel.LinearInterpolatorSettingsPanel;
import static de.johni0702.minecraft.gui.utils.Utils.link;

public abstract class GuiEditKeyframe<T extends GuiEditKeyframe<T>> extends AbstractGuiPopup<T> implements Typeable {
    private static GuiNumberField newGuiNumberField() {
        return new GuiNumberField().setPrecision(0).setValidateOnFocusChange(true);
    }

    protected static final Logger logger = LogManager.getLogger();

    protected final GuiPathing guiPathing;

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
                .addElements(new VerticalLayout.Data(0.5, false), title, inputs, timePanel, buttons);
    }

    public GuiEditKeyframe(GuiPathing gui, Path path, Keyframe keyframe, String type) {
        super(ReplayModReplay.instance.getReplayHandler().getOverlay());
        this.guiPathing = gui;
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
        public final GuiNumberField xField = newGuiNumberField().setSize(60, 20).setPrecision(5);
        public final GuiNumberField yField = newGuiNumberField().setSize(60, 20).setPrecision(5);
        public final GuiNumberField zField = newGuiNumberField().setSize(60, 20).setPrecision(5);

        public final GuiNumberField yawField = newGuiNumberField().setSize(60, 20).setPrecision(5);
        public final GuiNumberField pitchField = newGuiNumberField().setSize(60, 20).setPrecision(5);
        public final GuiNumberField rollField = newGuiNumberField().setSize(60, 20).setPrecision(5);

        public final InterpolationPanel interpolationPanel = new InterpolationPanel(guiPathing);

        {
            GuiPanel positionInputs = new GuiPanel()
                    .setLayout(new GridLayout().setCellsEqualSize(false).setColumns(4).setSpacingX(3).setSpacingY(5))
                    .addElements(new GridLayout.Data(1, 0.5),
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.xpos"), xField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camyaw"), yawField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.ypos"), yField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.campitch"), pitchField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.zpos"), zField,
                            new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camroll"), rollField);

            inputs.setLayout(new VerticalLayout().setSpacing(10)).addElements(new VerticalLayout.Data(0.5, false),
                    positionInputs, interpolationPanel);
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
            Change setInterpolatorChange = null;

            UpdateKeyframeProperties.Builder builder = UpdateKeyframeProperties.create(path, keyframe)
                    .setValue(CameraProperties.POSITION, Triple.of(xField.getDouble(), yField.getDouble(), zField.getDouble()))
                    .setValue(CameraProperties.ROTATION, Triple.of(yawField.getFloat(), pitchField.getFloat(), rollField.getFloat()))
                    .removeProperty(ExplicitInterpolationProperty.PROPERTY);

            // if the interpolator is not the default, set the ExplicitInterpolationProperty flag
            if (interpolationPanel.getInterpolatorType() != InterpolatorType.DEFAULT) {
                PathSegment toModify = null;
                for (PathSegment segment : path.getSegments()) {
                    if (segment.getStartKeyframe() == keyframe) {
                        toModify = segment;
                        break;
                    }
                }

                if (toModify != null) {
                    builder.setValue(ExplicitInterpolationProperty.PROPERTY, new Object());

                    Interpolator interpolator = interpolationPanel.getInterpolatorSettingsPanel().createInterpolator();
                    interpolator.registerProperty(CameraProperties.POSITION);
                    interpolator.registerProperty(CameraProperties.ROTATION);

                    setInterpolatorChange = SetInterpolator.create(toModify, interpolator);
                } else {
                    logger.warn("The Path segment to modify was not found. Setting interpolator to default.");
                }
            }

            Change keyframePropertiesChange = builder.done();
            keyframePropertiesChange.apply(path.getTimeline());

            if (setInterpolatorChange == null) {
                return keyframePropertiesChange;
            } else {
                setInterpolatorChange.apply(path.getTimeline());
                guiPathing.updateInterpolators();
                path.updateAll();
            }

            return CombinedChange.createFromApplied(keyframePropertiesChange, setInterpolatorChange);
        }

        @Override
        protected Position getThis() {
            return this;
        }

        public static class InterpolationPanel extends de.johni0702.minecraft.gui.container.AbstractGuiContainer<InterpolationPanel> {

            private final GuiPathing guiPathing;

            @Getter
            private InterpolatorSettingsPanel interpolatorSettingsPanel;

            private GuiDropdownMenu<InterpolatorType> dropdown;

            @AllArgsConstructor
            public enum InterpolatorType {
                DEFAULT("default", null),
                CUBIC("cubic", CubicSplineInterpolator.class),
                LINEAR("linear", LinearInterpolator.class);

                private String localizationKey;

                @Getter
                private Class<? extends Interpolator> interpolatorClass;

                @Override
                public String toString() {
                    return I18n.format(String.format("replaymod.gui.editkeyframe.interpolator.%1$s.name", localizationKey));
                }

                public String getI18nDescription() {
                    return String.format("replaymod.gui.editkeyframe.interpolator.%1$s.desc", localizationKey);
                }

                public static InterpolatorType fromString(String string) {
                    for (InterpolatorType t : values()) {
                        if (t.toString().equals(string)) return t;
                    }
                    return CUBIC; //the default
                }

            }

            public InterpolationPanel(GuiPathing guiPathing) {
                this.guiPathing = guiPathing;

                setLayout(new VerticalLayout());

                dropdown = new GuiDropdownMenu<InterpolatorType>().setValues(InterpolatorType.values()).setHeight(20);
                dropdown.onSelection((index) -> setSettingsPanel(dropdown.getSelectedValue()));

                // set hover tooltips
                for (Map.Entry<InterpolatorType, IGuiClickable> e : dropdown.getDropdownEntries().entrySet()) {
                    e.getValue().setTooltip(new GuiTooltip().setI18nText(e.getKey().getI18nDescription()));
                }

                GuiPanel dropdownPanel = new GuiPanel()
                        .setLayout(new GridLayout().setCellsEqualSize(false).setColumns(2).setSpacingX(3).setSpacingY(5))
                        .addElements(new GridLayout.Data(1, 0.5),
                                new GuiLabel().setI18nText("replaymod.gui.editkeyframe.interpolator"), dropdown);


                addElements(new VerticalLayout.Data(0.5, false), dropdownPanel);

                dropdown.onSelection(0); // trigger the callback once to display settings panel
            }

            public void setSettingsPanel(InterpolatorType type) {
                removeElement(this.interpolatorSettingsPanel);

                InterpolatorSettingsPanel settingsPanel = null;
                switch (getInterpolatorTypeNoDefault(type)) {
                    case CUBIC:
                        settingsPanel = new CubicInterpolatorSettingsPanel();
                        break;
                    case LINEAR:
                        settingsPanel = new LinearInterpolatorSettingsPanel();
                        break;
                }

                addElements(new GridLayout.Data(0.5, 0.5), settingsPanel);

                this.interpolatorSettingsPanel = settingsPanel;
            }

            protected InterpolatorType getInterpolatorTypeNoDefault(InterpolatorType interpolatorType) {
                if (interpolatorType == InterpolatorType.DEFAULT || interpolatorType == null) {
                    InterpolatorType defaultType = InterpolatorType.fromString(
                            guiPathing.getMod().getCore().getSettingsRegistry().get(Setting.DEFAULT_INTERPOLATION));
                    return defaultType;
                }
                return interpolatorType;
            }

            public InterpolatorType getInterpolatorType() {
                return dropdown.getSelectedValue();
            }

            @Override
            protected InterpolationPanel getThis() {
                return this;
            }

            public static abstract class InterpolatorSettingsPanel<I extends Interpolator, T extends InterpolatorSettingsPanel> extends de.johni0702.minecraft.gui.container.GuiPanel {

                public abstract void loadSettings(I interpolator);

                public abstract I createInterpolator();

                public static class CubicInterpolatorSettingsPanel extends InterpolatorSettingsPanel<CubicSplineInterpolator, CubicInterpolatorSettingsPanel> {

                    @Override
                    public void loadSettings(CubicSplineInterpolator interpolator) {
                    }

                    @Override
                    public CubicSplineInterpolator createInterpolator() {
                        return new CubicSplineInterpolator();
                    }

                    @Override
                    protected InterpolatorSettingsPanel<CubicSplineInterpolator, CubicInterpolatorSettingsPanel> getThis() {
                        return this;
                    }
                }

                public static class LinearInterpolatorSettingsPanel extends InterpolatorSettingsPanel<LinearInterpolator, LinearInterpolatorSettingsPanel> {

                    @Override
                    public void loadSettings(LinearInterpolator interpolator) {
                    }

                    @Override
                    public LinearInterpolator createInterpolator() {
                        return new LinearInterpolator();
                    }

                    @Override
                    protected InterpolatorSettingsPanel<LinearInterpolator, LinearInterpolatorSettingsPanel> getThis() {
                        return this;
                    }
                }
            }
        }
    }
}
