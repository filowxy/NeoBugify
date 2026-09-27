/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.client;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.client.gui.ConfigGuiHelper;
import dev.isxander.debugify.client.gui.NoYACLScreen;
import dev.isxander.debugify.client.utils.BugFixDescriptionCache;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class DebugifyClient {
    public static final String YACL_MOD_ID = "yet_another_config_lib_v3";

    public static BugFixDescriptionCache bugFixDescriptionCache;

    /**
     * Replaces the Fabric "client" entrypoint.
     */
    public static void onClientSetup(FMLClientSetupEvent event) {
        bugFixDescriptionCache = new BugFixDescriptionCache();
        bugFixDescriptionCache.loadDescriptions();
        bugFixDescriptionCache.cacheMissingDescriptions();
    }

    /**
     * Replaces the Fabric "modmenu" entrypoint, NeoForge's mod list screen asks the mod
     * container for an {@link IConfigScreenFactory} instead.
     */
    public static Screen createConfigScreen(ModContainer modContainer, Screen parent) {
        if (!Debugify.isModLoaded(YACL_MOD_ID))
            return new NoYACLScreen(parent);
        return ConfigGuiHelper.createConfigGui(Debugify.CONFIG, parent);
    }

    public static boolean isGameplayFixesEnabled() {
        return Debugify.CONFIG.gameplayFixesInMultiplayer;
    }
}
