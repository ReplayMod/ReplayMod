package com.replaymod.replay.gui.screen;

import com.replaymod.core.utils.ModCompat;
import com.replaymod.replaystudio.data.ModInfo;
import com.replaymod.replaystudio.util.I18n;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiVerticalList;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;

import java.util.Map;

public class GuiModCompatWarning extends AbstractGuiScreen<GuiModCompatWarning> {
    public final GuiVerticalList content = new GuiVerticalList(this).setDrawShadow(true).setDrawSlider(true);
    public final GuiButton loadButton = new GuiButton().setI18nLabel("replaymod.gui.load").setSize(200, 20);
    public final GuiButton cancelButton = new GuiButton().setI18nLabel("gui.cancel").setSize(200, 20);
    public final GuiPanel closeButtons = new GuiPanel(this).setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, loadButton, cancelButton);

    {
        setTitle(new GuiLabel().setI18nText("replaymod.gui.modwarning.title"));
        setLayout(new CustomLayout<GuiModCompatWarning>() {
            @Override
            protected void layout(GuiModCompatWarning container, int width, int height) {
                pos(content, 10, 35);
                pos(closeButtons, width / 2 - width(closeButtons) / 2, height - 10 - height(closeButtons));
                size(content, width - 20, y(closeButtons) - 10 - y(content));
            }
        });

        content.getListLayout().setSpacing(8);
    }

    public GuiModCompatWarning(ModCompat.ModInfoDifference difference) {
        VerticalLayout.Data data = new VerticalLayout.Data(0.5);
        GuiPanel content = this.content.getListPanel();
        content.addElements(data, new GuiLabel().setI18nText("replaymod.gui.modwarning.message1"));
        content.addElements(data, new GuiLabel().setI18nText("replaymod.gui.modwarning.message2"));
        content.addElements(data, new GuiLabel().setI18nText("replaymod.gui.modwarning.message3"));

        if (!difference.getMissing().isEmpty()) {
            content.addElements(data, new GuiLabel());
            content.addElements(data, new GuiLabel().setI18nText("replaymod.gui.modwarning.missing"));
            for (ModInfo modInfo : difference.getMissing()) {
                content.addElements(data, new GuiPanel().setLayout(new VerticalLayout().setSpacing(3)).addElements(null,
                        GuiPanel.builder().layout(new HorizontalLayout().setSpacing(3))
                                .with(new GuiLabel().setI18nText("replaymod.gui.modwarning.name"), null)
                                .with(new GuiLabel().setText(modInfo.getName()), null).build(),
                        GuiPanel.builder().layout(new HorizontalLayout().setSpacing(3))
                                .with(new GuiLabel().setI18nText("replaymod.gui.modwarning.id"), null)
                                .with(new GuiLabel().setText(modInfo.getId()), null).build(),
                        new GuiLabel().setText(I18n.format("replaymod.gui.modwarning.version.expected")
                                + ": " + modInfo.getVersion())
                ));
            }
        }

        if (!difference.getDiffering().isEmpty()) {
            content.addElements(data, new GuiLabel());
            content.addElements(data, new GuiLabel().setI18nText("replaymod.gui.modwarning.version"));
            for (Map.Entry<ModInfo, String> entry : difference.getDiffering().entrySet()) {
                content.addElements(data, new GuiPanel().setLayout(new VerticalLayout().setSpacing(3)).addElements(null,
                        GuiPanel.builder().layout(new HorizontalLayout().setSpacing(3))
                                .with(new GuiLabel().setI18nText("replaymod.gui.modwarning.name"), null)
                                .with(new GuiLabel().setText(entry.getKey().getName()), null).build(),
                        GuiPanel.builder().layout(new HorizontalLayout().setSpacing(3))
                                .with(new GuiLabel().setI18nText("replaymod.gui.modwarning.id"), null)
                                .with(new GuiLabel().setText(entry.getKey().getId()), null).build(),
                        new GuiLabel().setText(I18n.format("replaymod.gui.modwarning.version.expected")
                                + ": " + entry.getKey().getVersion()
                                + ", " + I18n.format("replaymod.gui.modwarning.version.found")
                                + ": " + entry.getValue())
                ));
            }
        }

        cancelButton.onClick(() -> getMinecraft().openScreen(null));
    }

    @Override
    protected GuiModCompatWarning getThis() {
        return this;
    }
}
