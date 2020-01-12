package com.replaymod.replay.handler;

import de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.resource.language.I18n;

//#if FABRIC>=1
import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
//#else
//$$ import net.minecraftforge.client.event.GuiScreenEvent;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//#endif

//#if MC>=11400
import net.minecraft.client.gui.widget.ButtonWidget;
//#endif

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.replay.ReplayModReplay.LOGGER;

public class GuiHandler extends EventRegistrations {
    private static final int BUTTON_REPLAY_VIEWER = 17890234;
    private static final int BUTTON_EXIT_REPLAY = 17890235;

    private final ReplayModReplay mod;

    public GuiHandler(ReplayModReplay mod) {
        this.mod = mod;
    }

    //#if FABRIC>=1
    { on(InitScreenCallback.EVENT, this::injectIntoIngameMenu); }
    private void injectIntoIngameMenu(Screen guiScreen, List<AbstractButtonWidget> buttonList) {
    //#else
    //$$ @SubscribeEvent
    //$$ public void injectIntoIngameMenu(GuiScreenEvent.InitGuiEvent.Post event) {
    //$$     Screen guiScreen = getGui(event);
    //$$     List<Widget> buttonList = getButtonList(event);
    //#endif
        if (!(guiScreen instanceof GameMenuScreen)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Pause replay when menu is opened
            mod.getReplayHandler().getReplaySender().setReplaySpeed(0);

            //#if MC>=11400
            final String BUTTON_OPTIONS = I18n.translate("menu.options");
            final String BUTTON_EXIT_SERVER = I18n.translate("menu.disconnect");
            final String BUTTON_ADVANCEMENTS = I18n.translate("gui.advancements");
            final String BUTTON_STATS = I18n.translate("gui.stats");
            final String BUTTON_OPEN_TO_LAN = I18n.translate("menu.shareToLan");
            //#else
            //#if MC>=11300
            //$$ final int BUTTON_OPTIONS = 0;
            //#endif
            //$$ final int BUTTON_EXIT_SERVER = 1;
            //$$ final int BUTTON_ADVANCEMENTS = 5;
            //$$ final int BUTTON_STATS = 6;
            //$$ final int BUTTON_OPEN_TO_LAN = 7;
            //#endif


            //#if MC<11300
            //$$ GuiButton openToLan = null;
            //#endif
            //#if MC>=11400
            AbstractButtonWidget achievements = null, stats = null;
            for(AbstractButtonWidget b : new ArrayList<>(buttonList)) {
            //#else
            //$$ GuiButton achievements = null, stats = null;
            //$$ for(GuiButton b : new ArrayList<>(buttonList)) {
            //#endif
                boolean remove = false;
                //#if MC>=11400
                String id = b.getMessage();
                //#else
                //$$ Integer id = b.id;
                //#endif
                if (id.equals(BUTTON_EXIT_SERVER)) {
                    // Replace "Exit Server" button with "Exit Replay" button
                    remove = true;
                    addButton(guiScreen, new InjectedButton(
                            guiScreen,
                            BUTTON_EXIT_REPLAY,
                            b.x,
                            b.y,
                            width(b),
                            height(b),
                            I18n.translate("replaymod.gui.exit"),
                            this::onButton
                    ));
                } else if (id.equals(BUTTON_ADVANCEMENTS)) {
                    // Remove "Advancements", "Stats" and "Open to LAN" buttons
                    remove = true;
                    achievements = b;
                } else if (id.equals(BUTTON_STATS)) {
                    remove = true;
                    stats = b;
                } else if (id.equals(BUTTON_OPEN_TO_LAN)) {
                    remove = true;
                    //#if MC<11300
                    //$$ openToLan = b;
                    //#endif
                //#if MC>=11300
                } else if (id.equals(BUTTON_OPTIONS)) {
                    //#if MC>=11400
                    width(b, 204);
                    //#else
                    //$$ width(b, 200);
                    //#endif
                //#endif
                }
                if (remove) {
                    // Moving the button far off-screen is easier to do cross-version than actually removing it
                    b.x = -1000;
                    b.y = -1000;
                }
            }
            if (achievements != null && stats != null) {
                moveAllButtonsDirectlyBelowUpwards(buttonList, achievements.y,
                        achievements.x, stats.x + width(stats));
            }
            // In 1.13+ Forge, the Options button shares one row with the Open to LAN button
            //#if MC<11300
            //$$ if (openToLan != null) {
            //$$     moveAllButtonsDirectlyBelowUpwards(buttonList, openToLan.y,
            //$$             openToLan.x, openToLan.x + openToLan.width);
            //$$ }
            //#endif
        }
    }

    /**
     * Moves all buttons that are within a rectangle below a certain y coordinate upwards by 24 units.
     * @param buttons List of buttons
     * @param belowY The Y limit
     * @param xStart Left x limit of the rectangle
     * @param xEnd Right x limit of the rectangle
     */
    private void moveAllButtonsDirectlyBelowUpwards(
            //#if MC>=11400
            List<AbstractButtonWidget> buttons,
            //#else
            //$$ List<GuiButton> buttons,
            //#endif
            int belowY,
            int xStart,
            int xEnd
    ) {
        buttons.stream()
                .filter(button -> button.y >= belowY)
                .filter(button -> button.x <= xEnd && button.x + width(button) >= xStart)
                .forEach(button -> button.y -= 24);
    }

    //#if FABRIC>=1
    { on(InitScreenCallback.EVENT, this::ensureReplayStopped); }
    private void ensureReplayStopped(Screen guiScreen, List<AbstractButtonWidget> buttonList) {
    //#else
    //$$ @SubscribeEvent
    //$$ public void ensureReplayStopped(GuiScreenEvent.InitGuiEvent event) {
    //$$     Screen guiScreen = getGui(event);
    //#endif
        if (!(guiScreen instanceof TitleScreen || guiScreen instanceof MultiplayerScreen)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Something went terribly wrong and we ended up in the main menu with the replay still active.
            // To prevent players from joining live servers and using the CameraEntity, try to stop the replay now.
            try {
                mod.getReplayHandler().endReplay();
            } catch (IOException e) {
                LOGGER.error("Trying to stop broken replay: ", e);
            } finally {
                if (mod.getReplayHandler() != null) {
                    mod.forcefullyStopReplay();
                }
            }
        }
    }

    //#if FABRIC>=1
    { on(InitScreenCallback.EVENT, this::injectIntoMainMenu); }
    private void injectIntoMainMenu(Screen guiScreen, List<AbstractButtonWidget> buttonList) {
    //#else
    //$$ @SubscribeEvent
    //$$ public void injectIntoMainMenu(GuiScreenEvent.InitGuiEvent event) {
    //$$     Screen guiScreen = getGui(event);
    //#endif
        if (!(guiScreen instanceof TitleScreen)) {
            return;
        }
        InjectedButton button = new InjectedButton(
                guiScreen,
                BUTTON_REPLAY_VIEWER,
                guiScreen.width / 2 - 100,
                guiScreen.height / 4 + 10 + 4 * 24,
                98,
                20,
                I18n.translate("replaymod.gui.replayviewer"),
                this::onButton
        );
        addButton(guiScreen, button);
    }

    //#if MC>=11300
    private void onButton(InjectedButton button) {
        Screen guiScreen = button.guiScreen;
    //#else
    //$$ @SubscribeEvent
    //$$ public void onButton(GuiScreenEvent.ActionPerformedEvent.Pre event) {
    //$$     GuiScreen guiScreen = getGui(event);
    //$$     GuiButton button = getButton(event);
    //#endif
        if(!button.active) return;

        if (guiScreen instanceof TitleScreen) {
            if (button.id == BUTTON_REPLAY_VIEWER) {
                new GuiReplayViewer(mod).display();
            }
        }

        if (guiScreen instanceof GameMenuScreen && mod.getReplayHandler() != null) {
            if (button.id == BUTTON_EXIT_REPLAY) {
                button.active = false;
                try {
                    mod.getReplayHandler().endReplay();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class InjectedButton extends
            //#if MC>=11400
            ButtonWidget
            //#else
            //$$ GuiButton
            //#endif
    {
        public final Screen guiScreen;
        public final int id;
        private Consumer<InjectedButton> onClick;
        public InjectedButton(Screen guiScreen, int buttonId, int x, int y, int width, int height, String buttonText,
                              //#if MC>=11300
                              Consumer<InjectedButton> onClick
                              //#else
                              //$$ Consumer<GuiScreenEvent.ActionPerformedEvent.Pre> onClick
                              //#endif
        ) {
            super(
                    //#if MC<11400
                    //$$ buttonId,
                    //#endif
                    x,
                    y,
                    width,
                    height,
                    buttonText
                    //#if MC>=11400
                    , self -> onClick.accept((InjectedButton) self)
                    //#endif
            );
            this.guiScreen = guiScreen;
            this.id = buttonId;
            //#if MC>=11300
            this.onClick = onClick;
            //#else
            //$$ this.onClick = null;
            //#endif
        }

        //#if MC>=11300 && MC<11400
        //$$ @Override
        //$$ public void onClick(double mouseX, double mouseY) {
        //$$     onClick.accept(this);
        //$$ }
        //#endif
    }
}
