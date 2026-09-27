/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.client.mixins.basic.mc93384;

import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * MC-93384: submerged bubble particles spawn at the block the entity's feet are in instead of
 * at eye level, so they show up in mid water rather than on its surface.
 * <p>
 * NeoForge relocates that spawn out of {@code LivingEntity#baseTick} and into
 * {@code Entity#doWaterSplashEffect}, so the mixin follows it there. The target is an
 * {@link Entity} again, hence the checked self reference for the eye height.
 */
@BugFix(id = "MC-93384", category = FixCategory.BASIC, env = BugFix.Env.CLIENT)
@Mixin(Entity.class)
public abstract class LivingEntityMixin {
    @ModifyArg(
            method = "doWaterSplashEffect",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", ordinal = 0),
            index = 2
    )
    private double modifyY(double y) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living)
            return y + living.getEyeHeight();
        return y;
    }
}