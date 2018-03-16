//#if MC<=10710
//$$ package com.replaymod.core.asm;
//$$
//$$ import net.minecraft.launchwrapper.IClassTransformer;
//$$ import org.objectweb.asm.ClassReader;
//$$ import org.objectweb.asm.ClassVisitor;
//$$ import org.objectweb.asm.ClassWriter;
//$$ import org.objectweb.asm.MethodVisitor;
//$$ import org.objectweb.asm.Opcodes;
//$$
//$$ import java.util.HashSet;
//$$ import java.util.Set;
//$$
//$$ /**
//$$  * Redirect all calls to GL11.glEnable and GL11.glDisable to the GLStateTracker (excluding that class itself).
//$$  */
//$$ public class GLStateTrackerTransformer implements IClassTransformer {
//$$     private static final String GL11_CLASS = "org.lwjgl.opengl.GL11";
//$$     private static final String GL11 = GL11_CLASS.replace('.', '/');
//$$     private static final String glEnable = "glEnable";
//$$     private static final String glDisable = "glDisable";
//$$     private static final String GLStateTracker_CLASS = "com.replaymod.render.hooks.GLStateTracker";
//$$     private static final String GLStateTracker = GLStateTracker_CLASS.replace('.', '/');
//$$     private static final String hook_glEnable = "hook_glEnable";
//$$     private static final String hook_glDisable = "hook_glDisable";
//$$
//$$     @Override
//$$     public byte[] transform(String name, String transformedName, byte[] basicClass) {
//$$         if (basicClass == null) return null;
//$$         // Ignore the state tracker itself
//$$         if (name.equals(GLStateTracker_CLASS)) return basicClass;
//$$
//$$         ClassReader reader = new ClassReader(basicClass);
//$$         Set<Method> eligibleMethods = findEligibleMethods(reader);
//$$         if (eligibleMethods.isEmpty()) {
//$$             return basicClass;
//$$         }
//$$
//$$         ClassWriter writer = new ClassWriter(reader, 0);
//$$         reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
//$$             @Override
//$$             public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//$$                 MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//$$                 if (!eligibleMethods.contains(new Method(name, desc))) {
//$$                     return mv;
//$$                 }
//$$                 return new MethodVisitor(Opcodes.ASM5, mv) {
//$$                     @Override
//$$                     public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
//$$                         if (owner.equals(GL11)) {
//$$                             if (name.equals(glEnable)) {
//$$                                 owner = GLStateTracker;
//$$                                 name = hook_glEnable;
//$$                             } else if (name.equals(glDisable)) {
//$$                                 owner = GLStateTracker;
//$$                                 name = hook_glDisable;
//$$                             }
//$$                         }
//$$                         super.visitMethodInsn(opcode, owner, name, desc, itf);
//$$                     }
//$$                 };
//$$             }
//$$         }, 0);
//$$         return writer.toByteArray();
//$$     }
//$$
//$$     private Set<Method> findEligibleMethods(ClassReader reader) {
//$$         Set<Method> eligibleMethods = new HashSet<>();
//$$         reader.accept(new ClassVisitor(Opcodes.ASM5) {
//$$             @Override
//$$             public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
//$$                 return new MethodVisitor(Opcodes.ASM5) {
//$$                     @Override
//$$                     public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
//$$                         if (owner.equals(GL11) && (name.equals(glEnable) || name.equals(glDisable))) {
//$$                             eligibleMethods.add(new Method(methodName, methodDesc));
//$$                         }
//$$                     }
//$$                 };
//$$             }
//$$         }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
//$$         return eligibleMethods;
//$$     }
//$$
//$$     private static class Method {
//$$         private final String name, desc;
//$$
//$$         private Method(String name, String desc) {
//$$             this.name = name;
//$$             this.desc = desc;
//$$         }
//$$
//$$         @Override
//$$         public boolean equals(Object o) {
//$$             if (o == null || getClass() != o.getClass()) return false;
//$$             Method method = (Method) o;
//$$             return name.equals(method.name) && desc.equals(method.desc);
//$$         }
//$$
//$$         @Override
//$$         public int hashCode() {
//$$             return name.hashCode() ^ desc.hashCode();
//$$         }
//$$     }
//$$ }
//#endif
