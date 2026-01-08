package dev.quentintyr.visiblearmorslots.network;

import dev.quentintyr.visiblearmorslots.action.ActionType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;

public record SlotActionPayload(ActionType actionType,
        EquipmentSlot targetSlot,
        int hotbarSlot,
        boolean isShiftPressed,
        boolean isCtrlPressed,
        boolean isCreativeMode) {

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
        return new SlotActionPayload(actionType, targetSlot, hotbarSlot, isShiftPressed, isCtrlPressed,
                isCreativeMode);
    }
}
