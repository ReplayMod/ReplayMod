package com.replaymod.render.rendering;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.blend.BlendFrameCapturer;
import com.replaymod.render.capturer.CubicOpenGlFrameCapturer;
import com.replaymod.render.capturer.CubicPboOpenGlFrameCapturer;
import com.replaymod.render.capturer.ODSFrameCapturer;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.SimpleOpenGlFrameCapturer;
import com.replaymod.render.capturer.SimplePboOpenGlFrameCapturer;
import com.replaymod.render.capturer.StereoscopicOpenGlFrameCapturer;
import com.replaymod.render.capturer.StereoscopicPboOpenGlFrameCapturer;
import com.replaymod.render.capturer.WorldRenderer;
import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.frame.StereoscopicOpenGlFrame;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.render.processor.CubicToRGBProcessor;
import com.replaymod.render.processor.DummyProcessor;
import com.replaymod.render.processor.EquirectangularToRGBProcessor;
import com.replaymod.render.processor.ODSToRGBProcessor;
import com.replaymod.render.processor.OpenGlToRGBProcessor;
import com.replaymod.render.processor.StereoscopicToRGBProcessor;
import com.replaymod.render.utils.PixelBufferObject;

public class Pipelines {
    public static Pipeline newPipeline(RenderSettings.RenderMethod method, RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        switch (method) {
            case DEFAULT:
                return newDefaultPipeline(renderInfo, consumer);
            case STEREOSCOPIC:
                return newStereoscopicPipeline(renderInfo, consumer);
            case CUBIC:
                return newCubicPipeline(renderInfo, consumer);
            case EQUIRECTANGULAR:
                return newEquirectangularPipeline(renderInfo, consumer);
            case ODS:
                return newODSPipeline(renderInfo, consumer);
            case BLEND:
                throw new UnsupportedOperationException("Use newBlendPipeline instead!");
        }
        throw new UnsupportedOperationException("Unknown method: " + method);
    }

    public static Pipeline<OpenGlFrame, RGBFrame> newDefaultPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<OpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED) {
            capturer = new SimplePboOpenGlFrameCapturer(worldRenderer, renderInfo);
        } else {
            capturer = new SimpleOpenGlFrameCapturer(worldRenderer, renderInfo);
        }
        return new Pipeline<>(worldRenderer, capturer, new OpenGlToRGBProcessor(), consumer);
    }

    public static Pipeline<StereoscopicOpenGlFrame, RGBFrame> newStereoscopicPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<StereoscopicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED) {
            capturer = new StereoscopicPboOpenGlFrameCapturer(worldRenderer, renderInfo);
        } else {
            capturer = new StereoscopicOpenGlFrameCapturer(worldRenderer, renderInfo);
        }
        return new Pipeline<>(worldRenderer, capturer, new StereoscopicToRGBProcessor(), consumer);
    }

    public static Pipeline<CubicOpenGlFrame, RGBFrame> newCubicPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<CubicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED) {
            capturer = new CubicPboOpenGlFrameCapturer(worldRenderer, renderInfo, settings.getVideoWidth() / 4);
        } else {
            capturer = new CubicOpenGlFrameCapturer(worldRenderer, renderInfo, settings.getVideoWidth() / 4);
        }
        return new Pipeline<>(worldRenderer, capturer, new CubicToRGBProcessor(), consumer);
    }

    public static Pipeline<CubicOpenGlFrame, RGBFrame> newEquirectangularPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);

        EquirectangularToRGBProcessor processor = new EquirectangularToRGBProcessor(settings.getVideoWidth(),
                settings.getVideoHeight(), settings.getSphericalFovX());

        FrameCapturer<CubicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED) {
            capturer = new CubicPboOpenGlFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        } else {
            capturer = new CubicOpenGlFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        }
        return new Pipeline<>(worldRenderer, capturer, processor, consumer);
    }

    public static Pipeline<ODSOpenGlFrame, RGBFrame> newODSPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);

        ODSToRGBProcessor processor = new ODSToRGBProcessor(settings.getVideoWidth(),
                settings.getVideoHeight(), settings.getSphericalFovX());

        FrameCapturer<ODSOpenGlFrame> capturer =
                new ODSFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        return new Pipeline<>(worldRenderer, capturer, processor, consumer);
    }

    public static Pipeline<RGBFrame, RGBFrame> newBlendPipeline(RenderInfo renderInfo) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<RGBFrame> capturer = new BlendFrameCapturer(worldRenderer, renderInfo);
        FrameConsumer<RGBFrame> consumer = new FrameConsumer<RGBFrame>() {
            @Override
            public void consume(RGBFrame frame) {
            }

            @Override
            public void close() {
            }
        };
        return new Pipeline<>(worldRenderer, capturer, new DummyProcessor<>(), consumer);
    }
}
