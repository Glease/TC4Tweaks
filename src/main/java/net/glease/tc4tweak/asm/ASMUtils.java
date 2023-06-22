package net.glease.tc4tweak.asm;

import java.util.Arrays;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

final class ASMUtils {
    private static final Object configHodgePodge;

    static  {
        Object cfg;
        try {
            Class<?> common = Class.forName("com.mitchej123.hodgepodge.Common", true, ASMUtils.class.getClassLoader());
            cfg = common.getField("config").get(null);
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            cfg = null;
        } catch (IllegalAccessException e) {
            log.error("Unforeseen changes in hodgepodge. Nothing will be disabled automatically!");
            cfg = null;
        }
        configHodgePodge = cfg;
    }

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
            targetDesc = '(' + Type.getObjectType(clazz).getDescriptor() + desc.substring(1);
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

    static <T> T[] arrayAppend(T[] arr, T newLast) {
        T[] out = Arrays.copyOf(arr, arr.length + 1);
        out[arr.length]  = newLast;
        return out;
    }

    /**
     *
     * @param configName a boolean field name in HodgePodge LoadingConfig class.
     * @return true if foreign field is true or HodgePodge changed in unforeseen ways, false if foreign field is missing
     * or is false.
     */
    static boolean isHodgepodgeFixActive(@SuppressWarnings("SameParameterValue") String configName) {
        if (configHodgePodge == null) return false;
        try {
            return (boolean) configHodgePodge.getClass().getField(configName).get(configHodgePodge);
        } catch (NoSuchFieldException e) {
            return false;
        } catch (IllegalAccessException e) {
            log.error("Unforeseen changes in hodgepodge. Disabling conflicting patches just in case.");
            return false;
        }
    }
}
