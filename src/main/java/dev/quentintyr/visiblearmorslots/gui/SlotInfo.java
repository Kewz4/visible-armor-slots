package dev.quentintyr.visiblearmorslots.gui;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * Stores information about armor and offhand slots
 */
public class SlotInfo {
    
    public enum SlotType {
        HELMET(EquipmentSlot.HEAD, 39),
        CHESTPLATE(EquipmentSlot.CHEST, 38),
        LEGGINGS(EquipmentSlot.LEGS, 37),
        BOOTS(EquipmentSlot.FEET, 36),
    OFFHAND(EquipmentSlot.OFFHAND, 40);
        
        private final EquipmentSlot equipmentSlot;
        private final int inventoryIndex;
        
        SlotType(EquipmentSlot equipmentSlot, int inventoryIndex) {
            this.equipmentSlot = equipmentSlot;
            this.inventoryIndex = inventoryIndex;
        }
        
        public EquipmentSlot getEquipmentSlot() {
            return equipmentSlot;
        }
        
        public int getInventoryIndex() {
            return inventoryIndex;
        }
        
        public boolean isOffhand() {
            return this == OFFHAND;
        }
    }
    
    private final SlotType slotType;
    private final int x;
    private final int y;
    
    public SlotInfo(SlotType slotType, int x, int y) {
        this.slotType = slotType;
        this.x = x;
        this.y = y;
    }
    
    public SlotType getSlotType() {
        return slotType;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public EquipmentSlot getEquipmentSlot() {
        return slotType.getEquipmentSlot();
    }
    
    public int getInventoryIndex() {
        return slotType.getInventoryIndex();
    }
    
    public boolean isOffhand() {
        return slotType.isOffhand();
    }
    
    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }
}
