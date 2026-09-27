/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.client.helpers.mc237493;

import net.minecraft.client.OptionInstance;

public interface DebugifyTelemetryAccessor {
    OptionInstance<DebugifyTelemetry> getTelemetryOption();
}
