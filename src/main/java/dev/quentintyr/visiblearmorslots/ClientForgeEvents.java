package dev.quentintyr.visiblearmorslots;

import dev.quentintyr.visiblearmorslots.gui.ArmorSlotsOverlay;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Visiblearmorslots.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> handledScreen) {
             ArmorSlotsOverlay overlay = ClientSetup.getArmorSlotsOverlay();
             if (overlay != null) {
                overlay.initialize(handledScreen);
             }
        }
    }
}
