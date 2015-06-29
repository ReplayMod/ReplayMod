package eu.crushedpixel.replaymod.coremod;

import akka.japi.Pair;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EnchantmentTimerCT implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName,
                            byte[] basicClass) {
        if(name.equals("cqh")) {
            return patchRenderEffectMethod(basicClass, true);
        }

        if(name.equals("net.minecraft.client.renderer.entity.RenderItem")) {
            return patchRenderEffectMethod(basicClass, false);
        }

        return basicClass;
    }

    public byte[] patchRenderEffectMethod(byte[] bytes, boolean obfuscated) {
        System.out.println("REPLAY MOD CORE PATCHER: Inside RenderItem class");

        String methodName = obfuscated ? "a" : "renderEffect";
        String classDescriptor = obfuscated ? "(Lcxe;)V" : "(Lnet/minecraft/client/resources/model/IBakedModel;)V";

        String minecraftClass = obfuscated ? "bsu" : "net/minecraft/client/Minecraft";
        String getSystemTime = obfuscated ? "I" : "getSystemTime";
        String sysTimeDesc = "()J";

        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(bytes);
        classReader.accept(classNode, 0);

        List<Pair<AbstractInsnNode, AbstractInsnNode>> toInsert = new ArrayList<Pair<AbstractInsnNode, AbstractInsnNode>>();

        for (MethodNode m : classNode.methods) {
            if (m.name.equals(methodName) && m.desc.equals(classDescriptor)) {
                System.out.println("REPLAY MOD CORE PATCHER: Inside renderEffect method");

                Iterator<AbstractInsnNode> nodeIterator = m.instructions.iterator();
                while (nodeIterator.hasNext()) {
                    AbstractInsnNode node = nodeIterator.next();
                    if (node instanceof MethodInsnNode) {
                        MethodInsnNode min = (MethodInsnNode) node;
                        if (min.getOpcode() == Opcodes.INVOKESTATIC && min.name.equals(getSystemTime) &&
                                min.owner.equals(minecraftClass) && min.desc.equals(sysTimeDesc)) {
                            MethodInsnNode n = new MethodInsnNode(Opcodes.INVOKESTATIC,
                                    "eu/crushedpixel/replaymod/timer/EnchantmentTimer", "getEnchantmentTime",
                                    min.desc, min.itf);
                            toInsert.add(new Pair<AbstractInsnNode, AbstractInsnNode>(min, n));
                        }
                    }
                }

                for (Pair<AbstractInsnNode, AbstractInsnNode> pair : toInsert) {
                    m.instructions.insertBefore(pair.first(), pair.second());
                    m.instructions.remove(pair.first());
                }

            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    // net.minecraft.client.renderer.entity.RenderItem -> cqh
    // private void renderEffect(IBakedModel model) -> private void a(cxe paramcxe)
    // float f = (float)(Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F; -> float f1 = (float)(bsu.I() % 3000L) / 3000.0F / 8.0F;
}
