//#if MC>=11600
package com.replaymod.render.mixin;

import com.replaymod.render.capturer.IrisODSFrameCapturer;
import net.coderbot.iris.Iris;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;

@Pseudo
@Mixin(value = Iris.class, remap = false)
public class Mixin_LoadIrisOdsShaderPack {
    @Redirect(method = "loadExternalShaderpack", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "Lnet/coderbot/iris/Iris;SHADERPACKS_DIRECTORY:Ljava/nio/file/Path;"))
    private static Path loadReplayModOdsPack(String name) {
        if (IrisODSFrameCapturer.INSTANCE != null && IrisODSFrameCapturer.SHADER_PACK_NAME.equals(name)) {
            return FabricLoader.getInstance().getModContainer("replaymod")
                    .orElseThrow(() -> new RuntimeException("Failed to get mod container for ReplayMod"))
                    .getRootPath();
        } else {
            return Iris.SHADERPACKS_DIRECTORY;
        }
    }
}
//#endif
