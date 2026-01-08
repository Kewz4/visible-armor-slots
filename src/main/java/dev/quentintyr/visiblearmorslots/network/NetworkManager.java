package dev.quentintyr.visiblearmorslots.network;

import dev.quentintyr.visiblearmorslots.action.handler.SlotActionHandler;
import dev.quentintyr.visiblearmorslots.Visiblearmorslots;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NetworkManager {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Visiblearmorslots.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void initialize() {
        int id = 0;
        CHANNEL.registerMessage(id++, SlotActionPayload.class, SlotActionPayload::encode, SlotActionPayload::decode, NetworkManager::handle);
        Visiblearmorslots.LOGGER.info("Network handlers registered successfully");
    }

    private static void handle(SlotActionPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            try {
                SlotActionHandler.handleAction(message, player);
            } catch (Exception e) {
                Visiblearmorslots.LOGGER.error(
                    "Error handling slot action {} for player {}: {}",
                    message.actionType(), player.getName().getString(), e.getMessage(), e
                );
            }
        });
        context.setPacketHandled(true);
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), message);
    }
}
