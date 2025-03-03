package net.glease.tc4tweak.asm;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static net.glease.tc4tweak.asm.LoadingPlugin.hodgepodge;
import static net.glease.tc4tweak.asm.TC4Transformer.log;
import static org.objectweb.asm.Opcodes.*;

final class ASMUtils {
    private static final Configuration configHodgePodge;

    static  {
        Configuration c;
        try {
            if (!hodgepodge) {
                c = null;
            } else {
                c = new Configuration(new File(Launch.minecraftHome, "config/hodgepodge.cfg"));
            }
        } catch (Exception e) {
            log.debug("unable to load hodgepodge.cfg", e);
            c = null;
        }
        configHodgePodge = c;
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
    public static void writeOverwrite(String targetClass, String targetName, MethodVisitor mv, String clazz, String desc) {
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

    /**
     * Write one INVOKESTATIC to redirect the method call to another class. This assumes the stack is properly setup.
     *
     * @param mv          The output destination
     * @param targetClass Internal name of class that the call gets delegated to
     * @param targetName  Name of the method that the call gets delegated to
     * @param clazz       The internal name of the owner class.
     * @param desc        The descriptor of this method.
     * @param append      Additional parameters to add at the end of original method desc
     */
    public static void writeRedirect(MethodVisitor mv, String targetClass, String targetName, String clazz, String origName, String desc, Type... append) {
        String targetDesc;
        if (append == null || append.length == 0) {
            if (clazz == null) {
                targetDesc = desc;
            } else {
                targetDesc = '(' + Type.getObjectType(clazz).getDescriptor() + desc.substring(1);
            }
        } else {
            Type orig = Type.getMethodType(desc);
            List<Type> args = new ArrayList<>();
            if (clazz != null) {
                args.add(Type.getObjectType(clazz));
            }
            Collections.addAll(args, orig.getArgumentTypes());
            Collections.addAll(args, append);
            targetDesc = Type.getMethodDescriptor(orig.getReturnType(), args.toArray(new Type[0]));
        }
        log.trace("Redirecting {}#{}{} to {}#{}{}", clazz, origName, desc, targetClass, targetName, targetDesc);
        mv.visitMethodInsn(INVOKESTATIC, targetClass, targetName, targetDesc, false);
    }

    static <T> T[] arrayAppend(T[] arr, T newLast) {
        T[] out = Arrays.copyOf(arr, arr.length + 1);
        out[arr.length]  = newLast;
        return out;
    }

    static <T> T[] arrayPrepend(T[] arr, T newLast) {
        @SuppressWarnings("unchecked")
        T[] out = (T[]) Array.newInstance(arr.getClass().getComponentType(), arr.length + 1);
        out[0]  = newLast;
        System.arraycopy(arr, 0, out, 1, arr.length);
        return out;
    }

    /**
     *
     * @param configName a boolean field name in HodgePodge LoadingConfig class.
     * @return true if foreign field is true or HodgePodge changed in unforeseen ways, false if foreign field is missing
     * or is false.
     */
    static boolean isHodgepodgeFixActive(String category, String configName, boolean defaultValue) {
        if (configHodgePodge == null) return false;
        return configHodgePodge.getBoolean(configName, category, defaultValue, "");
    }
}
