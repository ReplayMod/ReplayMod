//#if MC>=10800
package com.replaymod.render.blend;

import com.replaymod.render.blend.data.DMaterial;
import com.replaymod.render.blend.data.DMesh;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.ReadableVector3f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector2f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.lwjgl.opengl.GL11;

//#if MC>=10904
//#if MC>=11200
import net.minecraft.client.renderer.BufferBuilder;
//#else
//$$ import net.minecraft.client.renderer.VertexBuffer;
//#endif
//#else
//$$ import net.minecraft.client.renderer.WorldRenderer;
//#endif

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.replaymod.core.versions.MCVer.*;

public class BlendMeshBuilder
        //#if MC>=10904
        //#if MC>=11200
        extends BufferBuilder
        //#else
        //$$ extends VertexBuffer
        //#endif
        //#else
        //$$ extends WorldRenderer
        //#endif
{
    private static final ReadableVector3f VEC3F_ZERO = new Vector3f(0, 0, 0);
    private final DMesh mesh;
    private Vector3f offset;
    private boolean isDrawing;
    private boolean wellBehaved;

    public BlendMeshBuilder(DMesh mesh) {
        super(1024);
        this.mesh = mesh;
    }

    public void setWellBehaved(boolean wellBehaved) {
        this.wellBehaved = wellBehaved;
    }

    public void setReverseOffset(Vector3f offset) {
        this.offset = offset;
    }

    @Override
    //#if MC>=10809
    public void begin(int mode, VertexFormat vertexFormat) {
    //#else
    //$$ public void startDrawing(int mode) {
    //#endif
        if (isDrawing) {
            if (!wellBehaved) {
                // Someone probably finished drawing with the global instance instead of this one,
                // let's just assume that what's happened and finish our last draw by ourselves
                // (might miss correct texture though)
                super.finishDrawing();
                addBufferToMesh();
            } else {
                throw new IllegalStateException("Already drawing!");
            }
        }
        this.isDrawing = true;

        if (!wellBehaved) {
            // In case the calling code finishes with Tessellator.getInstance().draw()
            BufferBuilder_beginPosTexCol(mode);
        }

        //#if MC>=10809
        super.begin(mode, vertexFormat);
        //#else
        //$$ super.startDrawing(mode);
        //#endif
    }

    public void maybeFinishDrawing() {
        if (isDrawing) {
            isDrawing = false;
            super.finishDrawing();
            addBufferToMesh();
        }
    }

    @Override
    //#if MC>=10809
    public void finishDrawing() {
    //#else
    //$$ public int finishDrawing() {
    //#endif
        if (!isDrawing) {
            throw new IllegalStateException("Not building!");
        } else {
            if (!wellBehaved) {
                Tessellator.getInstance().getBuffer().finishDrawing();
            }

            //#if MC<10809
            //$$ int ret =
            //#endif
            super.finishDrawing();

            addBufferToMesh();

            //#if MC<10809
            //$$ return ret;
            //#endif
        }
    }

    private void addBufferToMesh() {
        addBufferToMesh(this, mesh, offset);
    }

    //#if MC>=10904
    //#if MC>=11200
    public static DMesh addBufferToMesh(BufferBuilder bufferBuilder, DMesh mesh, ReadableVector3f vertOffset) {
    //#else
    //$$ public static DMesh addBufferToMesh(VertexBuffer bufferBuilder, DMesh mesh, Vector3f vertOffset) {
    //#endif
    //#else
    //$$ public static DMesh addBufferToMesh(WorldRenderer bufferBuilder, DMesh mesh, Vector3f vertOffset) {
    //#endif
        return addBufferToMesh(bufferBuilder.getByteBuffer(), bufferBuilder.getDrawMode(), bufferBuilder.getVertexFormat(), mesh, vertOffset);
    }

    public static DMesh addBufferToMesh(ByteBuffer buffer, int mode, VertexFormat vertexFormat, DMesh mesh, ReadableVector3f vertOffset) {
        //#if MC>=11300
        int vertexCount = buffer.remaining() / vertexFormat.getSize();
        //#else
        //$$ int vertexCount = buffer.remaining() / vertexFormat.getNextOffset();
        //#endif
        return addBufferToMesh(buffer, vertexCount, mode, vertexFormat, mesh, vertOffset);
    }

    public static DMesh addBufferToMesh(ByteBuffer buffer, int vertexCount, int mode, VertexFormat vertexFormat, DMesh mesh, ReadableVector3f vertOffset) {
        if (mesh == null) {
            mesh = new DMesh();
        }
        if (vertOffset == null) {
            vertOffset = VEC3F_ZERO;
        }

        // Determine offset of vertex components
        int posOffset = -1, colorOffset = -1, uvOffset = -1;
        int index = 0;
        for (VertexFormatElement element : getElements(vertexFormat)) {
            //#if MC>=10809
            int offset = vertexFormat.getOffset(index);
            //#else
            //$$ int offset = element.getOffset();
            //#endif
            switch (element.getUsage()) {
                case POSITION:
                    if (element.getType() != VertexFormatElement.EnumType.FLOAT) {
                        throw new UnsupportedOperationException("Only float is supported for position elements!");
                    }
                    posOffset = offset;
                    break;
                case COLOR:
                    if (element.getType() != VertexFormatElement.EnumType.UBYTE) {
                        throw new UnsupportedOperationException("Only unsigned byte is supported for color elements!");
                    }
                    colorOffset = offset;
                    break;
                case UV:
                    if (element.getIndex() != 0) break;
                    if (element.getType() != VertexFormatElement.EnumType.FLOAT) {
                        throw new UnsupportedOperationException("Only float is supported for UV elements!");
                    }
                    uvOffset = offset;
                    break;
            }
            index++;
        }
        if (posOffset == -1) throw new IllegalStateException("No position element in " + vertexFormat);

        // Extract vertex components from byte buffer
        List<DMesh.Vertex> vertices = new ArrayList<>(vertexCount);
        List<Vector2f> uvs = new ArrayList<>(vertexCount);
        List<Integer> colors = new ArrayList<>(vertexCount);
        //#if MC>=11300
        int step = vertexFormat.getSize();
        //#else
        //$$ int step = vertexFormat.getNextOffset();
        //#endif
        for (int offset = 0; offset < vertexCount * step; offset += step) {
            vertices.add(new DMesh.Vertex(
                     buffer.getFloat(offset      ) - vertOffset.getX(),
                    -buffer.getFloat(offset + 8) + vertOffset.getZ(),
                     buffer.getFloat(offset + 4) - vertOffset.getY()
            ));

            if (colorOffset != -1) {
                colors.add(buffer.getInt(offset + colorOffset));
            } else {
                colors.add(0xffffffff);
            }

            if (uvOffset != -1) {
                uvs.add(new Vector2f(
                        buffer.getFloat(offset + uvOffset),
                        1 - buffer.getFloat(offset + uvOffset + 4)
                ));
            } else {
                uvs.add(new Vector2f(0, 0));
            }
        }

        // Determine and store current material
        DMaterial activeMaterial = BlendState.getState().getMaterials().getActiveMaterial();
        int materialSlot = mesh.materials.indexOf(activeMaterial);
        if (materialSlot < 0) {
            materialSlot = mesh.materials.size();
            mesh.materials.add(activeMaterial);
        }

        // Bundle vertices into shapes and add them to the mesh
        switch (mode) {
            case GL11.GL_TRIANGLES:
                for (int i = 0; i < vertices.size(); i+=3) {
                    mesh.addTriangle(
                            vertices.get(i    ),
                            vertices.get(i + 1),
                            vertices.get(i + 2),
                            uvs.get(i    ),
                            uvs.get(i + 1),
                            uvs.get(i + 2),
                            colors.get(i    ),
                            colors.get(i + 1),
                            colors.get(i + 2),
                            materialSlot
                    );
                }
                break;
            case GL11.GL_QUADS:
                for (int i = 0; i < vertices.size(); i+=4) {
                    mesh.addQuad(
                            vertices.get(i    ),
                            vertices.get(i + 1),
                            vertices.get(i + 2),
                            vertices.get(i + 3),
                            uvs.get(i    ),
                            uvs.get(i + 1),
                            uvs.get(i + 2),
                            uvs.get(i + 3),
                            colors.get(i    ),
                            colors.get(i + 1),
                            colors.get(i + 2),
                            colors.get(i + 3),
                            materialSlot
                    );
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported mode: " + mode);
        }

        return mesh;
    }
}
//#endif
