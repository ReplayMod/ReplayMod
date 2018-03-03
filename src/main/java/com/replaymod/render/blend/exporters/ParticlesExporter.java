package com.replaymod.render.blend.exporters;

import com.replaymod.render.blend.BlendMeshBuilder;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

//#if MC>=10904
import net.minecraft.client.particle.Particle;
//#else
//$$ import net.minecraft.client.particle.EntityFX;
//#endif

//#if MC>=10809
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
//#endif

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.render.blend.Util.getGlModelViewMatrix;

public class ParticlesExporter implements Exporter {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final RenderState renderState;
    private DObject pointAtObject;
    private DObject particlesObject;
    private DObject litParticlesObject;
    //#if MC>=10904
    private Map<Particle, DObject> particleObjects;
    private Map<Particle, DObject> particleObjectsSeen;
    //#else
    //$$ private Map<Entity, DObject> particleObjects;
    //$$ private Map<Entity, DObject> particleObjectsSeen;
    //#endif

    public ParticlesExporter(RenderState renderState) {
        this.renderState = renderState;
    }

    @Override
    public void setup() throws IOException {
        if (false) return; // FIXME make configurable (also other methods below)
        // FIXME replace with camera
        pointAtObject = new DObject(DObject.Type.OB_EMPTY);
        pointAtObject.id.name = "Particles Target";
        pointAtObject.layers = 1 << 2;
        BlendState.getState().getScene().base.add(pointAtObject);

        particlesObject = new DObject(DObject.Type.OB_EMPTY);
        particlesObject.id.name = "Particles";
        particlesObject.layers = 1 << 1;
        BlendState.getState().getScene().base.add(particlesObject);

        litParticlesObject = new DObject(DObject.Type.OB_EMPTY);
        litParticlesObject.id.name = "Lit Particles";
        litParticlesObject.layers = 1 << 1;
        BlendState.getState().getScene().base.add(litParticlesObject);

        particleObjects = new IdentityHashMap<>();
        particleObjectsSeen = new IdentityHashMap<>();
    }

    public void preParticlesRender(boolean lit) {
        if (false) return;
        Matrix4f modelView = getGlModelViewMatrix();
        // Particles are rendered relative to the viewer location.
        // We however want our Particles object to not move when the viewer does,
        // so we position it at 0/0/0 and instead have the particles themselves move more
        Matrix4f.translate(new Vector3f(
                (float) -mc.getRenderManager().viewerPosX,
                (float) -mc.getRenderManager().viewerPosY,
                (float) -mc.getRenderManager().viewerPosZ
        ), modelView, modelView);
        renderState.push(lit ? litParticlesObject : particlesObject, modelView);
    }

    public void postParticlesRender() {
        if (false) return;
        renderState.pop();
    }

    //#if MC>=10904
    public void onRender(Particle particle, float renderPartialTicks) {
    //#else
    //$$ public void onRender(EntityFX particle, float renderPartialTicks) {
    //#endif
        if (false) return;
        DObject particleObject = particleObjects.get(particle);
        if (particleObject == null) {
            particleObject = new DObject(DObject.Type.OB_EMPTY);
            particleObject.setParent(renderState.peekObject());
            particleObject.pointAt = pointAtObject;
            particleObject.layers = 1 << 2;
            particleObject.id.name = particle.getClass().getSimpleName();
            particleObject.keyframe("hide", 0, renderState.getFrame() - 1, 1f);
            particleObject.keyframe("hide", 0, renderState.getFrame(), 0f);
            particleObjects.put(particle, particleObject);
        }
        particleObjectsSeen.put(particle, particleObject);
        renderState.pushObject(particleObject);

        // Normally the particle renderer will translate to dx/dy/dz but we want that translation outside of
        // the particle object (on the particle object itself).
        // So, we translate there, then store the model-view-matrix and then translate back to let the
        // renderer translate there again.
        // Instead of actually translating, we just add the translation on the current model-view-matrix.
        Matrix4f modelView = getGlModelViewMatrix();
        double dx = particle.prevPosX + (particle.posX - particle.prevPosX) * renderPartialTicks - interpPosX();
        double dy = particle.prevPosY + (particle.posY - particle.prevPosY) * renderPartialTicks - interpPosY();
        double dz = particle.prevPosZ + (particle.posZ - particle.prevPosZ) * renderPartialTicks - interpPosZ();
        Vector3f offset = new Vector3f((float) dx, (float) dy, (float) dz);
        Matrix4f.translate(offset, modelView, modelView);
        renderState.pushModelView(modelView);
        renderState.applyLastModelViewTransformToObject();

        particleObject.keyframeLoc(renderState.getFrame());
        particleObject.keyframeScale(renderState.getFrame());

        DObject frameObject = new DObject(generateMeshForParticle(particle, offset));
        frameObject.setParent(particleObject);
        frameObject.layers = 1 << 2;
        frameObject.id.name = particleObject.id.name + " - Frame " + renderState.getFrame();
        frameObject.keyframe("hide", 0, renderState.getFrame() - 1, 1f);
        frameObject.keyframe("hide", 0, renderState.getFrame(), 0f);
        frameObject.keyframe("hide", 0, renderState.getFrame() + 1, 1f);

        renderState.pop();
    }

    //#if MC>=10904
    private DMesh generateMeshForParticle(Particle particle, Vector3f offset) {
    //#else
    //$$ private DMesh generateMeshForParticle(EntityFX particle, Vector3f offset) {
    //#endif
        DMesh mesh = new DMesh();
        BlendMeshBuilder builder = new BlendMeshBuilder(mesh);
        builder.setOffset(offset);
        builder.setWellBehaved(true);
        //#if MC>=10809
        builder.begin(7, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
        //#else
        //$$ builder.startDrawingQuads();
        //#endif
        //#if MC>=10904
        particle.renderParticle(builder, Minecraft.getMinecraft().getRenderViewEntity(), 0, 1, 1, 0, 0, 0);
        //#else
        //$$ particle.func_180434_a(builder, Minecraft.getMinecraft().getRenderViewEntity(), 0, 1, 1, 0, 0, 0);
        //#endif
        builder.finishDrawing();
        return mesh;
    }

    @Override
    public void postFrame(int frame) throws IOException {
        if (false) return;
        particleObjects.entrySet().stream()
                .filter(entry -> !particleObjectsSeen.containsKey(entry.getKey()))
                .forEach(entry -> entry.getValue().keyframe("hide", 0, renderState.getFrame(), 1f));
        particleObjects = particleObjectsSeen;
        particleObjectsSeen = new IdentityHashMap<>();
    }
}
