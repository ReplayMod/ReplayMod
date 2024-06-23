package com.replaymod.render.blend;

import com.replaymod.render.blend.data.DImage;
import com.replaymod.render.blend.data.DMaterial;
import com.replaymod.render.blend.data.DPackedFile;
import com.replaymod.render.blend.data.DTexture;
import de.johni0702.minecraft.gui.versions.Image;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class BlendMaterials {
    private final Map<Integer, DMaterial> materials = new HashMap<>();

    public DMaterial getActiveMaterial() {
        int textureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        DMaterial material = materials.get(textureId);
        if (material == null) {
            // Read raw image data from GL
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            // Convert to Image
            Image bufImage = new Image(width, height);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int a = buffer.get();
                    int b = buffer.get();
                    int g = buffer.get();
                    int r = buffer.get();
                    bufImage.setRGBA(x, y, r, b, g, a);
                }
            }

            // Encode as png image
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try {
                bufImage.writePNG(stream);
            } catch (IOException e) {
                throw new RuntimeException(e); // never happens unless ImageIO is bugged
            }
            byte[] bytes = stream.toByteArray();

            // Wrap into blender structs
            DImage image = new DImage();
            image.id.name = "texture.png";
            image.filePath = "texture.png";
            image.packedFiles.add(Pair.of("texture.png", new DPackedFile(bytes)));

            DTexture texture = new DTexture();
            texture.image = image;

            DMaterial.DMTex mTex = new DMaterial.DMTex();
            mTex.texture = texture;

            material = new DMaterial();
            material.textures.add(mTex);
            materials.put(textureId, material);
        }
        return material;
    }
}
