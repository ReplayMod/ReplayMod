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
import com.udojava.evalex.Expression;
import de.johni0702.minecraft.gui.container.AbstractGuiContainer;
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
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import net.minecraft.client.resource.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Optional;

//#if MC>=11400
import com.replaymod.core.versions.MCVer.Keyboard;
//#else
//$$ import org.lwjgl.input.Keyboard;
//#endif

import static de.johni0702.minecraft.gui.utils.Utils.link;

public abstract class GuiEditKeyframe<T extends GuiEditKeyframe<T>> extends AbstractGuiPopup<T> implements Typeable {
    private static GuiExpressionField newGuiExpressionField() {
        return new GuiExpressionField();
    }

    DecimalFormat df = new DecimalFormat("###.#####");

    protected static final Logger logger = LogManager.getLogger();

    protected final GuiPathing guiPathing;

    protected final long time;
    protected final Keyframe keyframe;
    protected final Path path;

    public final GuiLabel title = new GuiLabel();

    public final GuiPanel inputs = new GuiPanel();

    public final GuiExpressionField timeMinField = newGuiExpressionField().setSize(50, 20);
    public final GuiExpressionField timeSecField = newGuiExpressionField().setSize(50, 20);
    public final GuiExpressionField timeMSecField = newGuiExpressionField().setSize(50, 20);

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

    protected boolean canSave() {
        try {
            double timeMin = timeMinField.getDouble();
            double timeSec = timeSecField.getDouble();
            double timeMSec = timeMSecField.getDouble();

            long newTime = (long) ((timeMin * 60 + timeSec) * 1000 + timeMSec);

            if (newTime < 0 || newTime > guiPathing.kt.getTimeline().getLengthMillis()) {
                return false;
            }
            return newTime == keyframe.getTime() || path.getKeyframe(newTime) == null;
        } catch (Expression.ExpressionException | ArithmeticException | NumberFormatException e) { return false; }
    }

    public GuiEditKeyframe(GuiPathing gui, SPPath path, long time, String type) {
        super(ReplayModReplay.instance.getReplayHandler().getOverlay());
        this.guiPathing = gui;
        this.time = time;
        this.path = gui.getMod().getCurrentTimeline().getPath(path);
        this.keyframe = this.path.getKeyframe(time);

        Consumer<String> updateSaveButtonState = s -> saveButton.setEnabled(canSave());
        timeMinField.setText(String.valueOf(time / 1000 / 60)).onTextChanged(updateSaveButtonState);
        timeSecField.setText(String.valueOf(time / 1000 % 60)).onTextChanged(updateSaveButtonState);
        timeMSecField.setText(String.valueOf(time % 1000)).onTextChanged(updateSaveButtonState);

        title.setI18nText("replaymod.gui.editkeyframe.title." + type);
        saveButton.onClick(() -> {
            double timeMin = timeMinField.getDouble();
            double timeSec = timeSecField.getDouble();
            double timeMSec = timeMSecField.getDouble();

            Change change = save();
            long newTime = (long) ((timeMin * 60 + timeSec) * 1000 + timeMSec);
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

            popup.invokeAll(IGuiLabel.class, e -> e.setColor(Colors.BLACK));
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
        public final GuiExpressionField timestampMinField = newGuiExpressionField().setSize(50, 20);
        public final GuiExpressionField timestampSecField = newGuiExpressionField().setSize(50, 20);
        public final GuiExpressionField timestampMSecField = newGuiExpressionField().setSize(50, 20);

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
                timestampMinField.setText(String.valueOf(time / 1000 / 60));
                timestampSecField.setText(String.valueOf(time / 1000 % 60));
                timestampMSecField.setText(String.valueOf(time % 1000));
            });

            Consumer<String> updateSaveButtonState = s -> saveButton.setEnabled(canSave());

            timestampMinField.onTextChanged(updateSaveButtonState);
            timestampMSecField.onTextChanged(updateSaveButtonState);
            timestampMSecField.onTextChanged(updateSaveButtonState);

            link(timestampMinField, timestampSecField, timestampMSecField, timeMinField, timeSecField, timeMSecField);

            popup.invokeAll(IGuiLabel.class, e -> e.setColor(Colors.BLACK));
        }

        @Override
        protected Change save() throws Expression.ExpressionException, ArithmeticException, NumberFormatException {

            double timeMin = timestampMinField.getDouble();
            double timeSec = timestampSecField.getDouble();
            double timeMSec = timestampMSecField.getDouble();

            int time = (int) ((timeMin * 60 + timeSec) * 1000 + timeMSec);

            return guiPathing.getMod().getCurrentTimeline().updateTimeKeyframe(keyframe.getTime(), time);

        }

        @Override
        protected boolean canSave(){
            try {

                double timeMin = timestampMinField.getDouble();
                double timeSec = timestampSecField.getDouble();
                double timeMSec = timestampMSecField.getDouble();

                long time = (long) ((timeMin * 60 + timeSec) * 1000 + timeMSec);

                if (time < 0) { //TODO add check to make sure time isn't longer than the replay
                    return false;
                } else {
                    return super.canSave();
                }
            } catch (Expression.ExpressionException | ArithmeticException | NumberFormatException e) { return false; }
        }

        @Override
        protected Time getThis() {
            return this;
        }
    }

    public static class Position extends GuiEditKeyframe<Position> {
        public final GuiExpressionField xField = newGuiExpressionField().setSize(90, 20);
        public final GuiExpressionField yField = newGuiExpressionField().setSize(90, 20);
        public final GuiExpressionField zField = newGuiExpressionField().setSize(90, 20);

        public final GuiExpressionField yawField = newGuiExpressionField().setSize(90, 20);
        public final GuiExpressionField pitchField = newGuiExpressionField().setSize(90, 20);
        public final GuiExpressionField rollField = newGuiExpressionField().setSize(90, 20);

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


            Consumer<String> updateSaveButtonState = s -> saveButton.setEnabled(canSave());
            this.keyframe.getValue(CameraProperties.POSITION).ifPresent(pos -> {
                xField.setText(df.format(pos.getLeft()));
                yField.setText(df.format(pos.getMiddle()));
                zField.setText(df.format(pos.getRight()));
            });
            this.keyframe.getValue(CameraProperties.ROTATION).ifPresent(rot -> {
                yawField.setText(df.format(rot.getLeft()));
                pitchField.setText(df.format(rot.getMiddle()));
                rollField.setText(df.format(rot.getRight()));
            });

            xField.onTextChanged(updateSaveButtonState);
            yField.onTextChanged(updateSaveButtonState);
            zField.onTextChanged(updateSaveButtonState);
            yawField.onTextChanged(updateSaveButtonState);
            pitchField.onTextChanged(updateSaveButtonState);
            rollField.onTextChanged(updateSaveButtonState);

            link(xField, yField, zField, yawField, pitchField, rollField, timeMinField, timeSecField, timeMSecField);

            popup.invokeAll(IGuiLabel.class, e -> e.setColor(Colors.BLACK));
        }

        @Override
        protected boolean canSave(){

            if(xField.setPrecision(14).isExpressionValid() &&
                    yField.setPrecision(14).isExpressionValid() &&
                    zField.setPrecision(14).isExpressionValid() &&
                    yawField.setPrecision(11).isExpressionValid() &&
                    pitchField.setPrecision(11).isExpressionValid() &&
                    rollField.setPrecision(11).isExpressionValid()){
                return super.canSave();
            } else {
                return false;
            }
        }

        @Override
        protected Change save() throws Expression.ExpressionException, ArithmeticException, NumberFormatException  {

            double x = xField.setPrecision(14).getDouble();
            double y = yField.setPrecision(14).getDouble();
            double z = zField.setPrecision(14).getDouble();
            float yaw = yawField.setPrecision(11).getFloat();
            float pitch = pitchField.setPrecision(11).getFloat();
            float roll = rollField.setPrecision(11).getFloat();

            SPTimeline timeline = guiPathing.getMod().getCurrentTimeline();
            Change positionChange = timeline.updatePositionKeyframe(time, x, y, z, yaw, pitch, roll);
            if (interpolationPanel.getSettingsPanel() == null) {
                // The last keyframe doesn't have interpolator settings because there is no segment following it
                return positionChange;
            }
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

            private SettingsPanel settingsPanel;

            private GuiDropdownMenu<InterpolatorType> dropdown;

            public InterpolationPanel() {
                setLayout(new VerticalLayout());

                dropdown = new GuiDropdownMenu<InterpolatorType>()
                        .setToString(s -> I18n.translate(s.getI18nName()))
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
                        type = InterpolatorType.DEFAULT;
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

            public SettingsPanel getSettingsPanel() {
                return settingsPanel;
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
