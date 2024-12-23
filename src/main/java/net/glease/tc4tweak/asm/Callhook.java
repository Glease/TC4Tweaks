package net.glease.tc4tweak.asm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.objectweb.asm.ClassVisitor;

/**
 * Marker interface telling IDE method is not unused.
 * <p>
 * For supported IDEs add this annotation to the list of entry point to prevent false unused warnings.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(Callhooks.class)
@interface Callhook {
    Class<? extends ClassVisitor> adder() default ClassVisitor.class;
    ASMConstants.Modules[] module();
}

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@interface Callhooks {
    Callhook[] value();
}
