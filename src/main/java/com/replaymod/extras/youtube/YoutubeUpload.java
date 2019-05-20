package com.replaymod.extras.youtube;

import com.replaymod.core.ReplayMod;
import com.replaymod.extras.Extra;
import com.replaymod.render.gui.GuiRenderingDone;
import de.johni0702.minecraft.gui.container.GuiScreen;
import de.johni0702.minecraft.gui.element.GuiButton;
import de.johni0702.minecraft.gui.utils.EventRegistrations;

//#if MC>=11400
import de.johni0702.minecraft.gui.versions.callbacks.OpenGuiScreenCallback;
import net.minecraft.client.gui.screen.Screen;
//#else
//$$ import net.minecraftforge.client.event.GuiScreenEvent;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//#endif

import static com.replaymod.core.versions.MCVer.*;

public class YoutubeUpload extends EventRegistrations implements Extra {
    @Override
    public void register(ReplayMod mod) {
        register();
    }

    //#if MC>=11400
    { on(OpenGuiScreenCallback.EVENT, this::onGuiOpen); }
    private void onGuiOpen(Screen vanillaGui) {
    //#else
    //$$ @SubscribeEvent
    //$$ public void onGuiOpen(GuiScreenEvent.InitGuiEvent.Post event) {
    //$$     net.minecraft.client.gui.GuiScreen vanillaGui = getGui(event);
    //#endif
        if (GuiScreen.from(vanillaGui) instanceof GuiRenderingDone) {
            GuiRenderingDone gui = (GuiRenderingDone) GuiScreen.from(vanillaGui);
            // Check if there already is a youtube button
            if (gui.actionsPanel.getChildren().stream().anyMatch(it -> it instanceof YoutubeButton)) {
                return; // Button already added
            }
            // Add the Upload to YouTube button to actions panel
            gui.actionsPanel.addElements(null,
                    new YoutubeButton().onClick(() ->
                            new GuiYoutubeUpload(gui, gui.videoFile, gui.videoFrames, gui.settings).display()
                    ).setSize(200, 20).setI18nLabel("replaymod.gui.youtubeupload"));
        }
    }

    private static class YoutubeButton extends GuiButton {}
}
