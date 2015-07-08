package eu.crushedpixel.replaymod.assets;

import eu.crushedpixel.replaymod.holders.*;
import eu.crushedpixel.replaymod.interpolation.GenericSplineInterpolation;
import eu.crushedpixel.replaymod.interpolation.KeyframeList;
import eu.crushedpixel.replaymod.registry.ResourceHelper;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

public class CustomImageObject implements GuiEntryListEntry {

    public CustomImageObject(Transformations transformations, String name, UUID assetUUID) throws IOException {
        this.name = name;

        setLinkedAsset(assetUUID);
    }

    @Getter @Setter private String name;
    @Getter private UUID linkedAsset;

    @Getter @Setter private float width, height;

    public void setLinkedAsset(UUID assetUUID) throws IOException {
        ReplayAsset asset = ReplayHandler.getAssetRepository().getAssetByUUID(assetUUID);

        if(asset instanceof ReplayImageAsset) {
            setImage(((ReplayImageAsset)asset).getObject());
        } else {
            throw new UnsupportedOperationException("A CustomImageObject requires a ReplayImageAsset");
        }
    }

    public void setImage(final BufferedImage bufferedImage) throws IOException {

        this.textureWidth = bufferedImage.getWidth();
        this.textureHeight = bufferedImage.getHeight();

        float w;
        float h;

        if(bufferedImage.getWidth() > bufferedImage.getHeight()) {
            w = 1;
            h = (bufferedImage.getHeight()/(float)bufferedImage.getWidth());
        } else {
            w = (bufferedImage.getWidth()/(float)bufferedImage.getHeight());
            h = 1;
        }

        this.setWidth(w);
        this.setHeight(h);

        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                resourceLocation = new ResourceLocation("customImages/"+linkedAsset.toString());
                dynamicTexture = new DynamicTexture(bufferedImage);
            }
        });
    }

    private ResourceLocation resourceLocation;
    private DynamicTexture dynamicTexture;

    @Getter private float textureWidth, textureHeight;

    public ResourceLocation getResourceLocation() {
        if(!ResourceHelper.isRegistered(resourceLocation)) {
            ResourceHelper.registerResource(resourceLocation);
            Minecraft.getMinecraft().getTextureManager().loadTexture(resourceLocation, dynamicTexture);
            dynamicTexture.updateDynamicTexture();
        }

        return resourceLocation;
    }

    @Override
    public String getDisplayString() {
        return name;
    }

    /**
     * Keyframing Code
     */

    private KeyframeList<Keyframe<Position>> anchorPointKeyframes, positionKeyframes, orientationKeyframes;
    private KeyframeList<Keyframe<Point>> scaleKeyframes;
    private KeyframeList<Keyframe<TimestampValue>> opacityKeyframes;

    private GenericSplineInterpolation<Position> anchorSpline, positionSpline, orientationSpline;
    private GenericSplineInterpolation<Point> scaleSpline;
    private GenericSplineInterpolation<TimestampValue> opacitySpline;

    public Transformations getTransformationsForTimestamp(int timestamp) {
        return null; //TODO
    }

}
