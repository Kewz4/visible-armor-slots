package dev.quentintyr.visiblearmorslots;

import dev.quentintyr.visiblearmorslots.network.NetworkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Visiblearmorslots.MOD_ID)
public class Visiblearmorslots {
    public static final String MOD_ID = "visiblearmorslots";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Visiblearmorslots() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("Visible Armor Slots initializing...");
        // Initialize network communication
        NetworkManager.initialize();
        LOGGER.info("Visible Armor Slots initialized!");
    }
}
