package eu.crushedpixel.replaymod.assets;

import eu.crushedpixel.replaymod.registry.ResourceHelper;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.utils.BoundingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.Dimension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class ReplayImageAsset implements ReplayAsset<BufferedImage> {

    private final Minecraft mc = Minecraft.getMinecraft();

    private BufferedImage object;

    private String name;

    public ReplayImageAsset(String name) {
        this.name = name;
    }

    @Override
    public void setAssetName(String name) {
        this.name = name;
    }

    @Override
    public String getDisplayString() {
        return name;
    }

    @Override
    public String getSavedFileExtension() {
        return "png";
    }

    @Override
    public void loadFromStream(InputStream inputStream) throws IOException {
        this.object = ImageIO.read(inputStream);
        ResourceHelper.freeResource(previewResource);

        for(CustomImageObject object : ReplayHandler.getCustomImageObjects()) {
            if(object.getLinkedAsset() != null && object.getLinkedAsset().equals(ReplayHandler.getAssetRepository().getUUIDForAsset(this))) {
                object.setImage(this.object);
            }
        }
    }

    @Override
    public void writeToStream(OutputStream outputStream) throws IOException {
        ImageIO.write(object, "png", outputStream);
    }

    @Override
    public BufferedImage getObject() {
        return object;
    }

    private ResourceLocation previewResource = new ResourceLocation("/asset/"+ UUID.randomUUID().toString());
    private DynamicTexture dynamicTexture;

    @Override
    public void drawToScreen(int x, int y, int maxWidth, int maxHeight) {
        if(object == null) return;
        if(!ResourceHelper.isRegistered(previewResource) || dynamicTexture == null) {
            dynamicTexture = new DynamicTexture(object);
            mc.getTextureManager().loadTexture(previewResource, dynamicTexture);
            ResourceHelper.registerResource(previewResource);
        }

        mc.renderEngine.bindTexture(previewResource);

        Dimension dimension = BoundingUtils.fitIntoBounds(new Dimension(object.getWidth(), object.getHeight()), new Dimension(maxWidth, maxHeight));

        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, object.getWidth(), object.getHeight(), dimension.getWidth(), dimension.getHeight(), object.getWidth(), object.getHeight());
    }

    @Override
    protected void finalize() throws Throwable {
        ResourceHelper.freeResource(previewResource);
        super.finalize();
    }
}
