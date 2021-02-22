package com.replaymod.replay.handler;

import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.gui.screen.GuiReplayViewer;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;

//#if MC>=11600
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
//#else
//$$ import net.minecraft.client.resource.language.I18n;
//#endif

//#if FABRIC<1
//$$ import net.minecraftforge.client.event.GuiScreenEvent;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//#endif

//#if MC>=11400
import net.minecraft.client.gui.widget.ButtonWidget;
//#endif

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    { on(InitScreenCallback.EVENT, this::injectIntoIngameMenu); }
    private void injectIntoIngameMenu(Screen guiScreen, List<AbstractButtonWidget> buttonList) {
        if (!(guiScreen instanceof GameMenuScreen)) {
            return;
        }

        if (mod.getReplayHandler() != null) {
            // Pause replay when menu is opened
            mod.getReplayHandler().getReplaySender().setReplaySpeed(0);

            //#if MC>=11600
            final TranslatableText BUTTON_OPTIONS = new TranslatableText("menu.options");
            final TranslatableText BUTTON_EXIT_SERVER = new TranslatableText("menu.disconnect");
            final TranslatableText BUTTON_ADVANCEMENTS = new TranslatableText("gui.advancements");
            final TranslatableText BUTTON_STATS = new TranslatableText("gui.stats");
            final TranslatableText BUTTON_OPEN_TO_LAN = new TranslatableText("menu.shareToLan");
            //#else
            //#if MC>=11400
            //$$ final String BUTTON_OPTIONS = I18n.translate("menu.options");
            //$$ final String BUTTON_EXIT_SERVER = I18n.translate("menu.disconnect");
            //$$ final String BUTTON_ADVANCEMENTS = I18n.translate("gui.advancements");
            //$$ final String BUTTON_STATS = I18n.translate("gui.stats");
            //$$ final String BUTTON_OPEN_TO_LAN = I18n.translate("menu.shareToLan");
            //#else
            //#if MC>=11400
            //$$ final int BUTTON_OPTIONS = 0;
            //#endif
            //$$ final int BUTTON_EXIT_SERVER = 1;
            //$$ final int BUTTON_ADVANCEMENTS = 5;
            //$$ final int BUTTON_STATS = 6;
            //$$ final int BUTTON_OPEN_TO_LAN = 7;
            //#endif
            //#endif


            //#if MC<11400
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
                //#if MC>=11600
                Text id = b.getMessage();
                //#else
                //$$ String id = b.getMessage();
                //#endif
                if (id == null) {
                    // likely a button of some third-part mod
                    // e.g. https://github.com/Pokechu22/WorldDownloader/blob/b1b279f948beec2d7dac7524eea8f584a866d8eb/share_14/src/main/java/wdl/WDLHooks.java#L491
                    continue;
                }
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
                            "replaymod.gui.exit",
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
                    //#if MC<11400
                    //$$ openToLan = b;
                    //#endif
                //#if MC>=11400
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
                moveAllButtonsInRect(buttonList,
                        achievements.x, stats.x + width(stats),
                        achievements.y, Integer.MAX_VALUE,
                        -24);
            }
            // In 1.13+ Forge, the Options button shares one row with the Open to LAN button
            //#if MC<11400
            //$$ if (openToLan != null) {
            //$$     moveAllButtonsInRect(buttonList,
            //$$             openToLan.x, openToLan.x + openToLan.width,
            //$$             openToLan.y, Integer.MAX_VALUE,
            //$$             -24);
            //$$ }
            //#endif
        }
    }

    /**
     * Moves all buttons that in any way intersect a rectangle by a given amount on the y axis.
     * @param buttons List of buttons
     * @param yStart Top y limit of the rectangle
     * @param yEnd Bottom y limit of the rectangle
     * @param xStart Left x limit of the rectangle
     * @param xEnd Right x limit of the rectangle
     * @param moveBy Signed distance to move the buttons
     */
    private void moveAllButtonsInRect(
            //#if MC>=11400
            List<AbstractButtonWidget> buttons,
            //#else
            //$$ List<GuiButton> buttons,
            //#endif
            int xStart,
            int xEnd,
            int yStart,
            int yEnd,
            int moveBy
    ) {
        buttons.stream()
                .filter(button -> button.x <= xEnd && button.x + width(button) >= xStart)
                .filter(button -> button.y <= yEnd && button.y + height(button) >= yStart)
                .forEach(button -> button.y += moveBy);
    }

    { on(InitScreenCallback.EVENT, this::ensureReplayStopped); }
    private void ensureReplayStopped(Screen guiScreen, List<AbstractButtonWidget> buttonList) {
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

    { on(InitScreenCallback.EVENT, this::injectIntoMainMenu); }
    private void injectIntoMainMenu(Screen guiScreen, List<AbstractButtonWidget> buttonList) {
        if (!(guiScreen instanceof TitleScreen)) {
            return;
        }

        int x = guiScreen.width / 2 - 100;
        // We want to position our button below the realms button
        int y = findButton(buttonList, "menu.online", 14)
                .map(Optional::of)
                // or, if someone removed the realms button, we'll alternatively take the multiplayer one
                .orElse(findButton(buttonList, "menu.multiplayer", 2))
                // if we found some button, put our button at its position (we'll move it out of the way shortly)
                .map(it -> it.y)
                // and if we can't even find that one, then just guess
                .orElse(guiScreen.height / 4 + 10 + 4 * 24);

        // Move all buttons above or at our one upwards
        moveAllButtonsInRect(buttonList,
                x, x + 200,
                Integer.MIN_VALUE, y,
                -24);

        // Add our button
        InjectedButton button = new InjectedButton(
                guiScreen,
                BUTTON_REPLAY_VIEWER,
                x,
                y,
                200,
                20,
                "replaymod.gui.replayviewer",
                this::onButton
        );
        //#if FABRIC<=0
        //$$ if (guiScreen.getClass().getName().endsWith("custommainmenu.gui.GuiFakeMain")) {
        //$$     // CustomMainMenu uses a different list in the event than in its Fake gui
        //$$     buttonList.add(button);
        //$$     return;
        //$$ }
        //#endif
        addButton(guiScreen, button);
    }

    //#if MC>=11400
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
                              //#if MC>=11400
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
                    //#if MC>=11600
                    new TranslatableText(buttonText)
                    //#else
                    //$$ I18n.translate(buttonText)
                    //#endif
                    //#if MC>=11400
                    , self -> onClick.accept((InjectedButton) self)
                    //#endif
            );
            this.guiScreen = guiScreen;
            this.id = buttonId;
            //#if MC>=11400
            this.onClick = onClick;
            //#else
            //$$ this.onClick = null;
            //#endif
        }

        //#if MC>=11400 && MC<11400
        //$$ @Override
        //$$ public void onClick(double mouseX, double mouseY) {
        //$$     onClick.accept(this);
        //$$ }
        //#endif
    }
}
