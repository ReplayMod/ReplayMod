package eu.crushedpixel.replaymod.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * Transforms the RenderGlobal class:
 * - Adds a new field 'ChunkLoadingRenderGlobal hook'
 * - Moves code of 'setupTerrain' into 'setupTerrain$Original'
 * - Creates 'setupTerrain' method (see {@link #createSetupTerrain(ClassNode, MethodNode)} ()}
 * - Moves code of 'isPositionInRenderChunk' into 'isPositionInRenderChunk$Original'
 * - Creates 'isPositionInRenderChunk' method (see {@link #createIsPositionInRenderChunk(ClassNode, MethodNode)} ()}
 * - Moves code of 'updateChunks' into 'updateChunk$Original'
 * - Creates 'updateChunks' method (see {@link #createUpdateChunks(ClassNode, MethodNode)} ()}
 *
 * Transforms the ChunkLoadingRenderGlobal class:
 * - Method 'original_setupTerrain' calls 'setupTerrain$Original'
 * - Method 'original_isPositionInRenderChunk' calls 'isPositionInRenderChunk$Original'
 * - Method 'original_updateChunks' calls 'updateChunks$Original'
 */
public class ForceChunkLoadingCT implements IClassTransformer {

    private static final String HOOK = "eu.crushedpixel.replaymod.renderer.ChunkLoadingRenderGlobal";
    private static final String HOOK_JVM = HOOK.replace('.', '/');
    private static final String HOOK_TYPE = "L" + HOOK_JVM + ";";
    private static final String RENDER_GLOBAL = "net.minecraft.client.renderer.RenderGlobal";
    private static final String RENDER_GLOBAL_JVM = RENDER_GLOBAL.replace('.', '/');
    private static final String RENDER_GLOBAL_TYPE = "L" + RENDER_GLOBAL_JVM + ";";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (RENDER_GLOBAL.equals(transformedName)) {
            if (name.equals(transformedName)) {
                return transformRenderGlobal(bytes, "setupTerrain", "(Lnet/minecraft/entity/Entity;DLnet/minecraft/client/renderer/culling/ICamera;IZ)V",
                        "isPositionInRenderChunk", "(Lnet/minecraft/util/BlockPos;Lnet/minecraft/client/renderer/chunk/RenderChunk;)Z",
                        "updateChunks", "(J)V");
            } else {
                return transformRenderGlobal(bytes, "g", "",
                        "", "",
                        "", "(J)V");
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

        // Find methods
        for (MethodNode m : classNode.methods) {
            if ("original_setupTerrain".equals(m.name)) {
                m.instructions.clear();
                InsnList iter = m.instructions;
                iter.add(new VarInsnNode(ALOAD, 0)); // this
                iter.add(new FieldInsnNode(GETFIELD, classNode.name, "hooked", RENDER_GLOBAL_TYPE));
                iter.add(new VarInsnNode(ALOAD, 1)); // viewEntity
                iter.add(new VarInsnNode(DLOAD, 2)); // partialTicks
                iter.add(new VarInsnNode(ALOAD, 4)); // camera
                iter.add(new VarInsnNode(ILOAD, 5)); // frameCount
                iter.add(new VarInsnNode(ILOAD, 6)); // playerSpectator
                iter.add(new MethodInsnNode(INVOKEVIRTUAL, RENDER_GLOBAL_JVM, "setupTerrain$Original", m.desc, false));
                iter.add(new InsnNode(RETURN));
            }
            if ("original_isPositionInRenderChunk".equals(m.name)) {
                m.instructions.clear();
                InsnList iter = m.instructions;
                iter.add(new VarInsnNode(ALOAD, 0)); // this
                iter.add(new FieldInsnNode(GETFIELD, classNode.name, "hooked", RENDER_GLOBAL_TYPE));
                iter.add(new VarInsnNode(ALOAD, 1)); // pos
                iter.add(new VarInsnNode(ALOAD, 2)); // chunk
                iter.add(new MethodInsnNode(INVOKEVIRTUAL, RENDER_GLOBAL_JVM, "isPositionInRenderChunk$Original", m.desc, false));
                iter.add(new InsnNode(IRETURN));
            }
            if ("original_updateChunks".equals(m.name)) {
                m.instructions.clear();
                InsnList iter = m.instructions;
                iter.add(new VarInsnNode(ALOAD, 0)); // this
                iter.add(new FieldInsnNode(GETFIELD, classNode.name, "hooked", RENDER_GLOBAL_TYPE));
                iter.add(new VarInsnNode(LLOAD, 1)); // finishTimeNano
                iter.add(new MethodInsnNode(INVOKEVIRTUAL, RENDER_GLOBAL_JVM, "updateChunks$Original", m.desc, false));
                iter.add(new InsnNode(RETURN));
            }
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private byte[] transformRenderGlobal(byte[] bytes, String name_setupTerrain, String desc_setupTerrain,
                                         String name_isPositionInRenderChunk, String desc_isPositionInRenderChunk,
                                         String name_updateChunks, String desc_updateChunks) {
        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        // Add field
        classNode.visitField(ACC_PUBLIC, "hook", HOOK_TYPE, "", null);

        // Find methods
        MethodNode setupTerrain = null;
        MethodNode isPositionInRenderChunk = null;
        MethodNode updateChunks = null;
        for (MethodNode m : classNode.methods) {
            if (name_setupTerrain.equals(m.name) && desc_setupTerrain.equals(m.desc)) {
                setupTerrain = m;
            }
            if (name_isPositionInRenderChunk.equals(m.name) && desc_isPositionInRenderChunk.equals(m.desc)) {
                isPositionInRenderChunk = m;
            }
            if (name_updateChunks.equals(m.name) && desc_updateChunks.equals(m.desc)) {
                updateChunks = m;
            }
        }
        if (setupTerrain == null) {
            throw new NoSuchMethodError("setupTerrain");
        }
        if (isPositionInRenderChunk == null) {
            throw new NoSuchMethodError("isPositionInRenderChunk");
        }
        if (updateChunks == null) {
            throw new NoSuchMethodError("updateChunks");
        }

        // Generate new methods
        classNode.methods.add(createSetupTerrain(classNode, setupTerrain));
        classNode.methods.add(createIsPositionInRenderChunk(classNode, isPositionInRenderChunk));
        classNode.methods.add(createUpdateChunks(classNode, updateChunks));

        // Rename original methods
        setupTerrain.name = "setupTerrain$Original";
        isPositionInRenderChunk.name = "isPositionInRenderChunk$Original";
        updateChunks.name = "updateChunks$Original";

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    /**
     * public void setupTerrain(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator) {
     *     if (this.hook == null) {
     *         setupTerrain$Original(viewEntity, partialTicks, camera, frameCount, playerSpectator);
     *         return;
     *     }
     *     hook.setupTerrain(viewEntity, partialTicks, camera, frameCount, playerSpectator);
     * }
     */
    private MethodNode createSetupTerrain(ClassNode classNode, MethodNode org) {
        MethodNode method = new MethodNode(org.access, org.name, org.desc, org.signature, new String[0]);
        InsnList list = method.instructions;
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        LabelNode labelNotNull = new LabelNode();
        list.add(new JumpInsnNode(IFNONNULL, labelNotNull));

        list.add(new VarInsnNode(ALOAD, 0)); // this
        list.add(new VarInsnNode(ALOAD, 1)); // viewEntity
        list.add(new VarInsnNode(DLOAD, 2)); // partialTicks
        list.add(new VarInsnNode(ALOAD, 4)); // camera
        list.add(new VarInsnNode(ILOAD, 5)); // frameCount
        list.add(new VarInsnNode(ILOAD, 6)); // playerSpectator
        list.add(new MethodInsnNode(INVOKEVIRTUAL, classNode.name, "setupTerrain$Original", org.desc, false));
        list.add(new InsnNode(RETURN));

        list.add(labelNotNull);
        list.add(new FrameNode(F_SAME, 0, null, 0, null));
        list.add(new VarInsnNode(ALOAD, 0)); // this
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        list.add(new VarInsnNode(ALOAD, 1)); // viewEntity
        list.add(new VarInsnNode(DLOAD, 2)); // partialTicks
        list.add(new VarInsnNode(ALOAD, 4)); // camera
        list.add(new VarInsnNode(ILOAD, 5)); // frameCount
        list.add(new VarInsnNode(ILOAD, 6)); // playerSpectator
        list.add(new MethodInsnNode(INVOKEVIRTUAL, HOOK_JVM, "setupTerrain", org.desc, false));
        list.add(new InsnNode(RETURN));

        return method;
    }

    /**
     * public boolean isPositionInRenderChunk(BlockPos pos, RenderChunk chunk) {
     *     if (this.hook == null) {
     *         return isPositionInRenderChunk$Original(pos, chunk);
     *     }
     *     return hook.isPositionInRenderChunk(pos, chunk);
     * }
     */
    private MethodNode createIsPositionInRenderChunk(ClassNode classNode, MethodNode org) {
        MethodNode method = new MethodNode(org.access, org.name, org.desc, org.signature, new String[0]);
        InsnList list = method.instructions;
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        LabelNode labelNotNull = new LabelNode();
        list.add(new JumpInsnNode(IFNONNULL, labelNotNull));

        list.add(new VarInsnNode(ALOAD, 0)); // this
        list.add(new VarInsnNode(ALOAD, 1)); // pos
        list.add(new VarInsnNode(ALOAD, 2)); // chunk
        list.add(new MethodInsnNode(INVOKEVIRTUAL, classNode.name, "isPositionInRenderChunk$Original", org.desc, false));
        list.add(new InsnNode(IRETURN));

        list.add(labelNotNull);
        list.add(new FrameNode(F_SAME, 0, null, 0, null));
        list.add(new VarInsnNode(ALOAD, 0)); // this
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        list.add(new VarInsnNode(ALOAD, 1)); // pos
        list.add(new VarInsnNode(ALOAD, 2)); // chunk
        list.add(new MethodInsnNode(INVOKEVIRTUAL, HOOK_JVM, "isPositionInRenderChunk", org.desc, false));
        list.add(new InsnNode(IRETURN));

        return method;
    }

    /**
     * public void updateChunks(long finishTimeNano) {
     *     if (this.hook == null) {
     *         updateChunks$Original(finishTimeNano);
     *         return;
     *     }
     *     hook.updateChunks(finishTimeNano);
     * }
     */
    private MethodNode createUpdateChunks(ClassNode classNode, MethodNode org) {
        MethodNode method = new MethodNode(org.access, org.name, org.desc, org.signature, new String[0]);
        InsnList list = method.instructions;
        list.add(new VarInsnNode(ALOAD, 0));
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        LabelNode labelNotNull = new LabelNode();
        list.add(new JumpInsnNode(IFNONNULL, labelNotNull));

        list.add(new VarInsnNode(ALOAD, 0)); // this
        list.add(new VarInsnNode(LLOAD, 1)); // finishTimeNano
        list.add(new MethodInsnNode(INVOKEVIRTUAL, classNode.name, "updateChunks$Original", org.desc, false));
        list.add(new InsnNode(RETURN));

        list.add(labelNotNull);
        list.add(new FrameNode(F_SAME, 0, null, 0, null));
        list.add(new VarInsnNode(ALOAD, 0)); // this
        list.add(new FieldInsnNode(GETFIELD, classNode.name, "hook", HOOK_TYPE));
        list.add(new VarInsnNode(LLOAD, 1)); // finishTimeNano
        list.add(new MethodInsnNode(INVOKEVIRTUAL, HOOK_JVM, "updateChunks", org.desc, false));
        list.add(new InsnNode(RETURN));

        return method;
    }
}
