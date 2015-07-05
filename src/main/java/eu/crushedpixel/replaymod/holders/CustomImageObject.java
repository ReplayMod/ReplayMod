package eu.crushedpixel.replaymod.holders;

import eu.crushedpixel.replaymod.registry.ResourceHelper;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CustomImageObject {

    public CustomImageObject(Position position, String name, File imageSource) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(imageSource);

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

        this.position = new ExtendedPosition(position.getX(), position.getY(), position.getZ(), w, h);
        this.name = name;

        this.resourceLocation = new ResourceLocation("customImages/"+imageSource.getAbsolutePath());
        this.dynamicTexture = new DynamicTexture(bufferedImage);
    }

    @Getter private ExtendedPosition position;
    @Getter private String name;

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
}
