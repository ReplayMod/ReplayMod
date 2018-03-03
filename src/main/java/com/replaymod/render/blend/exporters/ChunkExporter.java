package com.replaymod.render.blend.exporters;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMaterial;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import com.replaymod.replaystudio.us.myles.ViaVersion.util.ReflectionUtil;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL15;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkExporter implements Exporter {
    private final Map<BlockPos, DObject> chunkObjects = new HashMap<>();
    private final Map<BlockPos, Map<EnumWorldBlockLayer, DObject>> chunkLayerObjects = new HashMap<>();
    private final List<Pair<RenderChunk, EnumWorldBlockLayer>> chunks = new ArrayList<>();
    private DObject chunksObject;
    private DMaterial material;
    private int frame;

    public void addChunkUpdate(RenderChunk chunk, EnumWorldBlockLayer layer) {
        chunks.add(Pair.of(chunk, layer));
    }

    @Override
    @SneakyThrows
    public void setup() throws IOException {

        Minecraft mc = Minecraft.getMinecraft();
        @SuppressWarnings("unchecked")
        List<RenderGlobal.ContainerLocalRenderInformation> renderInfos = mc.renderGlobal.renderInfos;
        for (RenderGlobal.ContainerLocalRenderInformation renderInfo : renderInfos) {
            RenderChunk renderChunk = ReflectionUtil.get(renderInfo, "renderChunk", RenderChunk.class); // SneakyThrows
            CompiledChunk compiledChunk = renderChunk.getCompiledChunk();
            if (!compiledChunk.isEmpty()) {
                for (EnumWorldBlockLayer layer : EnumWorldBlockLayer.values()) {
                    addChunkUpdate(renderChunk, layer);
                }
            }
        }

        chunksObject = new DObject(DObject.Type.OB_EMPTY);
        chunksObject.id.name = "Chunks";
        chunksObject.layers = 1 << 1;
        BlendState.getState().getScene().base.add(chunksObject);
    }

    @Override
    public void tearDown() throws IOException {
        chunks.clear();
        chunkObjects.clear();
        chunksObject = null;
    }

    @Override
    public void preFrame(int frame) throws IOException {
        this.frame = frame;
    }

    @Override
    public void postFrame(int frame) throws IOException {
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        material = BlendState.getState().getMaterials().getActiveMaterial();

        for (Pair<RenderChunk, EnumWorldBlockLayer> pair : chunks) {
            RenderChunk chunk = pair.getLeft();
            EnumWorldBlockLayer layer = pair.getRight();
            DObject chunkObject = chunkObjects.get(chunk.getPosition());
            if (chunkObject == null) {
                chunkObject = buildChunkObject(chunk);
                chunkObjects.put(chunk.getPosition(), chunkObject);
                chunkLayerObjects.put(chunk.getPosition(), new EnumMap<>(EnumWorldBlockLayer.class));
            }
            DObject layerObject = buildChunkLayerObject(chunkObject, chunk, layer);
            if (layerObject == null) continue;
            Map<EnumWorldBlockLayer, DObject> layerObjects = chunkLayerObjects.get(chunk.getPosition());
            DObject oldLayerObject = layerObjects.get(layer);
            if (oldLayerObject != null) {
                oldLayerObject.keyframe("hide", 0, frame, 1f);
            }
            if (frame > 0) {
                layerObject.keyframe("hide", 0, frame - 1, 1f);
                layerObject.keyframe("hide", 0, frame, 0f);
            } else {
                layerObject.keyframe("hide", 0, frame, 0f);
            }
            layerObjects.put(layer, layerObject);
        }
        chunks.clear();
    }

    private DObject buildChunkObject(RenderChunk renderChunk) {
        BlockPos pos = renderChunk.getPosition();
        DObject chunkObject = new DObject(DObject.Type.OB_EMPTY);
        chunkObject.setParent(chunksObject);
        chunkObject.id.name = "Chunk[" + pos.getX()/16 + "/" + pos.getY()/16 + "/" + pos.getZ()/16 + "]";
        chunkObject.loc = new Vector3f(pos.getX(), -pos.getZ(), pos.getY());
        return chunkObject;
    }

    private DObject buildChunkLayerObject(DObject chunkObject, RenderChunk renderChunk, EnumWorldBlockLayer layer) {
        VertexBuffer vertexBuffer = renderChunk.getVertexBufferByLayer(layer.ordinal());
        if (vertexBuffer == null) return null;

        DObject layerObject = new DObject(buildChunkLayerMesh(vertexBuffer));
        layerObject.setParent(chunkObject);
        layerObject.id.name = chunkObject.id.name + " - " + layer + " - Frame " + frame;
        return layerObject;
    }

    private DMesh buildChunkLayerMesh(VertexBuffer vertexBuffer) {
        vertexBuffer.bindBuffer();
        int size = GL15.glGetBufferParameteri(OpenGlHelper.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE);
        ByteBuffer byteBuffer = GLAllocation.createDirectByteBuffer(size);
        GL15.glGetBufferSubData(OpenGlHelper.GL_ARRAY_BUFFER, 0, byteBuffer);
        vertexBuffer.unbindBuffer();

        DMesh mesh = new DMesh();
        mesh.materials.add(material);
        DMesh.Vertex v1 = null, v2 = null, v3 = null;
        Vector2f uv1 = new Vector2f(),
                 uv2 = new Vector2f(),
                 uv3 = new Vector2f(),
                 uv4 = new Vector2f();
        int c1 = 0, c2 = 0, c3 = 0;
        for (int i = 0; i < size / STRIDE; i++) {
            // See VboRenderList.setupArrayPointers for offsets
            DMesh.Vertex vert = new DMesh.Vertex(
                    byteBuffer.getFloat(STRIDE * i),
                    -byteBuffer.getFloat(STRIDE * i + 8),
                    byteBuffer.getFloat(STRIDE * i + 4)
            );
            float u = byteBuffer.getFloat(STRIDE * i + 16);
            float v = 1 - byteBuffer.getFloat(STRIDE * i + 20);
            int c = byteBuffer.getInt(STRIDE * i + 12);
            // TODO tex, norm, etc.
            if (v1 == null) {
                v1 = vert;
                uv1.set(u, v);
                c1 = c;
            } else if (v2 == null) {
                v2 = vert;
                uv2.set(u, v);
                c2 = c;
            } else if (v3 == null) {
                v3 = vert;
                uv3.set(u, v);
                c3 = c;
            } else {
                uv4.set(u, v);
                mesh.addQuad(
                        v1, v2, v3, vert,
                        uv1, uv2, uv3, uv4,
                        c1, c2, c3, c,
                        0
                );
                v1 = v2 = v3 = null;
            }
        }

        return mesh;
    }

    private static final int STRIDE = 28;
}
