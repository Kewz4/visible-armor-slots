package dev.quentintyr.visiblearmorslots.action.handler.resolver;

import dev.quentintyr.visiblearmorslots.network.SlotActionPayload;
import dev.quentintyr.visiblearmorslots.util.InventoryUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles shift-click actions to move items to inventory
 */
public class QuickTransferResolver {

    public static void resolve(SlotActionPayload action, ServerPlayer player) {
        if (player == null || player.getInventory() == null) {
            return;
        }
        
        EquipmentSlot targetSlot = action.targetSlot();
        if (targetSlot == null)
            return;

        ItemStack equipped = player.getItemBySlot(targetSlot);
        if (equipped.isEmpty())
            return;

        // Try to move equipped item to main inventory
        if (insertIntoMainInventory(player, equipped)) {
            player.setItemSlot(targetSlot, ItemStack.EMPTY);

            // Force inventory sync to client
            InventoryUtil.syncInventory(player);
        }
    }

    private static boolean insertIntoMainInventory(ServerPlayer player, ItemStack stack) {
        // Try to insert into player's main inventory (slots 0-35)
        for (int i = 9; i < 36; i++) { // Skip hotbar, start with main inventory
            ItemStack slotStack = player.getInventory().getItem(i);
            if (slotStack.isEmpty()) {
                player.getInventory().setItem(i, stack.copy());
                return true;
            } else if (slotStack.getItem() == stack.getItem()
                    && slotStack.getCount() < slotStack.getMaxStackSize()) {
                int remaining = slotStack.getMaxStackSize() - slotStack.getCount();
                if (remaining >= stack.getCount()) {
                    slotStack.grow(stack.getCount());
                    return true;
                }
            }
        }

        // Try hotbar if main inventory is full
        for (int i = 0; i < 9; i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (slotStack.isEmpty()) {
                player.getInventory().setItem(i, stack.copy());
                return true;
            }
        }

        return false; // No space available
    }
}
