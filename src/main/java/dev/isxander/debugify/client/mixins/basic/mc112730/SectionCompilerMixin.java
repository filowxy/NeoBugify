/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.client.mixins.basic.mc112730;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

import net.minecraft.world.level.block.entity.BlockEntity;

@BugFix(id = "MC-112730", category = FixCategory.BASIC, env = BugFix.Env.CLIENT)
@Mixin(SectionCompiler.class)
public class SectionCompilerMixin {

    @WrapWithCondition(method = "handleBlockEntity", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
    private <E extends BlockEntity> boolean shouldAddToLocalList(List<E> instance, Object e, @Local BlockEntityRenderer<E> renderer) {
        E blockEntity = (E) e;
        return !renderer.shouldRenderOffScreen(blockEntity);
    }
}
