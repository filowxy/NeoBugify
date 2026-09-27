/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify;

import dev.isxander.debugify.api.DebugifyApi;
import dev.isxander.debugify.config.DebugifyConfig;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.BugFixData;
import dev.isxander.debugify.mixinplugin.DebugifyErrorHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixins;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Debugify {
    public static final String MOD_ID = "neobugify";

    public static final Logger LOGGER = LoggerFactory.getLogger("NeoBugify");
    public static final DebugifyConfig CONFIG = new DebugifyConfig();

    /**
     * DebugifyApi implementations registered by other mods, keyed by the providing mod id.
     */
    private static final Map<String, DebugifyApi> APIS = new ConcurrentHashMap<>();

    private static String version;

    /**
     * Called from mixin plugin to manage
     * disabled bug fixes
     */
    public static void onPreInitialize() {
        CONFIG.preload();
        Mixins.registerErrorHandlerClass(DebugifyErrorHandler.class.getName());
    }

    public static void onInitialize() {
        List<String> enabledBugs = CONFIG.getBugFixes().entrySet()
                .stream()
                .filter(Map.Entry::getValue)
                .map(entry -> entry.getKey().bugId())
                .toList();
        LOGGER.info("Enabled {} bug fixes: {}", enabledBugs.size(), enabledBugs);
        LOGGER.info("Successfully NeoBugify'd your game!");
    }

    public static BugFix.Env getEnv() {
        return FMLEnvironment.dist == Dist.CLIENT ? BugFix.Env.CLIENT : BugFix.Env.SERVER;
    }

    /**
     * NeoForge port of {@code FabricLoader#getInstance().isModLoaded(String)}.
     * <p>
     * Null safe on purpose: the mixin config plugin runs while the mixin configs are being
     * prepared, which happens before {@link ModList} is populated.
     */
    public static boolean isModLoaded(String id) {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(id);
    }

    /**
     * NeoForge port of {@code FabricLoader#getInstance().getModContainer(id).getMetadata().getName()},
     * used to show a human readable mod name next to conflicting bug fixes.
     */
    public static String getModName(String id) {
        ModList modList = ModList.get();
        if (modList == null) return id;
        return modList.getModContainerById(id)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(id);
    }

    /**
     * NeoForge port of {@code FabricLoader#getInstance().getConfigDir()}.
     */
    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    /**
     * Version of this mod, resolved lazily as this class is first touched by the mixin
     * plugin, way before the mod container exists.
     */
    public static String getVersion() {
        if (version == null) {
            ModList modList = ModList.get();
            version = modList == null ? "unknown" : modList.getModContainerById(MOD_ID)
                    .map(container -> container.getModInfo().getVersion().toString())
                    .orElse("unknown");
        }
        return version;
    }

    /**
     * NeoForge has no equivalent of Fabric's {@code debugify} entrypoint container, so other
     * mods hand their {@link DebugifyApi} over through this method instead, usually from their
     * mod constructor.
     * <p>
     * Conflicts are applied as soon as they are registered. Mixin configs are prepared before
     * any mod is constructed, so a fix whose target class was already transformed can no longer
     * be turned off, but the config screen still reports the conflict and the user can disable
     * the fix by hand.
     */
    public static void registerApi(String modId, DebugifyApi api) {
        APIS.put(modId, api);
        applyApi(modId, api);
    }

    /**
     * Applies every api registered so far, called by the mixin plugin on load.
     */
    public static void applyRegisteredApis() {
        APIS.forEach(Debugify::applyApi);
    }

    private static void applyApi(String modId, DebugifyApi api) {
        for (String bugId : api.getDisabledFixes()) {
            BugFixData.registerApiConflict(modId, bugId);
        }

        api.getProvidedDisabledFixes().forEach((otherModId, bugs) -> {
            if (isModLoaded(otherModId)) {
                bugs.forEach(bugId -> BugFixData.registerApiConflict(otherModId, bugId));
            }
        });
    }
}
