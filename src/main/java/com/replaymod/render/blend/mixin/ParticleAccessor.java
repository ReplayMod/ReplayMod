package com.replaymod.render.blend.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor
    //#if MC<10904
    //$$ extends EntityAccessor
    //#endif
{
    //#if MC>=10904
    @Accessor
    double getPrevPosX();
    @Accessor
    double getPrevPosY();
    @Accessor
    double getPrevPosZ();
    @Accessor("x")
    double getPosX();
    @Accessor("y")
    double getPosY();
    @Accessor("z")
    double getPosZ();
    //#endif
}
