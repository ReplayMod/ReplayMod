package com.replaymod.render.rendering;

import com.replaymod.render.RenderSettings;
import com.replaymod.render.capturer.*;
import com.replaymod.render.frame.CubicOpenGlFrame;
import com.replaymod.render.frame.OpenGlFrame;
import com.replaymod.render.frame.RGBFrame;
import com.replaymod.render.frame.StereoscopicOpenGlFrame;
import com.replaymod.render.hooks.EntityRendererHandler;
import com.replaymod.render.processor.CubicToRGBProcessor;
import com.replaymod.render.processor.EquirectangularToRGBProcessor;
import com.replaymod.render.processor.OpenGlToRGBProcessor;
import com.replaymod.render.processor.StereoscopicToRGBProcessor;
import com.replaymod.render.utils.PixelBufferObject;
import lombok.experimental.UtilityClass;

@UtilityClass
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
        }
        throw new UnsupportedOperationException("Unknown method: " + method);
    }

    public static Pipeline<OpenGlFrame, RGBFrame> newDefaultPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        FrameCapturer<OpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED) {
            capturer = new SimplePboOpenGlFrameCapturer(new EntityRendererHandler(settings), renderInfo);
        } else {
            capturer = new SimpleOpenGlFrameCapturer(new EntityRendererHandler(settings), renderInfo);
        }
        return new Pipeline<>(capturer, new OpenGlToRGBProcessor(), consumer);
    }

    public static Pipeline<StereoscopicOpenGlFrame, RGBFrame> newStereoscopicPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        FrameCapturer<StereoscopicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED) {
            capturer = new StereoscopicPboOpenGlFrameCapturer(new EntityRendererHandler(settings), renderInfo);
        } else {
            capturer = new StereoscopicOpenGlFrameCapturer(new EntityRendererHandler(settings), renderInfo);
        }
        return new Pipeline<>(capturer, new StereoscopicToRGBProcessor(), consumer);
    }

    public static Pipeline<CubicOpenGlFrame, RGBFrame> newCubicPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        FrameCapturer<CubicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED) {
            capturer = new CubicPboOpenGlFrameCapturer(new EntityRendererHandler(settings), renderInfo, settings.getVideoWidth() / 4);
        } else {
            capturer = new CubicOpenGlFrameCapturer(new EntityRendererHandler(settings), renderInfo, settings.getVideoWidth() / 4);
        }
        return new Pipeline<>(capturer, new CubicToRGBProcessor(), consumer);
    }

    public static Pipeline<CubicOpenGlFrame, RGBFrame> newEquirectangularPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        FrameCapturer<CubicOpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED) {
            capturer = new CubicPboOpenGlFrameCapturer(new EntityRendererHandler(settings), renderInfo, settings.getVideoWidth() / 4);
        } else {
            capturer = new CubicOpenGlFrameCapturer(new EntityRendererHandler(settings), renderInfo, settings.getVideoWidth() / 4);
        }
        return new Pipeline<>(capturer, new EquirectangularToRGBProcessor(settings.getVideoWidth() / 4), consumer);
    }
}
