package com.replaymod.compat;

import com.replaymod.compat.optifine.DisableFastRender;
import com.replaymod.compat.oranges17animations.HideInvisibleEntities;
import com.replaymod.core.Module;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC<11300
//$$ import com.replaymod.compat.bettersprinting.DisableBetterSprinting;
//#endif

//#if MC>=10800
import com.replaymod.compat.shaders.ShaderBeginRender;
//#endif

import static com.replaymod.core.versions.MCVer.*;

public class ReplayModCompat implements Module {
    public static Logger LOGGER = LogManager.getLogger();

    @Override
    public void initClient() {
        IEventBus bus = FML_BUS;
        //#if MC>=10800
        bus.register(new ShaderBeginRender());
        //#endif
        bus.register(new DisableFastRender());
        bus.register(new HideInvisibleEntities());
        //#if MC<11300
        //$$ DisableBetterSprinting.register();
        //#endif
    }

}
