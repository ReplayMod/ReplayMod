package eu.crushedpixel.replaymod.registry;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ResourceHelper {

    private static BufferedImage defaultThumb;
    private static List<ResourceLocation> openResources = new ArrayList<ResourceLocation>();

    static {
        try {
            defaultThumb = ImageIO.read(ResourceHelper.class.getClassLoader().getResourceAsStream("default_thumb.jpg"));
        } catch(Exception e) {
            defaultThumb = new BufferedImage(1, 1, BufferedImage.TYPE_3BYTE_BGR);
            e.printStackTrace();
        }
    }

    public static void registerResource(ResourceLocation loc) {
        openResources.add(loc);
    }

    public static void freeResource(ResourceLocation loc) {
        Minecraft.getMinecraft().getTextureManager().deleteTexture(loc);
        openResources.remove(loc);
    }

    public static void freeAllResources() {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                for(ResourceLocation loc : openResources) {
                    Minecraft.getMinecraft().getTextureManager().deleteTexture(loc);
                }

                openResources = new ArrayList<ResourceLocation>();
            }
        });
    }

    public static BufferedImage getDefaultThumbnail() {
        return defaultThumb;
    }
}
