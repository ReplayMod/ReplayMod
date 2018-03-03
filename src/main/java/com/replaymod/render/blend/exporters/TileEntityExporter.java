package com.replaymod.render.blend.exporters;

import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DObject;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.replaymod.render.blend.Util.getGlModelViewMatrix;
import static com.replaymod.render.blend.Util.getTileEntityId;

public class TileEntityExporter implements Exporter {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RenderState renderState;
    private DObject tileEntitiesObject;
    private Map<TileEntity, DObject> tileEntityObjects;
    private Map<TileEntity, DObject> tileEntityObjectsSeen;

    public TileEntityExporter(RenderState renderState) {
        this.renderState = renderState;
    }

    @Override
    @SneakyThrows
    public void setup() throws IOException {
        tileEntitiesObject = new DObject(DObject.Type.OB_EMPTY);
        tileEntitiesObject.id.name = "TileEntities";
        tileEntitiesObject.layers = 1 << 1;
        BlendState.getState().getScene().base.add(tileEntitiesObject);

        tileEntityObjects = new IdentityHashMap<>();
        tileEntityObjectsSeen = new IdentityHashMap<>();
    }

    public void preTileEntitiesRender() {
        Matrix4f modelView = getGlModelViewMatrix();
        // Tile entities are rendered relative to the viewer location.
        // We however want our TileEntities object to not move when the viewer does,
        // so we position it at 0/0/0 and instead have the tile entities themselves move more
        Matrix4f.translate(new Vector3f(
                (float) -mc.getRenderManager().viewerPosX,
                (float) -mc.getRenderManager().viewerPosY,
                (float) -mc.getRenderManager().viewerPosZ
        ), modelView, modelView);
        renderState.push(tileEntitiesObject, modelView);
    }

    public void postTileEntitiesRender() {
        renderState.pop();
    }

    public void preRender(TileEntity tileEntity, double dx, double dy, double dz, float renderPartialTicks, int destroyStage) {
        DObject tileEntityObject = tileEntityObjects.get(tileEntity);
        if (tileEntityObject == null) {
            tileEntityObject = new DObject(DObject.Type.OB_EMPTY);
            tileEntityObject.setParent(renderState.peekObject());
            BlockPos pos = tileEntity.getPos();
            tileEntityObject.id.name = getTileEntityId(tileEntity) + "( " + pos.getX() + "/" + pos.getY() + "/" + pos.getZ() + ")";
            tileEntityObject.keyframe("hide", 0, renderState.getFrame() - 1, 1f);
            tileEntityObject.keyframe("hide", 0, renderState.getFrame(), 0f);
            tileEntityObjects.put(tileEntity, tileEntityObject);
        }
        tileEntityObjectsSeen.put(tileEntity, tileEntityObject);
        renderState.pushObject(tileEntityObject);

        // Normally the tile entity renderer will translate to dx/dy/dz but we want that translation outside of
        // the tile entity object (on the tile entity object itself).
        // So, we translate there, then store the model-view-matrix and then translate back to let the
        // renderer translate there again.
        // Instead of actually translating, we just add the translation on the current model-view-matrix.
        Matrix4f modelView = getGlModelViewMatrix();
        Matrix4f.translate(new Vector3f((float) dx, (float) dy, (float) dz), modelView, modelView);
        renderState.pushModelView(modelView);
        renderState.applyLastModelViewTransformToObject();

        tileEntityObject.keyframeLocRotScale(renderState.getFrame());
    }

    public void postRender() {
        renderState.pop();
    }

    @Override
    public void postFrame(int frame) throws IOException {
        for (Map.Entry<TileEntity, DObject> entry : tileEntityObjects.entrySet()) {
            if (!tileEntityObjectsSeen.containsKey(entry.getKey())) {
                DObject object = entry.getValue();
                object.keyframe("hide", 0, renderState.getFrame(), 1f);
            }
        }
        tileEntityObjects = tileEntityObjectsSeen;
        tileEntityObjectsSeen = new IdentityHashMap<>();
    }
}
