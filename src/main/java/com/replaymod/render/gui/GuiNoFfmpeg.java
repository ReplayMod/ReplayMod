package com.replaymod.render.gui;

import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;

import java.net.URI;

import static com.replaymod.core.versions.MCVer.openURL;
import static de.johni0702.minecraft.gui.versions.MCVer.setClipboardString;

public class GuiNoFfmpeg extends GuiScreen {

    private static final String LINK = "https://www.replaymod.com/docs/#installing-ffmpeg";

    private final GuiLabel message = new GuiLabel()
            .setI18nText("replaymod.gui.rendering.error.message");
    private final GuiLabel link = new GuiLabel()
            .setText(LINK);
    private final GuiButton openLinkButton = new GuiButton()
            .setI18nLabel("chat.link.open")
            .setSize(100, 20)
            .onClick(() -> openURL(URI.create(LINK)));
    private final GuiButton copyToClipboardButton = new GuiButton()
            .setI18nLabel("chat.copy")
            .setSize(100, 20)
            .onClick(() -> setClipboardString(LINK));
    private final GuiButton backButton = new GuiButton()
            .setI18nLabel("gui.back")
            .setSize(100, 20);
    private final GuiPanel buttons = new GuiPanel()
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(4))
            .addElements(null, openLinkButton, copyToClipboardButton, backButton);

    {
        setBackground(Background.DIRT);
        setTitle(new GuiLabel().setI18nText("replaymod.gui.rendering.error.title"));
        setLayout(new VerticalLayout(VerticalLayout.Alignment.CENTER).setSpacing(30));
        addElements(new VerticalLayout.Data(0.5), message, link, buttons);
    }

    public GuiNoFfmpeg(Runnable goBack) {
        backButton.onClick(goBack);
    }
}
