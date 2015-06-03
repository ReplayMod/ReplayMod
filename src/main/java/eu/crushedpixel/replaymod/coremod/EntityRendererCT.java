package eu.crushedpixel.replaymod.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Transforms the EntityRenderer class:
 * - Adds a new field 'CustomEntityRenderer hook'
 * - Moves code of 'loadShader' into 'loadShader$Original'
 * - Creates 'loadShader' method (see {@link #createLoadShader(ClassNode, MethodNode)} ()}
 *
 * Transforms the CustomEntityRenderer class:
 * - Method 'original_loadShader' calls 'loadShader$Original'
 */
public class EntityRendererCT implements IClassTransformer {

    private static final String HOOK = "eu.crushedpixel.replaymod.video.entity.CustomEntityRenderer";
    private static final String HOOK_JVM = HOOK.replace('.', '/');
    private static final String HOOK_TYPE = "L" + HOOK_JVM + ";";
    private static final String ENTITY_RENDERER = "net.minecraft.client.renderer.EntityRenderer";
    private static final String ENTITY_RENDERER_JVM = ENTITY_RENDERER.replace('.', '/');
    private static final String ENTITY_RENDERER_TYPE = "L" + ENTITY_RENDERER_JVM + ";";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (ENTITY_RENDERER.equals(transformedName)) {
            if (name.equals(transformedName)) {
                return transformEntityRenderer(bytes, "loadShader", "(Lnet/minecraft/util/ResourceLocation;)V");
            } else {
                return transformEntityRenderer(bytes, "a", "(Loa;)V");
            }
        } else if (HOOK.equals(transformedName)) {
            return transformHook(bytes);
        }
        return bytes;
    }

    private byte[] transformHook(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        // Find method
        for (MethodNode m : classNode.methods) {
            if ("original_loadShader".equals(m.name)) {
                m.instructions.clear();
                InsnList iter = m.instructions;
                iter.add(new VarInsnNode(ALOAD, 0)); // this
                iter.add(new FieldInsnNode(GETFIELD, classNode.name, "proxied", ENTITY_RENDERER_TYPE));
                iter.add(new VarInsnNode(ALOAD, 1)); // resourceLocation
                iter.add(new MethodInsnNode(INVOKEVIRTUAL, ENTITY_RENDERER_JVM, "loadShader$Original", m.desc, false));
                iter.add(new InsnNode(RETURN));
            }
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private byte[] transformEntityRenderer(byte[] bytes, String name_loadShader, String desc_loadShader) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        // Add field
        classNode.visitField(ACC_PUBLIC, "hook", HOOK_TYPE, null, null);

        // Find method
        MethodNode setupTerrain = null;
        for (MethodNode m : classNode.methods) {
            if (name_loadShader.equals(m.name) && desc_loadShader.equals(m.desc)) {
                setupTerrain = m;
            }
        }
        if (setupTerrain == null) {
            throw new NoSuchMethodError("loadShader");
        }

        // Generate new method
        classNode.methods.add(createLoadShader(classNode, setupTerrain));

        // Rename original method
        setupTerrain.name = "loadShader$Original";

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    /**
     * public void loadShader(ResourceLocation resourceLocation) {
     *     if (this.hook == null) {
     *         loadShader$Original(resourceLocation);
     *         return;
     *     }
     *     hook.loadShader(resourceLocation);
     * }
     */
    private MethodNode createLoadShader(ClassNode classNode, MethodNode org) {
        MethodNode method = new MethodNode(org.access, org.name, org.desc, org.signature, new String[0]);
        InsnList list = method.instructions;
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        LabelNode labelNotNull = new LabelNode();
        list.add(new JumpInsnNode(IFNONNULL, labelNotNull));

        list.add(new VarInsnNode(ALOAD, 0)); // this
        list.add(new VarInsnNode(ALOAD, 1)); // resourceLocation
        list.add(new MethodInsnNode(INVOKEVIRTUAL, classNode.name, "loadShader$Original", org.desc, false));
        list.add(new InsnNode(RETURN));

        list.add(labelNotNull);
        list.add(new FrameNode(F_SAME, 0, null, 0, null));
        list.add(new VarInsnNode(ALOAD, 0)); // this
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        list.add(new VarInsnNode(ALOAD, 1)); // resourceLocation
        list.add(new MethodInsnNode(INVOKEVIRTUAL, HOOK_JVM, "loadShader", org.desc, false));
        list.add(new InsnNode(RETURN));

        return method;
    }
}
