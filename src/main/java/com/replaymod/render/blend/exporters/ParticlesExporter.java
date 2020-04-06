//#if MC>=10800
package com.replaymod.render.blend.exporters;

import com.replaymod.core.versions.MCVer;
import com.replaymod.render.blend.BlendMeshBuilder;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.blend.Exporter;
import com.replaymod.render.blend.data.DMesh;
import com.replaymod.render.blend.data.DObject;
import com.replaymod.render.blend.mixin.ParticleAccessor;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Matrix4f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.MinecraftClient;

//#if MC>=11400
import net.minecraft.util.math.Vec3d;
//#endif

//#if MC>=10904
import net.minecraft.client.particle.Particle;
//#else
//$$ import net.minecraft.client.particle.EntityFX;
//$$ import net.minecraft.entity.Entity;
//#endif

//#if MC>=10809
import net.minecraft.client.render.VertexFormats;
//#endif

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

import static com.replaymod.render.blend.Util.getCameraPos;
import static com.replaymod.render.blend.Util.getGlModelViewMatrix;

public class ParticlesExporter implements Exporter {
    private final MinecraftClient mc = MCVer.getMinecraft();
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
        Matrix4f modelView = getGlModelViewMatrix();
        // Particles are rendered relative to the viewer location.
        // We however want our Particles object to not move when the viewer does,
        // so we position it at 0/0/0 and instead have the particles themselves move more
        Matrix4f.translate(getCameraPos(), modelView, modelView);
        renderState.push(lit ? litParticlesObject : particlesObject, modelView);
    }

    public void postParticlesRender() {
        renderState.pop();
    }

    //#if MC>=10904
    public void onRender(Particle particle, float renderPartialTicks) {
    //#else
    //$$ public void onRender(EntityFX particle, float renderPartialTicks) {
    //#endif
        DObject particleObject = particleObjects.get(particle);
        if (particleObject == null) {
            particleObject = new DObject(DObject.Type.OB_EMPTY); // mesh generation is delayed, see below
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
        ParticleAccessor acc = (ParticleAccessor) particle;
        double dx = acc.getPrevPosX() + (acc.getPosX() - acc.getPrevPosX()) * renderPartialTicks;
        double dy = acc.getPrevPosY() + (acc.getPosY() - acc.getPrevPosY()) * renderPartialTicks;
        double dz = acc.getPrevPosZ() + (acc.getPosZ() - acc.getPrevPosZ()) * renderPartialTicks;
        //#if MC>=11500
        // FIXME 1.15 is this still required?
        //#else
        //$$ dx -= Particle.cameraX;
        //$$ dy -= Particle.cameraY;
        //$$ dz -= Particle.cameraZ;
        //#endif
        Vector3f offset = new Vector3f((float) dx, (float) dy, (float) dz);
        Matrix4f.translate(offset, modelView, modelView);
        renderState.pushModelView(modelView);
        renderState.applyLastModelViewTransformToObject();

        particleObject.keyframeLoc(renderState.getFrame());

        DMesh mesh = generateMeshForParticle(particle, offset);
        if (mesh.vertices.isEmpty()) {
            renderState.pop();
            return; // no particle to be found?
        }
        if (particleObject.mesh == null) {
            // Some particles start out as single points, so we can never scale those first meshes to other sizes
            if (mesh.hasZeroLengthEdge(0.1f)) {
                // therefore we must further delay mesh generation in these cases and assume a current model scale of 0
                particleObject.scale.set(0, 0, 0);
            } else {
                // until we find one proper mesh which we can then use as the "unit" mesh
                particleObject.type = DObject.Type.OB_MESH;
                particleObject.mesh = mesh;
                particleObject.scale.set(1, 1, 1);
            }
        } else {
            // Determine relative scale of current mesh to stored "unit" mesh
            // Note: currently assumes the same scale on all relevant axes (should be true for most particles?).
            float unitSize = particleObject.mesh.getSizeX();
            float currentSize = mesh.getSizeX();
            float relativeScale = currentSize / unitSize;
            particleObject.scale.set(relativeScale, relativeScale, relativeScale);
        }
        particleObject.keyframeScale(renderState.getFrame());

        renderState.pop();
    }

    //#if MC>=10904
    private DMesh generateMeshForParticle(Particle particle, Vector3f offset) {
    //#else
    //$$ private DMesh generateMeshForParticle(EntityFX particle, Vector3f offset) {
    //#endif
        DMesh mesh = new DMesh();
        BlendMeshBuilder builder = new BlendMeshBuilder(mesh);
        builder.setReverseOffset(offset);
        builder.setWellBehaved(true);
        //#if MC>=10809
        builder.begin(7, VertexFormats.POSITION_TEXTURE_COLOR_LIGHT);
        //#else
        //$$ builder.startDrawingQuads();
        //#endif
        //#if MC>=10809
        particle.buildGeometry(builder,
                //#if MC>=11400
                MCVer.getMinecraft().gameRenderer.getCamera(),
                //#else
                //$$ MCVer.getMinecraft().getRenderViewEntity(),
                //#endif
                0
                //#if MC<11500
                //$$ , 1, 1, 0, 0, 0
                //#endif
        );
        //#else
        //$$ particle.func_180434_a(builder, Minecraft.getMinecraft().getRenderViewEntity(), 0, 1, 1, 0, 0, 0);
        //#endif
        builder.end();
        return mesh;
    }

    @Override
    public void postFrame(int frame) throws IOException {
        particleObjects.entrySet().stream()
                .filter(entry -> !particleObjectsSeen.containsKey(entry.getKey()))
                .forEach(entry -> entry.getValue().keyframe("hide", 0, renderState.getFrame(), 1f));
        particleObjects = particleObjectsSeen;
        particleObjectsSeen = new IdentityHashMap<>();
    }
}
//#endif
