package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.POP;

public class FXSonicVisitor extends ClassVisitor {

	private static final String FIELD_MODEL_DESC = "Lnet/minecraftforge/client/model/IModelCustom;";
	private static final String FIELD_MODEL_NAME = "model";

	private static class FieldModelScrubberVisitor extends MethodVisitor {
		public FieldModelScrubberVisitor(int api, MethodVisitor mv) {
			super(api, mv);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			if (owner.equals("thaumcraft/client/fx/other/FXSonic") && name.equals(FIELD_MODEL_NAME) && desc.equals(FIELD_MODEL_DESC)) {
				TC4Transformer.log.debug("Replacing opcode {} with {}", opcode, opcode - 2);
				// pop this
				mv.visitInsn(POP);
				// (opcode - 2) will translate GETFIELD/PUTFIELD into GETSTATIC/PUTSTATIC
				super.visitFieldInsn(opcode - 2, owner, name, desc);
			} else {
				super.visitFieldInsn(opcode, owner, name, desc);
			}
		}
	}

	public FXSonicVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (name.equals(FIELD_MODEL_NAME) && desc.equals(FIELD_MODEL_DESC)) {
			TC4Transformer.log.debug("Making field model static");
			return super.visitField(access | ACC_STATIC, name, desc, signature, value);
		}
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new FieldModelScrubberVisitor(api, super.visitMethod(access, name, desc, signature, exceptions));
	}
}
