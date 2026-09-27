/*
 * SPDX-License-Identifier: LGPL-3.0-only
 */
package dev.isxander.debugify.client.mixins.basic.mc577;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.isxander.debugify.fixes.BugFix;
import dev.isxander.debugify.fixes.FixCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@BugFix(id = "MC-577", category = FixCategory.BASIC, env = BugFix.Env.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    @Shadow protected abstract void slotClicked(Slot slot, int slotId, int button, ClickType actionType);

    /**
     * Take the hovered slot from the field instead of capturing it as a local. NeoForge
     * recompiles and patches AbstractContainerScreen#mouseClicked, so the local variable table
     * differs from the one the Fabric build was written against and no by-name or by-type
     * capture resolves at the injection point. {@code findSlot} assigns this field before the
     * {@code Util.getMillis} call, so it holds exactly the slot the local used to hold.
     */
    @Shadow protected Slot hoveredSlot;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @ModifyExpressionValue(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(DDI)Z"))
    private boolean shouldReturn(boolean parentMouseClicked, double mouseX, double mouseY, int button) {
        return parentMouseClicked || mouseInventoryClose(button);
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;getMillis()J"), cancellable = true)
    private void dropWithMouse(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (hoveredSlot != null && minecraft.options.keyDrop.matchesMouse(button)) {
            slotClicked(hoveredSlot, hoveredSlot.index, hasControlDown() ? 1 : 0, ClickType.THROW);
            cir.setReturnValue(true);
        }
    }

    @Unique
    private boolean mouseInventoryClose(int button) {
        if (minecraft.options.keyInventory.matchesMouse(button)) {
            onClose();
            return true;
        }

        return false;
    }
}