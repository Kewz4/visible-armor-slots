package dev.quentintyr.visiblearmorslots.action.handler;

import dev.quentintyr.visiblearmorslots.action.handler.resolver.DropResolver;
import dev.quentintyr.visiblearmorslots.action.handler.resolver.HotbarSwapResolver;
import dev.quentintyr.visiblearmorslots.action.handler.resolver.MouseSwapResolver;
import dev.quentintyr.visiblearmorslots.action.handler.resolver.OffhandSwapResolver;
import dev.quentintyr.visiblearmorslots.action.handler.resolver.QuickTransferResolver;
import dev.quentintyr.visiblearmorslots.network.SlotActionPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * Central processor for slot actions using chain of responsibility pattern
 */
public class SlotActionHandler {

    public static void handleAction(SlotActionPayload action, ServerPlayer player) {
        switch (action.actionType()) {
            case MOUSE_SWAP -> MouseSwapResolver.resolve(action, player);
            case QUICK_TRANSFER -> QuickTransferResolver.resolve(action, player);
            case HOTBAR_SWAP -> HotbarSwapResolver.resolve(action, player);
            case OFFHAND_SWAP -> OffhandSwapResolver.resolve(action, player);
            case DROP -> DropResolver.resolve(action, player);
        }
    }
}
