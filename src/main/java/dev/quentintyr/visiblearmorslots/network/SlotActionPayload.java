package dev.quentintyr.visiblearmorslots.network;

import dev.quentintyr.visiblearmorslots.action.ActionType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;

public record SlotActionPayload(ActionType actionType,
        EquipmentSlot targetSlot,
        int hotbarSlot,
        boolean isShiftPressed,
        boolean isCtrlPressed,
        boolean isCreativeMode,
        String curiosIdentifier,
        int curiosIndex) {

    // Helper constructor for non-curios actions
    public SlotActionPayload(ActionType actionType, EquipmentSlot targetSlot, int hotbarSlot,
                             boolean isShiftPressed, boolean isCtrlPressed, boolean isCreativeMode) {
        this(actionType, targetSlot, hotbarSlot, isShiftPressed, isCtrlPressed, isCreativeMode, "", 0);
    }

    public static void encode(SlotActionPayload payload, FriendlyByteBuf buf) {
        buf.writeEnum(payload.actionType());
        buf.writeBoolean(payload.targetSlot() != null);
        if (payload.targetSlot() != null) {
            buf.writeEnum(payload.targetSlot());
        }
        buf.writeVarInt(payload.hotbarSlot());
        buf.writeBoolean(payload.isShiftPressed());
        buf.writeBoolean(payload.isCtrlPressed());
        buf.writeBoolean(payload.isCreativeMode());
        buf.writeUtf(payload.curiosIdentifier());
        buf.writeVarInt(payload.curiosIndex());
    }

    public static SlotActionPayload decode(FriendlyByteBuf buf) {
        ActionType actionType = buf.readEnum(ActionType.class);
        EquipmentSlot targetSlot = null;
        if (buf.readBoolean()) {
            targetSlot = buf.readEnum(EquipmentSlot.class);
        }
        int hotbarSlot = buf.readVarInt();
        boolean isShiftPressed = buf.readBoolean();
        boolean isCtrlPressed = buf.readBoolean();
        boolean isCreativeMode = buf.readBoolean();
        String curiosIdentifier = buf.readUtf();
        int curiosIndex = buf.readVarInt();

        return new SlotActionPayload(actionType, targetSlot, hotbarSlot, isShiftPressed, isCtrlPressed,
                isCreativeMode, curiosIdentifier, curiosIndex);
    }
}
