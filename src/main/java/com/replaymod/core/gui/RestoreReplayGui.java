package com.replaymod.core.gui;

import com.google.common.io.Files;
import com.replaymod.replaystudio.PacketData;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.replay.ReplayFile;
import com.replaymod.replaystudio.replay.ReplayMetaData;
import com.replaymod.replaystudio.replay.ZipReplayFile;
import com.replaymod.replaystudio.studio.ReplayStudio;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;

import java.io.File;
import java.io.IOException;

public class RestoreReplayGui extends AbstractGuiScreen<RestoreReplayGui> {

    public final GuiScreen parent;
    public final File file;
    public final GuiPanel textPanel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(3));
    public final GuiPanel buttonPanel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5));
    public final GuiPanel contentPanel = new GuiPanel(this).addElements(new VerticalLayout.Data(0.5),
            textPanel, buttonPanel).setLayout(new VerticalLayout().setSpacing(20));
    public final GuiButton yesButton = new GuiButton(buttonPanel).setSize(150, 20).setI18nLabel("gui.yes");
    public final GuiButton noButton = new GuiButton(buttonPanel).setSize(150, 20).setI18nLabel("gui.no");

    public RestoreReplayGui(GuiScreen parent, File file) {
        this.parent = parent;
        this.file = file;

        textPanel.addElements(new VerticalLayout.Data(0.5),
                    new GuiLabel().setI18nText("replaymod.gui.restorereplay1"),
                    new GuiLabel().setI18nText("replaymod.gui.restorereplay2", Files.getNameWithoutExtension(file.getName())),
                    new GuiLabel().setI18nText("replaymod.gui.restorereplay3"));
        yesButton.onClick(() -> {
            try {
                ReplayFile replayFile = new ZipReplayFile(new ReplayStudio(), null, file);
                ReplayMetaData metaData = replayFile.getMetaData();
                if (metaData != null && metaData.getDuration() == 0) {
                    // Try to restore replay duration
                    try (ReplayInputStream in = replayFile.getPacketData()) {
                        PacketData last = null;
                        while ((last = in.readPacket()) != null) {
                            metaData.setDuration((int) last.getTime());
                        }
                        replayFile.writeMetaData(metaData);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
                replayFile.save();
                replayFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            parent.display();
        });
        noButton.onClick(() -> {
            try {
                Files.move(new File(file.getParentFile(), file.getName() + ".tmp"),
                        new File(file.getParentFile(), file.getName() + ".del"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            parent.display();
        });

        setLayout(new CustomLayout<RestoreReplayGui>() {
            @Override
            protected void layout(RestoreReplayGui container, int width, int height) {
                pos(contentPanel, width / 2 - width(contentPanel) / 2, height / 2 - height(contentPanel) / 2);
            }
        });
    }

    @Override
    protected RestoreReplayGui getThis() {
        return this;
    }
}
