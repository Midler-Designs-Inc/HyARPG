package com.example.hyarpg.ui;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
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

// Java Imports
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.example.hyarpg.modules.Module_RPGSystem.componentTypeRPGPlayer;

public class CustomPage_Inventory extends InteractiveCustomUIPage<CustomPage_Inventory.PageData> {

    // slot counts
    private static final int STORAGE_SLOTS = 36;
    private static final int HOTBAR_SLOTS  = 9;
    private static final int ARMOR_SLOTS   = 4;
    private static final int UTILITY_SLOTS = 5;

    // armor slot indices — Helmet=0, Chest=1, Gloves=2, Pants=3
    private static final String[] ARMOR_SLOT_LABELS = { "Helmet", "Chest", "Gloves", "Pants" };

    // rarity display colors
    private static final String COLOR_COMMON    = "#aaaaaa";
    private static final String COLOR_UNCOMMON  = "#55ff55";
    private static final String COLOR_RARE      = "#5588ff";
    private static final String COLOR_EPIC      = "#cc44cc";
    private static final String COLOR_LEGENDARY = "#ffaa00";

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

        // apply full initial state
        applyFullState(ref, store, cmd);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null) { sendUpdate((UICommandBuilder) null, false); return; }

        UICommandBuilder cmd = new UICommandBuilder();

        if (data.action.startsWith("select:")) {
            handleSlotClick(data.action.substring("select:".length()), ref, store, cmd);
        } else {
            sendUpdate((UICommandBuilder) null, false);
            return;
        }

        sendUpdate(cmd, false);
    }

    // -------------------------------------------------------------------------
    // Slot click handler — selection, deselection, and equip/swap routing
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

        if (clickingSelf) {
            // clicking the already-selected slot deselects
            clearSelection(cmd);
            clearGearSlotOverlays(cmd);
            clearInventoryInvalidOverlays(cmd);
            updateInspectPanel(cmd, null);
            return;
        }

        if (hasSelection) {
            String selSource = this.selectedSlotId.split(":")[0];
            boolean selIsGear      = selSource.equals("armor") || selSource.equals("utility");
            boolean selIsInventory = selSource.equals("storage") || selSource.equals("hotbar");

            if (selIsInventory && isGearSlot) {
                // inventory item -> gear slot: attempt equip
                handleEquip(this.selectedSlotId, slotId, ref, store, cmd);
                return;
            } else if (selIsGear && isInventorySlot) {
                // gear item -> inventory slot: attempt unequip into specific slot
                handleUnequip(this.selectedSlotId, slotId, ref, store, cmd);
                return;
            } else if (selIsGear && isGearSlot) {
                // gear -> gear: swap between gear slots
                handleGearSwap(this.selectedSlotId, slotId, ref, store, cmd);
                return;
            }
            // inventory -> inventory or any other combo: just change selection
        }

        // select the clicked slot
        clearSelection(cmd);
        clearGearSlotOverlays(cmd);
        clearInventoryInvalidOverlays(cmd);

        if (clickedItem != null) {
            this.selectedSlotId = slotId;
            this.selectedItem   = clickedItem;
            applySelectedOverlay(cmd, source, index, true);

            // if an inventory item is selected, highlight valid gear slots
            if (isInventorySlot) applyGearSlotCompatibility(cmd, clickedItem, ref, store);

            updateInspectPanel(cmd, clickedStack);
        } else {
            this.selectedSlotId = null;
            this.selectedItem   = null;
            updateInspectPanel(cmd, null);
        }
    }

    // -------------------------------------------------------------------------
    // Equip / unequip / swap — TODO: replace replaceItemStackInSlot calls
    // with the proper equip API once confirmed
    // -------------------------------------------------------------------------
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

        // TODO: replace with proper equip API call when confirmed
        toInv.replaceItemStackInSlot((short) toIndex, toStack, fromStack);
        fromInv.replaceItemStackInSlot((short) fromIndex, fromStack, toStack != null && !toStack.isEmpty() ? toStack : null);

        clearSelection(cmd);
        clearGearSlotOverlays(cmd);
        clearInventoryInvalidOverlays(cmd);
        applyFullState(ref, store, cmd);
    }
    private void handleUnequip(@Nonnull String fromSlotId, @Nonnull String toSlotId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        // same logic as equip — just swapping direction
        handleEquip(fromSlotId, toSlotId, ref, store, cmd);
    }
    private void handleGearSwap(@Nonnull String fromSlotId, @Nonnull String toSlotId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        handleEquip(fromSlotId, toSlotId, ref, store, cmd);
    }

    // -------------------------------------------------------------------------
    // Full state push — called on open and after any equip/unequip
    // -------------------------------------------------------------------------
    private void applyFullState(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        pushStorageSlots(ref, store, cmd);
        pushHotbarSlots(ref, store, cmd);
        pushArmorSlots(ref, store, cmd);
        pushUtilitySlots(ref, store, cmd);
        pushStats(ref, store, cmd);
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
            cmd.set("#UtilityActiveOverlay" + i + ".Visible", i == activeSlot);
        }
    }

    // push all stat values to the left panel labels
    private void pushStats(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
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
        cmd.set("#StatLifeRegenPct.Text",     "+" + fmtPct(stats.getFlatResourceRegen("Life")));
        cmd.set("#StatStaminaRegenFlat.Text", "+" + fmt(stats.getFlatResourceRegen("Stamina")) + "s");
        cmd.set("#StatStaminaRegenPct.Text",  "+" + fmtPct(stats.getFlatResourceRegen("Stamina")));
        cmd.set("#StatManaRegenFlat.Text",    "+" + fmt(stats.getFlatResourceRegen("Mana")) + "s");
        cmd.set("#StatManaRegenPct.Text",     "+" + fmtPct(stats.getFlatResourceRegen("Mana")));

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
    }

    // -------------------------------------------------------------------------
    // Inspect panel — populated when any item is selected
    // -------------------------------------------------------------------------
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
        cmd.set("#InspectItemIcon.ItemId", itemId);
        cmd.set("#InspectItemName.Text",   stack.getItem().getId());

        String rarity     = deriveRarity(itemId);
        String rarityColor = rarityColor(rarity);
        cmd.set("#InspectItemRarity.Text",      rarity.toUpperCase());

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

    // -------------------------------------------------------------------------
    // Gear slot compatibility overlays — green = can equip, red = cannot
    // -------------------------------------------------------------------------

    private void applyGearSlotCompatibility(@Nonnull UICommandBuilder cmd, @Nonnull Item item, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        String itemId = item.getId();
        boolean isGear    = isGearItem(item);
        boolean isUtility = isUtilityItem(item);

        for (int i = 0; i < ARMOR_SLOTS; i++) {
            boolean canEquip = isGear && isCompatibleArmorSlot(itemId, i);
            cmd.set("#ArmorValidOverlay" + i + ".Visible",   canEquip);
            cmd.set("#ArmorInvalidOverlay" + i + ".Visible", isGear && !canEquip);
        }
        for (int i = 0; i < UTILITY_SLOTS; i++) {
            cmd.set("#UtilityValidOverlay" + i + ".Visible",   isUtility);
            cmd.set("#UtilityInvalidOverlay" + i + ".Visible", !isUtility && !isGear);
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

    // -------------------------------------------------------------------------
    // Selection overlay helpers
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Slot and container lookup helpers
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Item classification helpers
    // -------------------------------------------------------------------------

    private static boolean isGearItem(@Nonnull Item item) {
        String[] cats = item.getCategories();
        if (cats == null) return false;
        for (String cat : cats) { if ("Items.HyARPG.Gear".equals(cat)) return true; }
        return false;
    }

    private static boolean isUtilityItem(@Nonnull Item item) {
        return item.getUtility() != null && item.getUtility().isUsable();
    }

    private static boolean isCompatibleArmorSlot(@Nonnull String itemId, int slot) {
        // derive armor type from item id and match to slot index
        if (slot == 0) return itemId.contains("Helmet") || itemId.contains("Hood");
        if (slot == 1) return itemId.contains("Chest") || itemId.contains("Vest") || itemId.contains("Tunic");
        if (slot == 2) return itemId.contains("Gloves");
        if (slot == 3) return itemId.contains("Pants");
        return false;
    }

    private static String deriveRarity(@Nonnull String itemId) {
        for (String r : new String[]{"Legendary", "Epic", "Rare", "Uncommon", "Common"}) { if (itemId.endsWith("_" + r)) return r; }
        return "Common";
    }

    private static String rarityColor(@Nonnull String rarity) {
        return switch (rarity) {
            case "Uncommon"  -> COLOR_UNCOMMON;
            case "Rare"      -> COLOR_RARE;
            case "Epic"      -> COLOR_EPIC;
            case "Legendary" -> COLOR_LEGENDARY;
            default          -> COLOR_COMMON;
        };
    }

    // -------------------------------------------------------------------------
    // Formatting helpers
    // -------------------------------------------------------------------------

    private static String fmt(float v) { float r = Math.round(v * 10) / 10f; return r == (int) r ? String.valueOf((int) r) : String.valueOf(r); }
    private static String fmtPct(float v) { return fmt(v) + "%"; }

    // -------------------------------------------------------------------------
    // PageData codec
    // -------------------------------------------------------------------------

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .build();
        public String action;
    }
}
