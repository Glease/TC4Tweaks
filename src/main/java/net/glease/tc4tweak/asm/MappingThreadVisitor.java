package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.*;

public class MappingThreadVisitor extends ClassVisitor {
	private static class RunVisitor extends MethodVisitor {
		public RunVisitor(int api, MethodVisitor mv) {
			super(api, mv);
		}

		@Override
		public void visitCode() {
			super.visitCode();
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "setPriority", "(I)V", false);
		}
	}

	public MappingThreadVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (name.equals("run") && desc.equals("()V")) {
			return new RunVisitor(api, mv);
		}
		return mv;
	}
}
