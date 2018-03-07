//#if MC<=10710
//$$ package com.replaymod.core;
//$$
//$$ import net.minecraft.launchwrapper.IClassTransformer;
//$$ import org.lwjgl.opengl.GL11;
//$$ import org.lwjgl.util.glu.GLU;
//$$ import org.objectweb.asm.ClassReader;
//$$ import org.objectweb.asm.ClassVisitor;
//$$ import org.objectweb.asm.ClassWriter;
//$$ import org.objectweb.asm.MethodVisitor;
//$$ import org.objectweb.asm.Opcodes;
//$$
//$$ /**
//$$  * Insert glGetError checks after all calls to any method in any GL*, ARB* and EXT* classes
//$$  */
//$$ public class GLErrorTransformer implements IClassTransformer {
//$$     private static final String GL_CLASS = "org.lwjgl.opengl.GL";
//$$     private static final String GL = GL_CLASS.replace('.', '/');
//$$     private static final String ARB_CLASS = "org.lwjgl.opengl.ARB";
//$$     private static final String ARB = ARB_CLASS.replace('.', '/');
//$$     private static final String EXT_CLASS = "org.lwjgl.opengl.EXT";
//$$     private static final String EXT = EXT_CLASS.replace('.', '/');
//$$     private static final String GLErrorTransformer_CLASS = GLErrorTransformer.class.getName();
//$$     private static final String GLErrorTransformer = GLErrorTransformer_CLASS.replace('.', '/');
//$$
//$$     @Override
//$$     public byte[] transform(String name, String transformedName, byte[] basicClass) {
//$$         // Ignore (anonymous) inner classes of this transformer
//$$         if (name.startsWith(GLErrorTransformer_CLASS)) return basicClass;
//$$
//$$         ClassReader reader = new ClassReader(basicClass);
//$$         ClassWriter writer = new ClassWriter(reader, 0);
//$$         reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
//$$             @Override
//$$             public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//$$                 MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
//$$                 return new MethodVisitor(Opcodes.ASM5, mv) {
//$$                     @Override
//$$                     public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
//$$                         super.visitMethodInsn(opcode, owner, name, desc, itf);
//$$                         if (owner.startsWith(GL) || owner.startsWith(ARB) || owner.startsWith(EXT)) {
//$$                             visitLdcInsn(owner.replace('/', '.'));
//$$                             visitLdcInsn(name);
//$$                             super.visitMethodInsn(Opcodes.INVOKESTATIC, GLErrorTransformer, "glErrorCheck", "(Ljava/lang/String;Ljava/lang/String;)V", false);
//$$                         }
//$$                     }
//$$
//$$                     @Override
//$$                     public void visitMaxs(int maxStack, int maxLocals) {
//$$                         super.visitMaxs(maxStack + 2, maxLocals);
//$$                     }
//$$                 };
//$$             }
//$$         }, 0);
//$$         return writer.toByteArray();
//$$     }
//$$
//$$     private static boolean inGlBegin;
//$$
//$$     @SuppressWarnings("unused") // Called via ASM
//$$     public static void glErrorCheck(String cls, String method) {
//$$         if (method.equals("glBegin") && cls.equals(GL_CLASS + "11")) {
//$$             if (inGlBegin) {
//$$                 // glBegin within glBegin
//$$                 throw new GLError(GL11.GL_INVALID_OPERATION, cls, method);
//$$             }
//$$             inGlBegin = true;
//$$             return;
//$$         }
//$$         if (inGlBegin && method.equals("glEnd") && cls.equals(GL_CLASS + "11")) {
//$$             inGlBegin = false;
//$$         }
//$$         if (inGlBegin) {
//$$             return; // Cannot call glGetError in between of glBegin and glEnd
//$$         }
//$$         int err = GL11.glGetError();
//$$         if (err != 0) {
//$$             GLError e = new GLError(err, cls, method);
//$$             if ("true".equals(System.getProperty("replaymod.glerrors.throw", "true"))) {
//$$                 throw e;
//$$             } else {
//$$                 e.printStackTrace();
//$$             }
//$$         }
//$$     }
//$$
//$$     public static class GLError extends RuntimeException {
//$$         private GLError(int err, String cls, String method) {
//$$             super(GLU.gluErrorString(err) + " (" + err + ")");
//$$             StackTraceElement[] stack = getStackTrace();
//$$             stack[0] = new StackTraceElement(cls, method, stack[0].getFileName(), stack[0].getLineNumber());
//$$             setStackTrace(stack);
//$$         }
//$$     }
//$$ }
//#endif
