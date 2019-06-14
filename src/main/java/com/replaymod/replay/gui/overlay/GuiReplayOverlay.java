package com.replaymod.replay.gui.overlay;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer.Keyboard;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.ReplaySender;
import de.johni0702.minecraft.gui.GuiRenderer;
import de.johni0702.minecraft.gui.RenderInfo;
import de.johni0702.minecraft.gui.container.AbstractGuiOverlay;
import de.johni0702.minecraft.gui.container.GuiPanel;
import de.johni0702.minecraft.gui.element.GuiElement;
import de.johni0702.minecraft.gui.element.GuiSlider;
import de.johni0702.minecraft.gui.element.GuiTexturedButton;
import de.johni0702.minecraft.gui.element.GuiTooltip;
import de.johni0702.minecraft.gui.element.advanced.IGuiTimeline;
import de.johni0702.minecraft.gui.layout.CustomLayout;
import de.johni0702.minecraft.gui.layout.HorizontalLayout;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadablePoint;
import de.johni0702.minecraft.gui.utils.lwjgl.WritablePoint;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.resource.language.I18n;

//#if MC>=11300
import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.KeyEventCallback;
//#else
//$$ import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//$$ import net.minecraftforge.fml.common.gameevent.InputEvent;
//#endif

import static com.replaymod.core.ReplayMod.TEXTURE_SIZE;

public class GuiReplayOverlay extends AbstractGuiOverlay<GuiReplayOverlay> {

    public final GuiPanel topPanel = new GuiPanel(this)
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.LEFT).setSpacing(5));
    public final GuiTexturedButton playPauseButton = new GuiTexturedButton() {
        @Override
        public GuiElement getTooltip(RenderInfo renderInfo) {
            GuiTooltip tooltip = (GuiTooltip) super.getTooltip(renderInfo);
            if (tooltip != null) {
                if (getTextureNormal().getY() == 0) { // Play button
                    tooltip.setI18nText("replaymod.gui.ingame.menu.unpause");
                } else { // Pause button
                    tooltip.setI18nText("replaymod.gui.ingame.menu.pause");
                }
            }
            return tooltip;
        }
    }.setSize(20, 20).setTexture(ReplayMod.TEXTURE, TEXTURE_SIZE).setTooltip(new GuiTooltip());
    public final GuiSlider speedSlider = new GuiSlider().setSize(100, 20).setSteps(37); // 0.0 is not included
    public final GuiMarkerTimeline timeline;

    /**
     * This is not used by the replay module itself but may be used by other modules/extras to show
     * when they're active.
     */
    public final GuiPanel statusIndicatorPanel = new GuiPanel(this).setSize(100, 20)
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(5));

    private final EventHandler eventHandler = new EventHandler();

    public GuiReplayOverlay(final ReplayHandler replayHandler) {
        timeline = new GuiMarkerTimeline(replayHandler){
            @Override
            public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
                setCursorPosition(replayHandler.getReplaySender().currentTimeStamp());
                super.draw(renderer, size, renderInfo);
            }
        }.setSize(Integer.MAX_VALUE, 20);

        topPanel.addElements(null, playPauseButton, speedSlider, timeline);
        setLayout(new CustomLayout<GuiReplayOverlay>() {
            @Override
            protected void layout(GuiReplayOverlay container, int width, int height) {
                pos(topPanel, 10, 10);
                size(topPanel, width - 20, 20);

                pos(statusIndicatorPanel, width / 2, height - 25);
                width(statusIndicatorPanel, width / 2 - 5);
            }
        });

        playPauseButton.setTexturePosH(new ReadablePoint() {
            @Override
            public int getX() {
                return 0;
            }

            @Override
            public int getY() {
                return replayHandler.getReplaySender().paused() ? 0 : 20;
            }

            @Override
            public void getLocation(WritablePoint dest) {
                dest.setLocation(getX(), getY());
            }
        }).onClick(new Runnable() {
            @Override
            public void run() {
                ReplaySender replaySender = replayHandler.getReplaySender();
                // If currently paused
                if (replaySender.paused()) {
                    // then play
                    replaySender.setReplaySpeed(getSpeedSliderValue());
                } else {
                    // else pause
                    replaySender.setReplaySpeed(0);
                }
            }
        });

        speedSlider.onValueChanged(new Runnable() {
            @Override
            public void run() {
                double speed = getSpeedSliderValue();
                speedSlider.setText(I18n.translate("replaymod.gui.speed") + ": " + speed + "x");
                ReplaySender replaySender = replayHandler.getReplaySender();
                if (!replaySender.paused()) {
                    replaySender.setReplaySpeed(speed);
                }
            }
        }).setValue(9);

        timeline.onClick(new IGuiTimeline.OnClick() {
            @Override
            public void run(int time) {
                replayHandler.doJump(time, true);
            }
        }).setLength(replayHandler.getReplayDuration());
    }

    public double getSpeedSliderValue() {
        int value = speedSlider.getValue() + 1;
        if (value <= 9) {
            return value / 10d;
        } else {
            return 1 + (0.25d * (value - 10));
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (isVisible() != visible) {
            if (visible) {
                eventHandler.register();
            } else {
                eventHandler.unregister();
            }
        }
        super.setVisible(visible);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        // Do not render overlay when user pressed F1 and we are not currently in some popup
        if (getMinecraft().options.hudHidden && isAllowUserInput()) {
            // Note that this only applies to when the mouse is visible, otherwise
            // the draw method isn't called in the first place
            return;
        }
        super.draw(renderer, size, renderInfo);
    }

    @Override
    protected GuiReplayOverlay getThis() {
        return this;
    }

    //#if MC>=10800
    private
    //#else
    //$$ public // All event handlers need to be public in 1.7.10
    //#endif
    class EventHandler extends EventRegistrations {
        //#if MC>=11300
        { on(KeyBindingEventCallback.EVENT, this::onKeyBindingEvent); }
        private void onKeyBindingEvent() {
        //#else
        //$$ @SubscribeEvent
        //$$ public void onKeyBindingEvent(InputEvent.KeyInputEvent event) {
        //#endif
            GameOptions gameSettings = getMinecraft().options;
            while (gameSettings.keyChat.wasPressed() || gameSettings.keyCommand.wasPressed()) {
                if (!isMouseVisible()) {
                    setMouseVisible(true);
                }
            }
        }

        //#if MC>=11300
        { on(KeyEventCallback.EVENT, (int key, int scanCode, int action, int modifiers) -> onKeyInput(key, action)); }
        private void onKeyInput(int key, int action) {
            if (action != 0) return;
        //#else
        //$$ @SubscribeEvent
        //$$ public void onKeyInput(InputEvent.KeyInputEvent event) {
        //$$     if (!Keyboard.getEventKeyState()) return;
        //$$     int key = Keyboard.getEventKey();
        //#endif
            GameOptions gameSettings = getMinecraft().options;
            // Handle the F1 key binding while the overlay is opened as a gui screen
            if (isMouseVisible() && key == Keyboard.KEY_F1) {
                gameSettings.hudHidden = !gameSettings.hudHidden;
            }
        }
    }
}
