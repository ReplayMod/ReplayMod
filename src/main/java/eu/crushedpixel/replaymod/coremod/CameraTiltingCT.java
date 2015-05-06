package eu.crushedpixel.replaymod.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class CameraTiltingCT implements IClassTransformer {

    private static final String REPLAY_HANDLER = "eu/crushedpixel/replaymod/replay/ReplayHandler";
    private static final String CLASS_NAME = "net.minecraft.client.renderer.EntityRenderer";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (CLASS_NAME.equals(transformedName)) {
            return transform(bytes, name.equals(transformedName) ? "orientCamera" : "g");
        }
        return bytes;
    }

    private byte[] transform(byte[] bytes, String name_orientCamera) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        boolean success = false;
        for (MethodNode m : classNode.methods) {
            if ("(F)V".equals(m.desc) && name_orientCamera.equals(m.name)) {
                inject(m.instructions.iterator());
                success = true;
            }
        }
        if (!success) {
            throw new NoSuchMethodError();
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    /**
     * public void orientCamera(float f) {
     *     ...
     *     if (ReplayHandler.isInReplay()) {
     *         GL11.glRotated(Math.toRadians(ReplayHandler.getCameraTilt()), 0, 0, 1);
     *     }
     *     ...
     * }
     */
    private void inject(ListIterator<AbstractInsnNode> iter) {
        LabelNode l = new LabelNode();
        iter.add(new MethodInsnNode(INVOKESTATIC, REPLAY_HANDLER, "isInReplay", "()Z", false));
        iter.add(new JumpInsnNode(IFEQ, l));
        iter.add(new MethodInsnNode(INVOKESTATIC, REPLAY_HANDLER, "getCameraTilt", "()F", false));
        iter.add(new InsnNode(F2D));
        iter.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Math", "toRadians", "(D)D", false));
        iter.add(new LdcInsnNode(0D));
        iter.add(new LdcInsnNode(0D));
        iter.add(new LdcInsnNode(1D));
        iter.add(new MethodInsnNode(INVOKESTATIC, "org/lwjgl/opengl/GL11", "glRotated", "(DDDD)V", false));
        iter.add(l);
        System.out.println("REPLAY MOD CORE PATCHER: Patched EntityRenderer.orientCamera(F) method");
    }
}
