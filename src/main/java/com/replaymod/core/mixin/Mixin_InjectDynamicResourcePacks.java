//#if FABRIC>=1
package com.replaymod.core.mixin;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.LangResourcePack;
import net.minecraft.resource.ResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.BiConsumer;
import java.util.stream.Collector;


//#if MC>=11600
@Mixin(net.minecraft.resource.ResourcePackManager.class)
//#else
//$$ @Mixin(net.minecraft.client.MinecraftClient.class)
//#endif
public class Mixin_InjectDynamicResourcePacks {
    @ModifyArg(
            //#if MC>=11600
            method = "createResourcePacks",
            //#elseif MC>=11500
            //$$ method = { "<init>", "reloadResources" },
            //#else
            //$$ method = { "init", "reloadResources" },
            //#endif
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;")
    )
    private Collector<ResourcePack, ?, ?> injectReplayModPacks(Collector<ResourcePack, ?, ?> collector) {
        collector = append(collector, new LangResourcePack());
        if (ReplayMod.jGuiResourcePack != null) {
            collector = append(collector, ReplayMod.jGuiResourcePack);
        }
        return collector;
    }

    private static <T, A, R> Collector<T, A, R> append(Collector<T, A, R> collector, T value) {
        BiConsumer<A, T> accumulator = collector.accumulator();
        return Collector.of(
                collector.supplier(),
                accumulator,
                collector.combiner(),
                result -> {
                    accumulator.accept(result, value);
                    return collector.finisher().apply(result);
                }
        );
    }
}
//#endif
