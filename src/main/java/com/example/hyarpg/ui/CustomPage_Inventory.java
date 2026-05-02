package com.example.hyarpg.ui;

// Hytale Imports
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
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.utils.StatTypeInfo;
import com.example.hyarpg.utils.affixes.Affix;
import com.example.hyarpg.utils.affixes.AffixPool;
import com.example.hyarpg.utils.affixes.StatType;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;
import com.example.hyarpg.ModEventBus;
import com.example.hyarpg.events.Event_PlayerInventoryItemEquip;
import com.example.hyarpg.events.Event_PlayerInventoryItemUnEquip;

// Java Imports
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.hyarpg.modules.Module_RPGSystem.componentTypeRPGPlayer;

public class CustomPage_Inventory extends InteractiveCustomUIPage<CustomPage_Inventory.PageData> {

    // slot counts
    private static final int STORAGE_SLOTS = 36;
    private static final int HOTBAR_SLOTS  = 9;
    private static final int ARMOR_SLOTS   = 4;
    private static final int UTILITY_SLOTS = 4;

    // armor slot indices — Helmet=0, Chest=1, Gloves=2, Pants=3
    private static final String[] ARMOR_SLOT_LABELS = { "Helmet", "Chest", "Gloves", "Pants" };

    // rarity display colors
    private static final String COLOR_COMMON    = "#aaaaaa";
    private static final String COLOR_UNCOMMON  = "#55ff55";
    private static final String COLOR_RARE      = "#5588ff";
    private static final String COLOR_EPIC      = "#cc44cc";
    private static final String COLOR_LEGENDARY = "#ffaa00";

    // Quick-craft slot definitions - Index maps directly to the UI slot number (0-5)
    private static final int QUICK_CRAFT_SLOTS = 6;

    // Item ids shown in each quick-craft slot — order matches UI grid left-to-right, top-to-bottom
    private static final String[] QUICK_CRAFT_ITEM_IDS = {
        "HyARPG_How_To_Play_Guide",   // 0 — no cost, lit if player doesn't have one
        "Tool_Hatchet_Crude",         // 1 — has recipe cost
        "Tool_Pickaxe_Crude",         // 2 — has recipe cost
        "Bench_Light_Well",           // 3 — no cost, lit if player doesn't have one AND no territory stake
        "Bench_WorkBench",            // 4 — has recipe cost
        "Dimensional_Cube",           // 5 — no cost, lit if player doesn't have one
    };

    // cache of asset-json Recipe blocks, keyed by item id — populated once per item, never changes
    private static final Map<String, BsonDocument> RECIPE_CACHE = new ConcurrentHashMap<>();

    // selection state — encodes source and index e.g. "storage:5", "armor:0", "utility:2", "hotbar:3"
    private String selectedSlotId = null;
    private Item   selectedItem   = null;

    public CustomPage_Inventory(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load main UI file
        cmd.append("CustomPage_Inventory.ui");

        // bind storage slot clicks
        for (int i = 0; i < STORAGE_SLOTS; i++) events.addEventBinding(CustomUIEventBindingType.Activating, "#InvStorageSlot" + i, EventData.of("Action", "select:storage:" + i));

        // bind hotbar slot clicks
        for (int i = 0; i < HOTBAR_SLOTS; i++) events.addEventBinding(CustomUIEventBindingType.Activating, "#InvHotbarSlot" + i, EventData.of("Action", "select:hotbar:" + i));

        // bind armor slot clicks
        for (int i = 0; i < ARMOR_SLOTS; i++) events.addEventBinding(CustomUIEventBindingType.Activating, "#ArmorSlot" + i, EventData.of("Action", "select:armor:" + i));

        // bind utility slot clicks
        for (int i = 0; i < UTILITY_SLOTS; i++) events.addEventBinding(CustomUIEventBindingType.Activating, "#UtilitySlot" + i, EventData.of("Action", "select:utility:" + i));

        // bind quick-craft slot clicks
        for (int i = 0; i < QUICK_CRAFT_SLOTS; i++) events.addEventBinding(CustomUIEventBindingType.Activating, "#QuickCraftSlot" + i, EventData.of("Action", "quickcraft:" + i));

        // apply full initial state
        applyFullState(ref, store, cmd);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null) { sendUpdate((UICommandBuilder) null, false); return; }

        UICommandBuilder cmd = new UICommandBuilder();

        if (data.action.startsWith("select:")) {
            handleSlotClick(data.action.substring("select:".length()), ref, store, cmd);
        } else if (data.action.startsWith("quickcraft:")) {
            handleQuickCraft(Integer.parseInt(data.action.substring("quickcraft:".length())), ref, store, cmd);
        } else {
            sendUpdate((UICommandBuilder) null, false);
            return;
        }

        sendUpdate(cmd, false);
    }

    // Slot click handler — selection, deselection, and equip/swap routing
    // -------------------------------------------------------------------------
    // Quick-craft logic
    // -------------------------------------------------------------------------

    // called when a quick-craft slot is clicked — checks availability and gives the item if valid
    private void handleQuickCraft(int slot, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        if (slot < 0 || slot >= QUICK_CRAFT_SLOTS) return;

        InventoryComponent.Storage storage = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storage == null) return;

        storage.getInventory().addItemStack(new ItemStack(QUICK_CRAFT_ITEM_IDS[slot], 1));

        pushStorageSlots(ref, store, cmd);
    }

    // pushes the item icon and dim overlay state for all 6 quick-craft slots
    private void pushQuickCraftStates(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        for (int i = 0; i < QUICK_CRAFT_SLOTS; i++) {
            cmd.set("#QuickCraftItem" + i + ".ItemId", QUICK_CRAFT_ITEM_IDS[i]);
            cmd.set("#QuickCraftDimOverlay" + i + ".Visible", false);
        }
    }

    // returns true if the given quick-craft slot should be lit (available to click)
    private boolean isQuickCraftAvailable(int slot, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        String itemId = QUICK_CRAFT_ITEM_IDS[slot];

        return switch (slot) {
            // HyARPG_How_To_Play_Guide — lit if player doesn't already have one
            case 0 -> !playerHasItem(itemId, ref, store);

            // Tool_Hatchet_Crude — lit if player has materials
            case 1 -> playerHasMaterials(itemId, ref, store);

            // Tool_Pickaxe_Crude — lit if player has materials
            case 2 -> playerHasMaterials(itemId, ref, store);

            // Bench_Light_Well — lit if no item on them AND no territory stake
            case 3 -> !playerHasItem(itemId, ref, store) && !playerHasTerritoryStake(ref, store);

            // Bench_WorkBench — lit if player has materials
            case 4 -> playerHasMaterials(itemId, ref, store);

            // Dimensional_Cube — lit if player doesn't already have one
            case 5 -> !playerHasItem(itemId, ref, store);

            default -> false;
        };
    }

    // returns true if the player has at least one of the given item in storage or hotbar
    private boolean playerHasItem(@Nonnull String itemId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComp != null) {
            ItemContainer inv = storageComp.getInventory();
            for (short i = 0; i < inv.getCapacity(); i++) {
                ItemStack stack = inv.getItemStack(i);
                if (stack != null && !stack.isEmpty() && stack.getItem().getId().equals(itemId)) return true;
            }
        }
        InventoryComponent.Hotbar hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbarComp != null) {
            ItemContainer inv = hotbarComp.getInventory();
            for (short i = 0; i < inv.getCapacity(); i++) {
                ItemStack stack = inv.getItemStack(i);
                if (stack != null && !stack.isEmpty() && stack.getItem().getId().equals(itemId)) return true;
            }
        }
        return false;
    }

    // returns true if the player has all required materials from the item's Recipe.Input in storage or hotbar
    private boolean playerHasMaterials(@Nonnull String itemId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        BsonDocument recipe = readRecipe(itemId);
        if (recipe == null || !recipe.containsKey("Input")) return false;
        BsonArray inputs = recipe.getArray("Input");
        for (BsonValue entry : inputs) {
            if (!entry.isDocument()) return false;
            BsonDocument input = entry.asDocument();
            String resourceId = input.getString("ResourceTypeId").getValue();
            int required = input.getInt32("Quantity").getValue();
            if (countItemInInventory(resourceId, ref, store) < required) return false;
        }
        return true;
    }

    // counts how many of a given item id the player has across storage and hotbar
    private int countItemInInventory(@Nonnull String itemId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        int count = 0;
        InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComp != null) {
            ItemContainer inv = storageComp.getInventory();
            for (short i = 0; i < inv.getCapacity(); i++) {
                ItemStack stack = inv.getItemStack(i);
                if (stack != null && !stack.isEmpty() && stack.getItem().getId().equals(itemId)) count += stack.getQuantity();
            }
        }
        InventoryComponent.Hotbar hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbarComp != null) {
            ItemContainer inv = hotbarComp.getInventory();
            for (short i = 0; i < inv.getCapacity(); i++) {
                ItemStack stack = inv.getItemStack(i);
                if (stack != null && !stack.isEmpty() && stack.getItem().getId().equals(itemId)) count += stack.getQuantity();
            }
        }
        return count;
    }

    // returns true if the player owns or co-owns any territory
    private boolean playerHasTerritoryStake(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return false;
        UUID ownerUuid = playerRef.getUuid();
        WorldRoomRegistry registry = WorldRoomRegistry.get(store.getExternalData().getWorld());
        if (registry == null) return false;
        return registry.getAllTerritories().stream()
                .anyMatch(t -> ownerUuid.equals(t.getOwnerUuid()) || t.isCoOwner(ownerUuid));
    }

    // removes the required materials from storage and hotbar, returns false if materials are missing
    private boolean consumeMaterials(@Nonnull BsonArray inputs, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        for (BsonValue entry : inputs) {
            if (!entry.isDocument()) return false;
            BsonDocument input = entry.asDocument();
            String resourceId = input.getString("ResourceTypeId").getValue();
            int required = input.getInt32("Quantity").getValue();
            if (!consumeItem(resourceId, required, ref, store)) return false;
        }
        return true;
    }

    // removes the requested quantity of an item from storage then hotbar, returns false if insufficient
    private boolean consumeItem(@Nonnull String itemId, int quantity, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        int remaining = quantity;

        InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComp != null) {
            ItemContainer inv = storageComp.getInventory();
            for (short i = 0; i < inv.getCapacity() && remaining > 0; i++) {
                ItemStack stack = inv.getItemStack(i);
                if (stack == null || stack.isEmpty() || !stack.getItem().getId().equals(itemId)) continue;
                int take = Math.min(stack.getQuantity(), remaining);
                int left = stack.getQuantity() - take;
                inv.replaceItemStackInSlot(i, stack, left > 0 ? stack.withQuantity(left) : null);
                remaining -= take;
            }
        }

        InventoryComponent.Hotbar hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbarComp != null) {
            ItemContainer inv = hotbarComp.getInventory();
            for (short i = 0; i < inv.getCapacity() && remaining > 0; i++) {
                ItemStack stack = inv.getItemStack(i);
                if (stack == null || stack.isEmpty() || !stack.getItem().getId().equals(itemId)) continue;
                int take = Math.min(stack.getQuantity(), remaining);
                int left = stack.getQuantity() - take;
                inv.replaceItemStackInSlot(i, stack, left > 0 ? stack.withQuantity(left) : null);
                remaining -= take;
            }
        }

        return remaining == 0;
    }

    // reads and caches the Recipe block from a raw asset json — returns null if not present
    @Nullable
    private static BsonDocument readRecipe(@Nonnull String itemId) {
        return RECIPE_CACHE.computeIfAbsent(itemId, id -> {
            Path assetPath = Item.getAssetMap().getPath(id);
            if (assetPath == null) return null;
            try {
                BsonDocument doc = BsonDocument.parse(Files.readString(assetPath));
                BsonValue recipe = doc.get("Recipe");
                return (recipe != null && recipe.isDocument()) ? recipe.asDocument() : null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Existing logic — unchanged below this line
    // -------------------------------------------------------------------------

    private void handleSlotClick(@Nonnull String slotId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        String[] parts = slotId.split(":");
        String source = parts[0];
        int    index  = Integer.parseInt(parts[1]);

        ItemStack clickedStack = getStackFromSlot(source, index, ref, store);
        Item      clickedItem  = clickedStack != null && !clickedStack.isEmpty() ? clickedStack.getItem() : null;

        boolean isGearSlot      = source.equals("armor") || source.equals("utility");
        boolean isInventorySlot = source.equals("storage") || source.equals("hotbar");
        boolean hasSelection    = this.selectedSlotId != null;
        boolean clickingSelf    = slotId.equals(this.selectedSlotId);

        // clicking the already-selected slot deselects
        if (clickingSelf) {
            clearSelection(cmd); clearGearSlotOverlays(cmd); clearInventoryInvalidOverlays(cmd); updateInspectPanel(cmd, null);
            return;
        }

        // clicking any empty slot with no selection — do nothing
        if (!hasSelection) {
            if (clickedItem == null) return;
            // utility slots with items still need the empty check since active slot can be -1
            if (source.equals("utility")) {
                InventoryComponent.Utility u = store.getComponent(ref, InventoryComponent.Utility.getComponentType());
                if (u == null || u.getActiveSlot() == -1 && clickedStack.isEmpty()) return;
            }
        }

        if (hasSelection) {
            String  selSource    = this.selectedSlotId.split(":")[0];
            int     selIndex     = Integer.parseInt(this.selectedSlotId.split(":")[1]);
            boolean selIsGear      = selSource.equals("armor") || selSource.equals("utility");
            boolean selIsInventory = selSource.equals("storage") || selSource.equals("hotbar");

            if (selIsInventory && isGearSlot) {
                // inventory -> gear slot: validate compatibility, then equip
                if (!isItemCompatibleWithSlot(this.selectedItem, source, index)) {
                    clearSelection(cmd); clearGearSlotOverlays(cmd); clearInventoryInvalidOverlays(cmd); updateInspectPanel(cmd, null);
                    return;
                }
                handleEquip(this.selectedSlotId, slotId, ref, store, cmd);
                return;
            } else if (selIsGear && isInventorySlot) {
                // empty inventory slot = unequip, always allowed
                // occupied inventory slot = only allow if that item is compatible with source gear slot
                if (clickedItem != null && !isItemCompatibleWithSlot(clickedItem, selSource, selIndex)) {
                    clearSelection(cmd); clearGearSlotOverlays(cmd); clearInventoryInvalidOverlays(cmd); updateInspectPanel(cmd, null);
                    return;
                }
                handleEquip(this.selectedSlotId, slotId, ref, store, cmd);
                return;
            } else if (selIsGear && isGearSlot) {
                if (!isItemCompatibleWithSlot(this.selectedItem, source, index)
                        || (clickedItem != null && !isItemCompatibleWithSlot(clickedItem, selSource, selIndex))) {
                    clearSelection(cmd); clearGearSlotOverlays(cmd); clearInventoryInvalidOverlays(cmd); updateInspectPanel(cmd, null);
                    return;
                }
                handleEquip(this.selectedSlotId, slotId, ref, store, cmd);
                return;
            } else if (selIsInventory && isInventorySlot) {
                // inventory -> inventory: change selection to newly clicked item, or deselect if empty
                if (clickedItem == null) {
                    clearSelection(cmd); clearGearSlotOverlays(cmd); clearInventoryInvalidOverlays(cmd); updateInspectPanel(cmd, null);
                    return;
                }
                // fall through to re-select below
            }
        }

        // clicking an empty slot at this point — do nothing
        if (clickedItem == null) return;

        // select the clicked slot and apply compatibility overlays
        clearSelection(cmd); clearGearSlotOverlays(cmd); clearInventoryInvalidOverlays(cmd);
        this.selectedSlotId = slotId;
        this.selectedItem   = clickedItem;
        applySelectedOverlay(cmd, source, index, true);
        applyGearSlotCompatibility(cmd, clickedItem, ref, store);
        updateInspectPanel(cmd, clickedStack);
    }

    // Equip / unequip / swap
    private void handleEquip(@Nonnull String fromSlotId, @Nonnull String toSlotId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        String[] fromParts = fromSlotId.split(":");
        String[] toParts   = toSlotId.split(":");
        int fromIndex = Integer.parseInt(fromParts[1]);
        int toIndex   = Integer.parseInt(toParts[1]);

        ItemContainer fromInv = getContainerBySource(fromParts[0], ref, store);
        ItemContainer toInv   = getContainerBySource(toParts[0],  ref, store);
        if (fromInv == null || toInv == null) { clearSelection(cmd); return; }

        ItemStack fromStack = fromInv.getItemStack((short) fromIndex);
        ItemStack toStack   = toInv.getItemStack((short) toIndex);

        toInv.replaceItemStackInSlot((short) toIndex, toStack, fromStack);
        fromInv.replaceItemStackInSlot((short) fromIndex, fromStack, toStack != null && !toStack.isEmpty() ? toStack : null);

        clearSelection(cmd); clearGearSlotOverlays(cmd); clearInventoryInvalidOverlays(cmd);
        applyFullState(ref, store, cmd);

        // delayed refresh to catch async stat recalculation after equip
        store.getExternalData().getWorld().execute(() -> {
            Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
            if (rpgPlayer == null) return;

            rpgPlayer.calculateGearScore(ref, store);
            rpgPlayer.calculateAffixStats(ref, store);

            UICommandBuilder refreshCmd = new UICommandBuilder();
            pushStats(ref, store, refreshCmd);
            sendUpdate(refreshCmd, false);
        });
    }

    // Full state push — called on open and after any equip/unequip
    private void applyFullState(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        pushStorageSlots(ref, store, cmd);
        pushHotbarSlots(ref, store, cmd);
        pushArmorSlots(ref, store, cmd);
        pushUtilitySlots(ref, store, cmd);
        pushStats(ref, store, cmd);
        pushQuickCraftStates(ref, store, cmd);
        updateInspectPanel(cmd, null);
    }

    // push all storage slot item ids
    private void pushStorageSlots(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComp == null) return;
        ItemContainer inv = storageComp.getInventory();
        for (short i = 0; i < Math.min(inv.getCapacity(), STORAGE_SLOTS); i++) {
            ItemStack stack = inv.getItemStack(i);
            if (stack != null && !stack.isEmpty()) cmd.set("#InvStorageItem" + i + ".ItemId", stack.getItem().getId());
            else cmd.setNull("#InvStorageItem" + i + ".ItemId");
        }
    }

    // push all hotbar slot item ids
    private void pushHotbarSlots(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        InventoryComponent.Hotbar hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbarComp == null) return;
        ItemContainer inv = hotbarComp.getInventory();
        for (short i = 0; i < Math.min(inv.getCapacity(), HOTBAR_SLOTS); i++) {
            ItemStack stack = inv.getItemStack(i);
            if (stack != null && !stack.isEmpty()) cmd.set("#InvHotbarItem" + i + ".ItemId", stack.getItem().getId());
            else cmd.setNull("#InvHotbarItem" + i + ".ItemId");
        }
    }

    // push all armor slot item ids
    private void pushArmorSlots(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        InventoryComponent.Armor armorComp = store.getComponent(ref, InventoryComponent.Armor.getComponentType());
        if (armorComp == null) return;
        ItemContainer inv = armorComp.getInventory();
        for (short i = 0; i < Math.min(inv.getCapacity(), ARMOR_SLOTS); i++) {
            ItemStack stack = inv.getItemStack(i);
            if (stack != null && !stack.isEmpty()) cmd.set("#ArmorItem" + i + ".ItemId", stack.getItem().getId());
            else cmd.setNull("#ArmorItem" + i + ".ItemId");
        }
    }

    // push all utility slot item ids and highlight the active slot
    private void pushUtilitySlots(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        InventoryComponent.Utility utilityComp = store.getComponent(ref, InventoryComponent.Utility.getComponentType());
        if (utilityComp == null) return;
        ItemContainer inv       = utilityComp.getInventory();
        byte          activeSlot = utilityComp.getActiveSlot();
        for (short i = 0; i < Math.min(inv.getCapacity(), UTILITY_SLOTS); i++) {
            ItemStack stack = inv.getItemStack(i);
            if (stack != null && !stack.isEmpty()) cmd.set("#UtilityItem" + i + ".ItemId", stack.getItem().getId());
            else cmd.setNull("#UtilityItem" + i + ".ItemId");
            // gold highlight on the active utility slot so player knows which one is equipped
            cmd.set("#UtilityActiveOverlay" + i + ".Visible", activeSlot >= 0 && i == activeSlot);
        }
    }

    // push all stat values to the left panel labels
    private void pushStats(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        try {
            Component_RPG_Player rpg = store.getComponent(ref, componentTypeRPGPlayer);
            if (rpg == null) return;

            var stats = rpg.stats;

            // overview
            cmd.set("#StatPlayerLevel.Text", String.valueOf(rpg.level));
            cmd.set("#StatGearScore.Text",   String.valueOf(rpg.gearScore));

            // offense
            cmd.set("#StatPhysDmgFlat.Text",     "+" + fmt(stats.getFlatDamage("Physical")));
            cmd.set("#StatPhysDmgPct.Text",      "+" + fmtPct(stats.getIncreasedDamage("Physical")));
            cmd.set("#StatMagicDmgFlat.Text",    "+" + fmt(stats.getFlatDamage("Magic")));
            cmd.set("#StatMagicDmgPct.Text",     "+" + fmtPct(stats.getIncreasedDamage("Magic")));
            cmd.set("#StatFireDmgFlat.Text",     "+" + fmt(stats.getFlatDamage("Fire")));
            cmd.set("#StatFireDmgPct.Text",      "+" + fmtPct(stats.getIncreasedDamage("Fire")));
            cmd.set("#StatIceDmgFlat.Text",      "+" + fmt(stats.getFlatDamage("Ice")));
            cmd.set("#StatIceDmgPct.Text",       "+" + fmtPct(stats.getIncreasedDamage("Ice")));
            cmd.set("#StatLightningDmgFlat.Text","+" + fmt(stats.getFlatDamage("Lightning")));
            cmd.set("#StatLightningDmgPct.Text", "+" + fmtPct(stats.getIncreasedDamage("Lightning")));
            cmd.set("#StatPoisonDmgFlat.Text",   "+" + fmt(stats.getFlatDamage("Poison")));
            cmd.set("#StatPoisonDmgPct.Text",    "+" + fmtPct(stats.getIncreasedDamage("Poison")));
            cmd.set("#StatCritChance.Text",      fmtPct(stats.getCriticalStrikeChance()));
            cmd.set("#StatCritDamage.Text",      fmt(stats.getCriticalStrikeDamage()) + "x");
            cmd.set("#StatAxeDmg.Text",          fmtPct(stats.getIncreasedDamage("Axe")));
            cmd.set("#StatBattleaxeDmg.Text",    fmtPct(stats.getIncreasedDamage("Battleaxe")));
            cmd.set("#StatClubDmg.Text",         fmtPct(stats.getIncreasedDamage("Club")));
            cmd.set("#StatDaggersDmg.Text",      fmtPct(stats.getIncreasedDamage("Daggers")));
            cmd.set("#StatKunaiDmg.Text",        fmtPct(stats.getIncreasedDamage("Kunai")));
            cmd.set("#StatLongswordDmg.Text",    fmtPct(stats.getIncreasedDamage("Longsword")));
            cmd.set("#StatMaceDmg.Text",         fmtPct(stats.getIncreasedDamage("Mace")));
            cmd.set("#StatShortbowDmg.Text",     fmtPct(stats.getIncreasedDamage("Shortbow")));
            cmd.set("#StatCrossbowDmg.Text",     fmtPct(stats.getIncreasedDamage("Crossbow")));
            cmd.set("#StatSwordDmg.Text",        fmtPct(stats.getIncreasedDamage("Sword")));

            // defense
            cmd.set("#StatDodgeChance.Text",    fmtPct(stats.getDodgeChance()));
            cmd.set("#StatStability.Text",      fmtPct(stats.getStabilityPercent(false)));
            cmd.set("#StatParryWindow.Text",    "+" + fmt(stats.getParryWindow()) + "(s)");
            cmd.set("#StatBarrierOnBlock.Text", fmtPct(stats.getBarrierOnBlock()));

            // resources
            cmd.set("#StatLifeFlat.Text",    "+" + fmt(stats.getFlatResource("Life")));
            cmd.set("#StatLifePct.Text",     "+" + fmtPct(stats.getIncreasedResource("Life")));
            cmd.set("#StatStaminaFlat.Text", "+" + fmt(stats.getFlatResource("Stamina")));
            cmd.set("#StatStaminaPct.Text",  "+" + fmtPct(stats.getIncreasedResource("Stamina")));
            cmd.set("#StatManaFlat.Text",    "+" + fmt(stats.getFlatResource("Mana")));
            cmd.set("#StatManaPct.Text",     "+" + fmtPct(stats.getIncreasedResource("Mana")));

            // regeneration
            cmd.set("#StatLifeRegenFlat.Text",    "+" + fmt(stats.getFlatResourceRegen("Life")) + "s");
            cmd.set("#StatLifeRegenPct.Text",     "+" + fmtPct(stats.getIncreasedResourceRegen("Life")));
            cmd.set("#StatStaminaRegenFlat.Text", "+" + fmt(stats.getFlatResourceRegen("Stamina")) + "s");
            cmd.set("#StatStaminaRegenPct.Text",  "+" + fmtPct(stats.getIncreasedResourceRegen("Stamina")));
            cmd.set("#StatManaRegenFlat.Text",    "+" + fmt(stats.getFlatResourceRegen("Mana")) + "s");
            cmd.set("#StatManaRegenPct.Text",     "+" + fmtPct(stats.getIncreasedResourceRegen("Mana")));

            // resistances
            cmd.set("#StatPhysResist.Text",      fmtPct(stats.getResistance("Physical")));
            cmd.set("#StatMagicResist.Text",     fmtPct(stats.getResistance("Magic")));
            cmd.set("#StatElementalResist.Text", fmtPct(stats.getResistance("Elemental")));
            cmd.set("#StatFireResist.Text",      fmtPct(stats.getResistance("Fire")));
            cmd.set("#StatIceResist.Text",       fmtPct(stats.getResistance("Ice")));
            cmd.set("#StatLightningResist.Text", fmtPct(stats.getResistance("Lightning")));
            cmd.set("#StatPoisonResist.Text",    fmtPct(stats.getResistance("Poison")));
            cmd.set("#StatFallResist.Text",      fmtPct(stats.getResistance("Fall")));

            // advanced
            cmd.set("#StatLifeLeech.Text",      fmtPct(stats.getLeech("Life")));
            cmd.set("#StatManaLeech.Text",      fmtPct(stats.getLeech("Mana")));
            cmd.set("#StatStaminaLeech.Text",   fmtPct(stats.getLeech("Stamina")));
            cmd.set("#StatDmgFromMana.Text",    fmtPct(stats.getDamageTakenFrom("Mana")));
            cmd.set("#StatDmgFromStamina.Text", fmtPct(stats.getDamageTakenFrom("Stamina")));

            // utility
            cmd.set("#StatRunSpeed.Text", "+" + fmtPct(stats.getRunSpeedPercent()));
            cmd.set("#StatAmmo.Text",     "+" + fmt(stats.getAddedAmmo()));
            cmd.set("#StatAmmoRegen.Text","+" + fmtPct(stats.getAmmoRegenPercent()));
        } catch (Exception _) {}
    }

    // Inspect panel — populated when any item is selected
    private void updateInspectPanel(@Nonnull UICommandBuilder cmd, @Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            cmd.set("#InspectEmpty.Visible",   true);
            cmd.set("#InspectDetails.Visible", false);
            return;
        }

        cmd.set("#InspectEmpty.Visible",   false);
        cmd.set("#InspectDetails.Visible", true);

        // item icon, name, rarity
        String itemId = stack.getItem().getId();
        String nameKey = stack.getItem().getTranslationProperties().getName();
        String name = Message.translation(nameKey).getAnsiMessage();
        cmd.set("#InspectItemIcon.ItemId", itemId);
        cmd.set("#InspectItemName.Text", name);

        String rarity = deriveRarity(itemId);
        cmd.set("#InspectItemRarity.Text", rarity.toUpperCase());

        // gear score from metadata
        Integer gearScore = stack.getFromMetadataOrNull("GearScore", com.hypixel.hytale.codec.Codec.INTEGER);
        cmd.set("#InspectGearScore.Text", gearScore != null ? String.valueOf(gearScore) : "-");

        // implicits and weapon damage from metadata
        String[] implicits = stack.getFromMetadataOrNull("implicits", com.hypixel.hytale.codec.Codec.STRING_ARRAY);
        String weaponDamage = "";
        List<String> implicitLines = new ArrayList<>();
        if (implicits != null) {
            for (String implicit : implicits) {
                String[] p = implicit.split("\\|");
                if (p.length < 3) continue;
                StatType stat;
                try { stat = StatType.valueOf(p[0]); } catch (Exception e) { continue; }
                if (weaponDamage.isEmpty() && StatTypeInfo.isWeaponDamageStat(stat)) weaponDamage = p[2];
                else implicitLines.add(p[2]);
            }
        }

        // weapon damage row
        boolean hasWeaponDamage = !weaponDamage.isEmpty();
        cmd.set("#InspectWeaponDamageRow.Visible",    hasWeaponDamage);
        cmd.set("#InspectImplicitSeparator.Visible",  hasWeaponDamage || !implicitLines.isEmpty());
        if (hasWeaponDamage) cmd.set("#InspectWeaponDamageLabel.Text", weaponDamage);

        // implicit lines 1-5
        for (int i = 0; i < 5; i++) {
            boolean show = i < implicitLines.size();
            cmd.set("#InspectImplicit" + (i + 1) + ".Visible", show);
            if (show) cmd.set("#InspectImplicit" + (i + 1) + ".Text", implicitLines.get(i));
        }

        // affixes from metadata
        String[] affixes = stack.getFromMetadataOrNull("affixes", com.hypixel.hytale.codec.Codec.STRING_ARRAY);
        List<String> affixLines = new ArrayList<>();
        if (affixes != null) {
            for (String affix : affixes) {
                String[] p = affix.split("\\|");
                if (p.length < 3) continue;
                Affix affixDef = AffixPool.getAffixByStatName(p[0]);
                if (affixDef == null) continue;
                float value = Float.parseFloat(p[1]);
                int   tier  = (int) Float.parseFloat(p[2]);
                affixLines.add("T" + tier + " " + affixDef.display().formatted(Math.round(value * 10) / 10f));
            }
        }

        boolean hasAffixes = !affixLines.isEmpty();
        cmd.set("#InspectAffixSeparator.Visible", hasAffixes);
        for (int i = 0; i < 4; i++) {
            boolean show = i < affixLines.size();
            cmd.set("#InspectAffix" + (i + 1) + ".Visible", show);
            if (show) cmd.set("#InspectAffix" + (i + 1) + ".Text", affixLines.get(i));
        }
    }

    // Gear slot compatibility overlays — green = can equip, red = cannot
    private void applyGearSlotCompatibility(@Nonnull UICommandBuilder cmd, @Nonnull Item item, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        boolean isGear    = isGearItem(item);
        boolean isUtility = isUtilityItem(item);

        // armor slots — green only if gear AND matches that specific slot, red otherwise
        for (int i = 0; i < ARMOR_SLOTS; i++) {
            boolean canEquip = isGear && isCompatibleArmorSlot(item, i);
            cmd.set("#ArmorValidOverlay" + i + ".Visible",   canEquip);
            cmd.set("#ArmorInvalidOverlay" + i + ".Visible", !canEquip);
        }

        // utility slots — green if utility item, red if not
        for (int i = 0; i < UTILITY_SLOTS; i++) {
            cmd.set("#UtilityValidOverlay" + i + ".Visible",   isUtility);
            cmd.set("#UtilityInvalidOverlay" + i + ".Visible", !isUtility);
        }

        // inventory slots — if selected item came from a gear slot, mark inventory items
        // red if they aren't compatible with that source slot, green if they are
        if (this.selectedSlotId != null) {
            String selSource = this.selectedSlotId.split(":")[0];
            boolean selIsGear = selSource.equals("armor") || selSource.equals("utility");
            int     selIndex  = Integer.parseInt(this.selectedSlotId.split(":")[1]);

            if (selIsGear) {
                // mark each storage slot based on whether that item could go into the source gear slot
                InventoryComponent.Storage storageComp = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
                if (storageComp != null) {
                    ItemContainer inv = storageComp.getInventory();
                    for (short i = 0; i < Math.min(inv.getCapacity(), STORAGE_SLOTS); i++) {
                        ItemStack stack = inv.getItemStack(i);
                        Item slotItem = stack != null && !stack.isEmpty() ? stack.getItem() : null;
                        boolean compatible = slotItem != null && isItemCompatibleWithSlot(slotItem, selSource, selIndex);
                        cmd.set("#InvStorageInvalidOverlay" + i + ".Visible", slotItem != null && !compatible);
                    }
                }

                // mark each hotbar slot
                InventoryComponent.Hotbar hotbarComp = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
                if (hotbarComp != null) {
                    ItemContainer inv = hotbarComp.getInventory();
                    for (short i = 0; i < Math.min(inv.getCapacity(), HOTBAR_SLOTS); i++) {
                        ItemStack stack = inv.getItemStack(i);
                        Item slotItem = stack != null && !stack.isEmpty() ? stack.getItem() : null;
                        boolean compatible = slotItem != null && isItemCompatibleWithSlot(slotItem, selSource, selIndex);
                        cmd.set("#InvHotbarInvalidOverlay" + i + ".Visible", slotItem != null && !compatible);
                    }
                }
            }
        }
    }

    private void clearGearSlotOverlays(@Nonnull UICommandBuilder cmd) {
        for (int i = 0; i < ARMOR_SLOTS;   i++) { cmd.set("#ArmorValidOverlay" + i + ".Visible", false);   cmd.set("#ArmorInvalidOverlay" + i + ".Visible", false); }
        for (int i = 0; i < UTILITY_SLOTS; i++) { cmd.set("#UtilityValidOverlay" + i + ".Visible", false); cmd.set("#UtilityInvalidOverlay" + i + ".Visible", false); }
    }

    private void clearInventoryInvalidOverlays(@Nonnull UICommandBuilder cmd) {
        for (int i = 0; i < STORAGE_SLOTS; i++) cmd.set("#InvStorageInvalidOverlay" + i + ".Visible", false);
        for (int i = 0; i < HOTBAR_SLOTS;  i++) cmd.set("#InvHotbarInvalidOverlay" + i + ".Visible", false);
    }

    // Selection overlay helpers
    private void clearSelection(@Nonnull UICommandBuilder cmd) {
        if (this.selectedSlotId != null) {
            String[] p = this.selectedSlotId.split(":");
            applySelectedOverlay(cmd, p[0], Integer.parseInt(p[1]), false);
        }
        this.selectedSlotId = null;
        this.selectedItem   = null;
    }

    private void applySelectedOverlay(@Nonnull UICommandBuilder cmd, @Nonnull String source, int index, boolean visible) {
        String element = switch (source) {
            case "storage" -> "#InvStorageSelectedOverlay" + index;
            case "hotbar"  -> "#InvHotbarSelectedOverlay" + index;
            case "armor"   -> "#ArmorSelectedOverlay" + index;
            case "utility" -> "#UtilitySelectedOverlay" + index;
            default -> null;
        };
        if (element != null) cmd.set(element + ".Visible", visible);
    }

    // Slot and container lookup helpers
    @Nullable
    private ItemStack getStackFromSlot(@Nonnull String source, int index, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        ItemContainer inv = getContainerBySource(source, ref, store);
        if (inv == null) return null;
        return inv.getItemStack((short) index);
    }

    @Nullable
    private ItemContainer getContainerBySource(@Nonnull String source, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        return switch (source) {
            case "storage" -> { InventoryComponent.Storage  c = store.getComponent(ref, InventoryComponent.Storage.getComponentType());  yield c != null ? c.getInventory() : null; }
            case "hotbar"  -> { InventoryComponent.Hotbar   c = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());   yield c != null ? c.getInventory() : null; }
            case "armor"   -> { InventoryComponent.Armor    c = store.getComponent(ref, InventoryComponent.Armor.getComponentType());    yield c != null ? c.getInventory() : null; }
            case "utility" -> { InventoryComponent.Utility  c = store.getComponent(ref, InventoryComponent.Utility.getComponentType()); yield c != null ? c.getInventory() : null; }
            default -> null;
        };
    }

    // Item classification helpers
    private static boolean isGearItem(@Nonnull Item item) {
        return item.getArmor() != null && item.getArmor().getArmorSlot() != null;
    }

    private static boolean isUtilityItem(@Nonnull Item item) {
        return item.getUtility().isUsable();
    }

    private static boolean isCompatibleArmorSlot(@Nonnull Item item, int slot) {
        if (item.getArmor() == null || item.getArmor().getArmorSlot() == null) return false;
        if (slot == 0) return item.getArmor().getArmorSlot() == ItemArmorSlot.Head;
        if (slot == 1) return item.getArmor().getArmorSlot() == ItemArmorSlot.Chest;
        if (slot == 2) return item.getArmor().getArmorSlot() == ItemArmorSlot.Hands;
        if (slot == 3) return item.getArmor().getArmorSlot() == ItemArmorSlot.Legs;
        return false;
    }

    private static String deriveRarity(@Nonnull String itemId) {
        for (String r : new String[]{"Legendary", "Epic", "Rare", "Uncommon", "Common"}) { if (itemId.endsWith("_" + r)) return r; }
        return "Common";
    }

    // checks if an item is compatible with a specific gear slot
    private static boolean isItemCompatibleWithSlot(@Nonnull Item item, @Nonnull String slotSource, int slotIndex) {
        if (slotSource.equals("utility")) return isUtilityItem(item);
        if (slotSource.equals("armor"))   return isCompatibleArmorSlot(item, slotIndex);
        return false;
    }

    // Formatting helpers
    private static String fmt(float v) { float r = Math.round(v * 10) / 10f; return r == (int) r ? String.valueOf((int) r) : String.valueOf(r); }
    private static String fmtPct(float v) { return fmt(v) + "%"; }

    // PageData codec
    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .build();
        public String action;
    }
}