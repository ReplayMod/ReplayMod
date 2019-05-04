//#if MC>=11400
//$$ package com.replaymod.core.mixin;
//$$
//$$ import de.johni0702.minecraft.gui.versions.callbacks.InitScreenCallback;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.gui.Screen;
//$$ import net.minecraft.client.gui.widget.AbstractButtonWidget;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ import java.util.List;
//$$
//$$ @Mixin(Screen.class)
//$$ public class MixinScreen {
//$$     @Shadow
//$$     protected @Final List<AbstractButtonWidget> buttons;
//$$
//$$     @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("RETURN"))
//$$     private void init(MinecraftClient minecraftClient_1, int int_1, int int_2, CallbackInfo ci) {
//$$         InitScreenCallback.EVENT.invoker().initScreen((Screen) (Object) this, buttons);
//$$     }
//$$ }
//#endif
