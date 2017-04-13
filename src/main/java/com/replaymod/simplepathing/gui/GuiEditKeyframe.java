package com.replaymod.simplepathing.gui;

import com.replaymod.pathing.properties.CameraProperties;
import com.replaymod.pathing.properties.TimestampProperty;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.change.CombinedChange;
import com.replaymod.replaystudio.pathing.interpolation.CatmullRomSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;
import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.PathSegment;
import com.replaymod.simplepathing.InterpolatorType;
import com.replaymod.simplepathing.SPTimeline;
import com.replaymod.simplepathing.SPTimeline.SPPath;
import com.replaymod.simplepathing.Setting;
import com.replaymod.simplepathing.properties.ExplicitInterpolationProperty;
import de.johni0702.minecraft.gui.container.AbstractGuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiNumberField;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.element.IGuiClickable;
import de.johni0702.minecraft.gui.element.IGuiLabel;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import lombok.Getter;
import net.minecraft.client.resources.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.ReadablePoint;

import java.util.Map;
import java.util.Optional;

import static de.johni0702.minecraft.gui.utils.Utils.link;

public abstract class GuiEditKeyframe<T extends GuiEditKeyframe<T>> extends AbstractGuiPopup<T> implements Typeable {
    private static GuiNumberField newGuiNumberField() {
        return new GuiNumberField().setPrecision(0).setValidateOnFocusChange(true);
    }

    protected static final Logger logger = LogManager.getLogger();

    protected final GuiPathing guiPathing;

    protected final long time;
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

    public GuiEditKeyframe(GuiPathing gui, SPPath path, long time, String type) {
        super(ReplayModReplay.instance.getReplayHandler().getOverlay());
        this.guiPathing = gui;
        this.time = time;
        this.path = gui.getMod().getCurrentTimeline().getPath(path);
        this.keyframe = this.path.getKeyframe(time);

        Consumer<String> updateSaveButtonState = s -> saveButton.setEnabled(canSave());
        timeMinField.setValue(time / 1000 / 60).onTextChanged(updateSaveButtonState);
        timeSecField.setValue(time / 1000 % 60).onTextChanged(updateSaveButtonState);
        timeMSecField.setValue(time % 1000).onTextChanged(updateSaveButtonState);

        title.setI18nText("replaymod.gui.editkeyframe.title." + type);
        saveButton.onClick(() -> {
            Change change = save();
            long newTime = (timeMinField.getInteger() * 60 + timeSecField.getInteger()) * 1000 + timeMSecField.getInteger();
            if (newTime != time) {
                change = CombinedChange.createFromApplied(change,
                        gui.getMod().getCurrentTimeline().moveKeyframe(path, time, newTime));
                if (gui.getMod().getSelectedPath() == path && gui.getMod().getSelectedTime() == time) {
                    gui.getMod().setSelected(path, newTime);
                }
            }
            gui.getMod().getCurrentTimeline().getTimeline().pushChange(change);
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
        public Spectator(GuiPathing gui, SPPath path, long keyframe) {
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

        public Time(GuiPathing gui, SPPath path, long keyframe) {
            super(gui, path, keyframe, "time");

            this.keyframe.getValue(TimestampProperty.PROPERTY).ifPresent(time -> {
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
            return guiPathing.getMod().getCurrentTimeline().updateTimeKeyframe(keyframe.getTime(), time);
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

        public final InterpolationPanel interpolationPanel = new InterpolationPanel();

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

        public Position(GuiPathing gui, SPPath path, long keyframe) {
            super(gui, path, keyframe, "pos");

            this.keyframe.getValue(CameraProperties.POSITION).ifPresent(pos -> {
                xField.setValue(pos.getLeft());
                yField.setValue(pos.getMiddle());
                zField.setValue(pos.getRight());
            });
            this.keyframe.getValue(CameraProperties.ROTATION).ifPresent(rot -> {
                yawField.setValue(rot.getLeft());
                pitchField.setValue(rot.getMiddle());
                rollField.setValue(rot.getRight());
            });

            link(xField, yField, zField, yawField, pitchField, rollField, timeMinField, timeSecField, timeMSecField);

            popup.forEach(IGuiLabel.class).setColor(Colors.BLACK);
        }

        @Override
        protected Change save() {
            SPTimeline timeline = guiPathing.getMod().getCurrentTimeline();
            Change positionChange = timeline.updatePositionKeyframe(time,
                    xField.getDouble(), yField.getDouble(), zField.getDouble(),
                    yawField.getFloat(), pitchField.getFloat(), rollField.getFloat()
            );
            Interpolator interpolator = interpolationPanel.getSettingsPanel().createInterpolator();
            if (interpolationPanel.getInterpolatorType() == InterpolatorType.DEFAULT) {
                return CombinedChange.createFromApplied(positionChange, timeline.setInterpolatorToDefault(time),
                        timeline.setDefaultInterpolator(interpolator));
            } else {
                return CombinedChange.createFromApplied(positionChange, timeline.setInterpolator(time, interpolator));
            }
        }

        @Override
        protected Position getThis() {
            return this;
        }

        public class InterpolationPanel extends AbstractGuiContainer<InterpolationPanel> {

            @Getter
            private SettingsPanel settingsPanel;

            private GuiDropdownMenu<InterpolatorType> dropdown;

            public InterpolationPanel() {
                setLayout(new VerticalLayout());

                dropdown = new GuiDropdownMenu<InterpolatorType>()
                        .setToString(s -> I18n.format(s.getI18nName()))
                        .setValues(InterpolatorType.values()).setHeight(20)
                        .onSelection(i -> setSettingsPanel(dropdown.getSelectedValue()));

                // set hover tooltips
                for (Map.Entry<InterpolatorType, IGuiClickable> e : dropdown.getDropdownEntries().entrySet()) {
                    e.getValue().setTooltip(new GuiTooltip().setI18nText(e.getKey().getI18nDescription()));
                }

                GuiPanel dropdownPanel = new GuiPanel()
                        .setLayout(new GridLayout().setCellsEqualSize(false).setColumns(2).setSpacingX(3).setSpacingY(5))
                        .addElements(new GridLayout.Data(1, 0.5),
                                new GuiLabel().setI18nText("replaymod.gui.editkeyframe.interpolator"), dropdown);


                addElements(new VerticalLayout.Data(0.5, false), dropdownPanel);

                Optional<PathSegment> segment = path.getSegments().stream()
                        .filter(s -> s.getStartKeyframe() == keyframe).findFirst();
                if (segment.isPresent()) {
                    Interpolator interpolator = segment.get().getInterpolator();
                    InterpolatorType type = InterpolatorType.fromClass(interpolator.getClass());
                    if (keyframe.getValue(ExplicitInterpolationProperty.PROPERTY).isPresent()) {
                        dropdown.setSelected(type); // trigger the callback once to display settings panel
                    } else {
                        setSettingsPanel(InterpolatorType.DEFAULT);
                    }
                    if (getInterpolatorTypeNoDefault(type).getInterpolatorClass().isInstance(interpolator)) {
                        //noinspection unchecked
                        settingsPanel.loadSettings(interpolator);
                    }
                } else {
                    // Disable dropdown if this is the last keyframe
                    dropdown.setDisabled();
                }
            }

            public void setSettingsPanel(InterpolatorType type) {
                removeElement(this.settingsPanel);

                switch (getInterpolatorTypeNoDefault(type)) {
                    case CATMULL_ROM:
                        settingsPanel = new CatmullRomSettingsPanel();
                        break;
                    case CUBIC:
                        settingsPanel = new CubicSettingsPanel();
                        break;
                    case LINEAR:
                        settingsPanel = new LinearSettingsPanel();
                        break;
                }

                addElements(new GridLayout.Data(0.5, 0.5), settingsPanel);
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

            public abstract class SettingsPanel<I extends Interpolator, T extends SettingsPanel<I, T>> extends AbstractGuiContainer<T> {

                public abstract void loadSettings(I interpolator);

                public abstract I createInterpolator();
            }

            public class CatmullRomSettingsPanel extends SettingsPanel<CatmullRomSplineInterpolator, CatmullRomSettingsPanel> {
                public final GuiLabel alphaLabel = new GuiLabel().setColor(Colors.BLACK)
                        .setI18nText("replaymod.gui.editkeyframe.interpolator.catmullrom.alpha");
                public final GuiNumberField alphaField = new GuiNumberField().setSize(100, 20).setPrecision(5)
                        .setMinValue(0).setValidateOnFocusChange(true);

                {
                    setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER));
                    addElements(new HorizontalLayout.Data(0.5), alphaLabel, alphaField);
                }

                @Override
                public void loadSettings(CatmullRomSplineInterpolator interpolator) {
                    alphaField.setValue(interpolator.getAlpha());
                }

                @Override
                public CatmullRomSplineInterpolator createInterpolator() {
                    return new CatmullRomSplineInterpolator(alphaField.getDouble());
                }

                @Override
                protected CatmullRomSettingsPanel getThis() {
                    return this;
                }
            }

            public class CubicSettingsPanel extends SettingsPanel<CubicSplineInterpolator, CubicSettingsPanel> {

                @Override
                public void loadSettings(CubicSplineInterpolator interpolator) {
                }

                @Override
                public CubicSplineInterpolator createInterpolator() {
                    return new CubicSplineInterpolator();
                }

                @Override
                protected CubicSettingsPanel getThis() {
                    return this;
                }
            }

            public class LinearSettingsPanel extends SettingsPanel<LinearInterpolator, LinearSettingsPanel> {

                @Override
                public void loadSettings(LinearInterpolator interpolator) {
                }

                @Override
                public LinearInterpolator createInterpolator() {
                    return new LinearInterpolator();
                }

                @Override
                protected LinearSettingsPanel getThis() {
                    return this;
                }
            }
        }
    }
}
