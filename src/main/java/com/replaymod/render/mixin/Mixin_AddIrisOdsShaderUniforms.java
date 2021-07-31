//#if MC>=11600
package com.replaymod.render.mixin;

import com.replaymod.render.capturer.IrisODSFrameCapturer;
import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.gl.uniform.UniformUpdateFrequency;
import net.coderbot.iris.uniforms.CommonUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(value = CommonUniforms.class, remap = false)
public class Mixin_AddIrisOdsShaderUniforms {
    // Using ModifyVariable, so we only depend on a single argument. Hoping that reduces the chance of breaking changes.
    @ModifyVariable(method = "generalCommonUniforms", at = @At("HEAD"), argsOnly = true)
    private static UniformHolder addReplayModOdsUniforms(UniformHolder uniforms) {
        IrisODSFrameCapturer ods = IrisODSFrameCapturer.INSTANCE;
        if (ods != null) {
            uniforms.uniform1b(UniformUpdateFrequency.PER_FRAME, "leftEye", ods::isLeftEye);
            uniforms.uniform1i(UniformUpdateFrequency.PER_FRAME, "direction", ods::getDirection);
        }
        return uniforms;
    }
}
//#endif
