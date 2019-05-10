package com.replaymod.render.gui;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.replaymod.core.utils.Utils;
import com.replaymod.render.ReplayModRender;
import com.replaymod.render.VideoWriter;
import com.replaymod.render.rendering.VideoRenderer;
import com.replaymod.render.utils.RenderJob;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replaystudio.pathing.path.Timeline;
import com.replaymod.replaystudio.util.I18n;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiClickableContainer;
import de.johni0702.minecraft.gui.container.AbstractGuiScreen;
import de.johni0702.minecraft.gui.container.GuiContainer;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.container.GuiVerticalList;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.element.GuiLabel;
import de.johni0702.minecraft.gui.element.GuiTextField;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.GridLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.popup.AbstractGuiPopup;
import de.johni0702.minecraft.gui.popup.GuiYesNoPopup;
import de.johni0702.minecraft.gui.utils.Colors;
import de.johni0702.minecraft.gui.utils.lwjgl.Dimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.client.gui.menu.NoticeScreen;
import net.minecraft.util.crash.CrashReport;

import javax.annotation.Nullable;
import java.util.List;

import static com.replaymod.render.ReplayModRender.LOGGER;

//#if MC>=11400
import net.minecraft.text.TranslatableTextComponent;
//#endif

public class GuiRenderQueue extends AbstractGuiPopup<GuiRenderQueue> {
    private final GuiLabel title = new GuiLabel().setI18nText("replaymod.gui.renderqueue.title").setColor(Colors.BLACK);
    private final GuiVerticalList list = new GuiVerticalList().setDrawShadow(true).setDrawSlider(true);
    private final GuiButton addButton = new GuiButton().setI18nLabel("replaymod.gui.renderqueue.add").setSize(150, 20);
    private final GuiButton renameButton = new GuiButton().setI18nLabel("replaymod.gui.rename").setSize(73, 20);
    private final GuiButton removeButton = new GuiButton().setI18nLabel("replaymod.gui.remove").setSize(73, 20);
    private final GuiButton renderButton = new GuiButton().setI18nLabel("replaymod.gui.render").setSize(150, 20);
    private final GuiButton closeButton = new GuiButton().setI18nLabel("replaymod.gui.close").setSize(150, 20).onClick(this::close);

    /*

    |---------------------------------|
    |       Add       |     Render    |
    |---------------------------------|
    | Rename | Remove |     Close     |
    |---------------------------------|

     */
    private final GuiPanel buttonPanel = new GuiPanel()
            .setLayout(new GridLayout().setSpacingX(5).setSpacingY(5).setColumns(2))
            .addElements(null,
                    addButton,
                    renderButton,
                    new GuiPanel().setLayout(new HorizontalLayout().setSpacing(4)).addElements(null,
                            renameButton, removeButton),
                    closeButton);

    private final AbstractGuiScreen container;
    private final ReplayHandler replayHandler;
    private Entry selectedEntry;

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

    public GuiRenderQueue(AbstractGuiScreen container, GuiRenderSettings guiRenderSettings, ReplayHandler replayHandler, Timeline timeline) {
        super(container);
        this.container = container;
        this.replayHandler = replayHandler;
        LOGGER.trace("Opening render queue popup");

        setBackgroundColor(Colors.DARK_TRANSPARENT);

        List<RenderJob> queue = ReplayModRender.instance.getRenderQueue();

        for (RenderJob renderJob : queue) {
            LOGGER.trace("Adding {} to job queue list", renderJob);
            list.getListPanel().addElements(null, new Entry(renderJob));
        }

        addButton.onClick(() -> {
            LOGGER.trace("Add button clicked");
            // Open popup
            GuiYesNoPopup popup = GuiYesNoPopup.open(container)
                    .setYesI18nLabel("replaymod.gui.add").setNoI18nLabel("replaymod.gui.cancel");
            popup.getInfo().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(5));
            // Add content
            GuiLabel label = new GuiLabel().setI18nText("replaymod.gui.renderqueue.jobname").setColor(Colors.BLACK);
            GuiTextField nameField = new GuiTextField().setSize(150, 20).setFocused(true);
            popup.getInfo().addElements(new HorizontalLayout.Data(0.5), label, nameField);
            // Disable "Yes" button while name is empty
            nameField.onTextChanged(old -> popup.getYesButton().setEnabled(!nameField.getText().isEmpty())).onEnter(() -> {
                if (popup.getYesButton().isEnabled()) {
                    popup.getYesButton().onClick();
                }
            });
            popup.getYesButton().setDisabled();
            // Register callback
            Futures.addCallback(popup.getFuture(), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean result) {
                    if (result == Boolean.TRUE) {
                        RenderJob newJob = new RenderJob();
                        newJob.setName(nameField.getText());
                        newJob.setSettings(guiRenderSettings.save(false));
                        newJob.setTimeline(timeline);
                        LOGGER.trace("Adding new job: {}", newJob);
                        queue.add(newJob);
                        list.getListPanel().addElements(null, new Entry(newJob));
                    } else {
                        LOGGER.trace("Adding cancelled");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    LOGGER.error("Add Job popup:", t);
                }
            });
        });
        if (guiRenderSettings != null) {
            addButton.setEnabled(guiRenderSettings.renderButton.isEnabled());
        }

        renameButton.onClick(() -> {
            LOGGER.trace("Rename button clicked for {}", selectedEntry.job);
            // Open popup
            GuiYesNoPopup popup = GuiYesNoPopup.open(container)
                    .setYesI18nLabel("replaymod.gui.rename").setNoI18nLabel("replaymod.gui.cancel");
            popup.getInfo().setLayout(new HorizontalLayout(HorizontalLayout.Alignment.CENTER).setSpacing(5));
            // Add content
            GuiLabel label = new GuiLabel().setI18nText("replaymod.gui.renderqueue.jobname").setColor(Colors.BLACK);
            GuiTextField nameField = new GuiTextField().setSize(150, 20).setFocused(true)
                    .setText(selectedEntry.job.getName());
            popup.getInfo().addElements(new HorizontalLayout.Data(0.5), label, nameField);
            // Disable "Yes" button while name is empty
            nameField.onTextChanged(old -> popup.getYesButton().setEnabled(!nameField.getText().isEmpty())).onEnter(() -> {
                if (popup.getYesButton().isEnabled()) {
                    popup.getYesButton().onClick();
                }
            });
            // Register callback
            Futures.addCallback(popup.getFuture(), new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(@Nullable Boolean result) {
                    if (result == Boolean.TRUE) {
                        LOGGER.trace("Renaming {} to \"{}\"", selectedEntry.job, nameField.getText());
                        selectedEntry.setName(nameField.getText());
                    } else {
                        LOGGER.trace("Renaming cancelled");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    LOGGER.error("Rename Job popup:", t);
                }
            });
        });

        removeButton.onClick(() -> {
            LOGGER.trace("Remove button clicked for {}", selectedEntry.job);
            list.getListPanel().removeElement(selectedEntry);
            queue.remove(selectedEntry.job);
            selectedEntry = null;
            updateButtons();
        });

        renderButton.onClick(() -> {
            LOGGER.trace("Render button clicked");
            processQueue(queue);
        });

        updateButtons();
    }

    private void processQueue(Iterable<RenderJob> queue) {
        // Close all GUIs (so settings in GuiRenderSettings are saved)
        getMinecraft().openScreen(null);
        // Start rendering
        int jobsDone = 0;
        for (RenderJob renderJob : queue) {
            LOGGER.info("Starting render job {}", renderJob);
            try {
                VideoRenderer videoRenderer = new VideoRenderer(renderJob.getSettings(), replayHandler, renderJob.getTimeline());
                videoRenderer.renderVideo();
            } catch (VideoWriter.NoFFmpegException e) {
                LOGGER.error("Rendering video:", e);
                NoticeScreen errorScreen = new NoticeScreen(
                        //#if MC>=11400
                        () -> {},
                        new TranslatableTextComponent("replaymod.gui.rendering.error.title"),
                        new TranslatableTextComponent("replaymod.gui.rendering.error.message")
                        //#else
                        //$$ I18n.format("replaymod.gui.rendering.error.title"),
                        //$$ I18n.format("replaymod.gui.rendering.error.message")
                        //#endif
                );
                getMinecraft().openScreen(errorScreen);
                return;
            } catch (VideoWriter.FFmpegStartupException e) {
                int jobsToSkip = jobsDone;
                GuiExportFailed.tryToRecover(e, newSettings -> {
                    // Update current job with fixed ffmpeg arguments
                    renderJob.setSettings(newSettings);
                    // Restart queue, skipping the already completed jobs
                    processQueue(Iterables.skip(queue, jobsToSkip));
                });
                return;
            } catch (Throwable t) {
                Utils.error(LOGGER, this, CrashReport.create(t, "Rendering video"), () -> {});
                container.display(); // Re-show the queue popup and the new error popup
                return;
            }
            jobsDone++;
        }
    }

    @Override
    public void open() {
        super.open();
    }

    @Override
    protected GuiRenderQueue getThis() {
        return this;
    }

    public void updateButtons() {
        renameButton.setEnabled(selectedEntry != null);
        removeButton.setEnabled(selectedEntry != null);
        renderButton.setEnabled(!list.getListPanel().getChildren().isEmpty());
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
            selectedEntry = this;
            updateButtons();
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            if (selectedEntry == this) {
                renderer.drawRect(0, 0, size.getWidth(), size.getHeight(), Colors.BLACK);
                renderer.drawRect(0, 0, 2, size.getHeight(), Colors.WHITE);
            }
            super.draw(renderer, size, renderInfo);
        }

        public void setName(String name) {
            job.setName(name);
            label.setText(name);
        }

        @Override
        protected Entry getThis() {
            return this;
        }
    }
}
