package com.replaymod.render.capturer;

import com.replaymod.render.rendering.Channel;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import com.replaymod.render.RenderSettings;
import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.hooks.FogStateCallback;
import com.replaymod.render.hooks.Texture2DStateCallback;
import com.replaymod.render.rendering.FrameCapturer;
import com.replaymod.render.shader.Program;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static de.johni0702.minecraft.gui.versions.MCVer.identifier;

public class ODSFrameCapturer implements FrameCapturer<ODSOpenGlFrame> {
    private static final Identifier vertexResource = identifier("replaymod", "shader/ods.vert");
    private static final Identifier fragmentResource = identifier("replaymod", "shader/ods.frag");

    private final CubicPboOpenGlFrameCapturer left, right;
    private final Program shaderProgram;
    private final Program.Uniform directionVariable;
    private final Program.Uniform leftEyeVariable;

    private EventRegistrations renderStateEvents;

    public ODSFrameCapturer(WorldRenderer worldRenderer, final RenderInfo renderInfo, int frameSize) {
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
                    unbindProgram();
                    partialTicks = renderInfo.updateForNextFrame();
                    bindProgram();
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
        try {
            shaderProgram = new Program(vertexResource, fragmentResource);
            leftEyeVariable = shaderProgram.getUniformVariable("leftEye");
            directionVariable = shaderProgram.getUniformVariable("direction");
        } catch (Exception e) {
            throw new CrashException(CrashReport.create(e, "Creating ODS shaders"));
        }
    }

    private void bindProgram() {
        shaderProgram.use();
        setTexture("texture", 0);
        //#if MC>=11500
        setTexture("overlay", 1);
        setTexture("lightMap", 2);
        //#else
        //$$ setTexture("lightMap", 1);
        //#endif

        renderStateEvents = new EventRegistrations();
        Program.Uniform[] texture2DUniforms = new Program.Uniform[]{
                shaderProgram.getUniformVariable("textureEnabled"),
                //#if MC>=11500
                shaderProgram.getUniformVariable("overlayEnabled"),
                shaderProgram.getUniformVariable("lightMapEnabled"),
                //#else
                //$$ shaderProgram.getUniformVariable("lightMapEnabled"),
                //$$ shaderProgram.getUniformVariable("overlayEnabled"),
                //#endif
        };
        renderStateEvents.on(Texture2DStateCallback.EVENT, (id, enabled) -> {
            if (id >= 0 && id < texture2DUniforms.length) {
                texture2DUniforms[id].set(enabled);
            }
        });
        Program.Uniform fogUniform = shaderProgram.getUniformVariable("fogEnabled");
        renderStateEvents.on(FogStateCallback.EVENT, fogUniform::set);

        renderStateEvents.register();
    }

    private void unbindProgram() {
        renderStateEvents.unregister();
        renderStateEvents = null;
        shaderProgram.stopUsing();
    }

    private void setTexture(String texture, int i) {
        shaderProgram.getUniformVariable(texture).set(i);
    }

    @Override
    public boolean isDone() {
        return left.isDone() && right.isDone();
    }

    @Override
    public Map<Channel, ODSOpenGlFrame> process() {
        bindProgram();
        leftEyeVariable.set(true);
        Map<Channel, CubicOpenGlFrame> leftChannels = left.process();
        leftEyeVariable.set(false);
        Map<Channel, CubicOpenGlFrame> rightChannels = right.process();
        unbindProgram();

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
        shaderProgram.delete();
    }

    private class CubicStereoFrameCapturer extends CubicPboOpenGlFrameCapturer {
        public CubicStereoFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
            super(worldRenderer, renderInfo, frameSize);
        }

        @Override
        protected OpenGlFrame renderFrame(int frameId, float partialTicks, CubicOpenGlFrameCapturer.Data captureData) {
            directionVariable.set(captureData.ordinal());
            return super.renderFrame(frameId, partialTicks, captureData);
        }
    }
}
