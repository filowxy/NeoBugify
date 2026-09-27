/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.mixins.basic.mc121706;

import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@BugFix(id = "MC-121706", env = BugFix.Env.SERVER, category = FixCategory.BASIC)
@Mixin(RangedBowAttackGoal.class)
public abstract class RangedBowAttackGoalMixin {
    // Shadowed as Mob, not as the target's T (bounded by Mob & RangedAttackMob): NeoForge
    // runs on official mappings and has no refmap to widen the shadowed descriptor, so the
    // erased type has to match exactly.
    @Shadow @Final private Mob mob;

    // NeoForge patches lookAt(Entity, float, float) out of LivingEntity and into Mob, which
    // is also the erased type of the target's T mob field, so that is the call site owner.
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;lookAt(Lnet/minecraft/world/entity/Entity;FF)V", shift = At.Shift.AFTER))
    private void lookAtTarget(CallbackInfo ci) {
        mob.getLookControl().setLookAt(mob.getTarget(), 30f, 30f);
    }
}
