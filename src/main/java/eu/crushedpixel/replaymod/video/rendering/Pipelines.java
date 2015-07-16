package eu.crushedpixel.replaymod.video.rendering;

import eu.crushedpixel.replaymod.opengl.PixelBufferObject;
import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.capturer.*;
import eu.crushedpixel.replaymod.video.entity.CubicEntityRenderer;
import eu.crushedpixel.replaymod.video.entity.CustomEntityRenderer;
import eu.crushedpixel.replaymod.video.entity.StereoscopicEntityRenderer;
import eu.crushedpixel.replaymod.video.frame.CubicOpenGlFrame;
import eu.crushedpixel.replaymod.video.frame.OpenGlFrame;
import eu.crushedpixel.replaymod.video.frame.RGBFrame;
import eu.crushedpixel.replaymod.video.frame.StereoscopicOpenGlFrame;
import eu.crushedpixel.replaymod.video.processor.CubicToRGBProcessor;
import eu.crushedpixel.replaymod.video.processor.EquirectangularToRGBProcessor;
import eu.crushedpixel.replaymod.video.processor.OpenGlToRGBProcessor;
import eu.crushedpixel.replaymod.video.processor.StereoscopicToRGBProcessor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Pipelines {
    public enum Preset {
        DEFAULT, STEREOSCOPIC, CUBIC, EQUIRECTANGULAR
    }

    public static Pipeline newPipeline(Preset preset, RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        switch (preset) {
            case DEFAULT:
                return newDefaultPipeline(renderInfo, consumer);
            case STEREOSCOPIC:
                return newStereoscopicPipeline(renderInfo, consumer);
            case CUBIC:
                return newCubicPipeline(renderInfo, consumer);
            case EQUIRECTANGULAR:
                return newEquirectangularPipeline(renderInfo, consumer);
        }
        throw new UnsupportedOperationException("Unknown preset: " + preset);
    }

    public static Pipeline<OpenGlFrame, RGBFrame> newDefaultPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderOptions options = renderInfo.getRenderOptions();
        FrameCapturer<OpenGlFrame> capturer;
        if (PixelBufferObject.SUPPORTED) {
            capturer = new SimplePboOpenGlFrameCapturer(new CustomEntityRenderer<CaptureData>(options), renderInfo);
        } else {
            capturer = new SimpleOpenGlFrameCapturer(new CustomEntityRenderer<CaptureData>(options), renderInfo);
        }
        return new Pipeline<OpenGlFrame, RGBFrame>(capturer, new OpenGlToRGBProcessor(), consumer);
    }

    public static Pipeline<StereoscopicOpenGlFrame, RGBFrame> newStereoscopicPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderOptions options = renderInfo.getRenderOptions();
        return new Pipeline<StereoscopicOpenGlFrame, RGBFrame>(
                new StereoscopicOpenGlFrameCapturer(new StereoscopicEntityRenderer(options), renderInfo),
                new StereoscopicToRGBProcessor(),
                consumer
        );
    }

    public static Pipeline<CubicOpenGlFrame, RGBFrame> newCubicPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderOptions options = renderInfo.getRenderOptions();
        return new Pipeline<CubicOpenGlFrame, RGBFrame>(
                new CubicOpenGlFrameCapturer(new CubicEntityRenderer(options), renderInfo, options.getWidth() / 4),
                new CubicToRGBProcessor(),
                consumer
        );
    }

    public static Pipeline<CubicOpenGlFrame, RGBFrame> newEquirectangularPipeline(RenderInfo renderInfo, FrameConsumer<RGBFrame> consumer) {
        RenderOptions options = renderInfo.getRenderOptions();
        return new Pipeline<CubicOpenGlFrame, RGBFrame>(
                new CubicOpenGlFrameCapturer(new CubicEntityRenderer(options), renderInfo, options.getWidth() / 4),
                new EquirectangularToRGBProcessor(options.getWidth() / 4),
                consumer
        );
    }
}
