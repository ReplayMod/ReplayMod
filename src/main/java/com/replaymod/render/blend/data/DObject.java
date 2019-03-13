package com.replaymod.render.blend.data;

import com.replaymod.render.blend.Util;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Quaternion;
import de.johni0702.minecraft.gui.utils.lwjgl.vector.Vector3f;
import org.blender.dna.AnimData;
import org.blender.dna.BlenderObject;
import org.blender.dna.Material;
import org.blender.dna.Mesh;
import org.blender.dna.bAction;
import org.blender.dna.bConstraint;
import org.blender.dna.bTrackToConstraint;
import org.cakelab.blender.io.block.BlockCodes;
import org.cakelab.blender.nio.CArrayFacade;
import org.cakelab.blender.nio.CPointer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DObject {
    public final DId id = new DId(BlockCodes.ID_OB);
    public Vector3f loc = new Vector3f(0, 0, 0);
    public Vector3f scale = new Vector3f(1, 1, 1);
    public Quaternion rot = new Quaternion();
    private DObject parent;
    private List<DObject> children = new ArrayList<>();
    private List<DObject> unmodifiableChildren;
    public DAction action;
    public DMesh mesh;
    public Type type;
    public int layers = 1;
    public DObject pointAt;
    public int lastFrame;
    private int lastVisibleFrame = -2;

    public DObject(Type type) {
        this.type = type;
    }

    public DObject(DMesh mesh) {
        this(Type.OB_MESH);
        this.mesh = mesh;
    }

    public boolean isValid() {
        return true;
    }

    public void setVisible(int frame) {
        if (frame < lastVisibleFrame) throw new IllegalStateException("Already at frame " + lastVisibleFrame);
        if (lastVisibleFrame < frame - 1) {
            if (lastVisibleFrame < 0) {
                if (frame > 0) {
                    keyframe("hide", 0, 0, 1);
                }
            } else {
                keyframe("hide", 0, lastVisibleFrame + 1, 1);
            }
            keyframe("hide", 0, frame, 0);
        }
        lastVisibleFrame = frame;
    }

    public void setParent(DObject parent) {
        if (this.parent != null) {
            this.parent.children.remove(this);
        }
        this.parent = parent;
        parent.children.add(this);
    }

    public DObject getParent() {
        return parent;
    }

    public List<DObject> getChildren() {
        if (unmodifiableChildren == null) {
            unmodifiableChildren = Collections.unmodifiableList(children);
        }
        return unmodifiableChildren;
    }

    public void keyframeLocRotScale(int frame) {
        keyframeLoc(frame);
        keyframeRot(frame);
        keyframeScale(frame);
    }

    public void keyframeLoc(int frame) {
        keyframe("location", frame, loc);
    }

    public void keyframeRot(int frame) {
        keyframe("rotation_quaternion", frame, rot);
    }

    public void keyframeScale(int frame) {
        keyframe("scale", frame, scale);
    }

    public void keyframe(String rnaPath, int frame, Quaternion q) {
        keyframe(rnaPath, 0, frame, q.w);
        keyframe(rnaPath, 1, frame, q.x);
        keyframe(rnaPath, 2, frame, q.y);
        keyframe(rnaPath, 3, frame, q.z);
    }

    public void keyframe(String rnaPath, int frame, Vector3f vec) {
        keyframe(rnaPath, 0, frame, vec.x);
        keyframe(rnaPath, 1, frame, vec.y);
        keyframe(rnaPath, 2, frame, vec.z);
    }

    public void keyframe(String rnaPath, int rnaArrayIndex, int frame, float value) {
        if (frame < 0) return;
        DAction.DKeyframe keyframe = new DAction.DKeyframe();
        keyframe.frame = frame;
        keyframe.value = value;
        keyframe(rnaPath, rnaArrayIndex, keyframe, rnaPath.startsWith("hide"));
        if (rnaPath.equals("hide")) {
            keyframe("hide_render", rnaArrayIndex, frame, value);
        }
    }

    public void keyframe(String rnaPath, int rnaArrayIndex, DAction.DKeyframe keyframe, boolean constant) {
        if (action == null) {
            action = new DAction();
        }
        DAction.DFCurve theCurve = null;
        for (DAction.DFCurve curve : action.curves) {
            if (curve.rnaArrayIndex == rnaArrayIndex && curve.rnaPath.equals(rnaPath)) {
                theCurve = curve;
                break;
            }
        }
        if (theCurve == null) {
            theCurve = new DAction.DFCurve();
            theCurve.rnaPath = rnaPath;
            theCurve.rnaArrayIndex = rnaArrayIndex;
            action.curves.add(theCurve);
        }
        keyframe.interpolationType = constant ? DAction.InterpolationType.CONSTANT : DAction.InterpolationType.LINEAR;
        if (constant) {
            if (!theCurve.keyframes.isEmpty()) {
                DAction.DKeyframe prev = theCurve.keyframes.get(theCurve.keyframes.size() - 1);
                if (Math.abs(prev.value - keyframe.value) < 1e-4) {
                    return;
                }
            }
        } else {
            if (theCurve.keyframes.size() >= 2) {
                DAction.DKeyframe prev = theCurve.keyframes.get(theCurve.keyframes.size() - 1);
                DAction.DKeyframe prev2 = theCurve.keyframes.get(theCurve.keyframes.size() - 2);
                float m = (prev.value - prev2.value) / (prev.frame - prev2.frame);
                float interpolatedValue = prev.value + m * (keyframe.frame - prev.frame);
                if (Math.abs(interpolatedValue - keyframe.value) < 1e-4) {
                    theCurve.keyframes.remove(theCurve.keyframes.size() - 1);
                }
            }
        }
        theCurve.keyframes.add(keyframe);
    }

    public CPointer<BlenderObject> serialize(Serializer serializer) throws IOException {
        return serializer.maybeMajor(this, id, BlenderObject.class, () -> {
            if (lastVisibleFrame >= 0) {
                keyframe("hide", 0, lastVisibleFrame + 1, 1);
            }

            CPointer<BlenderObject> parent = this.parent == null ? null : this.parent.serialize(serializer);
            CPointer<Mesh> mesh = this.mesh == null ? null : this.mesh.serialize(serializer);
            CPointer<bAction> action = this.action == null ? null : this.action.serialize(serializer);
            CPointer<BlenderObject> pointAt = this.pointAt == null ? null : this.pointAt.serialize(serializer);

            return object -> {
                object.setParent(parent);
                if (parent != null) {
                    CArrayFacade<CArrayFacade<Float>> parentinv = object.getParentinv();
                    parentinv.get(0).set(0, 1f);
                    parentinv.get(1).set(1, 1f);
                    parentinv.get(2).set(2, 1f);
                    parentinv.get(3).set(3, 1f);
                }
                if (mesh != null) {
                    object.setData(mesh.cast(Object.class));
                    Mesh meshObj = mesh.get();
                    short totcol = meshObj.getTotcol();
                    if (totcol > 0) {
                        byte[] matbits = new byte[totcol];
                        Arrays.fill(matbits, (byte) 1);
                        object.setMatbits(serializer.writeBytes(matbits));
                        object.setMat(serializer.writeDataPArray(Material.class, totcol, i -> Util.plus(meshObj.getMat(), i).get()));
                        object.setTotcol(totcol);
                    }
                }
                object.setType((short) type.ordinal());
                object.setLay(layers);
                object.setDt((byte) 5);
                CArrayFacade<Float> loc = object.getLoc();
                loc.set(0, this.loc.x);
                loc.set(1, this.loc.y);
                loc.set(2, this.loc.z);
                CArrayFacade<Float> size = object.getSize();
                size.set(0, scale.x);
                size.set(1, scale.y);
                size.set(2, scale.z);
                CArrayFacade<Float> quat = object.getQuat();
                quat.set(0, rot.w);
                quat.set(1, rot.x);
                quat.set(2, rot.y);
                quat.set(3, rot.z);
                CArrayFacade<Float> dquat = object.getDquat();
                dquat.set(0, 1f);
                dquat.set(1, 0f);
                dquat.set(2, 0f);
                dquat.set(3, 0f);

                object.getDscale().fromArray(new float[]{1, 1, 1});

                AnimData animData = serializer.writeData(AnimData.class);
                animData.setAction(action);
                object.setAdt(animData.__io__addressof());

                if (pointAt != null) {
                    serializer.writeDataList(bConstraint.class, object.getConstraints(), 1, (i, bConstraint) -> {
                        bConstraint.setEnforce(1);
                        bConstraint.setType((short) 2 /* CONSTRAINT_TYPE_TRACKTO */);
                        bTrackToConstraint constraint = serializer.writeData(bTrackToConstraint.class);
                        constraint.setTar(pointAt);
                        constraint.setReserved1(1 /* TRACK_Y */);
                        constraint.setReserved2(2 /* UP_Z */);
                        bConstraint.setData(constraint.__io__addressof().cast(Object.class));
                    });
                }
            };
        });
    }

    public enum Type {
        OB_EMPTY,
        OB_MESH,
        OB_CURVE,
        OB_SURF,
        OB_FONT,
        OB_MBALL,
        OB_6,
        OB_7,
        OB_8,
        OB_9,
        OB_LAMP,
        OB_CAMERA,
        OB_SPEAKER,
        OB_13,
        OB_14,
        OB_15,
        OB_16,
        OB_17,
        OB_18,
        OB_19,
        OB_20,
        OB_WAVE,
        OB_LATTICE,
        OB_23,
        OB_24,
        OB_ARMATURE
    }
}
