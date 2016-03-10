package com.replaymod.pathing.gui;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.pathing.PathingRegistry;
import com.replaymod.pathing.path.Timeline;
import com.replaymod.pathing.serialize.TimelineSerialization;
import com.replaymod.replay.ReplayModReplay;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.*;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.element.IGuiButton;
import de.johni0702.minecraft.gui.function.Closeable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import de.johni0702.replaystudio.replay.ReplayFile;
import org.lwjgl.util.Dimension;
import org.lwjgl.util.ReadableDimension;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gui for loading and saving {@link com.replaymod.pathing.path.Timeline Timelines}.
 */
public class GuiKeyframeRepository extends GuiScreen implements Closeable {
    public final GuiPanel contentPanel = new GuiPanel(this).setBackgroundColor(Colors.DARK_TRANSPARENT);
    public final GuiLabel title = new GuiLabel(contentPanel).setI18nText("replaymod.gui.keyframerepository.title");
    public final GuiVerticalList list = new GuiVerticalList(contentPanel).setDrawShadow(true).setDrawSlider(true);
    public final GuiPanel buttonPanel = new GuiPanel(contentPanel).setLayout(new HorizontalLayout().setSpacing(5));
    public final GuiButton overwriteButton = new GuiButton(buttonPanel).onClick(new Runnable() {
        @Override
        public void run() {
            timelines.put(selectedEntry.name, currentTimeline);
            overwriteButton.setDisabled();
            save();
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.overwrite").setDisabled();
    public final GuiButton saveAsButton = new GuiButton(buttonPanel).onClick(new Runnable() {
        @Override
        public void run() {
            final GuiTextField nameField = new GuiTextField().setSize(200, 20).setFocused(true);
            final GuiYesNoPopup popup = GuiYesNoPopup.open(GuiKeyframeRepository.this,
                    new GuiLabel().setI18nText("replaymod.gui.saveas").setColor(Colors.BLACK),
                    nameField
            ).setYesI18nLabel("replaymod.gui.save").setNoI18nLabel("replaymod.gui.cancel");
            popup.getYesButton().setDisabled();
            ((VerticalLayout) popup.getInfo().getLayout()).setSpacing(7);
            nameField.onEnter(new Runnable() {
                @Override
                public void run() {
                    if (popup.getYesButton().isEnabled()) {
                        popup.getYesButton().onClick();
                    }
                }
            }).onTextChanged(new Consumer<String>() {
                @Override
                public void consume(String obj) {
                    popup.getYesButton().setEnabled(!nameField.getText().isEmpty()
                            && !timelines.containsKey(nameField.getText()));
                }
            });
            Futures.addCallback(popup.getFuture(), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean save) {
                    if (save) {
                        String name = nameField.getText();
                        timelines.put(name, currentTimeline);
                        list.getListPanel().addElements(null, new Entry(name));
                        save();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.saveas");
    public final GuiButton loadButton = new GuiButton(buttonPanel).onClick(new Runnable() {
        @Override
        public void run() {
            getMinecraft().displayGuiScreen(null);
            future.set(timelines.get(selectedEntry.name));
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.load").setDisabled();
    public final GuiButton renameButton = new GuiButton(buttonPanel).onClick(new Runnable() {
        @Override
        public void run() {
            final GuiTextField nameField = new GuiTextField().setSize(200, 20).setFocused(true).setText(selectedEntry.name);
            final GuiYesNoPopup popup = GuiYesNoPopup.open(GuiKeyframeRepository.this,
                    new GuiLabel().setI18nText("replaymod.gui.rename").setColor(Colors.BLACK),
                    nameField
            ).setYesI18nLabel("replaymod.gui.done").setNoI18nLabel("replaymod.gui.cancel");
            popup.getYesButton().setDisabled();
            ((VerticalLayout) popup.getInfo().getLayout()).setSpacing(7);
            nameField.onEnter(new Runnable() {
                @Override
                public void run() {
                    if (popup.getYesButton().isEnabled()) {
                        popup.getYesButton().onClick();
                    }
                }
            }).onTextChanged(new Consumer<String>() {
                @Override
                public void consume(String obj) {
                    popup.getYesButton().setEnabled(!nameField.getText().isEmpty()
                            && !timelines.containsKey(nameField.getText()));
                }
            });
            Futures.addCallback(popup.getFuture(), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean save) {
                    if (save) {
                        String name = nameField.getText();
                        timelines.put(name, timelines.remove(selectedEntry.name));
                        selectedEntry.name = name;
                        selectedEntry.label.setText(name);
                        save();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.rename").setDisabled();
    public final GuiButton removeButton = new GuiButton(buttonPanel).onClick(new Runnable() {
        @Override
        public void run() {
            GuiYesNoPopup popup = GuiYesNoPopup.open(GuiKeyframeRepository.this,
                    new GuiLabel().setI18nText("replaymod.gui.keyframerepo.delete").setColor(Colors.BLACK)
            ).setYesI18nLabel("replaymod.gui.delete").setNoI18nLabel("replaymod.gui.cancel");
            Futures.addCallback(popup.getFuture(), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean delete) {
                    if (delete) {
                        timelines.remove(selectedEntry.name);
                        list.getListPanel().removeElement(selectedEntry);

                        selectedEntry = null;
                        overwriteButton.setDisabled();
                        loadButton.setDisabled();
                        renameButton.setDisabled();
                        removeButton.setDisabled();
                        save();
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.remove").setDisabled();

    private final Map<String, Timeline> timelines = new LinkedHashMap<>();
    private final Timeline currentTimeline;
    private final SettableFuture<Timeline> future = SettableFuture.create();
    private final TimelineSerialization serialization;

    private Entry selectedEntry;

    {
        setDrawBackground(false);
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(contentPanel, width / 2 - width(contentPanel) / 2, height / 2 - height(contentPanel) / 2);
            }
        });
        contentPanel.setLayout(new CustomLayout<GuiPanel>() {
            @Override
            protected void layout(GuiPanel container, int width, int height) {
                pos(title, width / 2 - width(title) / 2, 5);
                size(list, width, height - 10 - height(buttonPanel) - 10 - y(title) - height(title) - 5);
                pos(list, width / 2 - width(list) / 2, y(title) + height(title) + 5);
                pos(buttonPanel, width / 2 - width(buttonPanel) / 2, y(list) + height(list) + 10);
            }

            @Override
            public ReadableDimension calcMinSize(GuiContainer<?> container) {
                ReadableDimension screenSize = getMinSize();
                return new Dimension(screenSize.getWidth() - 10, screenSize.getHeight() - 10);
            }
        });
    }

    public GuiKeyframeRepository(PathingRegistry registry, ReplayFile replayFile, Timeline currentTimeline) throws IOException {
        this.currentTimeline = currentTimeline;
        this.serialization = new TimelineSerialization(registry, replayFile);

        timelines.putAll(serialization.load());

        for (Map.Entry<String, Timeline> entry : timelines.entrySet()) {
            list.getListPanel().addElements(null, new Entry(entry.getKey()));
        }
    }

    @Override
    public void display() {
        super.display();
        ReplayModReplay.instance.getReplayHandler().getOverlay().setVisible(false);
    }

    @Override
    public void close() {
        ReplayModReplay.instance.getReplayHandler().getOverlay().setVisible(true);
    }

    public SettableFuture<Timeline> getFuture() {
        return future;
    }

    public void save() {
        try {
            serialization.save(timelines);
        } catch (IOException e) {
            e.printStackTrace();
            ReplayMod.instance.printWarningToChat("Error saving timelines: " + e.getMessage());
        }
    }

    public class Entry extends AbstractGuiClickableContainer<Entry> {
        public final GuiLabel label = new GuiLabel(this);
        private String name;

        public Entry(String name) {
            this.name = name;

            setLayout(new CustomLayout<Entry>() {
                @Override
                protected void layout(Entry container, int width, int height) {
                    pos(label, 5, height / 2 - height(label) / 2);
                }

                @Override
                public ReadableDimension calcMinSize(GuiContainer<?> container) {
                    return new Dimension(buttonPanel.calcMinSize().getWidth(), 16);
                }
            });
            label.setText(name);
        }

        @Override
        protected void onClick() {
            selectedEntry = this;
            buttonPanel.forEach(IGuiButton.class).setEnabled();
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            if (selectedEntry == this) {
                renderer.drawRect(0, 0, size.getWidth(), size.getHeight(), Colors.BLACK);
                renderer.drawRect(0, 0, 2, size.getHeight(), Colors.WHITE);
            }
            super.draw(renderer, size, renderInfo);
        }

        @Override
        protected Entry getThis() {
            return this;
        }
    }
}
