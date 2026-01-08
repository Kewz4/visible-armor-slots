package dev.quentintyr.visiblearmorslots.mixin.client;

import dev.quentintyr.visiblearmorslots.ClientSetup;
import dev.quentintyr.visiblearmorslots.gui.ArmorSlotsOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {

    @Unique
    private Boolean vas$lastRecipeOpen = null;

    @Unique
    private boolean vas$isRecipeBookOpen() {
        Object self = this;
        // Preferred: use the mapped interface when available (stable under remap)
        if (self instanceof RecipeUpdateListener provider) {
            try {
                return provider.getRecipeBookComponent().isVisible();
            } catch (Throwable ignored) {
            }
        }
        // Fallback: reflection for unexpected screens that expose the widget
        try {
            // Try calling getRecipeBookComponent().isVisible() via reflection
            java.lang.reflect.Method getWidget = self.getClass().getMethod("getRecipeBookComponent");
            Object widget = getWidget.invoke(self);
            if (widget != null) {
                java.lang.reflect.Method isOpen = widget.getClass().getMethod("isVisible");
                Object result = isOpen.invoke(widget);
                if (result instanceof Boolean b)
                    return b;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
    private void onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof InventoryScreen) {
            return;
        }

        // Refresh overlay when recipe book toggles
        boolean openNow = vas$isRecipeBookOpen();
        if (vas$lastRecipeOpen == null || vas$lastRecipeOpen != openNow) {
            vas$lastRecipeOpen = openNow;
            ArmorSlotsOverlay overlayRef = ClientSetup.getArmorSlotsOverlay();
            if (overlayRef != null) {
                overlayRef.initialize((AbstractContainerScreen<?>) (Object) this);
            }
        }

        // Do not render overlay while recipe book is open
        if (openNow) {
            return;
        }

        ArmorSlotsOverlay overlay = ClientSetup.getArmorSlotsOverlay();
        if (overlay != null && overlay.isVisible()) {
            overlay.render(context, mouseX, mouseY, delta);
            overlay.renderTooltips(context, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            return;
        }

        // While recipe book is open, ignore overlay interactions
        if (vas$isRecipeBookOpen()) {
            return;
        }

        ArmorSlotsOverlay overlay = ClientSetup.getArmorSlotsOverlay();
        if (overlay != null && overlay.mouseClicked(mouseX, mouseY, button)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseReleased(DDI)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            return;
        }

        if (vas$isRecipeBookOpen()) {
            return;
        }

        ArmorSlotsOverlay overlay = ClientSetup.getArmorSlotsOverlay();
        if (overlay != null && overlay.isVisible()) {
            // Block mouse release events over overlay to prevent drops
            if (mouseX >= overlay.getBaseX() && mouseX < overlay.getBaseX() + 24 &&
                    mouseY >= overlay.getBaseY() && mouseY < overlay.getBaseY() + overlay.getColumnHeight()) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

    @Inject(method = "keyPressed(III)Z", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof InventoryScreen) {
            return;
        }

        if (vas$isRecipeBookOpen()) {
            return;
        }

        ArmorSlotsOverlay overlay = ClientSetup.getArmorSlotsOverlay();
        if (overlay != null && overlay.keyPressed(keyCode, scanCode, modifiers)) {
            cir.setReturnValue(true);
        }
    }
}
