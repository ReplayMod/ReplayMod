//#if MC>=11400
package com.replaymod.core.events;

import de.johni0702.minecraft.gui.utils.Event;

//#if MC>=11500
import net.minecraft.client.util.math.MatrixStack;
//#endif

public interface PostRenderWorldCallback {
    Event<PostRenderWorldCallback> EVENT = Event.create((listeners) ->
            //#if MC>=11500
            (MatrixStack matrixStack) -> {
            //#else
            //$$ () -> {
            //#endif
                for (PostRenderWorldCallback listener : listeners) {
                    listener.postRenderWorld(
                            //#if MC>=11500
                            matrixStack
                            //#endif
                    );
                }
            }
    );

    void postRenderWorld(
            //#if MC>=11500
            MatrixStack matrixStack
            //#endif
    );
}
//#endif
