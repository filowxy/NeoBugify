/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify;

import dev.isxander.debugify.client.DebugifyClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge entry point, replaces the Fabric "main" and "client" entrypoints of
 * {@code fabric.mod.json}.
 * <p>
 * The bulk of Debugify is wired up by the mixin config plugin, which NeoForge runs while the
 * {@code META-INF/neoforge.mods.toml} declared mixin configs are prepared, long before this
 * class is instantiated. This constructor only covers what the loader itself owns.
 */
@Mod(Debugify.MOD_ID)
public class DebugifyMod {
    public DebugifyMod(IEventBus modEventBus, ModContainer modContainer) {
        Debugify.onInitialize();

        // Replaces the Fabric "client" and "modmenu" entrypoints. DebugifyClient and
        // IConfigScreenFactory both reference net.minecraft.client, so they must never be
        // touched on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(DebugifyClient::onClientSetup);
            // NeoForge's built in mod list screen opens the config screen through this.
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, DebugifyClient::createConfigScreen);
        }
    }
}