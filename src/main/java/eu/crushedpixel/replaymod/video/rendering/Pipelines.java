package eu.crushedpixel.replaymod.video.rendering;

import eu.crushedpixel.replaymod.settings.RenderOptions;
import eu.crushedpixel.replaymod.video.capturer.*;
import eu.crushedpixel.replaymod.video.entity.CubicEntityRenderer;
import eu.crushedpixel.replaymod.video.entity.CustomEntityRenderer;
import eu.crushedpixel.replaymod.video.entity.StereoscopicEntityRenderer;
import eu.crushedpixel.replaymod.video.frame.ARGBFrame;
import eu.crushedpixel.replaymod.video.frame.CubicOpenGlFrame;
import eu.crushedpixel.replaymod.video.frame.OpenGlFrame;
import eu.crushedpixel.replaymod.video.frame.StereoscopicOpenGlFrame;
import eu.crushedpixel.replaymod.video.processor.CubicToARGBProcessor;
import eu.crushedpixel.replaymod.video.processor.EquirectangularToARGBProcessor;
import eu.crushedpixel.replaymod.video.processor.OpenGlToARGBProcessor;
import eu.crushedpixel.replaymod.video.processor.StereoscopicToARGBProcessor;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Pipelines {
    public enum Preset {
        DEFAULT, STEREOSCOPIC, CUBIC, EQUIRECTANGULAR
    }

    public static Pipeline newPipeline(Preset preset, RenderInfo renderInfo, FrameConsumer<ARGBFrame> consumer) {
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

    public static Pipeline<OpenGlFrame, ARGBFrame> newDefaultPipeline(RenderInfo renderInfo, FrameConsumer<ARGBFrame> consumer) {
        RenderOptions options = renderInfo.getRenderOptions();
        return new Pipeline<OpenGlFrame, ARGBFrame>(
                new SimpleOpenGlFrameCapturer(new CustomEntityRenderer<CaptureData>(options), renderInfo),
                new OpenGlToARGBProcessor(),
                consumer
        );
    }

    public static Pipeline<StereoscopicOpenGlFrame, ARGBFrame> newStereoscopicPipeline(RenderInfo renderInfo, FrameConsumer<ARGBFrame> consumer) {
        RenderOptions options = renderInfo.getRenderOptions();
        return new Pipeline<StereoscopicOpenGlFrame, ARGBFrame>(
                new StereoscopicOpenGlFrameCapturer(new StereoscopicEntityRenderer(options), renderInfo),
                new StereoscopicToARGBProcessor(),
                consumer
        );
    }

    public static Pipeline<CubicOpenGlFrame, ARGBFrame> newCubicPipeline(RenderInfo renderInfo, FrameConsumer<ARGBFrame> consumer) {
        RenderOptions options = renderInfo.getRenderOptions();
        return new Pipeline<CubicOpenGlFrame, ARGBFrame>(
                new CubicOpenGlFrameCapturer(new CubicEntityRenderer(options), renderInfo, options.getWidth() / 4),
                new CubicToARGBProcessor(),
                consumer
        );
    }

    public static Pipeline<CubicOpenGlFrame, ARGBFrame> newEquirectangularPipeline(RenderInfo renderInfo, FrameConsumer<ARGBFrame> consumer) {
        RenderOptions options = renderInfo.getRenderOptions();
        return new Pipeline<CubicOpenGlFrame, ARGBFrame>(
                new CubicOpenGlFrameCapturer(new CubicEntityRenderer(options), renderInfo, options.getWidth() / 4),
                new EquirectangularToARGBProcessor(options.getWidth() / 4),
                consumer
        );
    }
}
