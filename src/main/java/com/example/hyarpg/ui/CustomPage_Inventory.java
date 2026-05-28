package com.example.hyarpg.ui;

// Hytale Imports
import com.example.hyarpg.utils.items.ItemFactory;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.metadata.ItemDisplayMetadata;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import static com.example.hyarpg.modules.Module_RPGSystem.componentTypeRPGPlayer;

// Java Imports
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CustomPage_Inventory extends InteractiveCustomUIPage<CustomPage_Inventory.PageData> {

    // slot counts
    private static final int STORAGE_SLOTS = 36;
    private static final int HOTBAR_SLOTS  = 9;
    private static final int ARMOR_SLOTS   = 4;
    private static final int UTILITY_SLOTS = 4;

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

        // bind grid click when clicking to start a drag
        events.addEventBinding(CustomUIEventBindingType.Dropped, "#InvStorageGrid", EventData.of("Action", "dropped:storage"));
        events.addEventBinding(CustomUIEventBindingType.Dropped, "#InvHotbarGrid", EventData.of("Action", "dropped:hotbar"));
        events.addEventBinding(CustomUIEventBindingType.Dropped, "#ArmorGrid", EventData.of("Action", "dropped:armor"));
        events.addEventBinding(CustomUIEventBindingType.Dropped, "#UtilityGrid", EventData.of("Action", "dropped:utility"));

        // bind grid click while dragging events (right clicked a single item to start, needs to left click to place)
        events.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#InvStorageGrid", EventData.of("Action", "clickedWhileDragging:storage"));
        events.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#InvHotbarGrid", EventData.of("Action", "clickedWhileDragging:hotbar"));
        events.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#ArmorGrid", EventData.of("Action", "clickedWhileDragging:armor"));
        events.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#UtilityGrid", EventData.of("Action", "clickedWhileDragging:utility"));

        // bind quick-craft slot clicks
        for (int i = 0; i < QUICK_CRAFT_SLOTS; i++) events.addEventBinding(CustomUIEventBindingType.Activating, "#QuickCraftSlot" + i, EventData.of("Action", "quickcraft:" + i));

        // apply full initial state
        applyFullState(ref, store, cmd);
    }

    // process UI element event bindings
    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        // bail if there isn't a valid action
        if (data.action == null) { sendUpdate(null, false); return; }

        UICommandBuilder cmd = new UICommandBuilder();

        // get the target container name from the action string and then get its inventory section ID
        String targetContainer = data.action.split(":")[1];
        int targetSectionId = targetContainer.equals("storage") ? InventoryComponent.STORAGE_SECTION_ID : targetContainer.equals("hotbar") ? InventoryComponent.HOTBAR_SECTION_ID : targetContainer.equals("armor") ? InventoryComponent.ARMOR_SECTION_ID : InventoryComponent.UTILITY_SECTION_ID;

        // quick craft slot was clicked
        if (data.action.startsWith("quickcraft:")) handleQuickCraft(Integer.parseInt(data.action.substring("quickcraft:".length())), ref, store, cmd);

        // left click drag completed — move the full stack from source to destination
        else if (data.action.startsWith("dropped:")) {
            handleDragComplete(ref, data.sourceSectionId, targetSectionId, data.sourceSlotId, data.slotIndex, false);
            applyFullState(ref, store, cmd);
        }

        // right click drag placed — move a single item from source to destination
        else if (data.action.startsWith("clickedWhileDragging:")) {
            // perform the single item move
            handleDragComplete(ref, data.dragSourceSectionId, targetSectionId, data.dragSourceSlotId, data.slotIndex, true);

            // reopen the page to clear client drag state — replicates the escape/reopen cycle
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (player != null && playerRef != null) player.getPageManager().openCustomPage(ref, store, new CustomPage_Inventory(playerRef));
            return;
        }

        // push empty state
        else {
            sendUpdate(null, false);
            return;
        }

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

    // called when a drag event is completed, moves items between containers/slots or stacks them
    private void handleDragComplete(@Nonnull Ref<EntityStore> ref, int sourceSectionId, int targetSectionId, int sourceSlot, int targetSlot, boolean moveSingle) {
        // get the store from the ref
        Store<EntityStore> store = ref.getStore();

        // resolve source and target containers from their section ids
        ItemContainer sourceInv = getContainerBySectionId(sourceSectionId, ref, store);
        ItemContainer targetInv = getContainerBySectionId(targetSectionId, ref, store);
        if (sourceInv == null || targetInv == null) return;

        // get the source and target stacks
        ItemStack sourceStack = sourceInv.getItemStack((short) sourceSlot);
        ItemStack targetStack = targetInv.getItemStack((short) targetSlot);
        if (sourceStack == null || sourceStack.isEmpty()) return;

        // determine how many items to move — single item for right click, full stack for left click
        int itemQty = moveSingle ? 1 : sourceStack.getQuantity();

        // check target state for merge or swap decisions
        boolean targetIsEmpty = targetStack == null || targetStack.isEmpty();
        boolean sameItemType = !targetIsEmpty && ItemStack.isSameItemType(sourceStack, targetStack);

        // move into empty slot directly
        if (targetIsEmpty) {
            sourceInv.moveItemStackFromSlotToSlot((short) sourceSlot, itemQty, targetInv, (short) targetSlot);
            return;
        }

        // merge into same item type if there is room in the stack
        if (sameItemType) {
            int maxStack = targetStack.getItem().getMaxStack();
            int room = maxStack - targetStack.getQuantity();
            if (room > 0) {
                int moveQty = Math.min(itemQty, room);
                sourceInv.moveItemStackFromSlotToSlot((short) sourceSlot, moveQty, targetInv, (short) targetSlot);
                return;
            }
        }

        // swap the two slots if items are different or stack is full and this is a full stack move
        if (!moveSingle) {
            sourceInv.replaceItemStackInSlot((short) sourceSlot, sourceStack, targetStack);
            targetInv.replaceItemStackInSlot((short) targetSlot, targetStack, sourceStack);
        }
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

            // update all stats before pushing it
            rpg.calculateGearScore(ref, store);
            rpg.calculateAffixStats(ref, store);

            // extract the stats
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

    // formatting helpers
    private static String fmt(float v) { float r = Math.round(v * 10) / 10f; return r == (int) r ? String.valueOf((int) r) : String.valueOf(r); }
    private static String fmtPct(float v) { return fmt(v) + "%"; }

    // maps an inventory section id to its container
    @Nullable
    private ItemContainer getContainerBySectionId(int sectionId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        return switch (sectionId) {
            case InventoryComponent.STORAGE_SECTION_ID -> { InventoryComponent.Storage c = store.getComponent(ref, InventoryComponent.Storage.getComponentType()); yield c != null ? c.getInventory() : null; }
            case InventoryComponent.HOTBAR_SECTION_ID  -> { InventoryComponent.Hotbar  c = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());  yield c != null ? c.getInventory() : null; }
            case InventoryComponent.ARMOR_SECTION_ID   -> { InventoryComponent.Armor   c = store.getComponent(ref, InventoryComponent.Armor.getComponentType());   yield c != null ? c.getInventory() : null; }
            case InventoryComponent.UTILITY_SECTION_ID -> { InventoryComponent.Utility c = store.getComponent(ref, InventoryComponent.Utility.getComponentType()); yield c != null ? c.getInventory() : null; }
            default -> null;
        };
    }

    // compile a class to handle the event data payloads
    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec.<PageData>builder(PageData.class, PageData::new)
                // default properties
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("SlotIndex", Codec.INTEGER), (d, v) -> d.slotIndex = v, d -> d.slotIndex).add()

                // dragComplete keys
                .append(new KeyedCodec<>("SourceSlotId", Codec.INTEGER), (d, v) -> d.sourceSlotId = v, d -> d.sourceSlotId).add()
                .append(new KeyedCodec<>("SourceInventorySectionId", Codec.INTEGER), (d, v) -> d.sourceSectionId = v, d -> d.sourceSectionId).add()

                // clickedWhileDragging keys
                .append(new KeyedCodec<>("DragSourceSlotId", Codec.INTEGER), (d, v) -> d.dragSourceSlotId = v, d -> d.dragSourceSlotId).add()
                .append(new KeyedCodec<>("DragSourceInventorySectionId", Codec.INTEGER), (d, v) -> d.dragSourceSectionId = v, d -> d.dragSourceSectionId).add()

                // build the codec
                .build();

        // default properties
        public String action;
        public int slotIndex = -1;

        // dragComplete properties
        public int sourceSlotId = -1;
        public int sourceSectionId = 0;

        // clickedWhileDragging properties
        public int dragSourceSlotId = -1;
        public int dragSourceSectionId = 0;
    }
}