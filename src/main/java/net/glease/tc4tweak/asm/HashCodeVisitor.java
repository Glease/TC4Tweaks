package net.glease.tc4tweak.asm;

import java.util.function.BiFunction;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import static net.glease.tc4tweak.asm.ASMConstants.ASMCALLHOOKSERVER_INTERNAL_NAME;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

public class HashCodeVisitor extends ClassVisitor {

    private final String targetName;
    private String className;
    private boolean visited;

    public static BiFunction<Integer, ClassVisitor, ClassVisitor> factory(String targetName) {
        return (api, cv) -> new HashCodeVisitor(api, cv, targetName);
    }

    public HashCodeVisitor(int api, ClassVisitor cv) {
        this(api, cv, "hash");
    }

    public HashCodeVisitor(int api, ClassVisitor cv, String targetName) {
        super(api, cv);
        this.targetName = targetName;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, className, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (name.equals("hashCode") && desc.equals("()I")) {
            log.debug("Replacing {}#hashCode()I", className);
            ASMUtils.writeMethodDeflected(ASMCALLHOOKSERVER_INTERNAL_NAME, targetName, mv, className, desc);
            visited = true;
            return null;
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (!visited) {
            log.debug("Adding {}#hashCode()I", className);
            MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
            ASMUtils.writeMethodDeflected(ASMCALLHOOKSERVER_INTERNAL_NAME, targetName, mv, className, null);
            visited = true;
        }
        super.visitEnd();
    }
}
