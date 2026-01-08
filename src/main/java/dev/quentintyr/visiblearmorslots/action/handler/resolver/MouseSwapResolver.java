package dev.quentintyr.visiblearmorslots.action.handler.resolver;

import dev.quentintyr.visiblearmorslots.network.SlotActionPayload;
import dev.quentintyr.visiblearmorslots.util.InventoryUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles mouse click actions (left/right click swapping)
 */
public class MouseSwapResolver {

    public static void resolve(SlotActionPayload action, ServerPlayer player) {
        if (player == null || player.containerMenu == null) {
            return;
        }
        
        EquipmentSlot targetSlot = action.targetSlot();
        if (targetSlot == null)
            return;

        // Get current item in equipment slot (supports OFFHAND too)
        ItemStack currentEquipped = player.getItemBySlot(targetSlot);

        // Get cursor item
        ItemStack cursorStack = player.containerMenu.getCarried();

        // Validate that the cursor item can be equipped in this slot
        if (!canEquipInSlot(cursorStack, targetSlot)) {
            return; // Invalid item for this slot - do nothing
        }

        // Perform the swap (same logic for both creative and survival)
        performSwap(player, targetSlot, currentEquipped, cursorStack);
    }

    private static boolean canEquipInSlot(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty()) {
            return true; // Can always remove items
        }

        // Check if it's armor and matches the slot
        if (stack.getItem() instanceof ArmorItem armorItem) {
            return armorItem.getEquipmentSlot() == slot;
        }

        // Allow any item in offhand
        return slot == EquipmentSlot.OFFHAND;
    }

    private static void performSwap(ServerPlayer player, EquipmentSlot slot,
            ItemStack equipped, ItemStack cursor) {
        // Equip the new item (or clear the slot if cursor is empty)
        player.setItemSlot(slot, cursor.copy());

        // Put the previously equipped item on the cursor
        player.containerMenu.setCarried(equipped.copy());

        // Force inventory sync to client
        InventoryUtil.syncInventoryFull(player);
    }
}
