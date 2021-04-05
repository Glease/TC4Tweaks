package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOK_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.*;

class MappingThreadVisitor extends ClassVisitor {
	private static class RunVisitor extends MethodVisitor {
		public RunVisitor(int api, MethodVisitor mv) {
			super(api, mv);
		}

		@Override
		public void visitCode() {
			super.visitCode();
			TC4Transformer.log.debug("Injecting thread priority change");
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", false);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Thread", "setPriority", "(I)V", false);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
			if (opcode == INVOKEINTERFACE && "java/util/Iterator".equals(owner) && "next".equals(name)) {
				TC4Transformer.log.debug("Injecting callhook before Iterator#next()");
				mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOK_INTERNAL_NAME, "onMappingDidWork", "()V", false);
			}
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
