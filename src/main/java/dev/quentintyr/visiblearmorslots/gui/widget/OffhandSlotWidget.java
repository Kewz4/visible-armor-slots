package dev.quentintyr.visiblearmorslots.gui.widget;

import dev.quentintyr.visiblearmorslots.gui.SlotInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Custom slot widget for off-hand items
 */
public class OffhandSlotWidget {
    private final int x;
    private final int y;

    private static final ResourceLocation EMPTY_OFFHAND_SLOT = new ResourceLocation("minecraft", "textures/item/empty_armor_slot_shield.png");

    public OffhandSlotWidget(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void render(GuiGraphics context, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty()) {
            // Draw shield icon for empty off-hand slot
            context.blit(EMPTY_OFFHAND_SLOT, x, y, 0, 0, 16, 16, 16, 16);
        } else {
            // Draw item with count
            context.renderItem(stack, x, y);
            context.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
        }
    }

    public boolean canAcceptItem(ItemStack stack) {
        // Off-hand has no item type restrictions
        return true;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public SlotInfo.SlotType getSlotType() {
        return SlotInfo.SlotType.OFFHAND;
    }
}
