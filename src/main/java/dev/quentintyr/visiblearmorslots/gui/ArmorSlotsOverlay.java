package dev.quentintyr.visiblearmorslots.gui;

import dev.quentintyr.visiblearmorslots.action.ActionType;
import dev.quentintyr.visiblearmorslots.config.ModConfig;
import dev.quentintyr.visiblearmorslots.gui.widget.ArmorSlotWidget;
import dev.quentintyr.visiblearmorslots.gui.widget.CuriosSlotWidget;
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
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final List<CuriosSlotWidget> curiosSlots = new ArrayList<>();
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
            if (handlerId != null && !ModConfig.getInstance().isContainerAllowed(handlerId)) {
                visible = false;
                return;
            }
        } catch (Throwable ignored) {
        }

        visible = true;

        // Calculate height based on curios slots
        int curiosCount = getVisibleCuriosCount();
        boolean showOffhand = ModConfig.getInstance().isShowOffhandSlot();

        // Base height for armor (4 slots * 18) + margins (approx 22)
        int baseArmorHeight = 78;
        if (showOffhand) baseArmorHeight = 100;

        columnHeight = baseArmorHeight + (curiosCount * 18);

        calculatePosition(screen);
        createSlots();
    }

    private int getVisibleCuriosCount() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 0;

        final int[] count = {0};
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            handler.getCurios().forEach((id, stackHandler) -> {
                if (stackHandler.isVisible()) {
                    count[0] += stackHandler.getStacks().getSlots();
                }
            });
        });
        return count[0];
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
        curiosSlots.clear();

        int itemX = baseX + 4;
        int currentY = baseY + 4;

        // Create armor slots
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.HELMET, itemX, currentY));
        currentY += 18;
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.CHESTPLATE, itemX, currentY));
        currentY += 18;
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.LEGGINGS, itemX, currentY));
        currentY += 18;
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.BOOTS, itemX, currentY));
        currentY += 18;

        // Create offhand slot
        if (ModConfig.getInstance().isShowOffhandSlot()) {
            offhandSlot = new OffhandSlotWidget(itemX, currentY + 4); // Small gap for offhand separation in texture
            currentY += 22;
        } else {
            offhandSlot = null;
        }

        // Create Curios slots
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            int finalY = currentY;
            CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                int yOffset = finalY;
                Map<String, ICurioStacksHandler> curios = handler.getCurios();
                for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                    ICurioStacksHandler stackHandler = entry.getValue();
                    if (!stackHandler.isVisible()) continue;

                    IDynamicStackHandler stacks = stackHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        // TODO: get icon for slot type if possible
                        ResourceLocation icon = null;
                        // Try to get icon from Curios API registry?
                        // CuriosApi.getIconHelper().getIcon(entry.getKey())?

                        curiosSlots.add(new CuriosSlotWidget(itemX, yOffset, entry.getKey(), i, icon));
                        yOffset += 18;
                    }
                }
            });
        }
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (!visible)
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        // Get fresh inventory reference
        Inventory inventory = player.getInventory();

        // Draw column background
        // For dynamic height, we might need to tile the background or stretch it.
        // The existing textures are fixed size.
        // We can use the top part, tile the middle, and use the bottom part.
        // Or simpler: draw the background repeatedly.
        // Since we don't have a dynamic GUI texture, let's just tile a generic background or use fill for now?
        // Or better: Use the existing texture for the top part (armor) and tile a slot background for curios.

        ModConfig config = ModConfig.getInstance();
        boolean dark = config.isDarkMode();

        // Draw Armor + Offhand background using the existing texture
        boolean showOffhand = offhandSlot != null;
        ResourceLocation baseTex;
        if (dark) {
            baseTex = showOffhand ? COLUMN_TEXTURE_FULL_DARK : COLUMN_TEXTURE_COMPACT_DARK;
        } else {
            baseTex = showOffhand ? COLUMN_TEXTURE_FULL : COLUMN_TEXTURE_COMPACT;
        }

        int baseHeight = showOffhand ? 100 : 78;

        // If we have curios, we need to extend the background.
        // We can draw the top part, then for Curios draw individual slot backgrounds.
        // Actually, the base texture has borders. Tiling might look weird.
        // Let's draw the standard texture first.
        drawContext.blit(baseTex, baseX, baseY, 0, 0, 24, baseHeight, 24, baseHeight);

        // Draw Curios backgrounds
        // We need a texture for a generic slot background.
        // We can reuse a part of the existing texture (e.g. one of the middle slots)
        int curiosStartY = baseY + baseHeight;
        for (CuriosSlotWidget slot : curiosSlots) {
            // Draw a 18x18 slot background (plus borders?). The column is 24 wide.
            // Let's grab a 24x18 slice from the texture (e.g. from y=22 to 40)
            drawContext.blit(baseTex, baseX, slot.getY() - 1, 0, 22, 24, 18, 24, baseHeight);
        }

        // Wait, if we draw curios below, we might need to draw the bottom border of the column at the very end.
        // The current textures include the bottom border.
        // Ideally we would split the texture rendering: Top cap, Middle (repeating), Bottom cap.
        // But we are limited by existing assets.
        // For now, let's just overlay the curios slots.

        // Render armor slots
        for (int i = 0; i < armorSlots.size(); i++) {
            ArmorSlotWidget slot = armorSlots.get(i);
            ItemStack stack = inventory.getArmor(3 - i);
            slot.render(drawContext, stack, mouseX, mouseY);

            if (slot.isMouseOver(mouseX, mouseY)) {
                drawContext.fill(slot.getX(), slot.getY(), slot.getX() + 16, slot.getY() + 16, 0x80FFFFFF);
            }
        }

        // Render offhand slot
        if (offhandSlot != null) {
            ItemStack offhandStack = player.getOffhandItem();
            offhandSlot.render(drawContext, offhandStack, mouseX, mouseY);

            if (offhandSlot.isMouseOver(mouseX, mouseY)) {
                drawContext.fill(offhandSlot.getX(), offhandSlot.getY(), offhandSlot.getX() + 16, offhandSlot.getY() + 16, 0x80FFFFFF);
            }
        }

        // Render Curios slots
        Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve();
        if (curiosHandler.isPresent()) {
            for (CuriosSlotWidget slot : curiosSlots) {
                curiosHandler.get().getStacksHandler(slot.getIdentifier()).ifPresent(stackHandler -> {
                    ItemStack stack = stackHandler.getStacks().getStackInSlot(slot.getIndex());
                    slot.render(drawContext, stack, mouseX, mouseY);

                    if (slot.isMouseOver(mouseX, mouseY)) {
                        drawContext.fill(slot.getX(), slot.getY(), slot.getX() + 16, slot.getY() + 16, 0x80FFFFFF);
                    }
                });
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
            // Armor
            for (int i = 0; i < armorSlots.size(); i++) {
                ArmorSlotWidget slot = armorSlots.get(i);
                if (slot.isMouseOver(mouseX, mouseY)) {
                    ItemStack stack = inventory.getArmor(3 - i);
                    if (!stack.isEmpty()) {
                        drawContext.renderTooltip(mc.font, stack, mouseX, mouseY);
                    } else {
                        Component tooltip = Component.translatable("gui.visiblearmorslots.empty." +
                                slot.getSlotType().name().toLowerCase());
                        drawContext.renderTooltip(mc.font, tooltip, mouseX, mouseY);
                    }
                    return;
                }
            }

            // Offhand
            if (offhandSlot != null && offhandSlot.isMouseOver(mouseX, mouseY)) {
                ItemStack offhandStack = player.getOffhandItem();
                if (!offhandStack.isEmpty()) {
                    drawContext.renderTooltip(mc.font, offhandStack, mouseX, mouseY);
                } else {
                    Component tooltip = Component.translatable("gui.visiblearmorslots.empty.offhand");
                    drawContext.renderTooltip(mc.font, tooltip, mouseX, mouseY);
                }
                return;
            }

            // Curios
            Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve();
            if (curiosHandler.isPresent()) {
                for (CuriosSlotWidget slot : curiosSlots) {
                    if (slot.isMouseOver(mouseX, mouseY)) {
                        curiosHandler.get().getStacksHandler(slot.getIdentifier()).ifPresent(stackHandler -> {
                            ItemStack stack = stackHandler.getStacks().getStackInSlot(slot.getIndex());
                            if (!stack.isEmpty()) {
                                drawContext.renderTooltip(mc.font, stack, mouseX, mouseY);
                            } else {
                                // TODO: Better tooltip for empty curio slot
                                Component tooltip = Component.literal(slot.getIdentifier());
                                drawContext.renderTooltip(mc.font, tooltip, mouseX, mouseY);
                            }
                        });
                        return;
                    }
                }
            }

        } catch (Exception e) {
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.debug("Tooltip rendering failed: {}", e.getMessage());
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

        // Check Curios slots
        for (CuriosSlotWidget slot : curiosSlots) {
            if (slot.isMouseOver((int) mouseX, (int) mouseY)) {
                handleCuriosClick(slot, button, mc);
                return true;
            }
        }

        if (mouseX >= baseX && mouseX < baseX + 24 && mouseY >= baseY && mouseY < baseY + columnHeight) {
            return true;
        }

        return false;
    }

    private void handleSlotClick(SlotInfo.SlotType slotType, int button, Minecraft mc) {
        if (mc.player == null || mc.screen == null) return;
        boolean isShiftPressed = Screen.hasShiftDown();
        boolean isCtrlPressed = Screen.hasControlDown();
        ActionType actionType = isShiftPressed ? ActionType.QUICK_TRANSFER : ActionType.MOUSE_SWAP;
        sendSlotAction(actionType, slotType.getEquipmentSlot(), -1, isShiftPressed, isCtrlPressed, "", 0);
    }

    private void handleCuriosClick(CuriosSlotWidget slot, int button, Minecraft mc) {
        if (mc.player == null || mc.screen == null) return;
        boolean isShiftPressed = Screen.hasShiftDown();
        boolean isCtrlPressed = Screen.hasControlDown();
        // For curios, we treat it similarly to MOUSE_SWAP, but target is null and identifier is set
        ActionType actionType = isShiftPressed ? ActionType.QUICK_TRANSFER : ActionType.MOUSE_SWAP;
        // NOTE: Quick transfer for curios needs backend support, currently MouseSwapResolver handles specific slots.
        // We need CuriosResolver to handle MOUSE_SWAP logic.

        sendSlotAction(actionType, null, -1, isShiftPressed, isCtrlPressed, slot.getIdentifier(), slot.getIndex());
    }

    private void sendSlotAction(ActionType actionType, EquipmentSlot targetSlot,
            int hotbarSlot, boolean isShiftPressed, boolean isCtrlPressed, String curiosId, int curiosIndex) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isCreative = mc.player.getAbilities().instabuild;

        SlotActionPayload payload = new SlotActionPayload(
                actionType, targetSlot, hotbarSlot,
                isShiftPressed, isCtrlPressed, isCreative, curiosId, curiosIndex);

        try {
            NetworkManager.sendToServer(payload);
        } catch (Exception e) {
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.error(
                "Failed to send slot action {}: {}", actionType, e.getMessage()
            );
        }
    }

    // Overload for backward compatibility / cleaner calls
    private void sendSlotAction(ActionType actionType, EquipmentSlot targetSlot,
            int hotbarSlot, boolean isShiftPressed, boolean isCtrlPressed) {
        sendSlotAction(actionType, targetSlot, hotbarSlot, isShiftPressed, isCtrlPressed, "", 0);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible)
            return false;

        Minecraft mc = Minecraft.getInstance();

        if (keyCode == KeyCodes.KEY_Q) {
            double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth()
                    / (double) mc.getWindow().getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight()
                    / (double) mc.getWindow().getScreenHeight();

            for (ArmorSlotWidget slot : armorSlots) {
                if (slot.isMouseOver((int) mouseX, (int) mouseY)) {
                    sendSlotAction(ActionType.DROP, slot.getSlotType().getEquipmentSlot(), -1, false, false);
                    return true;
                }
            }

            if (offhandSlot != null && offhandSlot.isMouseOver((int) mouseX, (int) mouseY)) {
                sendSlotAction(ActionType.DROP, SlotInfo.SlotType.OFFHAND.getEquipmentSlot(), -1, false, false);
                return true;
            }
            // TODO: Curios drop support
        }

        if (keyCode >= KeyCodes.KEY_1 && keyCode <= KeyCodes.KEY_9) {
            int hotbarSlot = keyCode - KeyCodes.KEY_1;
            double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth()
                    / (double) mc.getWindow().getScreenWidth();
            double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight()
                    / (double) mc.getWindow().getScreenHeight();

            for (ArmorSlotWidget slot : armorSlots) {
                if (slot.isMouseOver((int) mouseX, (int) mouseY)) {
                    sendSlotAction(ActionType.HOTBAR_SWAP, slot.getSlotType().getEquipmentSlot(), hotbarSlot, false,
                            false);
                    return true;
                }
            }

            if (offhandSlot != null && offhandSlot.isMouseOver((int) mouseX, (int) mouseY)) {
                sendSlotAction(ActionType.HOTBAR_SWAP, SlotInfo.SlotType.OFFHAND.getEquipmentSlot(), hotbarSlot, false,
                        false);
                return true;
            }
        }

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
