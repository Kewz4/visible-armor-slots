package dev.quentintyr.visiblearmorslots;

import dev.quentintyr.visiblearmorslots.config.ConfigScreen;
import dev.quentintyr.visiblearmorslots.config.ModConfig;
import dev.quentintyr.visiblearmorslots.gui.ArmorSlotsOverlay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Visiblearmorslots.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    private static ArmorSlotsOverlay armorSlotsOverlay;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        Visiblearmorslots.LOGGER.info("Visible Armor Slots Client initializing...");

        // Load (or create) JSON config
        ModConfig.load();
        ModConfig.save(); // ensure file exists with defaults if first run

        // Initialize overlay
        armorSlotsOverlay = new ArmorSlotsOverlay();

        // Register Config Screen
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new ConfigScreen(screen)));

        Visiblearmorslots.LOGGER.info("Visible Armor Slots Client initialized!");
    }

    public static ArmorSlotsOverlay getArmorSlotsOverlay() {
        return armorSlotsOverlay;
    }
}
