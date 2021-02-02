package net.glease.tc4tweak.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public final class ASMUtils {
	private ASMUtils() {

	}

	/**
	 * Write a method that delegate the call to a static method in another class.
	 * This reference (if any) and all arguments are captured in original sequence.
	 * visitCode() and visitEnd() will all be called.
	 *
	 * @param targetClass Internal name of class that the call gets delegated to
	 * @param targetName  Name of the method that the call gets delegated to
	 * @param mv          The output destination
	 * @param clazz       The internal name of the owner class. NULL if static method
	 * @param desc        The descriptor of this method.
	 */
	public static void writeMethodDeflected(String targetClass, String targetName, MethodVisitor mv, String clazz, String desc) {
		int base;
		String targetDesc;
		if (clazz == null) {
			base = 0;
			targetDesc = desc;
		} else {
			mv.visitVarInsn(ALOAD, 0);
			base = 1;
			targetDesc = '(' + Type.getObjectType(clazz).getDescriptor() + desc.substring(1, desc.length());
		}

		mv.visitCode();

		Type methodType = Type.getMethodType(desc);
		for (Type argumentType : methodType.getArgumentTypes()) {
			mv.visitVarInsn(argumentType.getOpcode(ILOAD), base++);
		}

		mv.visitMethodInsn(INVOKESTATIC, targetClass, targetName, targetDesc, false);

		mv.visitInsn(methodType.getReturnType().getOpcode(IRETURN));

		mv.visitMaxs(base, Type.VOID_TYPE.equals(methodType.getReturnType()) ? base : Math.max(1, base));

		mv.visitEnd();
	}
}
