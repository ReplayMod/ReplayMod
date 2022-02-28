package com.replaymod.render.gui;

import com.google.common.collect.Iterables;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.utils.Result;
import com.replaymod.core.utils.Utils;
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.ReplayModRender;
import com.replaymod.render.FFmpegWriter;
import com.replaymod.render.rendering.VideoRenderer;
import com.replaymod.render.utils.RenderJob;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.ReplaySender;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.replay.ReplayFile;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiClickableContainer;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiVerticalList;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.function.Typeable;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.popup.GuiInfoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.crash.CrashReport;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.replaymod.render.ReplayModRender.LOGGER;

//#if MC>=11400
import net.minecraft.text.TranslatableText;
//#else
//$$ import com.replaymod.replaystudio.util.I18n;
//#endif

public class GuiRenderQueue extends AbstractGuiPopup<GuiRenderQueue> implements Typeable {
    private final GuiLabel title = new GuiLabel().setI18nText("replaymod.gui.renderqueue.title").setColor(Colors.BLACK);
    private final GuiVerticalList list = new GuiVerticalList().setDrawShadow(true).setDrawSlider(true);
    private final GuiButton addButton = new GuiButton().setI18nLabel("replaymod.gui.renderqueue.add").setSize(150, 20);
    private final GuiButton editButton = new GuiButton().setI18nLabel("replaymod.gui.edit").setSize(73, 20);
    private final GuiButton removeButton = new GuiButton().setI18nLabel("replaymod.gui.remove").setSize(73, 20);
    private final GuiButton renderButton = new GuiButton().setSize(150, 20);
    private final GuiButton closeButton = new GuiButton().setI18nLabel("replaymod.gui.close").setSize(150, 20).onClick(this::close);

    /*

    |---------------------------------|
    |       Add       |     Render    |
    |---------------------------------|
    |  Edit  | Remove |     Close     |
    |---------------------------------|

     */
    private final GuiPanel buttonPanel = new GuiPanel()
            .setLayout(new GridLayout().setSpacingX(5).setSpacingY(5).setColumns(2))
            .addElements(null,
                    addButton,
                    renderButton,
                    new GuiPanel().setLayout(new HorizontalLayout().setSpacing(4)).addElements(null,
                            editButton, removeButton),
                    closeButton);

    private final AbstractGuiScreen<?> container;
    private final ReplayHandler replayHandler;
    private final Set<Entry> selectedEntries = new HashSet<>();
    private final Supplier<Result<Timeline, String[]>> timelineSupplier;
    private boolean opened;

    {
        popup.setLayout(new CustomLayout<GuiPanel>() {
            @Override
            protected void layout(GuiPanel container, int width, int height) {
                pos(title, width / 2 - width(title) / 2, 0);
                pos(list, 0, y(title) + height(title) + 5);
                pos(buttonPanel, width / 2 - width(buttonPanel) / 2, height - height(buttonPanel));
                size(list, width, y(buttonPanel) - y(list) - 10);
            }

            @Override
            public ReadableDimension calcMinSize(GuiContainer<?> container) {
                ReadableDimension screenSize = GuiRenderQueue.this.container.getMinSize();
                return new Dimension(screenSize.getWidth() - 40,
                        screenSize.getHeight() - 20 - buttonPanel.getMinSize().getHeight() - title.getMinSize().getHeight());
            }
        }).addElements(null, title, list, buttonPanel);
    }

    private final ReplayModRender mod = ReplayModRender.instance;
    private final List<RenderJob> jobs = mod.getRenderQueue();

    public GuiRenderQueue(AbstractGuiScreen<?> container, ReplayHandler replayHandler, Supplier<Result<Timeline, String[]>> timelineSupplier) {
        super(container);
        this.container = container;
        this.replayHandler = replayHandler;
        this.timelineSupplier = timelineSupplier;
        LOGGER.trace("Opening render queue popup");

        setBackgroundColor(Colors.DARK_TRANSPARENT);

        for (RenderJob renderJob : jobs) {
            LOGGER.trace("Adding {} to job queue list", renderJob);
            list.getListPanel().addElements(null, new Entry(renderJob));
        }

        addButton.onClick(() -> addButtonClicked().ifErr(lines -> GuiInfoPopup.open(container, lines)));

        editButton.onClick(() -> {
            Entry job = selectedEntries.iterator().next();
            GuiRenderSettings gui = job.edit();
            gui.open();
        });

        removeButton.onClick(() -> {
            for (Entry entry : selectedEntries) {
                LOGGER.trace("Remove button clicked for {}", entry.job);
                list.getListPanel().removeElement(entry);
                jobs.remove(entry.job);
            }
            selectedEntries.clear();
            updateButtons();
            mod.saveRenderQueue();
        });

        renderButton.onClick(() -> {
            LOGGER.trace("Render button clicked");
            List<RenderJob> renderQueue = new ArrayList<>();
            if (selectedEntries.isEmpty()) {
                renderQueue.addAll(jobs);
            } else {
                Set<RenderJob> selectedJobs = selectedEntries.stream().map(it -> it.job).collect(Collectors.toSet());
                for (RenderJob job : jobs) {
                    if (selectedJobs.contains(job)) {
                        renderQueue.add(job);
                    }
                }
            }
            ReplayMod.instance.runLaterWithoutLock(() -> processQueue(container, replayHandler, renderQueue, () -> {}));
        });

        updateButtons();
    }

    private static void processQueue(AbstractGuiScreen<?> container, ReplayHandler replayHandler, Iterable<RenderJob> queue, Runnable done) {
        MinecraftClient mc = MCVer.getMinecraft();

        // Close all GUIs (so settings in GuiRenderSettings are saved)
        mc.openScreen(null);
        // Start rendering
        int jobsDone = 0;
        for (RenderJob renderJob : queue) {
            LOGGER.info("Starting render job {}", renderJob);
            try {
                VideoRenderer videoRenderer = new VideoRenderer(renderJob.getSettings(), replayHandler, renderJob.getTimeline());
                videoRenderer.renderVideo();
            } catch (FFmpegWriter.NoFFmpegException e) {
                LOGGER.error("Rendering video:", e);
                mc.openScreen(new GuiNoFfmpeg(container::display).toMinecraft());
                return;
            } catch (FFmpegWriter.FFmpegStartupException e) {
                int jobsToSkip = jobsDone;
                GuiExportFailed.tryToRecover(e, newSettings -> {
                    // Update current job with fixed ffmpeg arguments
                    renderJob.setSettings(newSettings);
                    // Restart queue, skipping the already completed jobs
                    ReplayMod.instance.runLaterWithoutLock(() -> processQueue(container, replayHandler, Iterables.skip(queue, jobsToSkip), done));
                });
                return;
            } catch (Throwable t) {
                Utils.error(LOGGER, container, CrashReport.create(t, "Rendering video"), () -> {});
                container.display(); // Re-show the queue popup and the new error popup
                return;
            }
            jobsDone++;
        }
        done.run();
    }

    public static void processMultipleReplays(
            AbstractGuiScreen<?> container,
            ReplayModReplay mod,
            Iterator<Pair<File, List<RenderJob>>> queue,
            Runnable done
    ) {
        if (!queue.hasNext()) {
            done.run();
            return;
        }
        Pair<File, List<RenderJob>> next = queue.next();

        LOGGER.info("Opening replay {} for {} render jobs", next.getKey(), next.getValue().size());
        ReplayHandler replayHandler;
        ReplayFile replayFile = null;
        try {
            replayFile = mod.getCore().files.open(next.getKey().toPath());
            replayHandler = mod.startReplay(replayFile, true, false);
        } catch (IOException e) {
            Utils.error(LOGGER, container, CrashReport.create(e, "Opening replay"), () -> {});
            container.display(); // Re-show the queue popup and the new error popup
            IOUtils.closeQuietly(replayFile);
            return;
        }
        if (replayHandler == null) {
            LOGGER.warn("Replay failed to open (missing mods?), skipping..");
            IOUtils.closeQuietly(replayFile);
            processMultipleReplays(container, mod, queue, done);
            return;
        }
        ReplaySender replaySender = replayHandler.getReplaySender();

        MinecraftClient mc = mod.getCore().getMinecraft();
        int jumpTo = 1000;
        while (mc.world == null && jumpTo < replayHandler.getReplayDuration()) {
            replaySender.sendPacketsTill(jumpTo);
            jumpTo += 1000;
        }
        if (mc.world == null) {
            LOGGER.warn("Replay failed to load world (corrupted?), skipping..");
            IOUtils.closeQuietly(replayFile);
            processMultipleReplays(container, mod, queue, done);
            return;
        }

        processQueue(container, replayHandler, next.getValue(), () -> {
            try {
                replayHandler.endReplay();
            } catch (IOException e) {
                Utils.error(LOGGER, container, CrashReport.create(e, "Closing replay"), () -> {});
                container.display(); // Re-show the queue popup and the new error popup
                return;
            }
            processMultipleReplays(container, mod, queue, done);
        });
    }

    private Result<GuiRenderSettings, String[]> addButtonClicked() {
        return timelineSupplier.get().mapOk(timeline -> {
            GuiRenderSettings popup = addJob(timeline);
            popup.open();
            return popup;
        });
    }

    public GuiRenderSettings addJob(Timeline timeline) {
        return new GuiRenderSettings(container, replayHandler, timeline) {
            {
                if (!jobs.isEmpty()) {
                    buttonPanel.removeElement(renderButton);
                }
                queueButton.onClick(() -> {
                    RenderSettings settings = save(false);

                    RenderJob newJob = new RenderJob();
                    newJob.setSettings(settings);
                    newJob.setTimeline(timeline);
                    LOGGER.trace("Adding new job: {}", newJob);
                    jobs.add(newJob);
                    list.getListPanel().addElements(null, new Entry(newJob));
                    updateButtons();
                    mod.saveRenderQueue();

                    // Need to close the inner popup before we can open the outer one
                    close();
                    if (!opened) {
                        GuiRenderQueue.this.open();
                    }
                });
            }

            @Override
            public void close() {
                super.close();
                if (!opened && jobs.isEmpty()) {
                    GuiRenderQueue.this.close();
                }
            }
        };
    }

    @Override
    public void open() {
        if (jobs.isEmpty() && timelineSupplier != null) {
            addButtonClicked().ifErr(lines ->
                    GuiInfoPopup.open(container, lines).onClosed(this::close));
            return;
        }

        super.open();
        opened = true;
    }

    @Override
    protected void close() {
        if (opened) {
            super.close();
        }
        opened = false;
    }

    @Override
    protected GuiRenderQueue getThis() {
        return this;
    }

    public void updateButtons() {
        int selected = selectedEntries.size();
        addButton.setEnabled(timelineSupplier != null);
        editButton.setEnabled(selected == 1);
        removeButton.setEnabled(selected >= 1);
        renderButton.setEnabled(jobs.size() > 0);
        renderButton.setI18nLabel("replaymod.gui.renderqueue.render" + (selected > 0 ? "selected" : "all"));

        String[] compatError = VideoRenderer.checkCompat(jobs.stream().map(RenderJob::getSettings));
        if (compatError != null) {
            renderButton.setDisabled().setTooltip(new GuiTooltip().setText(compatError));
        }
    }

    @Override
    public boolean typeKey(ReadablePoint mousePosition, int keyCode, char keyChar, boolean ctrlDown, boolean shiftDown) {
        if (MCVer.Keyboard.hasControlDown() && keyCode == MCVer.Keyboard.KEY_A) {
            if (selectedEntries.size() < list.getListPanel().getChildren().size()) {
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
        }
        return false;
    }

    public class Entry extends AbstractGuiClickableContainer<Entry> {
        public final GuiLabel label = new GuiLabel(this);
        public final RenderJob job;

        public Entry(RenderJob job) {
            this.job = job;

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
            label.setText(job.getName());
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

        public GuiRenderSettings edit() {
            GuiRenderSettings gui = new GuiRenderSettings(container, replayHandler, job.getTimeline());
            gui.buttonPanel.removeElement(gui.renderButton);
            gui.queueButton.setI18nLabel("replaymod.gui.done").onClick(() -> {
                job.setSettings(gui.save(false));
                label.setText(job.getName());
                mod.saveRenderQueue();
                gui.close();
            });
            gui.load(job.getSettings());
            return gui;
        }
    }
}
