//#if MC>=11600
package com.replaymod.render.capturer;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.rendering.Channel;
import com.replaymod.render.rendering.FrameCapturer;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.coderbot.iris.Iris;
import net.coderbot.iris.config.IrisConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IrisODSFrameCapturer implements FrameCapturer<ODSOpenGlFrame> {

    public static final String SHADER_PACK_NAME = "assets/replaymod/iris/ods";
    public static IrisODSFrameCapturer INSTANCE;
    private final CubicPboOpenGlFrameCapturer left, right;
    private final String prevShaderPack;
    private final boolean prevShadersEnabled;
    private int direction;
    private boolean isLeftEye;

    public IrisODSFrameCapturer(WorldRenderer worldRenderer, final RenderInfo renderInfo, int frameSize) {
        RenderInfo fakeInfo = new RenderInfo() {
            private int call;
            private float partialTicks;
            @Override
            public ReadableDimension getFrameSize() {
                return renderInfo.getFrameSize();
            }

            @Override
            public int getFramesDone() {
                return renderInfo.getFramesDone();
            }

            @Override
            public int getTotalFrames() {
                return renderInfo.getTotalFrames();
            }

            @Override
            public float updateForNextFrame() {
                if (call++ % 2 == 0) {
                    partialTicks = renderInfo.updateForNextFrame();
                }
                return partialTicks;
            }

            @Override
            public void updatePostRender(float tickDelta) {}

            @Override
            public RenderSettings getRenderSettings() {
                return renderInfo.getRenderSettings();
            }
        };
        left = new CubicStereoFrameCapturer(worldRenderer, fakeInfo, frameSize);
        right = new CubicStereoFrameCapturer(worldRenderer, fakeInfo, frameSize);

        INSTANCE = this;
        IrisConfig irisConfig = Iris.getIrisConfig();
        prevShaderPack = irisConfig.getShaderPackName().orElse(null);
        prevShadersEnabled = irisConfig.areShadersEnabled();
        setShaderPack(SHADER_PACK_NAME, true);
    }

    private static void setShaderPack(String name, boolean enabled) {
        IrisConfig irisConfig = Iris.getIrisConfig();
        irisConfig.setShaderPackName(name);
        irisConfig.setShadersEnabled(enabled);
        try {
            irisConfig.save();
            Iris.reload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getDirection() {
        return direction;
    }

    public boolean isLeftEye() {
        return isLeftEye;
    }

    @Override
    public boolean isDone() {
        return left.isDone() && right.isDone();
    }

    @Override
    public Map<Channel, ODSOpenGlFrame> process() {
        isLeftEye = true;
        Map<Channel, CubicOpenGlFrame> leftChannels = left.process();
        isLeftEye = false;
        Map<Channel, CubicOpenGlFrame> rightChannels = right.process();

        if (leftChannels != null && rightChannels != null) {
            Map<Channel, ODSOpenGlFrame> result = new HashMap<>();
            for (Channel channel : Channel.values()) {
                CubicOpenGlFrame leftFrame = leftChannels.get(channel);
                CubicOpenGlFrame rightFrame = rightChannels.get(channel);
                if (leftFrame != null && rightFrame != null) {
                    result.put(channel, new ODSOpenGlFrame(leftFrame, rightFrame));
                }
            }
            return result;
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        left.close();
        right.close();
        INSTANCE = null;
        setShaderPack(prevShaderPack, prevShadersEnabled);
    }

    private class CubicStereoFrameCapturer extends CubicPboOpenGlFrameCapturer {
        public CubicStereoFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
            super(worldRenderer, renderInfo, frameSize);
        }

        @Override
        protected OpenGlFrame renderFrame(int frameId, float partialTicks, CubicOpenGlFrameCapturer.Data captureData) {
            direction = captureData.ordinal();
            return super.renderFrame(frameId, partialTicks, captureData);
        }
    }
}
//#endif
