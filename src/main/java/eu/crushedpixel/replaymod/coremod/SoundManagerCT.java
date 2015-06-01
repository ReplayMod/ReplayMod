package eu.crushedpixel.replaymod.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;

public class SoundManagerCT implements IClassTransformer {

    private static final String REPLAY_HANDLER = "eu/crushedpixel/replaymod/replay/ReplayHandler";
    private static final String CAMERA_ENTITY = "eu/crushedpixel/replaymod/entities/CameraEntity";
    private static final String CLASS_NAME = "net.minecraft.client.audio.SoundManager";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (CLASS_NAME.equals(transformedName)) {
            if (name.equals(transformedName)) {
                return transform(bytes, "setListener", "(Lnet/minecraft/entity/player/EntityPlayer;F)V");
            } else {
                return transform(bytes, "a", "(Lahd;F)V");
            }
        }
        return bytes;
    }

    private byte[] transform(byte[] bytes, String name_setListener, String desc_setListener) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        boolean success = false;
        for (MethodNode m : classNode.methods) {
            if (desc_setListener.equals(m.desc) && name_setListener.equals(m.name)) {
                ListIterator<AbstractInsnNode> iter = m.instructions.iterator();
                inject(iter);
                success = true;
                break;
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
     * public void setListener(EntityPlayer player, float partialTicks) {
     *     ...
     *     if (ReplayHandler.isInReplay()) {
     *         player = ReplayHandler.getCameraEntity();
     *     }
     *     ...
     * }
     */
    private void inject(ListIterator<AbstractInsnNode> iter) {
        LabelNode l = new LabelNode();
        iter.add(new MethodInsnNode(INVOKESTATIC, REPLAY_HANDLER, "isInReplay", "()Z", false));
        iter.add(new JumpInsnNode(IFEQ, l));
        iter.add(new MethodInsnNode(INVOKESTATIC, REPLAY_HANDLER, "getCameraEntity", "()L" + CAMERA_ENTITY + ";", false));
        iter.add(new VarInsnNode(ASTORE, 1));
        iter.add(l);
    }
}
