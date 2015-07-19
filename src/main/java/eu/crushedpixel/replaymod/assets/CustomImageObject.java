package eu.crushedpixel.replaymod.assets;

import eu.crushedpixel.replaymod.holders.GuiEntryListEntry;
import eu.crushedpixel.replaymod.holders.Transformations;
import eu.crushedpixel.replaymod.registry.ResourceHelper;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

@EqualsAndHashCode
public class CustomImageObject implements GuiEntryListEntry {

    public CustomImageObject(String name, UUID assetUUID) throws IOException {
        this.name = name;

        setLinkedAsset(assetUUID);
    }

    @Getter @Setter private String name;
    @Getter private UUID linkedAsset;

    @Getter @Setter private float width, height;

    private transient ResourceLocation resourceLocation;
    private transient DynamicTexture dynamicTexture;

    @Getter private float textureWidth, textureHeight;

    @Getter private Transformations transformations = new Transformations();

    public void setLinkedAsset(UUID assetUUID) throws IOException {
        ReplayAsset asset = ReplayHandler.getAssetRepository().getAssetByUUID(assetUUID);

        if(asset instanceof ReplayImageAsset) {
            this.linkedAsset = assetUUID;
            setImage(((ReplayImageAsset)asset).getObject());
        } else if(asset != null) {
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

        resourceLocation = new ResourceLocation(linkedAsset.toString());

        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                dynamicTexture = new DynamicTexture(bufferedImage);

                ResourceHelper.freeResource(resourceLocation);
            }
        });
    }

    public ResourceLocation getResourceLocation() {
        if(resourceLocation == null) {
            ReplayAsset asset = ReplayHandler.getAssetRepository().getAssetByUUID(linkedAsset);
            if(asset instanceof ReplayImageAsset) {
                try {
                    setImage(((ReplayImageAsset) asset).getObject());
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

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

}
