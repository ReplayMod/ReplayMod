package com.replaymod.render.rendering;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.blend.BlendFrameCapturer;
import com.replaymod.render.capturer.CubicOpenGlFrameCapturer;
import com.replaymod.render.capturer.CubicPboOpenGlFrameCapturer;
import com.replaymod.render.capturer.ODSFrameCapturer;
import com.replaymod.render.capturer.RenderInfo;
import com.replaymod.render.capturer.SimpleOpenGlFrameCapturer;
import com.replaymod.render.capturer.SimpleOpenGlTextureFrameCapturer;
import com.replaymod.render.capturer.SimplePboOpenGlFrameCapturer;
import com.replaymod.render.capturer.StereoscopicOpenGlFrameCapturer;
import com.replaymod.render.capturer.StereoscopicPboOpenGlFrameCapturer;
import com.replaymod.render.capturer.WorldRenderer;
import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.ODSOpenGlFrame;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.frame.BitmapFrame;
import com.replaymod.render.frame.OpenGlTextureFrame;
import com.replaymod.render.frame.StereoscopicOpenGlFrame;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.render.processor.CubicToBitmapProcessor;
import com.replaymod.render.processor.DummyProcessor;
import com.replaymod.render.processor.EquirectangularToBitmapProcessor;
import com.replaymod.render.processor.ODSToBitmapProcessor;
import com.replaymod.render.processor.OpenGlToBitmapProcessor;
import com.replaymod.render.processor.StereoscopicToBitmapProcessor;
import com.replaymod.render.utils.PixelBufferObject;

import java.util.Map;

public class Pipelines {
    public static Pipeline newPipeline(RenderSettings.RenderMethod method, RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        return newPipeline(method, renderInfo, consumer, false);
    }

    public static Pipeline newPipeline(RenderSettings.RenderMethod method, RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer, boolean useRawDefaultOpenGlFrames) {
        switch (method) {
            case DEFAULT:
                return newDefaultPipeline(renderInfo, consumer, useRawDefaultOpenGlFrames);
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

    public static Pipeline<OpenGlFrame, BitmapFrame> newDefaultPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        return newDefaultPipeline(renderInfo, consumer, false);
    }

    public static Pipeline<OpenGlFrame, BitmapFrame> newDefaultPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer, boolean useRawOpenGlFrames) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<OpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED || settings.isDepthMap()) {
            capturer = new SimplePboOpenGlFrameCapturer(worldRenderer, renderInfo, useRawOpenGlFrames);
        } else {
            capturer = new SimpleOpenGlFrameCapturer(worldRenderer, renderInfo);
        }
        if (useRawOpenGlFrames) {
            com.replaymod.render.ReplayModRender.LOGGER.info("Using raw OpenGL frame fast path; vertical flip will be handled by FFmpeg.");
        }
        return new Pipeline<>(settings, worldRenderer, capturer, new OpenGlToBitmapProcessor(!useRawOpenGlFrames), consumer);
    }

    public static RenderPipeline newNativeOpenGlEncoderPipeline(RenderInfo renderInfo, FrameConsumer<OpenGlTextureFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        SimpleOpenGlTextureFrameCapturer capturer = new SimpleOpenGlTextureFrameCapturer(worldRenderer, renderInfo);
        com.replaymod.render.ReplayModRender.LOGGER.info("Using native OpenGL texture encoder pipeline; frames stay on the GPU until NVENC consumes them.");
        return new DirectOpenGlEncoderPipeline(settings, worldRenderer, capturer, consumer);
    }

    public static Pipeline<StereoscopicOpenGlFrame, BitmapFrame> newStereoscopicPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<StereoscopicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED || settings.isDepthMap()) {
            capturer = new StereoscopicPboOpenGlFrameCapturer(worldRenderer, renderInfo);
        } else {
            capturer = new StereoscopicOpenGlFrameCapturer(worldRenderer, renderInfo);
        }
        return new Pipeline<>(settings, worldRenderer, capturer, new StereoscopicToBitmapProcessor(), consumer);
    }

    public static Pipeline<CubicOpenGlFrame, BitmapFrame> newCubicPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<CubicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED || settings.isDepthMap()) {
            capturer = new CubicPboOpenGlFrameCapturer(worldRenderer, renderInfo, settings.getVideoWidth() / 4);
        } else {
            capturer = new CubicOpenGlFrameCapturer(worldRenderer, renderInfo, settings.getVideoWidth() / 4);
        }
        return new Pipeline<>(settings, worldRenderer, capturer, new CubicToBitmapProcessor(), consumer);
    }

    public static Pipeline<CubicOpenGlFrame, BitmapFrame> newEquirectangularPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);

        EquirectangularToBitmapProcessor processor = new EquirectangularToBitmapProcessor(settings.getVideoWidth(),
                settings.getVideoHeight(), settings.getSphericalFovX());

        FrameCapturer<CubicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED || settings.isDepthMap()) {
            capturer = new CubicPboOpenGlFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        } else {
            capturer = new CubicOpenGlFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        }
        return new Pipeline<>(settings, worldRenderer, capturer, processor, consumer);
    }

    public static Pipeline<ODSOpenGlFrame, BitmapFrame> newODSPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);

        ODSToBitmapProcessor processor = new ODSToBitmapProcessor(settings.getVideoWidth(),
                settings.getVideoHeight(), settings.getSphericalFovX());

        //#if MC>=11600
        boolean iris = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris");
        FrameCapturer<ODSOpenGlFrame> capturer = iris
                ? new com.replaymod.render.capturer.IrisODSFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize())
                : new ODSFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        //#else
        //$$ FrameCapturer<ODSOpenGlFrame> capturer = new ODSFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        //#endif
        return new Pipeline<>(settings, worldRenderer, capturer, processor, consumer);
    }

    public static Pipeline<BitmapFrame, BitmapFrame> newBlendPipeline(RenderInfo renderInfo) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<BitmapFrame> capturer = new BlendFrameCapturer(worldRenderer, renderInfo);
        FrameConsumer<BitmapFrame> consumer = new FrameConsumer<BitmapFrame>() {
            @Override
            public void consume(Map<Channel, BitmapFrame> channels) {
            }

            @Override
            public void close() {
            }

            @Override
            public boolean isParallelCapable() {
                return true;
            }
        };
        return new Pipeline<>(settings, worldRenderer, capturer, new DummyProcessor<>(), consumer);
    }
}
