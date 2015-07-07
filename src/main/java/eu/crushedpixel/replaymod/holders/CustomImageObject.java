package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.registry.ResourceHelper;
import eu.crushedpixel.replaymod.utils.RoundUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CustomImageObject implements GuiEntryListEntry {

    public CustomImageObject(Position position, String name, final File imageSource) throws IOException {

        this.position = new ExtendedPosition(RoundUtils.round(position.getX()), RoundUtils.round(position.getY()), RoundUtils.round(position.getZ()), 0, 0);

        this.name = name;

        setImageFile(imageSource);
    }

    @Getter @Setter private ExtendedPosition position;
    @Getter @Setter private String name;
    @Getter private File imageFile;

    public void setImageFile(final File imageSource) throws IOException {
        this.imageFile = imageSource;

        final BufferedImage bufferedImage = ImageIO.read(imageSource);

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

        this.position.setWidth(w);
        this.position.setHeight(h);

        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                resourceLocation = new ResourceLocation("customImages/"+imageSource.getAbsolutePath());
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
}
