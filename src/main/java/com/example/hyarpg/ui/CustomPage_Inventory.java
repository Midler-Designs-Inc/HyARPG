package com.example.hyarpg.ui;

// Hytale Imports
import com.example.hyarpg.utils.items.ItemFactory;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.metadata.ItemDisplayMetadata;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.ValueCodec;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.utils.StatTypeInfo;
import com.example.hyarpg.utils.affixes.Affix;
import com.example.hyarpg.utils.affixes.AffixPool;
import com.example.hyarpg.utils.affixes.StatType;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;

import static com.example.hyarpg.modules.Module_RPGSystem.componentTypeRPGPlayer;

// Java Imports
import org.bson.BsonDocument;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CustomPage_Inventory extends InteractiveCustomUIPage<CustomPage_Inventory.PageData> {

    // slot counts
    private static final int STORAGE_SLOTS = 36;
    private static final int HOTBAR_SLOTS  = 9;
    private static final int ARMOR_SLOTS   = 4;
    private static final int UTILITY_SLOTS = 4;

    // flags for the drag/drop
    private int droppedSlot = -1;
    private String droppedContainer = null;

    // flags for the right click move
    private int rightClickedSlot = -1;
    private String rightClickedContainer = null;

    // quick-craft slot definitions
    private static final int QUICK_CRAFT_SLOTS = 6;
    private static final String[] QUICK_CRAFT_ITEM_IDS = {
        "HyARPG_How_To_Play_Guide",
        "Tool_Hatchet_Crude",
        "Tool_Pickaxe_Crude",
        "Bench_Light_Well",
        "Bench_WorkBench",
        "Dimensional_Cube",
    };

    public CustomPage_Inventory(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load main UI file
        cmd.append("CustomPage_Inventory.ui");

        // bind grid drop events, released left click while an item was being dragged
        events.addEventBinding(CustomUIEventBindingType.Dropped, "#InvStorageGrid", EventData.of("Action", "dropped:storage"));
        events.addEventBinding(CustomUIEventBindingType.Dropped, "#InvHotbarGrid", EventData.of("Action", "dropped:hotbar"));
        events.addEventBinding(CustomUIEventBindingType.Dropped, "#ArmorGrid", EventData.of("Action", "dropped:armor"));
        events.addEventBinding(CustomUIEventBindingType.Dropped, "#UtilityGrid", EventData.of("Action", "dropped:utility"));

        // bind grid click when clicking to start a drag
        events.addEventBinding(CustomUIEventBindingType.SlotMouseDragCompleted, "#InvStorageGrid", EventData.of("Action", "dragComplete:storage"));
        events.addEventBinding(CustomUIEventBindingType.SlotMouseDragCompleted, "#InvHotbarGrid", EventData.of("Action", "dragComplete:hotbar"));
        events.addEventBinding(CustomUIEventBindingType.SlotMouseDragCompleted, "#ArmorGrid", EventData.of("Action", "dragComplete:armor"));
        events.addEventBinding(CustomUIEventBindingType.SlotMouseDragCompleted, "#UtilityGrid", EventData.of("Action", "dragComplete:utility"));

        // bind grid click while dragging events (right clicked a single item to start, needs to left click to place)
        events.addEventBinding(CustomUIEventBindingType.RightClicking, "#InvStorageGrid", EventData.of("Action", "clickedWhileDragging:storage"));
        events.addEventBinding(CustomUIEventBindingType.RightClicking, "#InvHotbarGrid", EventData.of("Action", "clickedWhileDragging:hotbar"));
        events.addEventBinding(CustomUIEventBindingType.SlotClickPressWhileDragging, "#ArmorGrid", EventData.of("Action", "clickedWhileDragging:armor"));
        events.addEventBinding(CustomUIEventBindingType.SlotClickPressWhileDragging, "#UtilityGrid", EventData.of("Action", "clickedWhileDragging:utility"));

//        // bind the grid right click events
//        events.addEventBinding(CustomUIEventBindingType.RightClicking, "#InvStorageGrid", EventData.of("Action", "rightClicked:storage"));
//        events.addEventBinding(CustomUIEventBindingType.RightClicking, "#InvHotbarGrid", EventData.of("Action", "rightClicked:hotbar"));
//        events.addEventBinding(CustomUIEventBindingType.RightClicking, "#ArmorGrid", EventData.of("Action", "rightClicked:armor"));
//        events.addEventBinding(CustomUIEventBindingType.RightClicking, "#UtilityGrid", EventData.of("Action", "rightClicked:utility"));

        // bind quick-craft slot clicks
        for (int i = 0; i < QUICK_CRAFT_SLOTS; i++) events.addEventBinding(CustomUIEventBindingType.Activating, "#QuickCraftSlot" + i, EventData.of("Action", "quickcraft:" + i));

        // apply full initial state
        applyFullState(ref, store, cmd);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null) { sendUpdate((UICommandBuilder) null, false); return; }

        UICommandBuilder cmd = new UICommandBuilder();

        // get the origin container
        String[] actionParts = data.action.split(":");
        String originContainer = actionParts[1];

        // clicked on a quick craft slot
        if (data.action.startsWith("quickcraft:")) {
            handleQuickCraft(Integer.parseInt(data.action.substring("quickcraft:".length())), ref, store, cmd);
        }

        // update the slot the drop was placed into (drag complete fires after with the origin slot)
        else if (data.action.startsWith("dropped:")) {
            droppedSlot = data.slotIndex;
            droppedContainer = originContainer;
        }

        // update teh slot the right click happened on
        else if (data.action.startsWith("rightClicked:")) {
            rightClickedSlot = data.slotIndex;
            rightClickedContainer = originContainer;
        }

        // released on a slot while dragging something
        else if (data.action.startsWith("dragComplete:")) {
            // if a dropped event was set we need to handle the move
            if (droppedSlot != -1) handleDragComplete(ref, originContainer, data.slotIndex, droppedSlot, false);

            // clear all drag/drop stuff
            droppedSlot = -1;
            droppedContainer = null;
            rightClickedSlot = -1;
            rightClickedContainer = null;

            // refresh all grids to reflect updated inventory state
            applyFullState(ref, store, cmd);
        }

        // Right clicked to start the drag with a single item, fires when clicking to place
        else if (data.action.startsWith("clickedWhileDragging:")) {
            // if a right click event was set we need to handle the move
            if (rightClickedSlot != -1) handleDragComplete(ref, originContainer, rightClickedSlot, data.slotIndex, true);

            // clear all drag/drop stuff
            droppedSlot = -1;
            droppedContainer = null;
            rightClickedSlot = -1;
            rightClickedContainer = null;

            // refresh all grids to reflect updated inventory state
            applyFullState(ref, store, cmd);
        }

        // push empty state
        else {
            sendUpdate((UICommandBuilder) null, false);
            return;
        }

        // otherwise fire off whatever was applied above in apply full state
        sendUpdate(cmd, false);
    }

    // called when a quick-craft slot is clicked
    private void handleQuickCraft(int slot, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        if (slot < 0 || slot >= QUICK_CRAFT_SLOTS) return;
        InventoryComponent.Storage storage = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storage == null) return;
        storage.getInventory().addItemStack(new ItemStack(QUICK_CRAFT_ITEM_IDS[slot], 1));
        pushStorageGrid(ref, store, cmd);
    }

    // called when a drag event is completed
    private  void handleDragComplete(@Nonnull Ref<EntityStore> ref, String containerName, int beforeSlot, int afterSlot, boolean moveSingle) {
        // get the store from the ref
        Store<EntityStore> store = ref.getStore();

        // get the ref/player's inventory comp
        InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComp == null) return;

        // get the item container from the inventory comp
        ItemContainer inv = storageComp.getInventory();
        if (inv == null) return;

        // get the item stack being dragged
        ItemStack beforeStack = inv.getItemStack((short) beforeSlot);
        if (beforeStack == null) return;

        // check if the dragged stack can merge into the dropped stack
        int itemQty = moveSingle ? 1 : beforeStack.getQuantity();
        boolean canMergeStacks = storageComp.getInventory().canAddItemStackToSlot((short) afterSlot, new ItemStack(beforeStack.getItemId(), itemQty), true, false);

        // the items can merge, move them over
        if (canMergeStacks) inv.moveItemStackFromSlotToSlot((short) beforeSlot, itemQty, inv, (short) afterSlot);
    }

    // full state push — called on open and after any equip/unequip
    private void applyFullState(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        pushStorageGrid(ref, store, cmd);
        pushHotbarGrid(ref, store, cmd);
        pushArmorGrid(ref, store, cmd);
        pushUtilityGrid(ref, store, cmd);
        pushStats(ref, store, cmd);
        pushQuickCraftStates(ref, store, cmd);
    }

    // push the individual item grids
    private void pushStorageGrid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        // get the ref/player's inventory comp
        InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComp == null) return;

        // get the item container and init an empty slots array
        ItemContainer inv = storageComp.getInventory();
        List<ItemGridSlot> slots = new ArrayList<>();

        // loop over container slots and push them into the slots array
        for (short i = 0; i < Math.min(inv.getCapacity(), STORAGE_SLOTS); i++) {
            // get the item stack and init a new grid slot
            ItemStack stack = inv.getItemStack(i);
            ItemGridSlot slot = new ItemGridSlot();
            slot.setActivatable(true);

            // if the stack exists and isn't empty, set the appropriate details
            if (stack != null && !stack.isEmpty()) {
                // use a clean stack with just id and quantity
                slot.setItemStack(new ItemStack(stack.getItem().getId(), stack.getQuantity()));

                // set the overridden description if applicable
                ItemDisplayMetadata displayMeta = stack.getFromMetadataOrNull(ItemDisplayMetadata.KEYED_CODEC);
                if (displayMeta != null)  slot.setDescription(ItemFactory.buildSlotDescription(stack));

            }
            slots.add(slot);
        }
        cmd.set("#InvStorageGrid.Slots", slots);
    }
    private void pushHotbarGrid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        // get the ref/player's inventory comp
        InventoryComponent.Hotbar hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbarComp == null) return;

        // get the item container and init an empty slots array
        ItemContainer inv = hotbarComp.getInventory();
        List<ItemGridSlot> slots = new ArrayList<>();

        // loop over container slots and push them into the slots array
        for (short i = 0; i < Math.min(inv.getCapacity(), HOTBAR_SLOTS); i++) {
            // get the item stack and init a new grid slot
            ItemStack stack = inv.getItemStack(i);
            ItemGridSlot slot = new ItemGridSlot();
            slot.setActivatable(true);

            // if the stack exists and isn't empty, set the appropriate details
            if (stack != null && !stack.isEmpty()) {
                // use a clean stack with just id and quantity
                slot.setItemStack(new ItemStack(stack.getItem().getId(), stack.getQuantity()));

                // set the overridden description if applicable
                ItemDisplayMetadata displayMeta = stack.getFromMetadataOrNull(ItemDisplayMetadata.KEYED_CODEC);
                if (displayMeta != null)  slot.setDescription(ItemFactory.buildSlotDescription(stack));
            }
            slots.add(slot);
        }
        cmd.set("#InvHotbarGrid.Slots", slots);
    }
    private void pushArmorGrid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        // get the armor inventory or bail
        InventoryComponent.Armor armorComp = store.getComponent(ref, InventoryComponent.Armor.getComponentType());
        if (armorComp == null) return;

        // get the item container and init an empty slots list
        ItemContainer inv = armorComp.getInventory();
        List<ItemGridSlot> slots = new ArrayList<>();

        // loop over container slots and build the slot list
        for (short i = 0; i < Math.min(inv.getCapacity(), ARMOR_SLOTS); i++) {
            // get the item stack and init a new grid slot
            ItemStack stack = inv.getItemStack(i);
            ItemGridSlot slot = new ItemGridSlot();
            slot.setActivatable(true);

            // populate slot details if an item is present
            if (stack != null && !stack.isEmpty()) {
                // use a clean stack with just id and quantity
                slot.setItemStack(new ItemStack(stack.getItem().getId(), stack.getQuantity()));

                // set the description if applicable
                ItemDisplayMetadata displayMeta = stack.getFromMetadataOrNull(ItemDisplayMetadata.KEYED_CODEC);
                if (displayMeta != null) slot.setDescription(ItemFactory.buildSlotDescription(stack));
            }
            slots.add(slot);
        }

        // push the slot list to the grid
        cmd.set("#ArmorGrid.Slots", slots);
    }
    private void pushUtilityGrid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        // get the utility inventory or bail
        InventoryComponent.Utility utilityComp = store.getComponent(ref, InventoryComponent.Utility.getComponentType());
        if (utilityComp == null) return;

        // get the item container and init an empty slots list
        ItemContainer inv = utilityComp.getInventory();
        List<ItemGridSlot> slots = new ArrayList<>();

        // loop over container slots and build the slot list
        for (short i = 0; i < Math.min(inv.getCapacity(), UTILITY_SLOTS); i++) {
            // get the item stack and init a new grid slot
            ItemStack stack = inv.getItemStack(i);
            ItemGridSlot slot = new ItemGridSlot();
            slot.setActivatable(true);

            // populate slot details if an item is present
            if (stack != null && !stack.isEmpty()) {
                // use a clean stack with just id and quantity
                slot.setItemStack(new ItemStack(stack.getItem().getId(), stack.getQuantity()));

                // set the description if applicable
                ItemDisplayMetadata displayMeta = stack.getFromMetadataOrNull(ItemDisplayMetadata.KEYED_CODEC);
                if (displayMeta != null) slot.setDescription(ItemFactory.buildSlotDescription(stack));
            }
            slots.add(slot);
        }

        // push the slot list to the grid
        cmd.set("#UtilityGrid.Slots", slots);
    }

    // push quick-craft slot item icons
    private void pushQuickCraftStates(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        for (int i = 0; i < QUICK_CRAFT_SLOTS; i++) {
            cmd.set("#QuickCraftItem" + i + ".ItemId", QUICK_CRAFT_ITEM_IDS[i]);
            cmd.set("#QuickCraftDimOverlay" + i + ".Visible", false);
        }
    }

    // push all stat values to the left panel labels
    private void pushStats(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        try {
            Component_RPG_Player rpg = store.getComponent(ref, componentTypeRPGPlayer);
            if (rpg == null) return;
            var stats = rpg.stats;

            cmd.set("#StatPlayerLevel.Text", String.valueOf(rpg.level));
            cmd.set("#StatGearScore.Text",   String.valueOf(rpg.gearScore));

            cmd.set("#StatPhysDmgFlat.Text",      "+" + fmt(stats.getFlatDamage("Physical")));
            cmd.set("#StatPhysDmgPct.Text",       "+" + fmtPct(stats.getIncreasedDamage("Physical")));
            cmd.set("#StatMagicDmgFlat.Text",     "+" + fmt(stats.getFlatDamage("Magic")));
            cmd.set("#StatMagicDmgPct.Text",      "+" + fmtPct(stats.getIncreasedDamage("Magic")));
            cmd.set("#StatFireDmgFlat.Text",      "+" + fmt(stats.getFlatDamage("Fire")));
            cmd.set("#StatFireDmgPct.Text",       "+" + fmtPct(stats.getIncreasedDamage("Fire")));
            cmd.set("#StatIceDmgFlat.Text",       "+" + fmt(stats.getFlatDamage("Ice")));
            cmd.set("#StatIceDmgPct.Text",        "+" + fmtPct(stats.getIncreasedDamage("Ice")));
            cmd.set("#StatLightningDmgFlat.Text", "+" + fmt(stats.getFlatDamage("Lightning")));
            cmd.set("#StatLightningDmgPct.Text",  "+" + fmtPct(stats.getIncreasedDamage("Lightning")));
            cmd.set("#StatPoisonDmgFlat.Text",    "+" + fmt(stats.getFlatDamage("Poison")));
            cmd.set("#StatPoisonDmgPct.Text",     "+" + fmtPct(stats.getIncreasedDamage("Poison")));
            cmd.set("#StatCritChance.Text",       fmtPct(stats.getCriticalStrikeChance()));
            cmd.set("#StatCritDamage.Text",       fmt(stats.getCriticalStrikeDamage()) + "x");
            cmd.set("#StatAxeDmg.Text",           fmtPct(stats.getIncreasedDamage("Axe")));
            cmd.set("#StatBattleaxeDmg.Text",     fmtPct(stats.getIncreasedDamage("Battleaxe")));
            cmd.set("#StatClubDmg.Text",          fmtPct(stats.getIncreasedDamage("Club")));
            cmd.set("#StatDaggersDmg.Text",       fmtPct(stats.getIncreasedDamage("Daggers")));
            cmd.set("#StatKunaiDmg.Text",         fmtPct(stats.getIncreasedDamage("Kunai")));
            cmd.set("#StatLongswordDmg.Text",     fmtPct(stats.getIncreasedDamage("Longsword")));
            cmd.set("#StatMaceDmg.Text",          fmtPct(stats.getIncreasedDamage("Mace")));
            cmd.set("#StatShortbowDmg.Text",      fmtPct(stats.getIncreasedDamage("Shortbow")));
            cmd.set("#StatCrossbowDmg.Text",      fmtPct(stats.getIncreasedDamage("Crossbow")));
            cmd.set("#StatSwordDmg.Text",         fmtPct(stats.getIncreasedDamage("Sword")));
            cmd.set("#StatStaffDmg.Text",         fmtPct(stats.getIncreasedDamage("Staff")));
            cmd.set("#StatWandDmg.Text",          fmtPct(stats.getIncreasedDamage("Wand")));

            cmd.set("#StatDodgeChance.Text",    fmtPct(stats.getDodgeChance()));
            cmd.set("#StatStability.Text",      fmtPct(stats.getStabilityPercent(false)));
            cmd.set("#StatParryWindow.Text",    "+" + fmt(stats.getParryWindow()) + "(s)");
            cmd.set("#StatBarrierOnBlock.Text", fmtPct(stats.getBarrierOnBlock()));

            cmd.set("#StatLifeFlat.Text",    "+" + fmt(stats.getFlatResource("Life")));
            cmd.set("#StatLifePct.Text",     "+" + fmtPct(stats.getIncreasedResource("Life")));
            cmd.set("#StatStaminaFlat.Text", "+" + fmt(stats.getFlatResource("Stamina")));
            cmd.set("#StatStaminaPct.Text",  "+" + fmtPct(stats.getIncreasedResource("Stamina")));
            cmd.set("#StatManaFlat.Text",    "+" + fmt(stats.getFlatResource("Mana")));
            cmd.set("#StatManaPct.Text",     "+" + fmtPct(stats.getIncreasedResource("Mana")));

            cmd.set("#StatLifeRegenFlat.Text",    "+" + fmt(stats.getFlatResourceRegen("Life")) + "s");
            cmd.set("#StatLifeRegenPct.Text",     "+" + fmtPct(stats.getIncreasedResourceRegen("Life")));
            cmd.set("#StatStaminaRegenFlat.Text", "+" + fmt(stats.getFlatResourceRegen("Stamina")) + "s");
            cmd.set("#StatStaminaRegenPct.Text",  "+" + fmtPct(stats.getIncreasedResourceRegen("Stamina")));
            cmd.set("#StatManaRegenFlat.Text",    "+" + fmt(stats.getFlatResourceRegen("Mana")) + "s");
            cmd.set("#StatManaRegenPct.Text",     "+" + fmtPct(stats.getIncreasedResourceRegen("Mana")));

            cmd.set("#StatPhysResist.Text",      fmtPct(stats.getResistance("Physical")));
            cmd.set("#StatMagicResist.Text",     fmtPct(stats.getResistance("Magic")));
            cmd.set("#StatElementalResist.Text", fmtPct(stats.getResistance("Elemental")));
            cmd.set("#StatFireResist.Text",      fmtPct(stats.getResistance("Fire")));
            cmd.set("#StatIceResist.Text",       fmtPct(stats.getResistance("Ice")));
            cmd.set("#StatLightningResist.Text", fmtPct(stats.getResistance("Lightning")));
            cmd.set("#StatPoisonResist.Text",    fmtPct(stats.getResistance("Poison")));
            cmd.set("#StatFallResist.Text",      fmtPct(stats.getResistance("Fall")));

            cmd.set("#StatLifeLeech.Text",      fmtPct(stats.getLeech("Life")));
            cmd.set("#StatManaLeech.Text",      fmtPct(stats.getLeech("Mana")));
            cmd.set("#StatStaminaLeech.Text",   fmtPct(stats.getLeech("Stamina")));
            cmd.set("#StatDmgFromMana.Text",    fmtPct(stats.getDamageTakenFrom("Mana")));
            cmd.set("#StatDmgFromStamina.Text", fmtPct(stats.getDamageTakenFrom("Stamina")));

            cmd.set("#StatRunSpeed.Text", "+" + fmtPct(stats.getRunSpeedPercent()));
            cmd.set("#StatAmmo.Text",     "+" + fmt(stats.getAddedAmmo()));
            cmd.set("#StatAmmoRegen.Text","+" + fmtPct(stats.getAmmoRegenPercent()));
        } catch (Exception _) {}
    }

    // item classification helpers
    private static boolean isGearItem(@Nonnull Item item) { return item.getArmor() != null && item.getArmor().getArmorSlot() != null; }
    private static boolean isUtilityItem(@Nonnull Item item) { return item.getUtility().isUsable(); }
    private static boolean isCompatibleArmorSlot(@Nonnull Item item, int slot) {
        if (item.getArmor() == null || item.getArmor().getArmorSlot() == null) return false;
        if (slot == 0) return item.getArmor().getArmorSlot() == ItemArmorSlot.Head;
        if (slot == 1) return item.getArmor().getArmorSlot() == ItemArmorSlot.Chest;
        if (slot == 2) return item.getArmor().getArmorSlot() == ItemArmorSlot.Hands;
        if (slot == 3) return item.getArmor().getArmorSlot() == ItemArmorSlot.Legs;
        return false;
    }
    private static boolean isItemCompatibleWithSlot(@Nonnull Item item, @Nonnull String slotSource, int slotIndex) {
        if (slotSource.equals("utility")) return isUtilityItem(item);
        if (slotSource.equals("armor"))   return isCompatibleArmorSlot(item, slotIndex);
        return false;
    }
    private static String deriveRarity(@Nonnull String itemId) {
        for (String r : new String[]{"Legendary", "Epic", "Rare", "Uncommon", "Common"}) { if (itemId.endsWith("_" + r)) return r; }
        return "Common";
    }

    // formatting helpers
    private static String fmt(float v) { float r = Math.round(v * 10) / 10f; return r == (int) r ? String.valueOf((int) r) : String.valueOf(r); }
    private static String fmtPct(float v) { return fmt(v) + "%"; }

    // PageData codec — now includes slotIndex for grid click events
    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("SlotIndex", Codec.INTEGER), (d, v) -> d.slotIndex = v, d -> d.slotIndex).add()
                .build();
        public String action;
        public int slotIndex = -1;
    }
}