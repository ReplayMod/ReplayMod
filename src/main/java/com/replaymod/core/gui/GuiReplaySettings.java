package com.replaymod.core.gui;

import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiToggleButton;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import com.replaymod.core.SettingsRegistry;
import net.minecraft.client.resources.I18n;

public class GuiReplaySettings extends AbstractGuiScreen<GuiReplaySettings> {

    public GuiReplaySettings(final net.minecraft.client.gui.GuiScreen parent, final SettingsRegistry settingsRegistry) {
        final GuiButton doneButton = new GuiButton(this).setI18nLabel("gui.done").setSize(200, 20).onClick(new Runnable() {
            @Override
            public void run() {
                getMinecraft().displayGuiScreen(parent);
            }
        });

        final GuiPanel allElements = new GuiPanel(this).setLayout(new HorizontalLayout().setSpacing(10));
        GuiPanel leftColumn = new GuiPanel().setLayout(new VerticalLayout().setSpacing(4));
        GuiPanel rightColumn = new GuiPanel().setLayout(new VerticalLayout().setSpacing(4));
        allElements.addElements(new VerticalLayout.Data(0), leftColumn, rightColumn);
        HorizontalLayout.Data leftHorizontalData = new HorizontalLayout.Data(1);
        HorizontalLayout.Data rightHorizontalData = new HorizontalLayout.Data(0);
        int i = 0;
        for (final SettingsRegistry.SettingKey<?> key : settingsRegistry.getSettings()) {
            if (key.getDisplayString() != null) {
                GuiElement<?> element;
                if (key.getDefault() instanceof Boolean) {
                    @SuppressWarnings("unchecked")
                    final SettingsRegistry.SettingKey<Boolean> booleanKey = (SettingsRegistry.SettingKey<Boolean>) key;
                    final GuiToggleButton button = new GuiToggleButton<>().setSize(150, 20)
                            .setLabel(key.getDisplayString()).setSelected(settingsRegistry.get(booleanKey) ? 0 : 1)
                            .setValues(I18n.format("options.on"), I18n.format("options.off"));
                    element = button.onClick(new Runnable() {
                        @Override
                        public void run() {
                            settingsRegistry.set(booleanKey, button.getSelected() == 0);
                            settingsRegistry.save();
                        }
                    });
                } else {
                    throw new IllegalArgumentException("Type " + key.getDefault().getClass() + " not supported.");
                }

                if (i++ % 2 == 0) {
                    leftColumn.addElements(leftHorizontalData, element);
                } else {
                    rightColumn.addElements(rightHorizontalData, element);
                }
            }
        }

        setLayout(new CustomLayout<GuiReplaySettings>() {
            @Override
            protected void layout(GuiReplaySettings container, int width, int height) {
                pos(allElements, width / 2 - 155, height / 6);
                pos(doneButton, width / 2 - 100, height - 27);
            }
        });

        setTitle(new GuiLabel().setI18nText("replaymod.gui.settings.title"));
    }

    @Override
    protected GuiReplaySettings getThis() {
        return this;
    }
}
