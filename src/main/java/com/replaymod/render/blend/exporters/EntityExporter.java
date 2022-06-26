//#if MC>=10800 && MC<11900
package com.replaymod.render.blend.exporters;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DObject;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Matrix4f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.IdentityHashMap;
import java.util.Map;

import static com.replaymod.render.blend.Util.getCameraPos;
import static com.replaymod.render.blend.Util.getGlModelViewMatrix;

public class EntityExporter implements Exporter {
    private final MinecraftClient mc = MCVer.getMinecraft();
    private final RenderState renderState;
    private DObject entitiesObject;
    private Map<Entity, DObject> entityObjects;

    public EntityExporter(RenderState renderState) {
        this.renderState = renderState;
    }

    @Override
    public void setup() {
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
        Matrix4f.translate(getCameraPos(), modelView, modelView);
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
            //#if MC>=11400
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
        if (!(entity instanceof LivingEntity)) { // except for EntityLivingBase which get special (better) treatment
            Matrix4f modelView = getGlModelViewMatrix();
            Matrix4f.translate(new Vector3f((float) dx, (float) dy, (float) dz), modelView, modelView);
            renderState.pushModelView(modelView);
            renderState.applyLastModelViewTransformToObject();

            entityObject.keyframeLocRotScale(renderState.getFrame());
        }
        entityObject.setVisible(renderState.getFrame());
    }

    public void postEntityLivingSetup() {
        // For any entity which extends EntityLivingBase, we capture its rotation and scale after it has been setup
        // in addition to its position (which we capture in preRender for other entities).
        // We could add custom hooks for various other entities types as well but it's probably not worth it.
        renderState.pushModelView();
        renderState.applyLastModelViewTransformToObject();
        renderState.peekObject().keyframeLocRotScale(renderState.getFrame());
    }

    public void postRender(Entity entity, double dx, double dy, double dz, float yaw, float renderPartialTicks) {
        renderState.pop();
    }
}
//#endif
