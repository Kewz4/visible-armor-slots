package dev.quentintyr.visiblearmorslots.action.handler.resolver;

import dev.quentintyr.visiblearmorslots.network.SlotActionPayload;
import dev.quentintyr.visiblearmorslots.util.InventoryUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles dropping equipped armor items (Q key)
 */
public class DropResolver {

    public static void resolve(SlotActionPayload action, ServerPlayer player) {
        if (player == null) {
            return;
        }
        
        EquipmentSlot targetSlot = action.targetSlot();
        if (targetSlot == null)
            return;

        ItemStack equipped = player.getItemBySlot(targetSlot);
        if (equipped.isEmpty())
            return;

        // Drop the equipped item into the world
        player.drop(equipped, false);

        // Clear the equipment slot
        player.setItemSlot(targetSlot, ItemStack.EMPTY);

        // Force inventory sync to client
        InventoryUtil.syncInventory(player);
    }
}
