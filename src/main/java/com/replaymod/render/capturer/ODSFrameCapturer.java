package com.replaymod.render.capturer;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.rendering.FrameCapturer;
import com.replaymod.render.shader.Program;
import de.johni0702.minecraft.gui.utils.lwjgl.ReadableDimension;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.util.ResourceLocation;

import static net.minecraft.client.renderer.GlStateManager.*;
import static net.minecraft.client.renderer.GlStateManager.BooleanState;

import java.io.IOException;

import static com.replaymod.core.versions.MCVer.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class ODSFrameCapturer implements FrameCapturer<ODSOpenGlFrame> {
    private static final ResourceLocation vertexResource = new ResourceLocation("replaymod", "shader/ods.vert");
    private static final ResourceLocation fragmentResource = new ResourceLocation("replaymod", "shader/ods.frag");

    private final CubicPboOpenGlFrameCapturer left, right;
    private final Program shaderProgram;
    private final Program.Uniform directionVariable;
    private final Program.Uniform leftEyeVariable;

    private final BooleanState[] previousStates = new BooleanState[3];
    private BooleanState previousFogState;

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
            throw new ReportedException(CrashReport.makeCrashReport(e, "Creating ODS shaders"));
        }
    }

    private void bindProgram() {
        shaderProgram.use();
        setTexture("texture", 0);
        setTexture("lightMap", 1);
        linkState(0, "textureEnabled");
        linkState(1, "lightMapEnabled");
        linkState(2, "hurtTextureEnabled");
        final Program.Uniform uniform = shaderProgram.getUniformVariable("fogEnabled");
        previousFogState = fog();
        uniform.set(previousFogState.currentState);
        fog(new BooleanState(previousFogState.capability) {
            { currentState = previousFogState.currentState; }
            @Override
            public void setState(boolean state) {
                super.setState(state);
                uniform.set(state);
            }
        });
    }

    private void unbindProgram() {
        for (int i = 0; i < previousStates.length; i++) {
            previousStates[i].currentState = texture2DState(i).currentState;
            texture2DState(i, previousStates[i]);
        }
        previousFogState.currentState = fog().currentState;
        fog(previousFogState);
        shaderProgram.stopUsing();
    }

    private void setTexture(String texture, int i) {
        shaderProgram.getUniformVariable(texture).set(i);
    }

    private void linkState(int id, String var) {
        final Program.Uniform uniform = shaderProgram.getUniformVariable(var);
        previousStates[id] = texture2DState(id);
        uniform.set(previousStates[id].currentState);
        texture2DState(id, new BooleanState(previousStates[id].capability) {
            { currentState = previousStates[id].currentState; }
            @Override
            public void setState(boolean state) {
                super.setState(state);
                uniform.set(state);
            }
        });
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
            frameBuffer().bindFramebuffer(true);

            clear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            enableTexture2D();

            directionVariable.set(captureData.ordinal());
            worldRenderer.renderWorld(partialTicks, null);

            frameBuffer().unbindFramebuffer();
            popMatrix();

            return captureFrame(frameId, captureData);
        }
    }
}
