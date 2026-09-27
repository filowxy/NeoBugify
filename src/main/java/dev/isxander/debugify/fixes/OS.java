/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.fixes;

import java.util.Locale;

/**
 * Cannot use {@link net.minecraft.Util.OS} because
 * this code needs to be run from a Mixin plugin, where you can't access
 * Minecraft classes.
 */
public enum OS {
    WINDOWS("neobugify.os.windows"),
    MAC("neobugify.os.macos"),
    LINUX("neobugify.os.linux"),
    SOLARIS("neobugify.os.solaris"),
    UNKNOWN("neobugify.os.unknown");

    public static OS getOperatingSystem() {
        String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (string.contains("win")) {
            return OS.WINDOWS;
        } else if (string.contains("mac")) {
            return OS.MAC;
        } else if (string.contains("solaris")) {
            return OS.SOLARIS;
        } else if (string.contains("sunos")) {
            return OS.SOLARIS;
        } else if (string.contains("linux")) {
            return OS.LINUX;
        } else {
            return string.contains("unix") ? OS.LINUX : OS.UNKNOWN;
        }
    }

    private final String displayName;

    OS(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
