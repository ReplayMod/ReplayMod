//#if MC>=10800
package com.replaymod.render.blend.exporters;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.blend.BlendMeshBuilder;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import com.replaymod.replaystudio.us.myles.ViaVersion.util.ReflectionUtil;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

//#if MC>=11300
import net.minecraft.client.renderer.WorldRenderer.ContainerLocalRenderInformation;
//#else
//$$ import net.minecraft.client.renderer.RenderGlobal.ContainerLocalRenderInformation;
//#endif

//#if MC>=10904
import net.minecraft.util.BlockRenderLayer;
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

import static com.replaymod.core.versions.MCVer.*;

public class ChunkExporter implements Exporter {
    private final Map<BlockPos, DObject> chunkObjects = new HashMap<>();
    //#if MC>=10904
    private final Map<BlockPos, Map<BlockRenderLayer, DObject>> chunkLayerObjects = new HashMap<>();
    private final List<Pair<RenderChunk, BlockRenderLayer>> chunks = new ArrayList<>();
    //#else
    //$$ private final Map<BlockPos, Map<EnumWorldBlockLayer, DObject>> chunkLayerObjects = new HashMap<>();
    //$$ private final List<Pair<RenderChunk, EnumWorldBlockLayer>> chunks = new ArrayList<>();
    //#endif
    private DObject chunksObject;
    private int frame;

    public void addChunkUpdate(RenderChunk chunk, CompiledChunk compiledChunk) {
        //#if MC>=10904
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
        //#else
        //$$ for (EnumWorldBlockLayer layer : EnumWorldBlockLayer.values()) {
        //#endif
            if (compiledChunk == null || compiledChunk.isLayerStarted(layer)) {
                chunks.add(Pair.of(chunk, layer));
            }
        }
    }

    @Override
    public void setup() throws IOException {

        Minecraft mc = MCVer.getMinecraft();
        @SuppressWarnings("unchecked")
        List<ContainerLocalRenderInformation> renderInfos = mc.renderGlobal.renderInfos;
        for (ContainerLocalRenderInformation renderInfo : renderInfos) {
            RenderChunk renderChunk = ReflectionUtil.get(renderInfo, "renderChunk", RenderChunk.class); // SneakyThrows
            CompiledChunk compiledChunk = renderChunk.getCompiledChunk();
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
        for (Pair<RenderChunk, BlockRenderLayer> pair : chunks) {
            BlockRenderLayer layer = pair.getRight();
        //#else
        //$$ for (Pair<RenderChunk, EnumWorldBlockLayer> pair : chunks) {
        //$$     EnumWorldBlockLayer layer = pair.getRight();
        //#endif
            RenderChunk chunk = pair.getLeft();
            DObject chunkObject = chunkObjects.get(chunk.getPosition());
            if (chunkObject == null) {
                chunkObject = buildChunkObject(chunk);
                chunkObjects.put(chunk.getPosition(), chunkObject);
                chunkLayerObjects.put(chunk.getPosition(), new EnumMap<>(layer.getDeclaringClass()));
            }
            DObject layerObject = buildChunkLayerObject(chunkObject, chunk, layer);
            if (layerObject == null) continue;
            //#if MC>=10904
            Map<BlockRenderLayer, DObject> layerObjects
            //#else
            //$$ Map<EnumWorldBlockLayer, DObject> layerObjects
            //#endif
                    = chunkLayerObjects.get(chunk.getPosition());
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

    private DObject buildChunkLayerObject(DObject chunkObject, RenderChunk renderChunk,
                                          //#if MC>=10904
                                          BlockRenderLayer layer) {
                                          //#else
                                          //$$ EnumWorldBlockLayer layer) {
                                          //#endif
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

        MCVer.getMinecraft().getTextureManager().bindTexture(LOCATION_BLOCKS_TEXTURE);

        return BlendMeshBuilder.addBufferToMesh(byteBuffer, GL11.GL_QUADS, DefaultVertexFormats.BLOCK, null, null);
    }

    private static final int STRIDE = 28;
}
//#endif
