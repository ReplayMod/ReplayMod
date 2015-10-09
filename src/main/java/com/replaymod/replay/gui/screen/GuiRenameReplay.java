package com.replaymod.replay.gui.screen;

import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import eu.crushedpixel.replaymod.utils.ReplayFileIO;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.util.Color;

import java.io.File;
import java.io.IOException;

public class GuiRenameReplay extends AbstractGuiScreen<GuiRenameReplay> {
    public final GuiScreen parent;
    public final File file;

    public final GuiLabel nameLabel = new GuiLabel().setI18nText("replaymod.gui.viewer.rename.name")
            .setColor(new Color(0xa0, 0xa0, 0xa0));
    public final GuiTextField nameField = new GuiTextField().setSize(200, 20).setFocused(true).onEnter(new Runnable() {
        @Override
        public void run() {
            renameButton.onClick();
        }
    }).onTextChanged(new Runnable() {
        @Override
        public void run() {
            renameButton.setEnabled(!nameField.getText().isEmpty());
        }
    });

    public final GuiButton renameButton = new GuiButton().setSize(200, 20).setI18nLabel("replaymod.gui.rename")
            .onClick(new Runnable() {
                @Override
                public void run() {
                    // Sanitize their input
                    String name = nameField.getText().trim().replace("[^a-zA-Z0-9\\.\\- ]", "_");
                    // This file is what they want
                    File targetFile = new File(file.getParentFile(), name + ".zip");
                    // But if it's already used, this is what they get
                    File renamed = ReplayFileIO.getNextFreeFile(targetFile);
                    try {
                        // Finally, try to move it
                        FileUtils.moveFile(file, renamed);
                    } catch (IOException e) {
                        // We failed (might also be their OS)
                        e.printStackTrace();
                        getMinecraft().displayGuiScreen(new GuiErrorScreen(
                                I18n.format("replaymod.gui.viewer.delete.failed1"),
                                I18n.format("replaymod.gui.viewer.delete.failed2")
                        ));
                        return;
                    }
                    getMinecraft().displayGuiScreen(parent);
                }
            });
    public final GuiButton cancelButton = new GuiButton().setSize(200, 20).setI18nLabel("replaymod.gui.cancel")
            .onClick(new Runnable() {
                @Override
                public void run() {
                    getMinecraft().displayGuiScreen(parent);
                }
            });

    public final GuiPanel inputPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(5))
            .addElements(null, nameLabel, nameField);
    public final GuiPanel buttonPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(5))
            .addElements(null, renameButton, cancelButton);

    public GuiRenameReplay(GuiScreen parent, File file) {
        this.parent = parent;
        this.file = file;

        nameField.setText(FilenameUtils.getBaseName(file.getName()));

        setLayout(new CustomLayout<GuiRenameReplay>() {
            @Override
            protected void layout(GuiRenameReplay container, int width, int height) {
                pos(inputPanel, width / 2 - width(inputPanel) / 2, 60);
                pos(buttonPanel, width / 2 - width(buttonPanel) / 2, height / 4 + 100);
            }
        });

        setTitle(new GuiLabel().setI18nText("replaymod.gui.viewer.rename.title"));
    }

    @Override
    protected GuiRenameReplay getThis() {
        return this;
    }
}
