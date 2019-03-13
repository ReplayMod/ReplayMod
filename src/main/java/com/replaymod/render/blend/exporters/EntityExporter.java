//#if MC>=10800
package com.replaymod.render.blend.exporters;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DObject;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Matrix4f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.replaymod.render.blend.Util.getGlModelViewMatrix;

public class EntityExporter implements Exporter {
    private final Minecraft mc = MCVer.getMinecraft();
    private final RenderState renderState;
    private DObject entitiesObject;
    private Map<Entity, DObject> entityObjects;

    public EntityExporter(RenderState renderState) {
        this.renderState = renderState;
    }

    @Override
    @SneakyThrows
    public void setup() throws IOException {
        entitiesObject = new DObject(DObject.Type.OB_EMPTY);
        entitiesObject.id.name = "Entities";
        entitiesObject.layers = 1 << 1;
        BlendState.getState().getScene().base.add(entitiesObject);

        entityObjects = new IdentityHashMap<>();
    }

    public void preEntitiesRender() {
        Matrix4f modelView = getGlModelViewMatrix();
        // Entities are rendered relative to the viewer location.
        // We however want our Entities object to not move when the viewer does,
        // so we position it at 0/0/0 and instead have the entities themselves move more
        Matrix4f.translate(new Vector3f(
                (float) -mc.getRenderManager().viewerPosX,
                (float) -mc.getRenderManager().viewerPosY,
                (float) -mc.getRenderManager().viewerPosZ
        ), modelView, modelView);
        renderState.push(entitiesObject, modelView);
    }

    public void postEntitiesRender() {
        renderState.pop();
    }

    public void preRender(Entity entity, double dx, double dy, double dz, float yaw, float renderPartialTicks) {
        DObject entityObject = entityObjects.get(entity);
        if (entityObject == null) {
            entityObject = new DObject(DObject.Type.OB_EMPTY);
            entityObject.setParent(renderState.peekObject());
            //#if MC>=11300
            entityObject.id.name = entity.getName().getString();
            //#else
            //$$ entityObject.id.name = entity.getName();
            //#endif
            entityObjects.put(entity, entityObject);
        }
        renderState.pushObject(entityObject);

        // Normally the entity renderer will translate to dx/dy/dz but we want that translation outside of
        // the entity object (on the entity object itself).
        // So, we translate there, then store the model-view-matrix and then translate back to let the
        // renderer translate there again.
        // Instead of actually translating, we just add the translation on the current model-view-matrix.
        Matrix4f modelView = getGlModelViewMatrix();
        Matrix4f.translate(new Vector3f((float) dx, (float) dy, (float) dz), modelView, modelView);
        renderState.pushModelView(modelView);
        renderState.applyLastModelViewTransformToObject();

        entityObject.keyframeLocRotScale(renderState.getFrame());
        entityObject.setVisible(renderState.getFrame());
    }

    public void postRender(Entity entity, double dx, double dy, double dz, float yaw, float renderPartialTicks) {
        renderState.pop();
    }
}
//#endif
