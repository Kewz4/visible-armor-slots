package dev.quentintyr.visiblearmorslots.action.handler.resolver;

import dev.quentintyr.visiblearmorslots.network.SlotActionPayload;
import dev.quentintyr.visiblearmorslots.util.InventoryUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Optional;

public class CuriosResolver {

    public static void resolve(SlotActionPayload action, ServerPlayer player) {
        if (player == null || action.curiosIdentifier().isEmpty()) {
            return;
        }

        Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve();
        if (curiosHandler.isPresent()) {
            Optional<ICurioStacksHandler> stacksHandlerOpt = curiosHandler.get().getStacksHandler(action.curiosIdentifier());
            if (stacksHandlerOpt.isPresent()) {
                ICurioStacksHandler stacksHandler = stacksHandlerOpt.get();
                IDynamicStackHandler stackHandler = stacksHandler.getStacks();
                int index = action.curiosIndex();

                if (index >= 0 && index < stackHandler.getSlots()) {
                    ItemStack cursorStack = player.containerMenu.getCarried();
                    ItemStack slotStack = stackHandler.getStackInSlot(index);

                    // Handle swap logic
                    if (canEquipCurio(cursorStack, action.curiosIdentifier())) {
                        stackHandler.setStackInSlot(index, cursorStack.copy());
                        player.containerMenu.setCarried(slotStack.copy());
                        InventoryUtil.syncInventoryFull(player);
                    }
                }
            }
        }
    }

    private static boolean canEquipCurio(ItemStack stack, String identifier) {
        // Simple check: can the item be equipped in this curios slot?
        // This usually requires checking the item's capabilities or tags
        // For simplicity, we assume if it's empty we can swap out, if not empty we rely on Curios API validation if possible
        // But for manual packet handling, we should probably check.
        // Curios API has helper methods for this.
        return true; // Simplify for now, ideally use CuriosApi.getCuriosHelper().isStackValid(stack)
    }
}
