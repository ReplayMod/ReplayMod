package com.replaymod.online.gui;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.replaymod.core.utils.Utils;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
public class GuiSaveModifiedReplay extends GuiScreen {
    public final File file;
    public final GuiLabel message = new GuiLabel().setI18nText("replaymod.gui.replaymodified.message");
    public final GuiTextField name = new GuiTextField().setSize(300, 20).setI18nHint("replaymod.gui.viewer.rename.name");
    public final GuiButton saveButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            String resultName = name.getText().trim().replace("[^a-zA-Z0-9\\.\\- ]", "_");
            final File resultFile = new File(file.getParentFile(), Utils.replayNameToFileName(resultName));
            if (resultFile.exists()) {
                Futures.addCallback(GuiYesNoPopup.open(GuiSaveModifiedReplay.this,
                        new GuiLabel().setI18nText("replaymod.gui.replaymodified.warning1", resultName).setColor(Colors.BLACK),
                        new GuiLabel().setI18nText("replaymod.gui.replaymodified.warning2").setColor(Colors.BLACK))
                        .setYesI18nLabel("gui.yes").setNoI18nLabel("gui.no")
                        .getFuture(), new FutureCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        if (result) {
                            try {
                                FileUtils.forceDelete(resultFile);
                                FileUtils.moveFile(file, resultFile);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            getMinecraft().displayGuiScreen(null);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                    }
                });
            } else {
                try {
                    FileUtils.moveFile(file, resultFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                getMinecraft().displayGuiScreen(null);
            }
        }
    }).setSize(147, 20).setI18nLabel("replaymod.gui.replaymodified.yes");
    public final GuiButton deleteButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            FileUtils.deleteQuietly(file);
            getMinecraft().displayGuiScreen(null);
        }
    }).setSize(147, 20).setI18nLabel("replaymod.gui.replaymodified.no");

    public final GuiPanel contentPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(5))
            .addElements(new VerticalLayout.Data(0.5), message, name,
                    new GuiPanel().setSize(300, 20).setLayout(new HorizontalLayout().setSpacing(6))
                            .addElements(null, saveButton, deleteButton));

    {
        setTitle(new GuiLabel().setI18nText("replaymod.gui.replaysaving.title"));
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(contentPanel, width / 2 - width(contentPanel) / 2, height / 2 - height(contentPanel) / 2);
            }
        });
    }
}
