package dev.quentintyr.visiblearmorslots.gui;

import dev.quentintyr.visiblearmorslots.action.ActionType;
import dev.quentintyr.visiblearmorslots.config.ModConfig;
import dev.quentintyr.visiblearmorslots.gui.widget.ArmorSlotWidget;
import dev.quentintyr.visiblearmorslots.gui.widget.OffhandSlotWidget;
import dev.quentintyr.visiblearmorslots.mixin.client.HandledScreenAccessor;
import dev.quentintyr.visiblearmorslots.network.NetworkManager;
import dev.quentintyr.visiblearmorslots.network.SlotActionPayload;
import dev.quentintyr.visiblearmorslots.util.KeyCodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced armor slots overlay system
 */
public class ArmorSlotsOverlay {
    private static final ResourceLocation COLUMN_TEXTURE_FULL = new ResourceLocation(
            "visiblearmorslots", "textures/gui/extra-slots.png");
    private static final ResourceLocation COLUMN_TEXTURE_COMPACT = new ResourceLocation(
            "visiblearmorslots", "textures/gui/extra-slots-no-second-hand.png");
    private static final ResourceLocation COLUMN_TEXTURE_FULL_DARK = new ResourceLocation(
            "visiblearmorslots", "textures/gui/dark-extra-slots.png");
    private static final ResourceLocation COLUMN_TEXTURE_COMPACT_DARK = new ResourceLocation(
            "visiblearmorslots", "textures/gui/dark-extra-slots-no-second-hand.png");

    private final List<ArmorSlotWidget> armorSlots = new ArrayList<>();
    private OffhandSlotWidget offhandSlot;
    private int baseX, baseY;
    private int columnHeight = 100; // 100 with offhand, 78 without

    public int getBaseX() {
        return baseX;
    }

    public int getBaseY() {
        return baseY;
    }

    public int getColumnHeight() {
        return columnHeight;
    }

    private boolean visible = false;

    public void initialize(AbstractContainerScreen<?> screen) {
        if (screen == null) {
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.warn("Attempted to initialize overlay with null screen");
            visible = false;
            return;
        }
        
        if (!ModConfig.getInstance().isEnabled()) {
            visible = false;
            return;
        }

        // Don't show on inventory screen
        if (screen instanceof InventoryScreen) {
            visible = false;
            return;
        }

        // Respect allowed container whitelist
        try {
            MenuType<?> type = screen.getMenu().getType();
            ResourceLocation handlerId = ForgeRegistries.MENU_TYPES.getKey(type);
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.info("Container opened: {} - Allowed: {}", 
                handlerId, ModConfig.getInstance().isContainerAllowed(handlerId));
            if (handlerId != null && !ModConfig.getInstance().isContainerAllowed(handlerId)) {
                visible = false;
                return;
            }
        } catch (Throwable ignored) {
        }

        visible = true;
        columnHeight = ModConfig.getInstance().isShowOffhandSlot() ? 100 : 78;
        calculatePosition(screen);
        createSlots();
    }

    private void calculatePosition(AbstractContainerScreen<?> screen) {
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int screenLeft = accessor.getX();
        int screenTop = accessor.getY();
        int screenHeight = accessor.getBackgroundHeight();

        ModConfig config = ModConfig.getInstance();

        // Start from the chosen side, then apply margins and any auto offset
        if (config.getPositioning() == ModConfig.Side.RIGHT) {
            baseX = screenLeft + accessor.getBackgroundWidth() + config.getMarginX();
        } else {
            baseX = screenLeft - 28 - config.getMarginX();

            // Optional extra shift to avoid potion effects overlay (left side only)
            if (config.isAutoPositioning()) {
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;
                if (player != null && player.hasEffect(MobEffects.REGENERATION)) {
                    baseX -= 24; // shift further left
                }
            }
        }

        // Bottom-align the column
        baseY = screenTop + screenHeight - (columnHeight + 4) + config.getMarginY();
    }

    private void createSlots() {
        armorSlots.clear();

        int itemX = baseX + 4;

        // Create armor slots from top to bottom
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.HELMET, itemX, baseY + 4));
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.CHESTPLATE, itemX, baseY + 22));
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.LEGGINGS, itemX, baseY + 40));
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.BOOTS, itemX, baseY + 58));

        // Create offhand slot only if enabled in config
        if (ModConfig.getInstance().isShowOffhandSlot()) {
            offhandSlot = new OffhandSlotWidget(itemX, baseY + 80);
        } else {
            offhandSlot = null;
        }
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (!visible)
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        // Get fresh inventory reference each frame to ensure real-time updates
        Inventory inventory = player.getInventory();

        // Draw column background (choose texture based on offhand visibility and dark mode)
        boolean offhandShown = offhandSlot != null;
        ModConfig config = ModConfig.getInstance();
        ResourceLocation tex;
        if (config.isDarkMode()) {
            tex = offhandShown ? COLUMN_TEXTURE_FULL_DARK : COLUMN_TEXTURE_COMPACT_DARK;
        } else {
            tex = offhandShown ? COLUMN_TEXTURE_FULL : COLUMN_TEXTURE_COMPACT;
        }
        int texHeight = offhandShown ? 100 : 78;
        drawContext.blit(tex, baseX, baseY, 0, 0, 24, texHeight, 24, texHeight);

        // Render armor slots with fresh data
        for (int i = 0; i < armorSlots.size(); i++) {
            ArmorSlotWidget slot = armorSlots.get(i);
            ItemStack stack = inventory.getArmor(3 - i); // Reverse order: helmet=3, boots=0
            slot.render(drawContext, stack, mouseX, mouseY);

            // Highlight slot if mouse is over it
            if (slot.isMouseOver(mouseX, mouseY)) {
                drawContext.fill(slot.getX(), slot.getY(), slot.getX() + 16, slot.getY() + 16,
                        0x80FFFFFF); // Semi-transparent white overlay
            }
        }

        // Render offhand slot with fresh data if enabled
        if (offhandSlot != null) {
            ItemStack offhandStack = player.getOffhandItem();
            offhandSlot.render(drawContext, offhandStack, mouseX, mouseY);

            if (offhandSlot.isMouseOver(mouseX, mouseY)) {
                drawContext.fill(offhandSlot.getX(), offhandSlot.getY(),
                        offhandSlot.getX() + 16, offhandSlot.getY() + 16,
                        0x80FFFFFF);
            }
        }
    }

    public void renderTooltips(GuiGraphics drawContext, int mouseX, int mouseY) {
        if (!visible || !ModConfig.getInstance().shouldShowTooltips())
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        Inventory inventory = player.getInventory();

        try {
            // Check armor slots for tooltips
            for (int i = 0; i < armorSlots.size(); i++) {
                ArmorSlotWidget slot = armorSlots.get(i);
                if (slot.isMouseOver(mouseX, mouseY)) {
                    ItemStack stack = inventory.getArmor(3 - i);
                    if (!stack.isEmpty()) {
                        drawContext.renderTooltip(mc.font, stack, mouseX, mouseY);
                    } else {
                        // Show empty slot tooltip
                        Component tooltip = Component.translatable("gui.visiblearmorslots.empty." +
                                slot.getSlotType().name().toLowerCase());
                        drawContext.renderTooltip(mc.font, tooltip, mouseX, mouseY);
                    }
                    return;
                }
            }

            // Check offhand slot
            if (offhandSlot != null && offhandSlot.isMouseOver(mouseX, mouseY)) {
                ItemStack offhandStack = player.getOffhandItem();
                if (!offhandStack.isEmpty()) {
                    drawContext.renderTooltip(mc.font, offhandStack, mouseX, mouseY);
                } else {
                    Component tooltip = Component.translatable("gui.visiblearmorslots.empty.offhand");
                    drawContext.renderTooltip(mc.font, tooltip, mouseX, mouseY);
                }
            }
        } catch (Exception e) {
            // Catch tooltip rendering errors to prevent crashes from mod conflicts (e.g., Iceberg)
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.debug("Tooltip rendering failed (likely mod conflict): {}", e.getMessage());
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible)
            return false;

        Minecraft mc = Minecraft.getInstance();

        // Check armor slots
        for (ArmorSlotWidget slot : armorSlots) {
            if (slot.isMouseOver((int) mouseX, (int) mouseY)) {
                handleSlotClick(slot.getSlotType(), button, mc);
                return true;
            }
        }

        // Check offhand slot
        if (offhandSlot != null && offhandSlot.isMouseOver((int) mouseX, (int) mouseY)) {
            handleSlotClick(SlotInfo.SlotType.OFFHAND, button, mc);
            return true;
        }

        // If click is inside overlay column, block vanilla drop logic
        if (mouseX >= baseX && mouseX < baseX + 24 && mouseY >= baseY && mouseY < baseY + columnHeight) {
            return true;
        }

        return false;
    }

    private void handleSlotClick(SlotInfo.SlotType slotType, int button, Minecraft mc) {
        if (mc.player == null || mc.screen == null)
            return;

        boolean isShiftPressed = Screen.hasShiftDown();
        boolean isCtrlPressed = Screen.hasControlDown();

        ActionType actionType = isShiftPressed ? ActionType.QUICK_TRANSFER : ActionType.MOUSE_SWAP;
        sendSlotAction(actionType, slotType.getEquipmentSlot(), -1, isShiftPressed, isCtrlPressed);
    }

    private void sendSlotAction(ActionType actionType, EquipmentSlot targetSlot,
            int hotbarSlot, boolean isShiftPressed, boolean isCtrlPressed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.warn("Cannot send slot action - player is null");
            return;
        }
        
        boolean isCreative = mc.player.getAbilities().instabuild;

        SlotActionPayload payload = new SlotActionPayload(
                actionType, targetSlot, hotbarSlot,
                isShiftPressed, isCtrlPressed, isCreative);

        try {
            NetworkManager.sendToServer(payload);
        } catch (Exception e) {
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.error(
                "Failed to send slot action {}: {}", actionType, e.getMessage()
            );
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible)
            return false;

        Minecraft mc = Minecraft.getInstance();

        // Handle Q key for dropping armor
        if (keyCode == KeyCodes.KEY_Q) {
            // Get mouse position to determine which slot to drop
            double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth()
                    / (double) mc.getWindow().getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight()
                    / (double) mc.getWindow().getScreenHeight();

            // Check which slot the mouse is over
            for (ArmorSlotWidget slot : armorSlots) {
                if (slot.isMouseOver((int) mouseX, (int) mouseY)) {
                    sendSlotAction(ActionType.DROP, slot.getSlotType().getEquipmentSlot(), -1, false, false);
                    return true;
                }
            }

            // Check offhand slot
            if (offhandSlot != null && offhandSlot.isMouseOver((int) mouseX, (int) mouseY)) {
                sendSlotAction(ActionType.DROP, SlotInfo.SlotType.OFFHAND.getEquipmentSlot(), -1, false, false);
                return true;
            }
        }

        // Handle hotbar swapping (keys 1-9)
        if (keyCode >= KeyCodes.KEY_1 && keyCode <= KeyCodes.KEY_9) {
            int hotbarSlot = keyCode - KeyCodes.KEY_1;

            // Get mouse position to determine which slot to swap
            double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth()
                    / (double) mc.getWindow().getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight()
                    / (double) mc.getWindow().getScreenHeight();

            // Check which armor slot the mouse is over
            for (ArmorSlotWidget slot : armorSlots) {
                if (slot.isMouseOver((int) mouseX, (int) mouseY)) {
                    sendSlotAction(ActionType.HOTBAR_SWAP, slot.getSlotType().getEquipmentSlot(), hotbarSlot, false,
                            false);
                    return true;
                }
            }

            // Check offhand slot
            if (offhandSlot != null && offhandSlot.isMouseOver((int) mouseX, (int) mouseY)) {
                sendSlotAction(ActionType.HOTBAR_SWAP, SlotInfo.SlotType.OFFHAND.getEquipmentSlot(), hotbarSlot, false,
                        false);
                return true;
            }
        }

        // Handle F key for offhand swap
        if (keyCode == KeyCodes.KEY_F && offhandSlot != null) {
            sendSlotAction(ActionType.OFFHAND_SWAP, SlotInfo.SlotType.OFFHAND.getEquipmentSlot(), -1, false, false);
            return true;
        }

        return false;
    }

    public boolean isVisible() {
        return visible;
    }
}
