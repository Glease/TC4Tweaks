package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

class ThaumcraftApiVisitor extends ClassVisitor {
	public ThaumcraftApiVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if ("<clinit>".equals(name) && "()V".equals(desc)) {
			TC4Transformer.log.debug("Adding callhook to end of clinit");
			return new ClinitVisitor(api, mv);
		} else {
			return mv;
		}
	}

	private static class ClinitVisitor extends MethodVisitor {
		public ClinitVisitor(int api, MethodVisitor mv) {
			super(api, mv);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == RETURN)
				mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, "postThaumcraftApiClinit", "()V", false);
			super.visitInsn(opcode);
		}
	}
}
