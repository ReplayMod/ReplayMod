package com.replaymod.core.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Migrate invalid unbound keys saved by earlier 26.3 builds to SDL's unknown key. */
@Mixin(KeyMapping.class)
public abstract class MixinSavedReplayKeys {
    @ModifyVariable(method = "setKey", at = @At("HEAD"), argsOnly = true)
    private InputConstants.Key replaymod$normalizeSavedKey(InputConstants.Key key) {
        if (((KeyMapping) (Object) this).getName().startsWith("key.replaymod.")
                && key.getType() == InputConstants.Type.KEYBOARD && key.getValue() < 0) {
            return InputConstants.UNKNOWN;
        }
        return key;
    }
}
