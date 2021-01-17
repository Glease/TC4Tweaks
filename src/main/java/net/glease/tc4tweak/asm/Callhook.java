package net.glease.tc4tweak.asm;

import java.lang.annotation.*;

/**
 * Marker interface telling IDE method is not unused
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Callhook {
}
