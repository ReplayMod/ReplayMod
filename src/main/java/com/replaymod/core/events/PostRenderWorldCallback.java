//#if MC>=11400
//$$ package com.replaymod.core.events;
//$$
//$$ import net.fabricmc.fabric.api.event.Event;
//$$ import net.fabricmc.fabric.api.event.EventFactory;
//$$
//$$ public interface PostRenderWorldCallback {
//$$     Event<PostRenderWorldCallback> EVENT = EventFactory.createArrayBacked(
//$$             PostRenderWorldCallback.class,
//$$             (listeners) -> () -> {
//$$                 for (PostRenderWorldCallback listener : listeners) {
//$$                     listener.postRenderWorld();
//$$                 }
//$$             }
//$$     );
//$$
//$$     void postRenderWorld();
//$$ }
//#endif
