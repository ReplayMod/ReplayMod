package com.replaymod.render;

import com.replaymod.core.versions.MCVer;
import com.replaymod.replay.camera.CameraEntity;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Matrix4f;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Quaternion;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
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

public class AfterEffectsCameraPathExporter {

    private final MinecraftClient mc = MCVer.getMinecraft();
    private final RenderSettings settings;
    private int framesDone;
    private final StringBuilder cameraTranslation = new StringBuilder();
    private final StringBuilder cameraRotation = new StringBuilder();
    private final StringBuilder cameraZoom = new StringBuilder();
    private final StringBuilder times = new StringBuilder();

    private float aspect;

    private int totalFrames;
    private float xOffset = 0;
    private float yOffset = 0;
    private float zOffset = 0;


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

        float zoom = (float) (settings.getTargetVideoHeight()/(Math.tan(Math.toRadians(mc.options.fov/2))*2));

        yaw = (float) Math.toRadians(yaw);
        pitch = (float) -Math.toRadians(pitch);
        roll = (float) Math.toRadians(roll);

        x = -x * 100;
        y = -y * 100;
        z = z * 100;

        if(framesDone == 0) {
            xOffset = x;
            yOffset = y;
            zOffset = z;
        }
        x = x - xOffset;
        y = y - yOffset;
        z = z - zOffset;

        float c1 = (float) cos( roll / 2 );
        float c2 = (float) cos( pitch / 2 );
        float c3 = (float) cos( yaw / 2 );

        float s1 = (float) sin( roll / 2 );
        float s2 = (float) sin( pitch / 2 );
        float s3 = (float) sin( yaw / 2 );
        Quaternion quaternion = new Quaternion(
                s1 * c2 * c3 - c1 * s2 * s3,
                c1 * s2 * c3 + s1 * c2 * s3,
                c1 * c2 * s3 - s1 * s2 * c3,
                c1 * c2 * c3 + s1 * s2 * s3);

        Matrix4f matrix = makeRotationFromQuaternion(quaternion);

        float[] newRotation = setFromRotationMatrix(matrix);

        times.append((float)framesDone/(float)settings.getFramesPerSecond()).append(',');
        cameraTranslation.append('[').append(x).append(',').append(y).append(',').append(z).append("],");
        cameraRotation.append('[').append(Math.toDegrees(newRotation[1])).append(',')
                .append(Math.toDegrees(newRotation[2])).append(',')
                .append(Math.toDegrees(newRotation[0])).append("],");
        cameraZoom.append('[').append(zoom).append("],");


        framesDone++;
    }

    /*
    ------------------ BEGIN MIT LICENSE SECTION ------------------
    The MIT License

    Copyright © 2010-2021 three.js authors

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
     */

    public Matrix4f makeRotationFromQuaternion(Quaternion q) {
        return this.compose( new Vector3f( 0, 0, 0), q, new Vector3f(1, 1, 1));
    }

    public Matrix4f compose(Vector3f position, Quaternion quaternion, Vector3f scale) {
        Matrix4f m = new Matrix4f();

        float x = quaternion.x, y = quaternion.y, z = quaternion.z, w = quaternion.w;
        float x2 = x + x,	y2 = y + y, z2 = z + z;
        float xx = x * x2, xy = x * y2, xz = x * z2;
        float yy = y * y2, yz = y * z2, zz = z * z2;
        float wx = w * x2, wy = w * y2, wz = w * z2;

        float sx = scale.x, sy = scale.y, sz = scale.z;

        m.m00 = ( 1 - ( yy + zz ) ) * sx;
        m.m01 = ( xy + wz ) * sx;
        m.m02 = ( xz - wy ) * sx;
        m.m03 = 0;

        m.m10 = ( xy - wz ) * sy;
        m.m11 = ( 1 - ( xx + zz ) ) * sy;
        m.m12 = ( yz + wx ) * sy;
        m.m13 = 0;

        m.m20 = ( xz + wy ) * sz;
        m.m21 = ( yz - wx ) * sz;
        m.m22 = ( 1 - ( xx + yy ) ) * sz;
        m.m23 = 0;

        m.m30 = position.x;
        m.m31 = position.y;
        m.m32 = position.z;
        m.m33 = 1;

        return m;

    }

    private float[] setFromRotationMatrix(Matrix4f m) {
        // assumes the upper 3x3 of m is a pure rotation matrix (i.e, unscaled)
        float m11 = m.m00, m12 = m.m10, m13 = m.m20;
        float m21 = m.m01, m22 = m.m11, m23 = m.m21;
        float m31 = m.m02, m32 = m.m12, m33 = m.m22;

        float x, y, z;

        z = (float) Math.asin( clamp( m21, - 1, 1 ) );
        if ( Math.abs( m21 ) < 0.9999999 ) {
            x = (float) Math.atan2( - m23, m22 );
            y = (float) Math.atan2( - m31, m11 );
        } else {
            x = 0;
            y = (float) Math.atan2( m13, m33 );

        }

        return new float[]{x, y, z};
    }

    //------------------ END MIT LICENSE SECTION ------------------

    //clamp value to range [min, max]
    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    public void finish() throws IOException {
        String combinedData = "#target AfterEffects\n" +
                "\n" +
                "function createCameraFromReplayMod(){\n" +
                "\n" +
                "var compName = prompt(\"Enter name for a new composition that will include your camera\",\""+ settings.getOutputFile().getName() + "\",\"Composition's Name\");\n" +
                "if (compName){\n" +
                "var newComp = app.project.items.addComp(compName, " + settings.getTargetVideoWidth() + ", " + + settings.getTargetVideoHeight() + ", 1.0, " + totalFrames/(float)settings.getFramesPerSecond() + ", " + settings.getFramesPerSecond()+ ");\n" +
                "\n" +
                "var Camera = newComp.layers.addCamera(\"Camera\",[0,0]);\n" +
                "Camera.autoOrient = AutoOrientType.NO_AUTO_ORIENT;\n" +
                "Camera.property(\"position\").setValuesAtTimes([" + times + "],[" + cameraTranslation + "]);\n" +
                "Camera.property(\"orientation\").setValuesAtTimes(["  + times + "],[" + cameraRotation + "]);\n" +
                "Camera.property(\"zoom\").setValuesAtTimes(["  + times + "],[" + cameraZoom + "]);\n" +
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
        Path jsxBasePath = Files.isDirectory(videoPath)
                ? videoPath.resolve("AfterEffectsCamera.jsx")
                : videoPath.resolveSibling(FilenameUtils.getBaseName(videoPath.getFileName().toString()) + ".jsx");
        Path jsxPath = jsxBasePath;
        for (int i = 0; Files.exists(jsxPath); i++) {
            String baseName = FilenameUtils.getBaseName(jsxBasePath.getFileName().toString());
            jsxPath = jsxBasePath.resolveSibling(baseName + "." + i + ".jsx");
        }

        File file = jsxPath.toFile();

        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);

        byte[] bytes = combinedData.getBytes();
        bos.write(bytes);
        bos.close();
        fos.close();


    }
}
