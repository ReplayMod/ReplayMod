package com.replaymod.render;

import com.replaymod.core.ReplayMod;
import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.camera.CameraEntity;
import de.javagl.jgltf.impl.v2.Accessor;
import de.javagl.jgltf.impl.v2.Animation;
import de.javagl.jgltf.impl.v2.AnimationChannel;
import de.javagl.jgltf.impl.v2.AnimationChannelTarget;
import de.javagl.jgltf.impl.v2.AnimationSampler;
import de.javagl.jgltf.impl.v2.Asset;
import de.javagl.jgltf.impl.v2.Buffer;
import de.javagl.jgltf.impl.v2.BufferView;
import de.javagl.jgltf.impl.v2.Camera;
import de.javagl.jgltf.impl.v2.CameraPerspective;
import de.javagl.jgltf.impl.v2.GlTF;
import de.javagl.jgltf.impl.v2.Node;
import de.javagl.jgltf.model.io.v2.GltfAssetV2;
import de.javagl.jgltf.model.io.v2.GltfAssetWriterV2;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Quaternion;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector4f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.opengl.GL11;

//#if MC>=11400
import net.minecraft.util.math.Vec3d;
//#else
//#endif

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

import static com.replaymod.core.utils.Utils.configure;

public class CameraPathExporter {

    private final MinecraftClient mc = MCVer.getMinecraft();
    private final RenderSettings settings;
    private int framesDone;
    private ByteBuffer timeBuffer;
    private ByteBuffer cameraTranslationBuffer;
    private ByteBuffer cameraRotationBuffer;

    public CameraPathExporter(RenderSettings settings) {
        this.settings = settings;
    }

    public void setup(int totalFrames) {
        timeBuffer = ByteBuffer.allocate(4 * totalFrames).order(ByteOrder.LITTLE_ENDIAN);
        cameraTranslationBuffer = ByteBuffer.allocate(4 * totalFrames * 3).order(ByteOrder.LITTLE_ENDIAN);
        cameraRotationBuffer = ByteBuffer.allocate(4 * totalFrames * 4).order(ByteOrder.LITTLE_ENDIAN);
    }

    public void recordFrame(float tickDelta) {
        //#if MC>=10800
        Entity entity = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
        //#else
        //$$ Entity entity = mc.renderViewEntity == null ? mc.thePlayer : mc.renderViewEntity;
        //#endif

        //#if MC>=11400
        net.minecraft.client.render.Camera camera = mc.gameRenderer.getCamera();
        Vec3d vec = camera.getPos();
        float x = (float) vec.getX();
        float y = (float) vec.getY();
        float z = (float) vec.getZ();
        float yaw = camera.getYaw() + 180;
        float pitch = camera.getPitch();
        //#else
        //#if MC>=10800
        //$$ float eyeHeight = entity.getEyeHeight();
        //#else
        //$$ float eyeHeight = 1.62f - entity.yOffset;
        //#endif
        //$$ float x = (float) (entity.prevPosX + (entity.posX - entity.prevPosX) * tickDelta);
        //$$ float y = (float) (entity.prevPosY + (entity.posY - entity.prevPosY) * tickDelta + eyeHeight);
        //$$ float z = (float) (entity.prevPosZ + (entity.posZ - entity.prevPosZ) * tickDelta);
        //$$ float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * tickDelta + 180;
        //$$ float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * tickDelta;
        //#endif
        float roll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;

        Quaternion quatYaw = new Quaternion();
        Quaternion quatPitch = new Quaternion();
        Quaternion quatRoll = new Quaternion();

        quatYaw.setFromAxisAngle(new Vector4f(0, -1, 0, (float) Math.toRadians(yaw)));
        quatPitch.setFromAxisAngle(new Vector4f(-1, 0, 0, (float) Math.toRadians(pitch)));
        quatRoll.setFromAxisAngle(new Vector4f(0, 0, -1, (float) Math.toRadians(roll)));

        Quaternion quaternion = new Quaternion(0, 0, 0, 1);
        Quaternion.mul(quaternion, quatYaw, quaternion);
        Quaternion.mul(quaternion, quatPitch, quaternion);
        Quaternion.mul(quaternion, quatRoll, quaternion);
        quaternion.normalise(quaternion);

        float[] translation = new float[] { x, y, z };
        float[] rotation = new float[] { quaternion.getX(), quaternion.getY(), quaternion.getZ(), quaternion.getW() };

        timeBuffer.putFloat(framesDone / (float) settings.getFramesPerSecond());
        for (float f : translation) {
            cameraTranslationBuffer.putFloat(f);
        }
        for (float f : rotation) {
            cameraRotationBuffer.putFloat(f);
        }

        framesDone++;
    }

    public void finish() throws IOException {
        int timeBufferSize = timeBuffer.rewind().remaining();
        int cameraTranslationBufferSize = cameraTranslationBuffer.rewind().remaining();
        int cameraRotationBufferSize = cameraRotationBuffer.rewind().remaining();

        int binaryDataSize = 0;
        binaryDataSize += timeBufferSize;
        binaryDataSize += cameraTranslationBufferSize;
        binaryDataSize += cameraRotationBufferSize;

        ByteBuffer binaryData = ByteBuffer.allocate(binaryDataSize);
        int timeBufferOffset = binaryData.position();
        binaryData.put(timeBuffer);
        int cameraTranslationBufferOffset = binaryData.position();
        binaryData.put(cameraTranslationBuffer);
        int cameraRotationBufferOffset = binaryData.position();
        binaryData.put(cameraRotationBuffer);
        binaryData.rewind();

        GlTF glTF = new GlTF();
        glTF.setAsset(configure(new Asset(), asset -> {
            asset.setVersion("2.0");
            asset.setGenerator("ReplayMod v" + ReplayMod.instance.getVersion());
        }));
        glTF.addAnimations(configure(new Animation(), animation -> {
            animation.addChannels(configure(new AnimationChannel(), channel -> {
                channel.setTarget(configure(new AnimationChannelTarget(), target -> {
                    target.setNode(0);
                    target.setPath("translation");
                }));
                channel.setSampler(0);
            }));
            animation.addChannels(configure(new AnimationChannel(), channel -> {
                channel.setTarget(configure(new AnimationChannelTarget(), target -> {
                    target.setNode(0);
                    target.setPath("rotation");
                }));
                channel.setSampler(1);
            }));
            animation.addSamplers(configure(new AnimationSampler(), sampler -> {
                sampler.setInput(0);
                sampler.setOutput(1);
            }));
            animation.addSamplers(configure(new AnimationSampler(), sampler -> {
                sampler.setInput(0);
                sampler.setOutput(2);
            }));
        }));
        glTF.addCameras(configure(new Camera(), camera -> {
            camera.setType("perspective");
            camera.setPerspective(configure(new CameraPerspective(), perspective -> {
                float aspectRatio = (float) settings.getVideoWidth() / (float) settings.getVideoHeight();
                perspective.setAspectRatio(aspectRatio);
                perspective.setYfov((float) Math.toRadians(mc.options.fov));
                perspective.setZnear(0.05f);
                perspective.setZfar((float) mc.options.viewDistance * 16 * 4);
            }));
        }));
        glTF.addNodes(configure(new Node(), node -> node.setCamera(0)));
        glTF.addBuffers(configure(new Buffer(), buffer -> buffer.setByteLength(binaryData.limit())));
        // Time
        glTF.addBufferViews(configure(new BufferView(), bufferView -> {
            bufferView.setBuffer(0);
            bufferView.setByteOffset(timeBufferOffset);
            bufferView.setByteLength(timeBufferSize);
        }));
        glTF.addAccessors(configure(new Accessor(), accessor -> {
            accessor.setBufferView(0);
            accessor.setType("SCALAR");
            accessor.setComponentType(GL11.GL_FLOAT);
            accessor.setCount(framesDone);
        }));
        // Camera translation
        glTF.addBufferViews(configure(new BufferView(), bufferView -> {
            bufferView.setBuffer(0);
            bufferView.setByteOffset(cameraTranslationBufferOffset);
            bufferView.setByteLength(cameraTranslationBufferSize);
        }));
        glTF.addAccessors(configure(new Accessor(), accessor -> {
            accessor.setBufferView(1);
            accessor.setType("VEC3");
            accessor.setComponentType(GL11.GL_FLOAT);
            accessor.setCount(framesDone);
        }));
        // Camera rotation
        glTF.addBufferViews(configure(new BufferView(), bufferView -> {
            bufferView.setBuffer(0);
            bufferView.setByteOffset(cameraRotationBufferOffset);
            bufferView.setByteLength(cameraRotationBufferSize);
        }));
        glTF.addAccessors(configure(new Accessor(), accessor -> {
            accessor.setBufferView(2);
            accessor.setType("VEC4");
            accessor.setComponentType(GL11.GL_FLOAT);
            accessor.setCount(framesDone);
        }));

        java.nio.file.Path videoPath = settings.getOutputFile().toPath();
        java.nio.file.Path glbBasePath = Files.isDirectory(videoPath)
                ? videoPath.resolve("camera.glb")
                : videoPath.resolveSibling(FilenameUtils.getBaseName(videoPath.getFileName().toString()) + ".glb");
        java.nio.file.Path glbPath = glbBasePath;
        for (int i = 0; Files.exists(glbPath); i++) {
            String baseName = FilenameUtils.getBaseName(glbBasePath.getFileName().toString());
            glbPath = glbBasePath.resolveSibling(baseName + "." + i + ".glb");
        }
        try (OutputStream out = Files.newOutputStream(glbPath)) {
            GltfAssetV2 asset = new GltfAssetV2(glTF, binaryData);
            new GltfAssetWriterV2().writeBinary(asset, out);
        }
    }
}
