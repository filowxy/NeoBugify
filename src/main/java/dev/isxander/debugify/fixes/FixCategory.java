/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.fixes;

public enum FixCategory {
    BASIC("neobugify.basic"),
    GAMEPLAY("neobugify.gameplay");

    private final String displayName;

    FixCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
