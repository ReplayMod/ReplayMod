package com.replaymod.render;

import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.camera.CameraEntity;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Matrix;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Quaternion;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector4f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.util.math.Matrix4f;
import org.apache.commons.io.FilenameUtils;

//#if MC>=11400
import net.minecraft.util.math.Vec3d;
//#else
//#endif

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static net.minecraft.util.math.MathHelper.clamp;

public class AfterEffectsCameraPathExporter {

    private final MinecraftClient mc = MCVer.getMinecraft();
    private final RenderSettings settings;
    private int framesDone;
    private final StringBuilder cameraTranslation = new StringBuilder();
    private final StringBuilder cameraRotation = new StringBuilder();
    private final StringBuilder times = new StringBuilder();

    private float aspect;

    private int totalFrames;

    public AfterEffectsCameraPathExporter(RenderSettings settings) {
        this.settings = settings;

    }

    public void setup(int totalFrames) {
        aspect = (float)settings.getTargetVideoWidth()/(float)settings.getTargetVideoHeight();

        this.totalFrames = totalFrames;
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
        float yaw = camera.getYaw();
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
        //$$ float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * tickDelta;
        //$$ float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * tickDelta;
        //#endif
        float roll = entity instanceof CameraEntity ? ((CameraEntity) entity).roll : 0;

        x = (float) (-x * 100.0 / aspect + settings.getTargetVideoWidth() / 2.0);
        y = (float) (-y * 100.0 + settings.getTargetVideoHeight() / 2.0);
        z = (float) (z * 100.0);
        pitch = -(float) pitch;
        yaw = -(float) yaw;
        roll = (float) -roll;


        times.append((float)framesDone/(float)settings.getFramesPerSecond()).append(',');
        cameraTranslation.append('[').append(x).append(',').append(y).append(',').append(z).append("],");
        cameraRotation.append('[').append(pitch).append(',').append(yaw).append(',').append(roll).append("],");

        framesDone++;
    }

    public void finish() throws IOException {
        
        float zoom = (float) (settings.getTargetVideoHeight()/(Math.tan(mc.options.fov/2)*2));

        String combinedData = "#target AfterEffects\n" +
                "\n" +
                "function createCameraFromReplayMod(){\n" +
                "\n" +
                "var compName = prompt(\"Enter name for a new composition that will include your camera\",\"\",\"Composition's Name\");\n" +
                "if (compName){\n" +
                "var newComp = app.project.items.addComp(compName, " + settings.getTargetVideoWidth() + ", " + + settings.getTargetVideoHeight() + ", 1.0, " + totalFrames/settings.getFramesPerSecond() + ", " + settings.getFramesPerSecond()+ ");\n" +
                "\n" +
                "var Camera = newComp.layers.addCamera(\"Camera\",[0,0]);\n" +
                "Camera.autoOrient = AutoOrientType.NO_AUTO_ORIENT;\n" +
                "Camera.property(\"position\").setValuesAtTimes([" + times + "],[" + cameraTranslation + "]);\n" +
                "Camera.property(\"orientation\").setValuesAtTimes(["  + times + "],[" + cameraRotation + "]);\n" +
                "Camera.property(\"zoom\").setValue(" + zoom + ");\n" +
                "\n" +
                "\n" +
                "\n" +
                "}else{alert (\"No Comp name has been chosen\",\"EXIT\")};}\n" +
                "\n" +
                "\n" +
                "app.beginUndoGroup(\"Import Replay Mod Camera\");\n" +
                "createCameraFromReplayMod();\n" +
                "app.endUndoGroup();";

        Path videoPath = settings.getOutputFile().toPath();
        Path txtBasePath = Files.isDirectory(videoPath)
                ? videoPath.resolve("AfterEffectsCamera.jsx")
                : videoPath.resolveSibling(FilenameUtils.getBaseName(videoPath.getFileName().toString()) + ".jsx");
        Path txtPath = txtBasePath;
        for (int i = 0; Files.exists(txtPath); i++) {
            String baseName = FilenameUtils.getBaseName(txtBasePath.getFileName().toString());
            txtPath = txtBasePath.resolveSibling(baseName + "." + i + ".jsx");
        }

        File file = txtPath.toFile();

        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        byte[] bytes = combinedData.getBytes();
        bos.write(bytes);
        bos.close();
        fos.close();


    }
}
