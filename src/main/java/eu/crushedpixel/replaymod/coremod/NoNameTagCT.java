package eu.crushedpixel.replaymod.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

public class NoNameTagCT implements IClassTransformer {

    private static final String ASM_HOOKS = "eu/crushedpixel/replaymod/coremod/asm_Hooks";
    private static final String CLASS_NAME = "net.minecraft.client.renderer.entity.Render";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (CLASS_NAME.equals(transformedName)) {
            if (name.equals(transformedName)) {
                return transform(bytes, "renderLivingLabel", "(Lnet/minecraft/entity/Entity;Ljava/lang/String;DDDI)V");
            } else {
                return transform(bytes, "a", "(Lwv;Ljava/lang/String;DDDI)V");
            }
        }
        return bytes;
    }

    private byte[] transform(byte[] bytes, String name_method, String desc_method) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        boolean success = false;
        for (MethodNode m : classNode.methods) {
            if (desc_method.equals(m.desc) && name_method.equals(m.name)) {
                ListIterator<AbstractInsnNode> iter = m.instructions.iterator();
                iter.add(new FieldInsnNode(GETSTATIC, ASM_HOOKS, "DO_NOT_RENDER_NAME_TAGS", "Z"));
                iter.add(new InsnNode(ICONST_0));
                LabelNode label = new LabelNode();
                iter.add(new JumpInsnNode(IF_ICMPEQ, label));
                iter.add(new InsnNode(RETURN));
                iter.add(label);
                iter.add(new FrameNode(F_SAME, 0, null, 0, null));
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
}
