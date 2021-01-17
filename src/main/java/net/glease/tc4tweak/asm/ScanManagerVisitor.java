package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.MyConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static org.objectweb.asm.Opcodes.*;

public class ScanManagerVisitor extends ClassVisitor {
	public ScanManagerVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (LoadingPlugin.gt6 || !name.equals("generateItemHash") || !desc.equals("(Lnet/minecraft/item/Item;I)I")) {
			return mv;
		}
		TC4Transformer.log.debug("Replacing {}", name);
		mv.visitParameter("item", 0);
		mv.visitParameter("meta", 0);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ILOAD, 1);
		mv.visitMethodInsn(INVOKESTATIC, ASMCALLHOOKSERVER_INTERNAL_NAME, name, desc, false);
		mv.visitInsn(IRETURN);
		mv.visitMaxs(2,2);
		mv.visitEnd();
		return null;
	}
}
