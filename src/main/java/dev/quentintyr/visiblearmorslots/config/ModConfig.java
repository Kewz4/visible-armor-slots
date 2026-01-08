package dev.quentintyr.visiblearmorslots.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Configuration options for the mod (JSON-backed)
 */
public class ModConfig {

    public enum Side {
        LEFT, RIGHT
    }

    private Side positioning = Side.LEFT;
    private int marginX = 4;
    private int marginY = 0;
    private boolean enabled = true;
    private boolean showTooltips = true;
    private boolean autoPositioning = true;
    private boolean showOffhandSlot = true; 
    private boolean darkMode = false; 
    private Set<String> allowedContainers = new HashSet<>();

    private static final String FILE_NAME = "visiblearmorslots.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CONFIG_VERSION = 2; 
    
    private static ModConfig instance;

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }

    private ModConfig() {
        // Default screen handler IDs to show overlay on
        Collections.addAll(allowedContainers,
                "minecraft:crafting",
                "minecraft:anvil",
                "minecraft:enchantment",
                "minecraft:generic_9x3",
                "minecraft:generic_9x6",
                "minecraft:grindstone",
                "minecraft:smithing");
    }

    public Side getPositioning() {
        return positioning;
    }

    public int getMarginX() {
        return marginX;
    }

    public int getMarginY() {
        return marginY;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean shouldShowTooltips() {
        return showTooltips;
    }

    public boolean isAutoPositioning() {
        return autoPositioning;
    }

    public boolean isShowOffhandSlot() {
        return showOffhandSlot;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public Set<String> getAllowedContainers() {
        return Collections.unmodifiableSet(allowedContainers);
    }

    public void setPositioning(Side positioning) {
        this.positioning = positioning;
    }

    public void setMarginX(int marginX) {
        this.marginX = Math.max(0, Math.min(128, marginX));
    }

    public void setMarginY(int marginY) {
        this.marginY = Math.max(-64, Math.min(64, marginY));
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setShowTooltips(boolean showTooltips) {
        this.showTooltips = showTooltips;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public void setAutoPositioning(boolean autoPositioning) {
        this.autoPositioning = autoPositioning;
    }

    public void setShowOffhandSlot(boolean showOffhandSlot) {
        this.showOffhandSlot = showOffhandSlot;
    }

    public boolean isContainerAllowed(ResourceLocation id) {
        if (allowedContainers.isEmpty())
            return true; // empty means allow all
        return allowedContainers.contains(id.toString());
    }
    
    /**
     * Check if a container is in the allowed list (not checking if empty = allow all)
     */
    public boolean isContainerInList(String containerId) {
        return allowedContainers.contains(containerId);
    }
    
    /**
     * Enable or disable a specific container
     */
    public void setContainerEnabled(String containerId, boolean enabled) {
        if (enabled) {
            allowedContainers.add(containerId);
        } else {
            allowedContainers.remove(containerId);
        }
    }
    
    /**
     * Reset containers to default list
     */
    public void resetContainersToDefault() {
        allowedContainers.clear();
        Collections.addAll(allowedContainers,
                "minecraft:crafting",
                "minecraft:anvil",
                "minecraft:enchantment",
                "minecraft:generic_9x3",
                "minecraft:generic_9x6",
                "minecraft:grindstone",
                "minecraft:smithing");
    }
    
    /**
     * Get the config file path for debugging
     */
    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    public static void load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        if (!Files.exists(path)) {
            // Save defaults
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null)
                return;
                
            // Check config version
            int version = root.has("configVersion") ? root.get("configVersion").getAsInt() : 1;
            if (version < CONFIG_VERSION) {
                dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.info(
                    "Updating config from version {} to {}", version, CONFIG_VERSION
                );
            }
            
            ModConfig cfg = getInstance();
            if (root.has("enabled"))
                cfg.enabled = root.get("enabled").getAsBoolean();
            if (root.has("showOffhandSlot"))
                cfg.showOffhandSlot = root.get("showOffhandSlot").getAsBoolean();
            if (root.has("showTooltips"))
                cfg.showTooltips = root.get("showTooltips").getAsBoolean();
            if (root.has("darkMode"))
                cfg.darkMode = root.get("darkMode").getAsBoolean();
            if (root.has("autoPositioning"))
                cfg.autoPositioning = root.get("autoPositioning").getAsBoolean();
            if (root.has("positioning")) {
                try {
                    cfg.positioning = Side.valueOf(root.get("positioning").getAsString().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (root.has("marginX"))
                cfg.marginX = root.get("marginX").getAsInt();
            if (root.has("marginY"))
                cfg.marginY = root.get("marginY").getAsInt();
            if (root.has("allowedContainers")) {
                cfg.allowedContainers.clear();
                JsonArray arr = root.getAsJsonArray("allowedContainers");
                arr.forEach(e -> cfg.allowedContainers.add(e.getAsString()));
            }
        } catch (IOException e) {
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.warn("Failed to load config file, using defaults: {}", e.getMessage());
        } catch (Exception e) {
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.error("Error parsing config file, using defaults", e);
        }
    }

    public static void save() {
        Path path = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        JsonObject root = new JsonObject();
        ModConfig cfg = getInstance();
        
        root.addProperty("configVersion", CONFIG_VERSION);
        
        // Core settings
        root.addProperty("_comment_1", "=== Core Settings ===");
        root.addProperty("enabled", cfg.enabled);
        root.addProperty("showTooltips", cfg.showTooltips);
        
        // Display settings
        root.addProperty("_comment_2", "=== Display Settings ===");
        root.addProperty("positioning", cfg.positioning.name());
        root.addProperty("marginX", cfg.marginX);
        root.addProperty("marginY", cfg.marginY);
        root.addProperty("autoPositioning", cfg.autoPositioning);
        root.addProperty("showOffhandSlot", cfg.showOffhandSlot);
        root.addProperty("darkMode", cfg.darkMode);
        
        // Container whitelist
        root.addProperty("_comment_3", "=== Container Whitelist (leave empty to allow all) ===");
        JsonArray arr = new JsonArray();
        cfg.allowedContainers.forEach(arr::add);
        root.add("allowedContainers", arr);
        
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(root, writer);
        } catch (IOException e) {
            dev.quentintyr.visiblearmorslots.Visiblearmorslots.LOGGER.error("Failed to save config file: {}", e.getMessage());
        }
    }
}
