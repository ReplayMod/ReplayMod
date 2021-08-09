package com.replaymod.replay.gui.overlay;

import com.google.common.base.Strings;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.*;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replaystudio.data.Marker;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.function.Consumer;

public class GuiEditMarkerPopup extends AbstractGuiPopup<GuiEditMarkerPopup> implements Typeable {
    private static GuiTextField newGuiNumberField() {
        return new GuiTextField().setSize(150, 20);
    }


    private final Consumer<Marker> onSave;

    public final GuiLabel title = new GuiLabel().setI18nText("replaymod.gui.editkeyframe.title.marker");

    public final GuiTextField nameField = new GuiTextField().setSize(150, 20);
    // TODO: Replace with a min/sec/msec field
    public final GuiTextField timeField = newGuiNumberField();

    public final GuiTextField xField = newGuiNumberField();
    public final GuiTextField yField = newGuiNumberField();
    public final GuiTextField zField = newGuiNumberField();

    public final GuiTextField yawField = newGuiNumberField();
    public final GuiTextField pitchField = newGuiNumberField();
    public final GuiTextField rollField = newGuiNumberField();

    public final GuiPanel inputs = GuiPanel.builder()
            .layout(new GridLayout().setColumns(2).setSpacingX(7).setSpacingY(3))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.markername"), new GridLayout.Data(0, 0.5))
            .with(nameField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.timestamp"), new GridLayout.Data(0, 0.5))
            .with(timeField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.xpos"), new GridLayout.Data(0, 0.5))
            .with(xField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.ypos"), new GridLayout.Data(0, 0.5))
            .with(yField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.zpos"), new GridLayout.Data(0, 0.5))
            .with(zField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camyaw"), new GridLayout.Data(0, 0.5))
            .with(yawField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.campitch"), new GridLayout.Data(0, 0.5))
            .with(pitchField, new GridLayout.Data(1, 0.5))
            .with(new GuiLabel().setI18nText("replaymod.gui.editkeyframe.camroll"), new GridLayout.Data(0, 0.5))
            .with(rollField, new GridLayout.Data(1, 0.5))
            .build();

    public final GuiButton saveButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
            ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");

            Marker marker = new Marker();
            marker.setName(Strings.emptyToNull(nameField.getText()));
            try {

                marker.setTime((Integer) scriptEngine.eval(timeField.getText()));
                marker.setX((double) scriptEngine.eval(xField.getText()));
                marker.setY((double) scriptEngine.eval(yField.getText()));
                marker.setZ((double) scriptEngine.eval(zField.getText()));
                marker.setYaw((float) scriptEngine.eval(yawField.getText()));
                marker.setPitch((float) scriptEngine.eval(pitchField.getText()));
                marker.setRoll((float) scriptEngine.eval(rollField.getText()));
                onSave.accept(marker);
                close();
            } catch (ScriptException ignored) {}


        }
    }).setSize(150, 20).setI18nLabel("replaymod.gui.save");



    public final GuiButton cancelButton = new GuiButton().onClick(() -> close()).setSize(150, 20).setI18nLabel("replaymod.gui.cancel");

    public final GuiPanel buttons = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(7))
            .addElements(new HorizontalLayout.Data(0.5), saveButton, cancelButton);

    public GuiEditMarkerPopup(GuiContainer container, Marker marker, Consumer<Marker> onSave) {
        super(container);
        this.onSave = onSave;

        setBackgroundColor(Colors.DARK_TRANSPARENT);

        popup.setLayout(new VerticalLayout().setSpacing(5))
                .addElements(new VerticalLayout.Data(0.5), title, inputs, buttons);
        popup.invokeAll(IGuiLabel.class, e -> e.setColor(Colors.BLACK));

        nameField.setText(Strings.nullToEmpty(marker.getName()));
        timeField.setText(String.valueOf(marker.getTime()));
        xField.setText(String.valueOf(marker.getX()));
        yField.setText(String.valueOf(marker.getY()));
        zField.setText(String.valueOf(marker.getZ()));
        yawField.setText(String.valueOf(marker.getYaw()));
        pitchField.setText(String.valueOf(marker.getPitch()));
        rollField.setText(String.valueOf(marker.getRoll()));
    }

    @Override
    public void open() {
        super.open();
    }

    @Override
    protected GuiEditMarkerPopup getThis() {
        return this;
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            cancelButton.onClick();
            return true;
        }
        return false;
    }


}
