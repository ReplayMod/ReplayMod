package eu.crushedpixel.replaymod.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

import static org.objectweb.asm.Opcodes.*;

/**
 * Transforms the RenderManager class:
 * - Adds a new field 'CubicEntityRenderer hook'
 * - Injects CubicEntityRenderer.beforeEntityRender(dx, dy, dz) call at beginning of 'doRenderEntity'
 */
public class RenderManagerCT implements IClassTransformer {

    private static final String HOOK = "eu.crushedpixel.replaymod.video.entity.CubicEntityRenderer";
    private static final String HOOK_JVM = HOOK.replace('.', '/');
    private static final String HOOK_TYPE = "L" + HOOK_JVM + ";";
    private static final String RENDER_MANAGER = "net.minecraft.client.renderer.entity.RenderManager";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (RENDER_MANAGER.equals(transformedName)) {
            if (name.equals(transformedName)) {
                return transformRenderManager(bytes, "doRenderEntity", "(Lnet/minecraft/entity/Entity;DDDFFZ)Z");
            } else {
                return transformRenderManager(bytes, "a", "(Lwv;DDDFFZ)Z");
            }
        }
        return bytes;
    }

    private byte[] transformRenderManager(byte[] bytes, String name_doRenderEntity, String desc_doRenderEntity) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        // Add field
        classNode.visitField(ACC_PUBLIC, "hook", HOOK_TYPE, null, null);

        // Find method
        MethodNode doRenderEntity = null;
        for (MethodNode m : classNode.methods) {
            if (name_doRenderEntity.equals(m.name) && desc_doRenderEntity.equals(m.desc)) {
                doRenderEntity = m;
            }
        }
        if (doRenderEntity == null) {
            throw new NoSuchMethodError("doRenderEntity");
        }

        injectDoRenderEntity(classNode, doRenderEntity);

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private MethodNode injectDoRenderEntity(ClassNode classNode, MethodNode method) {
        ListIterator<AbstractInsnNode> list = method.instructions.iterator();
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        LabelNode labelNull = new LabelNode();
        list.add(new JumpInsnNode(IFNULL, labelNull));

        list.add(new VarInsnNode(ALOAD, 0)); // this
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        list.add(new VarInsnNode(DLOAD, 2)); // x
        list.add(new VarInsnNode(DLOAD, 4)); // y
        list.add(new VarInsnNode(DLOAD, 6)); // z
        list.add(new MethodInsnNode(INVOKEVIRTUAL, HOOK_JVM, "beforeEntityRender", "(DDD)V", false));

        list.add(labelNull);
        list.add(new FrameNode(F_SAME, 0, null, 0, null));

        return method;
    }
}
