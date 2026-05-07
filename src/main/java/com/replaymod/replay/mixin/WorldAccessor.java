package com.replaymod.replay.mixin;

//#if MC>=11700
//$$ import net.minecraft.world.World;
//$$ import net.minecraft.world.chunk.BlockEntityTickInvoker;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//$$
//$$ import java.util.List;
//$$
//$$ @Mixin(World.class)
//$$ public interface WorldAccessor {
//$$     @Accessor("pendingBlockEntityTickers")
//$$     List<BlockEntityTickInvoker> getPendingBlockEntityTickers();
//$$
//$$     @Accessor("blockEntityTickers")
//$$     List<BlockEntityTickInvoker> getBlockEntityTickers();
//$$ }
//#else
public interface WorldAccessor {
    // The pending block entity ticker system was added in MC 1.17 (21w19a),
    // so this accessor is a no-op stub on older versions to keep the source
    // compiling against the 1.16.4 main project.
}
//#endif
