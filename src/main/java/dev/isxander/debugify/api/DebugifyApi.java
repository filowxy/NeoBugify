/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.api;

import java.util.Map;
import java.util.Set;

/**
 * Lets other mods turn Debugify bug fixes off.
 * <p>
 * On Fabric this used to be picked up from the {@code debugify} entrypoint container. NeoForge
 * has no equivalent, so implementations are handed to
 * {@link dev.isxander.debugify.Debugify#registerApi(String, DebugifyApi)} instead, normally from
 * the providing mod's constructor.
 */
public interface DebugifyApi {
    /**
     * Returns an array of bug ids (e.g. MC-577) to disable due to this mod
     */
    default String[] getDisabledFixes() {
        return new String[0];
    }

    /**
     * Provides a map of mod id to a set of bug ids to disable fixes
     * due to other mods.
     */
    default Map<String, Set<String>> getProvidedDisabledFixes() {
        return Map.of();
    }
}
