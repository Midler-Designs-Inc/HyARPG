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
import com.hypixel.hytale.server.core.entity.entities.player.windows.ItemContainerWindow;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.utils.StatTypeInfo;
import com.example.hyarpg.utils.affixes.Affix;
import com.example.hyarpg.utils.affixes.AffixPool;
import com.example.hyarpg.utils.affixes.StatType;
import com.example.hyarpg.utils.items.ItemFactory;
import org.bson.BsonDocument;
import org.bson.BsonValue;

// Java Imports
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CustomPage_ForgeSalvagePage extends InteractiveCustomUIPage<CustomPage_ForgeSalvagePage.PageData> implements ItemContainerWindow {

    // category string that identifies mod gear items
    private static final String GEAR_CATEGORY = "Items.HyARPG.Gear";

    // known shard item ids — used to determine shard for non-common rarity items
    private static final java.util.Set<String> SHARD_IDS = java.util.Set.of(
        "Uncommon_Shards", "Rare_Shards", "Epic_Shards", "Legendary_Shards"
    );

    // number of components to return on salvage — 1 now, designed to scale with crafting level later
    private static final int SALVAGE_COMPONENT_COUNT = 1;
    private static final int SALVAGE_MATERIAL_COUNT = 1;
    private static final float SALVAGE_MATERIAL_YIELD = 0.0f;

    // containers backing the input and output slots
    private final SimpleItemContainer inputContainer;
    private final SimpleItemContainer outputContainer;
    private final CombinedItemContainer combinedItemContainer;

    // currently placed input slot entry — null means nothing placed
    private String inputSlotId = null;
    private Item inputItem = null;

    // currently selected inventory slot — null means nothing selected
    private String selectedSlotId = null;
    private Item selectedItem = null;

    public CustomPage_ForgeSalvagePage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);

        // 1 input slot for the gear item to salvage
        this.inputContainer = new SimpleItemContainer((short) 1);

        // 4 output slots (3 components + 1 shard), read-only
        this.outputContainer = new SimpleItemContainer((short) 4);
        this.outputContainer.setGlobalFilter(FilterType.DENY_ALL);

        // combine containers so the window system binds them to the item slots
        this.combinedItemContainer = new CombinedItemContainer(new ItemContainer[]{ this.inputContainer, this.outputContainer });
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load the UI file
        cmd.append("CustomForgeSalvagePanel.ui");

        // bind input slot click — places/clears the selected inventory item
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SalvageInputSlot", EventData.of("Action", "place"));

        // bind storage slot clicks — selects the item in that inventory slot
        for (int i = 0; i < 36; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#StorageSlot" + i, EventData.of("Action", "select:storage:" + i));
        }

        // bind hotbar slot clicks — selects the item in that hotbar slot
        for (int i = 0; i < 9; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#HotbarSlot" + i, EventData.of("Action", "select:hotbar:" + i));
        }

        // bind salvage button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SalvageButton", EventData.of("Action", "salvage"));

        // push initial inventory state
        pushInventoryState(ref, store, cmd);
    }

    @Nonnull
    @Override
    public ItemContainer getItemContainer() {
        return this.combinedItemContainer;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        // route incoming actions
        if (data.action == null) { sendUpdate((UICommandBuilder) null, false); return; }
        if (data.action.equals("place")) {
            handleInputPlace(ref, store);
        } else if (data.action.startsWith("select:")) {
            handleInventorySelect(data.action.substring("select:".length()), ref, store);
        } else if (data.action.equals("salvage")) {
            handleSalvage(ref, store);
        } else {
            sendUpdate((UICommandBuilder) null, false);
        }
    }

    private void handleInventorySelect(@Nonnull String slotId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        String[] parts = slotId.split(":");
        boolean isHotbar = parts[0].equals("hotbar");
        int index = Integer.parseInt(parts[1]);

        UICommandBuilder cmd = new UICommandBuilder();

        // get the inventory container for the clicked slot
        ItemContainer inv = isHotbar
                ? ((InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())).getInventory()
                : ((InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType())).getInventory();
        if (inv == null) { sendUpdate((UICommandBuilder) null, false); return; }

        // bail if the slot is empty
        ItemStack stack = inv.getItemStack((short) index);
        if (stack == null || stack.isEmpty()) { sendUpdate((UICommandBuilder) null, false); return; }

        // if this item is already in the input slot clicking it removes it
        if (slotId.equals(this.inputSlotId)) {
            cmd.set("#" + (isHotbarItem(this.inputSlotId) ? "Hotbar" : "Storage") + "InUseOverlay" + getSlotNumber(this.inputSlotId) + ".Visible", false);
            cmd.setNull("#SalvageInputItem.ItemId");
            this.inputSlotId = null;
            this.inputItem = null;
            clearOutputSlots(cmd);
            cmd.set("#SalvageButton.Disabled", true);
            cmd.set("#SalvageItemStats.Visible", false);
            sendUpdate(cmd, false);
            return;
        }

        // bail if item is not salvageable mod gear
        if (!isSalvageableItem(stack.getItem())) { sendUpdate((UICommandBuilder) null, false); return; }

        // deselect previously selected slot if any
        if (this.selectedSlotId != null) {
            cmd.set("#" + (isHotbarItem(this.selectedSlotId) ? "Hotbar" : "Storage") + "SelectedOverlay" + getSlotNumber(this.selectedSlotId) + ".Visible", false);
        }

        // if clicking the already selected slot deselect it
        if (slotId.equals(this.selectedSlotId)) {
            this.selectedSlotId = null;
            this.selectedItem = null;
            sendUpdate(cmd, false);
            return;
        }

        // select the new slot
        this.selectedSlotId = slotId;
        this.selectedItem = stack.getItem();
        cmd.set("#" + (isHotbarItem(slotId) ? "Hotbar" : "Storage") + "SelectedOverlay" + getSlotNumber(slotId) + ".Visible", true);

        sendUpdate(cmd, false);
    }

    private void handleInputPlace(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();

        // if input slot already has an item, clear it
        if (this.inputSlotId != null) {
            cmd.set("#" + (isHotbarItem(this.inputSlotId) ? "Hotbar" : "Storage") + "InUseOverlay" + getSlotNumber(this.inputSlotId) + ".Visible", false);
            cmd.setNull("#SalvageInputItem.ItemId");
            this.inputSlotId = null;
            this.inputItem = null;
            clearOutputSlots(cmd);
            cmd.set("#SalvageButton.Disabled", true);
            cmd.set("#SalvageItemStats.Visible", false);
        }

        // if nothing is selected there is nothing to place
        if (this.selectedItem == null) {
            sendUpdate(cmd, false);
            return;
        }

        // place the selected item into the input slot
        this.inputSlotId = this.selectedSlotId;
        this.inputItem = this.selectedItem;

        // update the input slot icon
        cmd.set("#SalvageInputItem.ItemId", this.inputItem.getId());

        // enable the in use overlay on the inventory slot
        cmd.set("#" + (isHotbarItem(this.inputSlotId) ? "Hotbar" : "Storage") + "InUseOverlay" + getSlotNumber(this.inputSlotId) + ".Visible", true);

        // clear the selection overlay and data
        clearSelectedSlot(cmd);

        // populate output slots and info panel from the placed item's metadata
        ItemStack stack = getStackFromSlotId(this.inputSlotId, ref, store);
        if (stack != null) setOutputSlotState(cmd, stack);

        sendUpdate(cmd, false);
    }

    private void handleSalvage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();

        // validate the input item is still in the player's inventory
        if (this.inputSlotId == null || this.inputItem == null) { sendUpdate((UICommandBuilder) null, false); return; }

        ItemContainer inv = isHotbarItem(this.inputSlotId)
                ? ((InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())).getInventory()
                : ((InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType())).getInventory();
        if (inv == null) { sendUpdate((UICommandBuilder) null, false); return; }

        short slot = Short.parseShort(getSlotNumber(this.inputSlotId));
        ItemStack stack = inv.getItemStack(slot);
        if (stack == null || stack.isEmpty() || !stack.getItem().getId().equals(this.inputItem.getId())) {
            sendUpdate((UICommandBuilder) null, false);
            return;
        }

        // branch salvage logic based on item type
        if (isComponent(this.inputItem.getId())) {
            handleSalvageComponent(cmd, inv, slot, stack, ref, store);
        } else {
            handleSalvageGear(cmd, inv, slot, stack, ref, store);
        }
    }

    private void handleSalvageGear(@Nonnull UICommandBuilder cmd, @Nonnull ItemContainer inv, short slot, @Nonnull ItemStack stack, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // read the components from item metadata
        String[] components = stack.getFromMetadataOrNull("components", Codec.STRING_ARRAY);
        if (components == null || components.length == 0) { sendUpdate((UICommandBuilder) null, false); return; }

        // build pool of valid components
        List<String> pool = new ArrayList<>();
        for (String comp : components) {
            if (comp != null && !comp.isEmpty()) pool.add(comp);
        }
        if (pool.isEmpty()) { sendUpdate((UICommandBuilder) null, false); return; }

        // randomly pick SALVAGE_COMPONENT_COUNT components to return
        List<String> toReturn = new ArrayList<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < Math.min(SALVAGE_COMPONENT_COUNT, pool.size()); i++) {
            toReturn.add(pool.remove(r.nextInt(pool.size())));
        }

        // remove input item and give back selected components
        inv.replaceItemStackInSlot(slot, stack, stack.withQuantity(stack.getQuantity() - 1));
        InventoryComponent.Storage storage = (InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storage != null) {
            for (String compId : toReturn) storage.getInventory().addItemStack(new ItemStack(compId));
        }

        finishSalvage(cmd, ref, store);
    }

    private void handleSalvageComponent(@Nonnull UICommandBuilder cmd, @Nonnull ItemContainer inv, short slot, @Nonnull ItemStack stack, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        String itemId = stack.getItem().getId();

        // read recipe inputs from the full asset json
        List<RecipeInput> recipeInputs = readRecipeInputs(itemId);
        if (recipeInputs.isEmpty()) { sendUpdate((UICommandBuilder) null, false); return; }

        // randomly pick SALVAGE_COMPONENT_COUNT ingredients to return
        List<RecipeInput> pool = new ArrayList<>(recipeInputs);
        List<RecipeInput> toReturn = new ArrayList<>();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < Math.min(SALVAGE_MATERIAL_COUNT, pool.size()); i++) {
            toReturn.add(pool.remove(r.nextInt(pool.size())));
        }

        // remove input component from inventory
        inv.replaceItemStackInSlot(slot, stack, stack.withQuantity(stack.getQuantity() - 1));

        // give back a random amount of each selected ingredient scaled by yield stat
        InventoryComponent.Storage storage = (InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storage != null) {
            for (RecipeInput input : toReturn) {
                // yield stat biases the minimum return amount upward — at 1.0 always returns full amount
                int minQty = Math.max(1, (int) Math.floor(SALVAGE_MATERIAL_YIELD * input.quantity));
                int maxQty = input.quantity;
                int returnQty = minQty >= maxQty ? maxQty : r.nextInt(minQty, maxQty + 1);
                storage.getInventory().addItemStack(new ItemStack(input.itemId, returnQty));
            }
        }

        finishSalvage(cmd, ref, store);
    }

    // shared cleanup after any salvage
    private void finishSalvage(@Nonnull UICommandBuilder cmd, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // clear the in-use overlay on the input slot before resetting state
        if (this.inputSlotId != null) {
            cmd.set("#" + (isHotbarItem(this.inputSlotId) ? "Hotbar" : "Storage") + "InUseOverlay" + getSlotNumber(this.inputSlotId) + ".Visible", false);
        }

        this.inputSlotId = null;
        this.inputItem = null;
        clearSelectedSlot(cmd);
        clearOutputSlots(cmd);
        cmd.setNull("#SalvageInputItem.ItemId");
        cmd.set("#SalvageButton.Disabled", true);
        cmd.set("#SalvageItemStats.Visible", false);
        pushInventoryState(ref, store, cmd);
        sendUpdate(cmd, false);
    }

    // populate the output slots and info panel from the placed item's metadata
    private void setOutputSlotState(@Nonnull UICommandBuilder cmd, @Nonnull ItemStack stack) {
        String itemId = stack.getItem().getId();

        if (isComponent(itemId)) {
            setOutputSlotStateForComponent(cmd, itemId);
        } else {
            setOutputSlotStateForGear(cmd, stack, itemId);
        }
    }

    // populate output slots and right panel for a gear item
    private void setOutputSlotStateForGear(@Nonnull UICommandBuilder cmd, @Nonnull ItemStack stack, @Nonnull String itemId) {
        String[] components = stack.getFromMetadataOrNull("components", Codec.STRING_ARRAY);
        String[] implicits  = stack.getFromMetadataOrNull("implicits",  Codec.STRING_ARRAY);
        String[] affixes    = stack.getFromMetadataOrNull("affixes",    Codec.STRING_ARRAY);
        String[] statLabels = { "a", "b", "c" };

        // populate output slots 1-3 with component icons, names and implicit stats
        if (components != null) {
            for (int i = 0; i < 3; i++) {
                int slot = i + 1;
                if (i < components.length && components[i] != null && !components[i].isEmpty()) {
                    String compId = components[i];
                    cmd.set("#SalvageOutputItem" + slot + ".ItemId", compId);

                    // derive display name from component id
                    String compName = compId.replace("Weapon_Component_", "").replace("Armor_Component_", "").replace("_", " ");
                    cmd.set("#SalvageOutputName" + slot + ".Text", compName);

                    // read implicits from component json and build stat lines
                    BsonDocument compDoc = ItemFactory.readCraftingComponent(compId);
                    List<String> statLines = buildComponentStatLines(compDoc);
                    for (int s = 0; s < 3; s++) {
                        cmd.set("#SalvageOutputStat" + slot + statLabels[s] + ".Text", s < statLines.size() ? statLines.get(s) : "");
                    }
                } else {
                    cmd.setNull("#SalvageOutputItem" + slot + ".ItemId");
                    cmd.set("#SalvageOutputName" + slot + ".Text", "");
                    for (String s : statLabels) cmd.set("#SalvageOutputStat" + slot + s + ".Text", "");
                }
            }
        }

        // slot 4 — shard based on rarity, Common gets nothing
        String rarity = deriveRarity(itemId);
        if (!rarity.equals("Common")) {
            cmd.set("#SalvageOutputItem4.ItemId", rarity + "_Shards");
            cmd.set("#SalvageOutputName4.Text", rarity + " Shard");
            cmd.set("#SalvageOutputStat4a.Text", "Used to craft " + rarity + " quality items");
        } else {
            cmd.setNull("#SalvageOutputItem4.ItemId");
            cmd.set("#SalvageOutputName4.Text", "");
            cmd.set("#SalvageOutputStat4a.Text", "");
        }

        // right panel
        String weaponType = ItemFactory.deriveWeaponType(itemId);
        cmd.set("#SalvageItemType.Text", (weaponType != null ? weaponType : itemId) + " (" + rarity + ")");
        populateRightPanel(cmd, implicits, affixes);

        cmd.set("#SalvageItemStats.Visible", true);
        cmd.set("#SalvageButton.Disabled", false);
    }

    // populate output slots and right panel for a crafting component
    private void setOutputSlotStateForComponent(@Nonnull UICommandBuilder cmd, @Nonnull String itemId) {
        List<RecipeInput> recipeInputs = readRecipeInputs(itemId);
        String[] statLabels = { "a", "b", "c" };

        // populate output slots with recipe ingredients (up to 4)
        for (int i = 0; i < 4; i++) {
            int slot = i + 1;
            if (i < recipeInputs.size()) {
                RecipeInput input = recipeInputs.get(i);
                int minQty = Math.max(1, (int) Math.floor(SALVAGE_MATERIAL_YIELD * input.quantity));
                int maxQty = input.quantity;

                // set ingredient icon if valid item asset exists
                Item ingredientItem = Item.getAssetMap().getAsset(input.itemId);
                if (ingredientItem != null) cmd.set("#SalvageOutputItem" + slot + ".ItemId", input.itemId);
                else cmd.setNull("#SalvageOutputItem" + slot + ".ItemId");

                // set ingredient name and return quantity
                cmd.set("#SalvageOutputName" + slot + ".Text", input.displayName);
                cmd.set("#SalvageOutputStat" + slot + "a.Text", "Returns: " + minQty + "-" + maxQty + "x");

                // clear remaining stat lines — slot 4 only has one stat label
                if (slot < 4) {
                    cmd.set("#SalvageOutputStat" + slot + "b.Text", "");
                    cmd.set("#SalvageOutputStat" + slot + "c.Text", "");
                }
            } else {
                // clear unused slot
                cmd.setNull("#SalvageOutputItem" + slot + ".ItemId");
                cmd.set("#SalvageOutputName" + slot + ".Text", "");
                cmd.set("#SalvageOutputStat" + slot + "a.Text", "");

                // clear remaining stat lines — slot 4 only has one stat label
                if (slot < 4) {
                    cmd.set("#SalvageOutputStat" + slot + "b.Text", "");
                    cmd.set("#SalvageOutputStat" + slot + "c.Text", "");
                }
            }
        }

        // right panel — component type/tier label and implicits from component json
        BsonDocument compDoc = ItemFactory.readCraftingComponent(itemId);
        BsonValue typeVal = compDoc != null ? compDoc.get("type") : null;
        BsonValue tierVal = compDoc != null ? compDoc.get("tier") : null;
        String typeName = typeVal != null && typeVal.isString() ? typeVal.asString().getValue() : itemId;
        String tierStr  = tierVal != null && tierVal.isInt32()  ? "T" + tierVal.asInt32().getValue() : "";
        cmd.set("#SalvageItemType.Text", typeName + " " + tierStr);

        // build implicit display lines from component json
        List<String> implicitLines = buildComponentStatLines(compDoc);
        String weaponDamageText = "";
        List<String> remaining = new ArrayList<>();
        for (String line : implicitLines) {
            if (weaponDamageText.isEmpty() && line.contains("Base Weapon Damage")) {
                weaponDamageText = line;
            } else {
                remaining.add(line);
            }
        }

        // set weapon damage and implicit lines on right panel
        cmd.set("#SalvageWeaponDamage.Text", weaponDamageText);
        for (int i = 0; i < 5; i++) {
            cmd.set("#SalvageImplicitLine" + (i + 1) + ".Text", i < remaining.size() ? remaining.get(i) : "");
        }

        // no affixes on components
        for (int i = 1; i <= 4; i++) cmd.set("#SalvageAffixLine" + i + ".Text", "");

        cmd.set("#SalvageItemStats.Visible", true);
        cmd.set("#SalvageButton.Disabled", false);
    }

    // populates the right panel weapon damage + implicits + affixes from raw metadata strings
    private void populateRightPanel(@Nonnull UICommandBuilder cmd, @Nullable String[] implicits, @Nullable String[] affixes) {
        String weaponDamageText = "";
        List<String> implicitLines = new ArrayList<>();
        if (implicits != null) {
            for (String implicit : implicits) {
                String[] parts = implicit.split("\\|");
                if (parts.length < 3) continue;
                StatType stat;
                try { stat = StatType.valueOf(parts[0]); } catch (Exception e) { continue; }
                if (weaponDamageText.isEmpty() && StatTypeInfo.isWeaponDamageStat(stat)) {
                    weaponDamageText = parts[2];
                } else {
                    implicitLines.add(parts[2]);
                }
            }
        }
        cmd.set("#SalvageWeaponDamage.Text", weaponDamageText);
        for (int i = 0; i < 5; i++) {
            cmd.set("#SalvageImplicitLine" + (i + 1) + ".Text", i < implicitLines.size() ? implicitLines.get(i) : "");
        }

        List<String> affixLines = new ArrayList<>();
        if (affixes != null) {
            for (String affix : affixes) {
                String[] parts = affix.split("\\|");
                if (parts.length < 3) continue;
                Affix affixDef = AffixPool.getAffixByStatName(parts[0]);
                if (affixDef == null) continue;
                float value = Float.parseFloat(parts[1]);
                int tier = (int) Float.parseFloat(parts[2]);
                affixLines.add("T" + tier + " " + affixDef.display().formatted(Math.round(value * 10) / 10f));
            }
        }
        for (int i = 0; i < 4; i++) {
            cmd.set("#SalvageAffixLine" + (i + 1) + ".Text", i < affixLines.size() ? affixLines.get(i) : "");
        }
    }

    // builds stat display lines from a component's implicits block in its json
    private List<String> buildComponentStatLines(@Nullable BsonDocument compDoc) {
        List<String> statLines = new ArrayList<>();
        if (compDoc == null) return statLines;
        BsonValue implicitsVal = compDoc.get("implicits");
        if (implicitsVal == null || !implicitsVal.isArray()) return statLines;
        for (BsonValue entry : implicitsVal.asArray()) {
            if (!entry.isDocument()) continue;
            BsonDocument implicit = entry.asDocument();
            BsonValue statVal = implicit.get("stat");
            BsonValue minVal  = implicit.get("min");
            BsonValue maxVal  = implicit.get("max");
            if (statVal == null || minVal == null || maxVal == null) continue;
            float min = minVal.isDouble() ? (float) minVal.asDouble().getValue() : (float) minVal.asInt32().getValue();
            float max = maxVal.isDouble() ? (float) maxVal.asDouble().getValue() : (float) maxVal.asInt32().getValue();
            try {
                StatType stat = StatType.valueOf(statVal.asString().getValue());
                if (StatTypeInfo.isWeaponDamageStat(stat)) {
                    statLines.add(fmt(min) + "-" + fmt(max) + " " + damageTypeName(stat));
                } else {
                    statLines.add(StatTypeInfo.getDisplay(stat, min, max));
                }
            } catch (Exception ignored) {}
        }
        return statLines;
    }

    // reads recipe inputs from a component's full asset json
    private List<RecipeInput> readRecipeInputs(@Nonnull String itemId) {
        List<RecipeInput> inputs = new ArrayList<>();
        try {
            java.nio.file.Path assetPath = Item.getAssetMap().getPath(itemId);
            if (assetPath == null) return inputs;
            BsonDocument fullDoc = BsonDocument.parse(java.nio.file.Files.readString(assetPath));
            BsonValue recipeVal = fullDoc.get("Recipe");
            if (recipeVal == null || !recipeVal.isDocument()) return inputs;
            BsonValue inputVal = recipeVal.asDocument().get("Input");
            if (inputVal == null || !inputVal.isArray()) return inputs;
            for (BsonValue entry : inputVal.asArray()) {
                if (!entry.isDocument()) continue;
                BsonDocument input = entry.asDocument();
                String inputId  = input.getString("ItemId").getValue();
                int quantity    = input.getInt32("Quantity").getValue();
                String dispName = inputId.replace("Ingredient_", "").replace("_", " ");
                inputs.add(new RecipeInput(inputId, dispName, quantity));
            }
        } catch (Exception ignored) {}
        return inputs;
    }

    // small record to hold recipe input data
    private record RecipeInput(String itemId, String displayName, int quantity) {}

    // also need this helper
    private static String damageTypeName(@Nonnull StatType stat) {
        if (stat == StatType.MAIN_HAND_FIRE_DAMAGE_FLAT      || stat == StatType.OFF_HAND_FIRE_DAMAGE_FLAT)      return "Fire";
        if (stat == StatType.MAIN_HAND_LIGHTNING_DAMAGE_FLAT || stat == StatType.OFF_HAND_LIGHTNING_DAMAGE_FLAT) return "Lightning";
        if (stat == StatType.MAIN_HAND_ICE_DAMAGE_FLAT       || stat == StatType.OFF_HAND_ICE_DAMAGE_FLAT)       return "Ice";
        if (stat == StatType.MAIN_HAND_POISON_DAMAGE_FLAT    || stat == StatType.OFF_HAND_POISON_DAMAGE_FLAT)    return "Poison";
        if (stat == StatType.MAIN_HAND_MAGIC_DAMAGE_FLAT     || stat == StatType.OFF_HAND_MAGIC_DAMAGE_FLAT)     return "Magic";
        return "Physical";
    }

    private static String fmt(float value) {
        float rounded = Math.round(value * 10) / 10f;
        return rounded == (int) rounded ? String.valueOf((int) rounded) : String.valueOf(rounded);
    }

    // clear all 4 output slot icons, names and stat labels
    private void clearOutputSlots(@Nonnull UICommandBuilder cmd) {
        String[] statLabels = { "a", "b", "c" };

        // slots 1-3 have 3 stat labels each
        for (int i = 1; i <= 3; i++) {
            cmd.setNull("#SalvageOutputItem" + i + ".ItemId");
            cmd.set("#SalvageOutputName" + i + ".Text", "");
            for (String s : statLabels) cmd.set("#SalvageOutputStat" + i + s + ".Text", "");
        }

        // slot 4 only has one stat label
        cmd.setNull("#SalvageOutputItem4.ItemId");
        cmd.set("#SalvageOutputName4.Text", "");
        cmd.set("#SalvageOutputStat4a.Text", "");
    }

    // push all inventory item ids and usability overlays to the UI
    private void pushInventoryState(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        // push storage slots
        InventoryComponent.Storage storageComponent = (InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComponent != null) {
            ItemContainer storage = storageComponent.getInventory();
            for (short i = 0; i < Math.min(storage.getCapacity(), 36); i++) {
                ItemStack item = storage.getItemStack(i);
                String itemId = (item != null && !item.isEmpty()) ? item.getItem().getId() : null;
                if (itemId != null) cmd.set("#StorageItem" + i + ".ItemId", itemId);
                else cmd.setNull("#StorageItem" + i + ".ItemId");
            }
        }

        // push hotbar slots
        InventoryComponent.Hotbar hotbarComponent = (InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbarComponent != null) {
            ItemContainer hotbar = hotbarComponent.getInventory();
            for (short i = 0; i < Math.min(hotbar.getCapacity(), 9); i++) {
                ItemStack item = hotbar.getItemStack(i);
                String itemId = (item != null && !item.isEmpty()) ? item.getItem().getId() : null;
                if (itemId != null) cmd.set("#HotbarItem" + i + ".ItemId", itemId);
                else cmd.setNull("#HotbarItem" + i + ".ItemId");
            }
        }

        // apply usability overlays — only mod gear items are valid
        applyInventoryUsabilityOverlays(ref, store, cmd);
    }

    // set invalid overlays on inventory items that are not salvageable mod gear
    private void applyInventoryUsabilityOverlays(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        InventoryComponent.Storage storageComponent = (InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComponent != null) {
            ItemContainer storage = storageComponent.getInventory();
            for (short i = 0; i < Math.min(storage.getCapacity(), 36); i++) {
                ItemStack stack = storage.getItemStack(i);
                if (stack == null || stack.isEmpty()) continue;
                cmd.set("#StorageInvalidOverlay" + i + ".Visible", !isSalvageableItem(stack.getItem()));
            }
        }

        InventoryComponent.Hotbar hotbarComponent = (InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbarComponent != null) {
            ItemContainer hotbar = hotbarComponent.getInventory();
            for (short i = 0; i < Math.min(hotbar.getCapacity(), 9); i++) {
                ItemStack stack = hotbar.getItemStack(i);
                if (stack == null || stack.isEmpty()) continue;
                cmd.set("#HotbarInvalidOverlay" + i + ".Visible", !isSalvageableItem(stack.getItem()));
            }
        }
    }

    // clear the selected slot overlay and data
    private void clearSelectedSlot(@Nonnull UICommandBuilder cmd) {
        if (this.selectedSlotId == null || this.selectedItem == null) return;
        cmd.set("#" + (isHotbarItem(this.selectedSlotId) ? "Hotbar" : "Storage") + "SelectedOverlay" + getSlotNumber(this.selectedSlotId) + ".Visible", false);
        this.selectedSlotId = null;
        this.selectedItem = null;
    }

    // get the ItemStack from a slotId e.g. "storage:5"
    @Nullable
    private ItemStack getStackFromSlotId(@Nonnull String slotId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        String[] parts = slotId.split(":");
        boolean isHotbar = parts[0].equals("hotbar");
        int index = Integer.parseInt(parts[1]);
        ItemContainer inv = isHotbar
                ? ((InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())).getInventory()
                : ((InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType())).getInventory();
        if (inv == null) return null;
        return inv.getItemStack((short) index);
    }

    // checks whether an item is salvageable — either mod gear or a crafting component
    private static boolean isSalvageableItem(@Nullable Item item) {
        if (item == null) return false;

        // check for mod gear category
        String[] categories = item.getCategories();
        if (categories != null) {
            for (String cat : categories) {
                if (GEAR_CATEGORY.equals(cat)) return true;
            }
        }

        // check for crafting component
        return ItemFactory.readCraftingComponent(item.getId()) != null;
    }

    // checks whether an item id is a crafting component
    private static boolean isComponent(@Nonnull String itemId) {
        return ItemFactory.readCraftingComponent(itemId) != null;
    }

    // derives rarity from item id suffix e.g. "Weapon_Axe_Copper_Rare" -> "Rare"
    private static String deriveRarity(@Nonnull String itemId) {
        for (String rarity : new String[]{"Legendary", "Epic", "Rare", "Uncommon", "Common"}) {
            if (itemId.endsWith("_" + rarity)) return rarity;
        }
        return "Common";
    }

    // determines if a slotId refers to the hotbar
    private boolean isHotbarItem(@Nonnull String slotId) {
        return slotId.split(":")[0].equals("hotbar");
    }

    // extracts the slot number from a slotId e.g. "storage:5" -> "5"
    private String getSlotNumber(@Nonnull String slotId) {
        return slotId.split(":")[1];
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
                .<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("ItemStackId", Codec.STRING), (d, v) -> d.itemStackId = v, d -> d.itemStackId).add()
                .append(new KeyedCodec<>("SourceSlotId", Codec.INTEGER), (d, v) -> d.sourceSlotId = v, d -> d.sourceSlotId).add()
                .append(new KeyedCodec<>("SourceInventorySectionId", Codec.INTEGER), (d, v) -> d.sourceInventorySectionId = v, d -> d.sourceInventorySectionId).add()
                .append(new KeyedCodec<>("SlotIndex", Codec.INTEGER), (d, v) -> d.slotIndex = v, d -> d.slotIndex).add()
                .build();

        public String action;
        public String itemStackId;
        public Integer sourceSlotId;
        public Integer sourceInventorySectionId;
        public Integer slotIndex;
    }
}
