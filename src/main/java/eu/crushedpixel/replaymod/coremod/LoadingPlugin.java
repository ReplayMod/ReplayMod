package eu.crushedpixel.replaymod.coremod;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class LoadingPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
                CameraRollCT.class.getName(),
                ForceChunkLoadingCT.class.getName(),
                EntityRendererCT.class.getName(),
                NoNameTagCT.class.getName(),
                RenderManagerCT.class.getName(),
                SoundManagerCT.class.getName(),
                EnchantmentTimerCT.class.getName()
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
