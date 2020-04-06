package com.replaymod.render.capturer;

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

import static com.mojang.blaze3d.platform.GlStateManager.*;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class ODSFrameCapturer implements FrameCapturer<ODSOpenGlFrame> {
    private static final Identifier vertexResource = new Identifier("replaymod", "shader/ods.vert");
    private static final Identifier fragmentResource = new Identifier("replaymod", "shader/ods.frag");

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
    public ODSOpenGlFrame process() {
        bindProgram();
        leftEyeVariable.set(true);
        CubicOpenGlFrame leftFrame = left.process();
        leftEyeVariable.set(false);
        CubicOpenGlFrame rightFrame = right.process();
        unbindProgram();

        if (leftFrame != null && rightFrame != null) {
            return new ODSOpenGlFrame(leftFrame, rightFrame);
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
            resize(getFrameWidth(), getFrameHeight());

            pushMatrix();
            frameBuffer().beginWrite(true);

            clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT
                    //#if MC>=11400
                    , false
                    //#endif
            );
            enableTexture();

            directionVariable.set(captureData.ordinal());
            worldRenderer.renderWorld(partialTicks, null);

            frameBuffer().endWrite();
            popMatrix();

            return captureFrame(frameId, captureData);
        }
    }
}
