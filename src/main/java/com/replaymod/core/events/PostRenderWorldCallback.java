package com.replaymod.core.events;

import de.johni0702.minecraft.gui.utils.Event;
import net.minecraft.client.util.math.MatrixStack;

//#if MC >= 26.2
//$$ import net.minecraft.client.renderer.SubmitNodeStorage;
//#endif

public interface PostRenderWorldCallback {
    Event<PostRenderWorldCallback> EVENT = Event.create((listeners) ->
            //#if MC >= 26.2
            //$$ (PoseStack matrixStack, SubmitNodeStorage submitNodeStorage) -> {
            //#else
            (MatrixStack matrixStack) -> {
            //#endif
                for (PostRenderWorldCallback listener : listeners) {
                    //#if MC >= 26.2
                    //$$ listener.postRenderWorld(matrixStack, submitNodeStorage);
                    //#else
                    listener.postRenderWorld(matrixStack);
                    //#endif
                }
            }
    );

    //#if MC >= 26.2
    //$$ void postRenderWorld(PoseStack matrixStack, SubmitNodeStorage submitNodeStorage);
    //#else
    void postRenderWorld(MatrixStack matrixStack);
    //#endif
}
