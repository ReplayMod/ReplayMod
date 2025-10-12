package com.replaymod.core.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC>=11800
//$$ import java.util.function.Supplier;
//#endif

//#if MC>=11400
import java.util.concurrent.CompletableFuture;
//#endif

//#if MC<11400
//$$ import java.util.concurrent.FutureTask;
//#endif

//#if MC<11400
//$$ import net.minecraft.client.resources.IResourcePack;
//$$ import java.util.List;
//$$ import java.util.Queue;
//#endif

@Mixin(MinecraftClient.class)
public interface MinecraftAccessor {
    //#if MC>=12100
    //$$ @Accessor("renderTickCounter")
    //$$ RenderTickCounter.Dynamic getTimer();
    //$$ @Accessor("renderTickCounter")
    //$$ @Mutable
    //$$ void setTimer(RenderTickCounter.Dynamic value);
    //#else
    @Accessor("renderTickCounter")
    RenderTickCounter getTimer();
    @Accessor("renderTickCounter")
    //#if MC>=11200
    @Mutable
    //#endif
    void setTimer(RenderTickCounter value);
    //#endif

    //#if MC>=11400
    @Accessor
    CompletableFuture<Void> getResourceReloadFuture();
    @Accessor
    void setResourceReloadFuture(CompletableFuture<Void> value);
    //#endif

    //#if MC>=11400
    //#else
    //$$ @Accessor
    //$$ Queue<FutureTask<?>> getScheduledTasks();
    //#endif

    @Accessor("crashReport")
    //#if MC>=11800
    //$$ Supplier<CrashReport> getCrashReporter();
    //#else
    CrashReport getCrashReporter();
    //#endif

    //#if MC<11400
    //$$ @Accessor
    //$$ List<IResourcePack> getDefaultResourcePacks();
    //#endif

    //#if MC>=11400
    @Accessor
    void setConnection(ClientConnection connection);
    //#endif
}
