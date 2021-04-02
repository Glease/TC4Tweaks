package net.glease.tc4tweak.asm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker interface telling IDE method is not unused.
 * <p>
 * For supported IDEs add this annotation to the list of entry point to prevent false unused warnings.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Callhook {
}
