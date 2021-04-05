package net.glease.tc4tweak.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;

class ScanManagerVisitor extends ClassVisitor {
	public ScanManagerVisitor(int api, ClassVisitor cv) {
		super(api, cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (!name.equals("generateItemHash") || !desc.equals("(Lnet/minecraft/item/Item;I)I")) {
			return mv;
		}
		TC4Transformer.log.debug("Replacing {}", name);
		mv.visitParameter("item", 0);
		mv.visitParameter("meta", 0);
		ASMUtils.writeMethodDeflected(ASMCALLHOOKSERVER_INTERNAL_NAME, name, mv, null, desc);
		return null;
	}
}
