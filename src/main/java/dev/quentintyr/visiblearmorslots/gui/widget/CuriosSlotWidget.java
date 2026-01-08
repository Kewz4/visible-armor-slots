package dev.quentintyr.visiblearmorslots.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class CuriosSlotWidget {
    private final int x;
    private final int y;
    private final String identifier;
    private final int index;
    private final ResourceLocation icon;

    public CuriosSlotWidget(int x, int y, String identifier, int index, ResourceLocation icon) {
        this.x = x;
        this.y = y;
        this.identifier = identifier;
        this.index = index;
        this.icon = icon;
    }

    public void render(GuiGraphics context, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty() && icon != null) {
            // Render the default icon (outline) with transparency
            context.blit(icon, x, y, 0, 0, 16, 16, 16, 16);
        } else {
            context.renderItem(stack, x, y);
            context.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
        }
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public String getIdentifier() { return identifier; }
    public int getIndex() { return index; }
}
