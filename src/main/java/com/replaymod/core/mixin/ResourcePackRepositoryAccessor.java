//#if MC<10800
//$$ package com.replaymod.core.mixin;
//$$
//$$ import net.minecraft.client.resources.IResourcePack;
//$$ import net.minecraft.client.resources.ResourcePackRepository;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//$$
//$$ import java.io.File;
//$$
//$$ @Mixin(ResourcePackRepository.class)
//$$ public interface ResourcePackRepositoryAccessor {
//$$     @Accessor("field_148533_g")
//$$     boolean isActive();
//$$     @Accessor("field_148533_g")
//$$     void setActive(boolean value);
//$$     @Accessor("field_148532_f")
//$$     void setPack(IResourcePack value);
//$$     @Accessor("field_148534_e")
//$$     File getCacheDir();
//$$ }
//#endif
