package com.replaymod.pathing.gui;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.gui.GuiRenderQueue;
import com.replaymod.render.gui.GuiRenderSettings;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replaystudio.pathing.PathingRegistry;
import com.replaymod.replaystudio.pathing.path.Path;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.pathing.serialize.TimelineSerialization;
import com.replaymod.replaystudio.replay.ReplayFile;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiClickableContainer;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.container.GuiVerticalList;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.function.Closeable;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.VerticalLayout;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.Consumer;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import net.minecraft.util.crash.CrashReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static de.johni0702.minecraft.gui.versions.MCVer.getClipboardString;
import static de.johni0702.minecraft.gui.versions.MCVer.setClipboardString;

/**
 * Gui for loading and saving {@link Timeline Timelines}.
 */
public class GuiKeyframeRepository extends GuiScreen implements Closeable, Typeable {
    private static final Logger LOGGER = LogManager.getLogger();

    public final GuiPanel contentPanel = new GuiPanel(this).setBackgroundColor(Colors.DARK_TRANSPARENT);
    public final GuiLabel title = new GuiLabel(contentPanel).setI18nText("replaymod.gui.keyframerepository.title");
    public final GuiVerticalList list = new GuiVerticalList(contentPanel).setDrawShadow(true).setDrawSlider(true);
    public final GuiButton overwriteButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            GuiYesNoPopup popup = GuiYesNoPopup.open(GuiKeyframeRepository.this,
                    new GuiLabel().setI18nText("replaymod.gui.keyframerepo.overwrite").setColor(Colors.BLACK)
            ).setYesI18nLabel("gui.yes").setNoI18nLabel("gui.no");
            Utils.addCallback(popup.getFuture(), doIt -> {
                if (doIt) {
                    for (Entry entry : selectedEntries) {
                        timelines.put(entry.name, currentTimeline);
                    }
                    overwriteButton.setDisabled();
                    save();
                }
            }, Throwable::printStackTrace);
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.overwrite").setDisabled();
    public final GuiButton saveAsButton = new GuiButton().onClick(new Runnable() {
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
    public final GuiButton loadButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            getMinecraft().openScreen(null);
            try {
                Timeline timeline = timelines.get(selectedEntries.iterator().next().name);
                for (Path path : timeline.getPaths()) {
                    path.updateAll();
                }
                future.set(timeline);
            } catch (Throwable t) {
                future.setException(t);
            }
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.load").setDisabled();
    public final GuiButton renameButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            Entry selectedEntry = selectedEntries.iterator().next();
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
    public final GuiButton removeButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            GuiYesNoPopup popup = GuiYesNoPopup.open(GuiKeyframeRepository.this,
                    new GuiLabel().setI18nText("replaymod.gui.keyframerepo.delete").setColor(Colors.BLACK)
            ).setYesI18nLabel("replaymod.gui.delete").setNoI18nLabel("replaymod.gui.cancel");
            Futures.addCallback(popup.getFuture(), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean delete) {
                    if (delete) {
                        for (Entry entry : selectedEntries) {
                            timelines.remove(entry.name);
                            list.getListPanel().removeElement(entry);
                        }

                        selectedEntries.clear();
                        updateButtons();
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

    public final GuiButton copyButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            Map<String, Timeline> toBeSerialized = new HashMap<>();
            for (Entry entry : selectedEntries) {
                toBeSerialized.put(entry.name, timelines.get(entry.name));
            }
            try {
                TimelineSerialization serialization = new TimelineSerialization(registry, null);
                setClipboardString(serialization.serialize(toBeSerialized));
            } catch (Throwable t) {
                t.printStackTrace();
                CrashReport report = CrashReport.create(t, "Copying timeline(s)");
                Utils.error(LOGGER, GuiKeyframeRepository.this, report, () -> {});
            }
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.copy").setDisabled();

    public final GuiButton pasteButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            try {
                TimelineSerialization serialization = new TimelineSerialization(registry, null);
                for (Map.Entry<String, Timeline> entry : serialization.deserialize(getClipboardString()).entrySet()) {
                    String name = entry.getKey();
                    while (timelines.containsKey(name)) {
                        name += " (Copy)";
                    }
                    timelines.put(name, entry.getValue());
                    list.getListPanel().addElements(null, new Entry(name));
                }
                save();
            } catch (Throwable t) {
                // Intentionally not making a fuzz about it cause this will likely happen when they have anything
                // else in their clipboard.
                // If it's actually not working, they'll go complain and we'll just check the log.
                t.printStackTrace();
            }
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.paste");

    public final GuiButton addToQueueButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
            GuiRenderQueue queue = new GuiRenderQueue(GuiKeyframeRepository.this, replayHandler, null);
            queue.open();

            Set<String> selected = selectedEntries.stream().map(e -> e.name).collect(Collectors.toSet());
            ArrayDeque<Map.Entry<String, Timeline>> toBeAdded = new ArrayDeque<>();
            // Iterating over timelines to get consistent ordering (cause selectedEntries is an unordered set)
            // and we need to get the Timelines anyway.
            for (Map.Entry<String, Timeline> entry : timelines.entrySet()) {
                if (selected.contains(entry.getKey())) {
                    toBeAdded.offerLast(entry);
                }
            }
            new Runnable() {
                @Override
                public void run() {
                    Map.Entry<String, Timeline> entry = toBeAdded.pollFirst();
                    if (entry == null) {
                        return;
                    }
                    String name = entry.getKey();
                    Timeline timeline = entry.getValue();
                    GuiRenderSettings settingsGui = queue.addJob(timeline);
                    settingsGui.buttonPanel.removeElement(settingsGui.renderButton);
                    settingsGui.setOutputFileBaseName(name);
                    Runnable orgOnClick = settingsGui.queueButton.getOnClick();
                    settingsGui.queueButton.onClick(() -> {
                        orgOnClick.run();
                        this.run();
                    });
                    settingsGui.open();
                }
            }.run();
        }
    }).setSize(75, 20).setI18nLabel("replaymod.gui.rendersettings.addtoqueue");

    public final GuiPanel buttonPanel = new GuiPanel(contentPanel)
            .setLayout(new GridLayout()
                    .setColumns(4)
                    .setSpacingX(5)
                    .setSpacingY(5))
            .addElements(null,
                    overwriteButton, saveAsButton, renameButton, removeButton,
                    loadButton, addToQueueButton, copyButton, pasteButton);

    private final Map<String, Timeline> timelines = new LinkedHashMap<>();
    private final Timeline currentTimeline;
    private final SettableFuture<Timeline> future = SettableFuture.create();
    private final PathingRegistry registry;
    private final ReplayFile replayFile;

    private final Set<Entry> selectedEntries = new HashSet<>();

    {
        setBackground(Background.NONE);
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
        this.registry = registry;
        this.replayFile = replayFile;
        this.currentTimeline = currentTimeline;

        timelines.putAll(replayFile.getTimelines(registry));

        for (Map.Entry<String, Timeline> entry : timelines.entrySet()) {
            if (entry.getKey().isEmpty()) continue; // don't show auto-save slot
            list.getListPanel().addElements(null, new Entry(entry.getKey()));
        }

        updateButtons();
    }

    private void updateButtons() {
        int selected = selectedEntries.size();

        overwriteButton.setEnabled(selected >= 1);
        loadButton.setEnabled(selected == 1);
        renameButton.setEnabled(selected == 1);
        removeButton.setEnabled(selected >= 1);
        copyButton.setEnabled(selected >= 1);
        addToQueueButton.setEnabled(selected >= 1);
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
            replayFile.writeTimelines(registry, timelines);
        } catch (IOException e) {
            e.printStackTrace();
            ReplayMod.instance.printWarningToChat("Error saving timelines: " + e.getMessage());
        }
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (MCVer.Keyboard.hasControlDown()) {
            switch (keyCode) {
                case MCVer.Keyboard.KEY_A:
                    if (selectedEntries.size() < timelines.size()) {
                        for (GuiElement<?> child : list.getListPanel().getChildren()) {
                            if (child instanceof Entry) {
                                selectedEntries.add((Entry) child);
                            }
                        }
                    } else {
                        selectedEntries.clear();
                    }
                    updateButtons();
                    return true;
                case MCVer.Keyboard.KEY_C:
                    copyButton.onClick();
                    return true;
                case MCVer.Keyboard.KEY_V:
                    pasteButton.onClick();
                    return true;
            }
        }
        return false;
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
            if (!MCVer.Keyboard.hasControlDown()) {
                selectedEntries.clear();
            }
            if (selectedEntries.contains(this)) {
                selectedEntries.remove(this);
            } else {
                selectedEntries.add(this);
            }
            updateButtons();
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            if (selectedEntries.contains(this)) {
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
