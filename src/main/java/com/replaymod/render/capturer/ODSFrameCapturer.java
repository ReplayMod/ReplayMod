package com.replaymod.render.capturer;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.hooks.GLStateTracker;
import com.replaymod.render.rendering.FrameCapturer;
import com.replaymod.render.shader.Program;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.ReadableDimension;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;

public class ODSFrameCapturer implements FrameCapturer<ODSOpenGlFrame> {
    private static final ResourceLocation vertexResource = new ResourceLocation("replaymod", "shader/ods.vert");
    private static final ResourceLocation fragmentResource = new ResourceLocation("replaymod", "shader/ods.frag");

    private final CubicPboOpenGlFrameCapturer left, right;
    private final Program shaderProgram;
    private final Program.Uniform directionVariable;
    private final Program.Uniform leftEyeVariable;

    private final GLStateTracker.EnabledState[] previousStates = new GLStateTracker.EnabledState[2];
    private final GLStateTracker.EnabledState previousFogState;

    private final Minecraft mc = Minecraft.getMinecraft();

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
                    shaderProgram.stopUsing();
                    partialTicks = renderInfo.updateForNextFrame();
                    shaderProgram.use();
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
            shaderProgram.use();
            leftEyeVariable = shaderProgram.getUniformVariable("leftEye");
            directionVariable = shaderProgram.getUniformVariable("direction");
            setTexture("texture", 0);
            setTexture("lightMap", 1);
            linkState(0, "textureEnabled");
            linkState(1, "lightMapEnabled");
            final Program.Uniform uniform = shaderProgram.getUniformVariable("fogEnabled");
            previousFogState = GLStateTracker.getInstance().fog;
            uniform.set(previousFogState.isEnabled());
            GLStateTracker.getInstance().fog = new GLStateTracker.EnabledState() {
                @Override
                public void setEnabled(boolean enabled) {
                    super.setEnabled(enabled);
                    uniform.set(enabled);
                }
            };
            shaderProgram.stopUsing();
        } catch (Exception e) {
            throw new ReportedException(CrashReport.makeCrashReport(e, "Creating ODS shaders"));
        }
    }

    private void setTexture(String texture, int i) {
        shaderProgram.getUniformVariable(texture).set(i);
    }

    private void linkState(int id, String var) {
        final Program.Uniform uniform = shaderProgram.getUniformVariable(var);
        previousStates[id] = GLStateTracker.getInstance().texture[id];
        uniform.set(previousStates[id].isEnabled());
        GLStateTracker.getInstance().texture[id] = new GLStateTracker.EnabledState() {
            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                uniform.set(enabled);
            }
        };
    }

    @Override
    public boolean isDone() {
        return left.isDone() && right.isDone();
    }

    @Override
    public ODSOpenGlFrame process() {
        shaderProgram.use();
        leftEyeVariable.set(true);
        CubicOpenGlFrame leftFrame = left.process();
        leftEyeVariable.set(false);
        CubicOpenGlFrame rightFrame = right.process();
        shaderProgram.stopUsing();

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
        for (int i = 0; i < previousStates.length; i++) {
            GLStateTracker.getInstance().texture[i] = previousStates[i];
        }
        GLStateTracker.getInstance().fog = previousFogState;
    }

    private class CubicStereoFrameCapturer extends CubicPboOpenGlFrameCapturer {
        public CubicStereoFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
            super(worldRenderer, renderInfo, frameSize);
        }

        @Override
        protected OpenGlFrame renderFrame(int frameId, float partialTicks, CubicOpenGlFrameCapturer.Data captureData) {
            resize(getFrameWidth(), getFrameHeight());

            glPushMatrix();
            frameBuffer().bindFramebuffer(true);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glEnable(GL_TEXTURE_2D);

            directionVariable.set(captureData.ordinal());
            worldRenderer.renderWorld(partialTicks, null);

            frameBuffer().unbindFramebuffer();
            glPopMatrix();

            return captureFrame(frameId, captureData);
        }
    }
}
