// FIXME 1.15
//#if MC>=10800 && MC<11500
package com.replaymod.render.blend.exporters;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.blend.BlendMeshBuilder;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import com.replaymod.render.blend.mixin.ContainerLocalRenderInformationAccessor;
import com.replaymod.render.blend.mixin.WorldRendererAccessor;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.GlAllocationUtils;
import com.mojang.blaze3d.platform.GLX;
import net.minecraft.client.render.chunk.ChunkRenderData;
import net.minecraft.client.render.chunk.ChunkRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.GlBuffer;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

//#if MC>=10904
import net.minecraft.block.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
//#else
//$$ import net.minecraft.util.BlockPos;
//$$ import net.minecraft.util.EnumWorldBlockLayer;
//#endif

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkExporter implements Exporter {
    private final Map<BlockPos, DObject> chunkObjects = new HashMap<>();
    //#if MC>=10904
    private final Map<BlockPos, Map<BlockRenderLayer, DObject>> chunkLayerObjects = new HashMap<>();
    private final List<Pair<ChunkRenderer, BlockRenderLayer>> chunks = new ArrayList<>();
    //#else
    //$$ private final Map<BlockPos, Map<EnumWorldBlockLayer, DObject>> chunkLayerObjects = new HashMap<>();
    //$$ private final List<Pair<RenderChunk, EnumWorldBlockLayer>> chunks = new ArrayList<>();
    //#endif
    private DObject chunksObject;
    private int frame;

    public void addChunkUpdate(ChunkRenderer chunk, ChunkRenderData compiledChunk) {
        //#if MC>=10904
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
        //#else
        //$$ for (EnumWorldBlockLayer layer : EnumWorldBlockLayer.values()) {
        //#endif
            if (compiledChunk == null || compiledChunk.isBufferInitialized(layer)) {
                chunks.add(Pair.of(chunk, layer));
            }
        }
    }

    @Override
    public void setup() throws IOException {

        MinecraftClient mc = MCVer.getMinecraft();
        @SuppressWarnings("unchecked")
        List<ContainerLocalRenderInformationAccessor> renderInfos = ((WorldRendererAccessor) mc.worldRenderer).getRenderInfos();
        for (ContainerLocalRenderInformationAccessor renderInfo : renderInfos) {
            ChunkRenderer renderChunk = renderInfo.getRenderChunk();
            ChunkRenderData compiledChunk = renderChunk.getData();
            if (!compiledChunk.isEmpty()) {
                addChunkUpdate(renderChunk, null);
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
        //#if MC>=10904
        for (Pair<ChunkRenderer, BlockRenderLayer> pair : chunks) {
            BlockRenderLayer layer = pair.getRight();
        //#else
        //$$ for (Pair<RenderChunk, EnumWorldBlockLayer> pair : chunks) {
        //$$     EnumWorldBlockLayer layer = pair.getRight();
        //#endif
            ChunkRenderer chunk = pair.getLeft();
            DObject chunkObject = chunkObjects.get(chunk.getOrigin());
            if (chunkObject == null) {
                chunkObject = buildChunkObject(chunk);
                chunkObjects.put(chunk.getOrigin(), chunkObject);
                chunkLayerObjects.put(chunk.getOrigin(), new EnumMap<>(layer.getDeclaringClass()));
            }
            DObject layerObject = buildChunkLayerObject(chunkObject, chunk, layer);
            if (layerObject == null) continue;
            //#if MC>=10904
            Map<BlockRenderLayer, DObject> layerObjects
            //#else
            //$$ Map<EnumWorldBlockLayer, DObject> layerObjects
            //#endif
                    = chunkLayerObjects.get(chunk.getOrigin());
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

    private DObject buildChunkObject(ChunkRenderer renderChunk) {
        BlockPos pos = renderChunk.getOrigin();
        DObject chunkObject = new DObject(DObject.Type.OB_EMPTY);
        chunkObject.setParent(chunksObject);
        chunkObject.id.name = "Chunk[" + pos.getX()/16 + "/" + pos.getY()/16 + "/" + pos.getZ()/16 + "]";
        chunkObject.loc = new Vector3f(pos.getX(), -pos.getZ(), pos.getY());
        return chunkObject;
    }

    private DObject buildChunkLayerObject(DObject chunkObject, ChunkRenderer renderChunk,
                                          //#if MC>=10904
                                          BlockRenderLayer layer) {
                                          //#else
                                          //$$ EnumWorldBlockLayer layer) {
                                          //#endif
        GlBuffer vertexBuffer = renderChunk.getGlBuffer(layer.ordinal());
        if (vertexBuffer == null) return null;

        DObject layerObject = new DObject(buildChunkLayerMesh(vertexBuffer));
        layerObject.setParent(chunkObject);
        layerObject.id.name = chunkObject.id.name + " - " + layer + " - Frame " + frame;
        return layerObject;
    }

    private DMesh buildChunkLayerMesh(GlBuffer vertexBuffer) {
        vertexBuffer.bind();
        int size = GL15.glGetBufferParameteri(GLX.GL_ARRAY_BUFFER, GL15.GL_BUFFER_SIZE);
        ByteBuffer byteBuffer = GlAllocationUtils.allocateByteBuffer(size);
        GL15.glGetBufferSubData(GLX.GL_ARRAY_BUFFER, 0, byteBuffer);
        vertexBuffer.unbind();

        MCVer.getMinecraft().getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);

        return BlendMeshBuilder.addBufferToMesh(byteBuffer, GL11.GL_QUADS, VertexFormats.POSITION_COLOR_UV_LMAP, null, null);
    }

    private static final int STRIDE = 28;
}
//#endif
