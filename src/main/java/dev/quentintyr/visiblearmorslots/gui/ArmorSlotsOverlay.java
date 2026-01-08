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
    private static final ResourceLocation CURIOS_INVENTORY_TEXTURE = new ResourceLocation(
            "curios", "textures/gui/inventory.png");

    private final List<ArmorSlotWidget> armorSlots = new ArrayList<>();
    private final List<CuriosSlotWidget> curiosSlots = new ArrayList<>();
    private OffhandSlotWidget offhandSlot;
    private int baseX, baseY;
    private int columnHeight = 100;

    private boolean showCuriosGrid = false;
    private int curiosButtonX, curiosButtonY;
    private boolean hasCurios = false;

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

        if (screen instanceof InventoryScreen) {
            visible = false;
            return;
        }

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

        boolean showOffhand = ModConfig.getInstance().isShowOffhandSlot();
        columnHeight = showOffhand ? 100 : 78;

        // Add extra space for Curios toggle button
        hasCurios = getVisibleCuriosCount() > 0;

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

        if (config.getPositioning() == ModConfig.Side.RIGHT) {
            baseX = screenLeft + accessor.getBackgroundWidth() + config.getMarginX();
        } else {
            baseX = screenLeft - 28 - config.getMarginX();

            if (config.isAutoPositioning()) {
                Minecraft mc = Minecraft.getInstance();
                Player player = mc.player;
                if (player != null && player.hasEffect(MobEffects.REGENERATION)) {
                    baseX -= 24;
                }
            }
        }

        baseY = screenTop + screenHeight - (columnHeight + 4) + config.getMarginY();
    }

    private void createSlots() {
        armorSlots.clear();
        curiosSlots.clear();

        int itemX = baseX + 4;
        int currentY = baseY + 4;

        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.HELMET, itemX, currentY));
        currentY += 18;
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.CHESTPLATE, itemX, currentY));
        currentY += 18;
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.LEGGINGS, itemX, currentY));
        currentY += 18;
        armorSlots.add(new ArmorSlotWidget(SlotInfo.SlotType.BOOTS, itemX, currentY));
        currentY += 18;

        if (ModConfig.getInstance().isShowOffhandSlot()) {
            offhandSlot = new OffhandSlotWidget(itemX, currentY + 4);
            currentY += 22;
        } else {
            offhandSlot = null;
        }

        if (hasCurios) {
            curiosButtonX = itemX + 3; // Center roughly
            curiosButtonY = currentY + 2;

            // Calculate grid for curios slots
            ModConfig config = ModConfig.getInstance();
            int gridXOffset = (config.getPositioning() == ModConfig.Side.RIGHT) ? 28 : -20;

            Player player = Minecraft.getInstance().player;
            if (player != null) {
                int startX = baseX + gridXOffset;
                int startY = baseY + 4; // Align top

                final int[] slotCounter = {0};
                final int[] col = {0};
                final int[] row = {0};

                int maxRows = 4 + (offhandSlot != null ? 1 : 0); // Match height roughly

                CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                    Map<String, ICurioStacksHandler> curios = handler.getCurios();
                    for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
                        ICurioStacksHandler stackHandler = entry.getValue();
                        if (!stackHandler.isVisible()) continue;

                        IDynamicStackHandler stacks = stackHandler.getStacks();
                        for (int i = 0; i < stacks.getSlots(); i++) {
                            ResourceLocation icon = CuriosApi.getIconHelper().getIcon(entry.getKey());

                            // Grid logic: Fill downwards, then move sideways (away from column)
                            int xPos = startX;
                            if (config.getPositioning() == ModConfig.Side.RIGHT) {
                                xPos += col[0] * 18;
                            } else {
                                xPos -= col[0] * 18;
                            }
                            int yPos = startY + (row[0] * 18);

                            curiosSlots.add(new CuriosSlotWidget(xPos, yPos, entry.getKey(), i, icon));

                            row[0]++;
                            if (row[0] >= maxRows) {
                                row[0] = 0;
                                col[0]++;
                            }
                        }
                    }
                });
            }
        }
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        if (!visible)
            return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null)
            return;

        Inventory inventory = player.getInventory();

        ModConfig config = ModConfig.getInstance();
        boolean dark = config.isDarkMode();
        boolean showOffhand = offhandSlot != null;
        ResourceLocation baseTex;
        if (dark) {
            baseTex = showOffhand ? COLUMN_TEXTURE_FULL_DARK : COLUMN_TEXTURE_COMPACT_DARK;
        } else {
            baseTex = showOffhand ? COLUMN_TEXTURE_FULL : COLUMN_TEXTURE_COMPACT;
        }

        // Draw Main Column Background
        int texHeight = showOffhand ? 100 : 78;
        drawContext.blit(baseTex, baseX, baseY, 0, 0, 24, texHeight, 24, texHeight);

        // Draw Curios Button if present
        if (hasCurios) {
            // Draw button icon. Gold icon is at 24, 10
            int u = 24;
            int v = 10;
            drawContext.blit(CURIOS_INVENTORY_TEXTURE, curiosButtonX, curiosButtonY, u, v, 9, 9, 256, 256);
        }

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

        // Render Curios Grid
        if (showCuriosGrid && hasCurios) {
            Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve();
            if (curiosHandler.isPresent()) {
                for (CuriosSlotWidget slot : curiosSlots) {
                    // Draw slot background using the Curios texture standalone slot (at 39, 0)
                    // The slot background is 18x18
                    drawContext.blit(CURIOS_INVENTORY_TEXTURE, slot.getX() - 1, slot.getY() - 1, 39, 0, 18, 18, 256, 256);

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

            // Curios Button Tooltip
            if (hasCurios && mouseX >= curiosButtonX && mouseX < curiosButtonX + 9 &&
                mouseY >= curiosButtonY && mouseY < curiosButtonY + 9) {
                // Optional tooltip for button
                return;
            }

            // Curios Slots
            if (showCuriosGrid && hasCurios) {
                Optional<ICuriosItemHandler> curiosHandler = CuriosApi.getCuriosHelper().getCuriosHandler(player).resolve();
                if (curiosHandler.isPresent()) {
                    for (CuriosSlotWidget slot : curiosSlots) {
                        if (slot.isMouseOver(mouseX, mouseY)) {
                            curiosHandler.get().getStacksHandler(slot.getIdentifier()).ifPresent(stackHandler -> {
                                ItemStack stack = stackHandler.getStacks().getStackInSlot(slot.getIndex());
                                if (!stack.isEmpty()) {
                                    drawContext.renderTooltip(mc.font, stack, mouseX, mouseY);
                                } else {
                                    // Use proper translation key
                                    Component tooltip = Component.translatable("curios.identifier." + slot.getIdentifier());
                                    drawContext.renderTooltip(mc.font, tooltip, mouseX, mouseY);
                                }
                            });
                            return;
                        }
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

        // Check Curios Button
        if (hasCurios && mouseX >= curiosButtonX && mouseX < curiosButtonX + 9 &&
            mouseY >= curiosButtonY && mouseY < curiosButtonY + 9) {
            showCuriosGrid = !showCuriosGrid;
            mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        // Check Curios slots
        if (showCuriosGrid && hasCurios) {
            for (CuriosSlotWidget slot : curiosSlots) {
                if (slot.isMouseOver((int) mouseX, (int) mouseY)) {
                    handleCuriosClick(slot, button, mc);
                    return true;
                }
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
        ActionType actionType = isShiftPressed ? ActionType.QUICK_TRANSFER : ActionType.MOUSE_SWAP;
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
