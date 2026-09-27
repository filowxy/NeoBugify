/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.mixinplugin;

public class DebugifyDebugFlags {
    public static final boolean FORCE_LINUX_FIXES = boolProp("neobugify.forceLinuxFixes");
    public static final boolean FORCE_WINDOWS_FIXES = boolProp("neobugify.forceWindowsFixes");
    public static final boolean FORCE_MACOS_FIXES = boolProp("neobugify.forceMacFixes");

    private static boolean boolProp(String property) {
        return Boolean.parseBoolean(System.getProperty(property));
    }
}
