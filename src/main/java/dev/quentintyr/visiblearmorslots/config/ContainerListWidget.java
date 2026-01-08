package dev.quentintyr.visiblearmorslots.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ContainerListWidget extends ObjectSelectionList<ContainerListWidget.BaseEntry> {

    public abstract static class BaseEntry extends ObjectSelectionList.Entry<BaseEntry> {}

    private final ModConfig config;

    private record ContainerDescriptor(ResourceLocation id, String title, List<String> sources) {}

    public ContainerListWidget(Minecraft client, int width, int height, int y, int b, int itemHeight, ModConfig config) {
        super(client, width, height, y, b, itemHeight);
        this.config = config;
        refreshEntries();
    }

    public void refreshEntries() {
        clearEntries();

        // Vanilla screens
        addEntry(new SectionTitleEntry(Component.literal("Vanilla Containers")));
        for (ContainerDescriptor d : getVanillaContainers()) {
            addEntry(new ContainerEntry(d.id(), d.title(), d.sources()));
        }

        // Modded Screens
        addEntry(new SectionTitleEntry(Component.literal("Modded Containers")));
        ForgeRegistries.MENU_TYPES.forEach(handler -> {
            ResourceLocation id = ForgeRegistries.MENU_TYPES.getKey(handler);
            if (id != null && !"minecraft".equals(id.getNamespace())) {
                addEntry(new ContainerEntry(
                        id,
                        formatModdedTitle(id),
                        List.of("Modded Container")
                ));
            }
        });
    }

    public void setAllEnabled(boolean enabled) {
        children().forEach(entry -> {
            if (entry instanceof ContainerEntry c) c.setEnabled(enabled);
        });
    }

    @Override
    public int getRowWidth() {
        return Math.min(400, width - 50);
    }

    @Override
    protected int getScrollbarPosition() {
        return getRowRight() - 6;
    }

    private class SectionTitleEntry extends BaseEntry {
        private final Component title;
        public SectionTitleEntry(Component title) { this.title = title; }

        @Override
        public void render(GuiGraphics ctx, int index, int y, int x, int w, int h,
                           int mx, int my, boolean hovered, float tickDelta) {
            ctx.drawCenteredString(minecraft.font, title, x + w / 2, y + (h - 8)/2, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public Component getNarration() {
            return title;
        }
    }

    private class ContainerEntry extends BaseEntry {
        private final ResourceLocation id;
        private boolean enabled;
        private final String title;
        
        private int lastToggleX, lastToggleY, lastToggleWidth, lastToggleHeight;

        public ContainerEntry(ResourceLocation id, String title, List<String> sources) {
            this.id = id;
            this.title = title;
            this.enabled = config.isContainerInList(id.toString());
        }

        @Override
        public void render(GuiGraphics ctx, int index, int y, int x, int w, int h,
                           int mx, int my, boolean hovered, float tickDelta) {

            // Toggle switch dimensions
            int toggleWidth = 32;
            int toggleHeight = 14;
            int toggleX = x + 5;
            int toggleY = y + (h - toggleHeight) / 2;
            
            // Store for click detection
            lastToggleX = toggleX;
            lastToggleY = toggleY;
            lastToggleWidth = toggleWidth;
            lastToggleHeight = toggleHeight;
            
            // Draw toggle background
            int bgColor = enabled ? 0xFF00AA00 : 0xFF555555;
            ctx.fill(toggleX, toggleY, toggleX + toggleWidth, toggleY + toggleHeight, bgColor);
            
            // Draw toggle knob
            int knobSize = 10;
            int knobY = toggleY + 2;
            int knobX = enabled ? toggleX + toggleWidth - knobSize - 2 : toggleX + 2;
            ctx.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, 0xFFFFFFFF);

            ctx.drawString(minecraft.font, title, x + 45, y + (h - 8)/2, enabled ? 0xFFFFFF : 0x888888, false);
        }

        private void toggle() {
            enabled = !enabled;
            config.setContainerEnabled(id.toString(), enabled);
        }

        public void setEnabled(boolean value) {
            this.enabled = value;
            config.setContainerEnabled(id.toString(), value);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (mx >= lastToggleX && mx < lastToggleX + lastToggleWidth && 
                my >= lastToggleY && my < lastToggleY + lastToggleHeight) {
                toggle();
                return true;
            }
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.literal(title + " (" + (enabled ? "enabled" : "disabled") + ")");
        }
    }

    private static String formatPath(String path) {
        String[] parts = path.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
    
    private static String formatModdedTitle(ResourceLocation id) {
        String modName = formatPath(id.getNamespace());
        String containerName = formatPath(id.getPath());
        return "(" + modName + ") " + containerName;
    }

    private static List<ContainerDescriptor> getVanillaContainers() {
        List<ContainerDescriptor> list = new ArrayList<>();
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "generic_9x3"),
                "Chest / Barrel",
                List.of("Chest", "Trapped Chest", "Barrel")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "generic_9x6"),
                "Large Chest",
                List.of("Large Chest")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "shulker_box"),
                "Shulker Box",
                List.of("Shulker Box")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "crafting"),
                "Crafting Table",
                List.of("Crafting Table")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "anvil"),
                "Anvil",
                List.of("Anvil", "Chipped Anvil", "Damaged Anvil")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "smithing"),
                "Smithing Table",
                List.of("Smithing Table")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "furnace"),
                "Furnace",
                List.of("Furnace")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "blast_furnace"),
                "Blast Furnace",
                List.of("Blast Furnace")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "smoker"),
                "Smoker",
                List.of("Smoker")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "brewing_stand"),
                "Brewing Stand",
                List.of("Brewing Stand")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "enchantment"),
                "Enchanting Table",
                List.of("Enchanting Table")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "grindstone"),
                "Grindstone",
                List.of("Grindstone")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "loom"),
                "Loom",
                List.of("Loom")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "cartography_table"),
                "Cartography Table",
                List.of("Cartography Table")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "stonecutter"),
                "Stonecutter",
                List.of("Stonecutter")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "hopper"),
                "Hopper",
                List.of("Hopper")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "lectern"),
                "Lectern",
                List.of("Lectern")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "beacon"),
                "Beacon",
                List.of("Beacon")
        ));
        list.add(new ContainerDescriptor(
                new ResourceLocation("minecraft", "merchant"),
                "Trading",
                List.of("Villagers", "Wandering Trader")
        ));
        return list;
    }
}
