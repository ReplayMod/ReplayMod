//#if MC>=11600
package com.replaymod.render.capturer;

import com.mojang.blaze3d.platform.GlStateManager;
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

import static com.replaymod.core.versions.MCVer.popMatrix;
import static com.replaymod.core.versions.MCVer.pushMatrix;
import static com.replaymod.core.versions.MCVer.resizeMainWindow;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class IrisODSFrameCapturer implements FrameCapturer<ODSOpenGlFrame> {

    public static final String SHADER_PACK_NAME = "assets/replaymod/iris/ods";
    public static IrisODSFrameCapturer INSTANCE;
    private final CubicPboOpenGlFrameCapturer left, right;
    private final String prevShaderPack;
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
            public RenderSettings getRenderSettings() {
                return renderInfo.getRenderSettings();
            }
        };
        left = new CubicStereoFrameCapturer(worldRenderer, fakeInfo, frameSize);
        right = new CubicStereoFrameCapturer(worldRenderer, fakeInfo, frameSize);

        INSTANCE = this;
        prevShaderPack = Iris.getIrisConfig().getShaderPackName().orElse(null);
        setShaderPack(SHADER_PACK_NAME);
    }

    private static void setShaderPack(String name) {
        IrisConfig irisConfig = Iris.getIrisConfig();
        irisConfig.setShaderPackName(name);
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
        setShaderPack(prevShaderPack);
    }

    private class CubicStereoFrameCapturer extends CubicPboOpenGlFrameCapturer {
        public CubicStereoFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
            super(worldRenderer, renderInfo, frameSize);
        }

        @Override
        protected OpenGlFrame renderFrame(int frameId, float partialTicks, CubicOpenGlFrameCapturer.Data captureData) {
            resizeMainWindow(mc, getFrameWidth(), getFrameHeight());

            pushMatrix();
            frameBuffer().beginWrite(true);

            GlStateManager.clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                    //#if MC>=11400
                    , false
                    //#endif
            );
            GlStateManager.enableTexture();

            direction = captureData.ordinal();
            worldRenderer.renderWorld(partialTicks, null);

            frameBuffer().endWrite();
            popMatrix();

            return captureFrame(frameId, captureData);
        }
    }
}
//#endif
