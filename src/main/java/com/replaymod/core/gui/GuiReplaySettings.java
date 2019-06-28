package com.replaymod.core.gui;

import com.replaymod.core.SettingsRegistry;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiToggleButton;
import de.johni0702.minecraft.gui.element.advanced.GuiDropdownMenu;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.utils.Consumer;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.resource.language.I18n;

import java.util.List;

public class GuiReplaySettings extends AbstractGuiScreen<GuiReplaySettings> {

    public GuiReplaySettings(final net.minecraft.client.gui.screen.Screen parent, final SettingsRegistry settingsRegistry) {
        final GuiButton doneButton = new GuiButton(this).setI18nLabel("gui.done").setSize(200, 20).onClick(new Runnable() {
            @Override
            public void run() {
                getMinecraft().openScreen(parent);
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
                            .setI18nLabel(key.getDisplayString()).setSelected(settingsRegistry.get(booleanKey) ? 0 : 1)
                            .setValues(I18n.translate("options.on"), I18n.translate("options.off"));
                    element = button.onClick(new Runnable() {
                        @Override
                        public void run() {
                            settingsRegistry.set(booleanKey, button.getSelected() == 0);
                            settingsRegistry.save();
                        }
                    });
                } else if (key instanceof SettingsRegistry.MultipleChoiceSettingKey) {
                    final SettingsRegistry.MultipleChoiceSettingKey<?> multipleChoiceKey =
                            (SettingsRegistry.MultipleChoiceSettingKey<?>) key;
                    List<?> values = multipleChoiceKey.getChoices();
                    MultipleChoiceDropdownEntry[] entries = new MultipleChoiceDropdownEntry[values.size()];
                    int selected = 0;
                    Object currentValue = settingsRegistry.get(multipleChoiceKey);
                    for (int j = 0; j < entries.length; j++) {
                        Object value = values.get(j);
                        entries[j] = new MultipleChoiceDropdownEntry(value,
                                I18n.translate(multipleChoiceKey.getDisplayString()) + ": " + I18n.translate(value.toString()));
                        if (currentValue.equals(value)) {
                            selected = j;
                        }
                    }
                    final GuiDropdownMenu<MultipleChoiceDropdownEntry> menu = new GuiDropdownMenu<MultipleChoiceDropdownEntry>() {
                        @Override
                        protected ReadableDimension calcMinSize() {
                            ReadableDimension size = super.calcMinSize();
                            if (size.getWidth() > 150) {
                                return new Dimension(150, size.getHeight());
                            } else {
                                return size;
                            }
                        }
                    }.setSize(150, 20).setValues(entries);
                    menu.setSelected(selected).onSelection(new Consumer<Integer>() {
                        @Override
                        public void consume(Integer obj) {
                            settingsRegistry.set((SettingsRegistry.SettingKey) multipleChoiceKey,
                                    menu.getSelectedValue().value);
                            settingsRegistry.save();
                        }
                    });
                    element = menu;
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

    @RequiredArgsConstructor
    private static class MultipleChoiceDropdownEntry {
        private final Object value;
        private final String text;

        @Override
        public String toString() {
            return text;
        }
    }
}
