//#if MC>=11300
package com.replaymod.core.mixin;

import com.replaymod.core.versions.MCVer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements MCVer.MinecraftMethodAccessor {
    @Shadow protected abstract void processKeyBinds();

    @Override
    public void replayModProcessKeyBinds() {
        processKeyBinds();
    }
}
//#endif
