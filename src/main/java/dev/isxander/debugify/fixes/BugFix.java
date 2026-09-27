/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.fixes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BugFix {
    String id();

    FixCategory category();

    Env env();

    boolean enabled() default true;

    String[] modConflicts() default {};

    OS os() default OS.UNKNOWN;

    enum Env {
        CLIENT("neobugify.env.client"),
        SERVER("neobugify.env.server");

        private final String displayName;

        Env(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescriptionKey() {
            return getDisplayName() + ".desc";
        }
    }
}
