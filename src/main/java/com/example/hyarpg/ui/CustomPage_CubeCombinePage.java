package com.example.hyarpg.ui;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
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
import com.example.hyarpg.utils.CubeCombineRecipeList;

// Java Imports
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class CustomPage_CubeCombinePage extends InteractiveCustomUIPage<CustomPage_CubeCombinePage.PageData> implements ItemContainerWindow {

    private static final int CUBE_SLOT_COUNT = 9;

    // tracks which inventory slotId, item, and quantity occupy each cube slot — null/0 means empty
    private final String[] cubeSlotSources   = new String[CUBE_SLOT_COUNT];
    private final Item[]   cubeSlotItems     = new Item[CUBE_SLOT_COUNT];
    private final int[]    cubeSlotQuantities = new int[CUBE_SLOT_COUNT];

    // currently selected inventory slot — null means nothing selected
    private String selectedSlotId = null;
    private Item   selectedItem   = null;

    // containers backing the 9 cube input slots and 1 read-only output preview slot
    private final SimpleItemContainer   cubeContainer;
    private final SimpleItemContainer   outputContainer;
    private final CombinedItemContainer combinedItemContainer;

    public CustomPage_CubeCombinePage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);

        // 9 input slots accept any item
        this.cubeContainer = new SimpleItemContainer((short) CUBE_SLOT_COUNT);

        // output slot is preview-only — deny all player interaction
        this.outputContainer = new SimpleItemContainer((short) 1);
        this.outputContainer.setGlobalFilter(FilterType.DENY_ALL);

        this.combinedItemContainer = new CombinedItemContainer(new ItemContainer[]{ this.cubeContainer, this.outputContainer });
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load the UI file
        cmd.append("CustomPage_CubePage.ui");

        // bind the 9 cube input slot clicks
        for (int i = 0; i < CUBE_SLOT_COUNT; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CubeSlot" + i, EventData.of("Action", "cube:" + i));
        }

        // bind the 36 storage slot clicks
        for (int i = 0; i < 36; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#StorageSlot" + i, EventData.of("Action", "select:storage:" + i));
        }

        // bind the 9 hotbar slot clicks
        for (int i = 0; i < 9; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#HotbarSlot" + i, EventData.of("Action", "select:hotbar:" + i));
        }

        // bind combine button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CombineButton", EventData.of("Action", "combine"));

        // push initial inventory state and output preview
        pushInventoryState(ref, store, cmd);
        refreshOutputPreview(cmd);
    }

    @Nonnull
    @Override
    public ItemContainer getItemContainer() {
        return this.combinedItemContainer;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        // route incoming actions to their handlers
        if (data.action == null)                        { sendUpdate((UICommandBuilder) null, false); return; }
        if (data.action.startsWith("cube:"))            { handleCubeSlotClick(Integer.parseInt(data.action.substring("cube:".length())), ref, store); }
        else if (data.action.startsWith("select:"))     { handleInventorySelect(data.action.substring("select:".length()), ref, store); }
        else if (data.action.equals("combine"))         { handleCombine(ref, store); }
        else                                            { sendUpdate((UICommandBuilder) null, false); }
    }

    private void handleInventorySelect(@Nonnull String slotId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        String[] parts = slotId.split(":");
        boolean isHotbar = parts[0].equals("hotbar");
        int index = Integer.parseInt(parts[1]);

        UICommandBuilder cmd = new UICommandBuilder();

        // get the right inventory container for this slot
        ItemContainer inv = isHotbar
                ? ((InventoryComponent.Hotbar)  store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())).getInventory()
                : ((InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType())).getInventory();
        if (inv == null) { sendUpdate((UICommandBuilder) null, false); return; }

        // bail if the slot is empty
        ItemStack stack = inv.getItemStack((short) index);
        if (stack == null || stack.isEmpty()) { sendUpdate((UICommandBuilder) null, false); return; }

        // deselect previously selected slot if any
        if (this.selectedSlotId != null) {
            cmd.set("#" + (isHotbarItem(this.selectedSlotId) ? "Hotbar" : "Storage") + "SelectedOverlay" + getSlotNumber(this.selectedSlotId) + ".Visible", false);
        }

        // clicking the already-selected slot deselects it
        if (slotId.equals(this.selectedSlotId)) {
            this.selectedSlotId = null;
            this.selectedItem   = null;
            sendUpdate(cmd, false);
            return;
        }

        // select the new slot
        this.selectedSlotId = slotId;
        this.selectedItem   = stack.getItem();
        cmd.set("#" + (isHotbar ? "Hotbar" : "Storage") + "SelectedOverlay" + index + ".Visible", true);

        sendUpdate(cmd, false);
    }

    private void handleCubeSlotClick(int cubeIndex, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();

        // clicking an occupied cube slot clears it
        if (this.cubeSlotSources[cubeIndex] != null) {
            clearCubeSlot(cmd, cubeIndex);
            refreshOutputPreview(cmd);
            sendUpdate(cmd, false);
            return;
        }

        // nothing to place if no inventory slot is selected
        if (this.selectedItem == null) { sendUpdate((UICommandBuilder) null, false); return; }

        // block the same inventory slot from occupying two cube slots
        for (int i = 0; i < CUBE_SLOT_COUNT; i++) {
            if (this.selectedSlotId.equals(this.cubeSlotSources[i])) { sendUpdate((UICommandBuilder) null, false); return; }
        }

        // read the live stack quantity from inventory at time of placement
        ItemContainer inv = isHotbarItem(this.selectedSlotId)
                ? ((InventoryComponent.Hotbar)  store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())).getInventory()
                : ((InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType())).getInventory();
        if (inv == null) { sendUpdate((UICommandBuilder) null, false); return; }

        ItemStack stack = inv.getItemStack(Short.parseShort(getSlotNumber(this.selectedSlotId)));
        if (stack == null || stack.isEmpty()) { sendUpdate((UICommandBuilder) null, false); return; }

        // place the item into the cube slot and record its quantity
        this.cubeSlotSources[cubeIndex]    = this.selectedSlotId;
        this.cubeSlotItems[cubeIndex]      = this.selectedItem;
        this.cubeSlotQuantities[cubeIndex] = stack.getQuantity();
        cmd.set("#CubeSlotItem" + cubeIndex + ".ItemId", this.selectedItem.getId());

        // show qty label only for stacks larger than 1
        if (stack.getQuantity() > 1) {
            cmd.set("#CubeSlotQty" + cubeIndex + ".Text", String.valueOf(stack.getQuantity()));
            cmd.set("#CubeSlotQty" + cubeIndex + ".Visible", true);
        }

        // mark the source inventory slot as in-use and clear its selection overlay
        cmd.set("#" + (isHotbarItem(this.selectedSlotId) ? "Hotbar" : "Storage") + "InUseOverlay"    + getSlotNumber(this.selectedSlotId) + ".Visible", true);
        cmd.set("#" + (isHotbarItem(this.selectedSlotId) ? "Hotbar" : "Storage") + "SelectedOverlay" + getSlotNumber(this.selectedSlotId) + ".Visible", false);
        this.selectedSlotId = null;
        this.selectedItem   = null;

        refreshOutputPreview(cmd);
        sendUpdate(cmd, false);
    }

    private void handleCombine(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();

        // find a satisfiable recipe and compute how many times it can run
        Map<String, Integer> provided = buildProvidedMap();
        if (provided.isEmpty()) { sendUpdate((UICommandBuilder) null, false); return; }

        CubeCombineRecipeList.Recipe recipe = CubeCombineRecipeList.findCraftable(provided);
        if (recipe == null) { sendUpdate((UICommandBuilder) null, false); return; }

        int multiplier = computeMultiplier(recipe, provided);
        if (multiplier <= 0) { sendUpdate((UICommandBuilder) null, false); return; }

        // get inventory references
        InventoryComponent.Storage storage = (InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        InventoryComponent.Hotbar  hotbar  = (InventoryComponent.Hotbar)  store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());

        // validate all source slots are still present and unchanged before touching anything
        for (int i = 0; i < CUBE_SLOT_COUNT; i++) {
            if (this.cubeSlotSources[i] == null) continue;
            ItemContainer inv = isHotbarItem(this.cubeSlotSources[i]) ? hotbar.getInventory() : storage.getInventory();
            if (inv == null) { sendUpdate((UICommandBuilder) null, false); return; }
            ItemStack stack = inv.getItemStack(Short.parseShort(getSlotNumber(this.cubeSlotSources[i])));
            if (stack == null || stack.isEmpty() || !stack.getItem().getId().equals(this.cubeSlotItems[i].getId())) { sendUpdate((UICommandBuilder) null, false); return; }
        }

        // consume exactly (required * multiplier) of each ingredient, draining slots in order
        for (Map.Entry<String, Integer> req : recipe.inputs().entrySet()) {
            int toConsume = req.getValue() * multiplier;
            for (int i = 0; i < CUBE_SLOT_COUNT && toConsume > 0; i++) {
                if (this.cubeSlotItems[i] == null || !this.cubeSlotItems[i].getId().equals(req.getKey())) continue;
                ItemContainer inv = isHotbarItem(this.cubeSlotSources[i]) ? hotbar.getInventory() : storage.getInventory();
                short slot = Short.parseShort(getSlotNumber(this.cubeSlotSources[i]));
                ItemStack stack = inv.getItemStack(slot);
                int consume = Math.min(toConsume, stack.getQuantity());
                inv.replaceItemStackInSlot(slot, stack, stack.withQuantity(stack.getQuantity() - consume));
                toConsume -= consume;
            }
        }

        // give the output scaled by the multiplier
        if (storage != null) {
            storage.getInventory().addItemStack(new ItemStack(recipe.outputItemId(), recipe.outputQuantity() * multiplier));
        }

        finishCombine(cmd, ref, store);
    }

    // remove an item from a cube slot and restore its source inventory overlay
    private void clearCubeSlot(@Nonnull UICommandBuilder cmd, int cubeIndex) {
        if (this.cubeSlotSources[cubeIndex] != null) {
            cmd.set("#" + (isHotbarItem(this.cubeSlotSources[cubeIndex]) ? "Hotbar" : "Storage") + "InUseOverlay" + getSlotNumber(this.cubeSlotSources[cubeIndex]) + ".Visible", false);
        }
        cmd.setNull("#CubeSlotItem" + cubeIndex + ".ItemId");
        cmd.set("#CubeSlotQty" + cubeIndex + ".Visible", false);
        this.cubeSlotSources[cubeIndex]    = null;
        this.cubeSlotItems[cubeIndex]      = null;
        this.cubeSlotQuantities[cubeIndex] = 0;
    }

    // sum total quantity per item ID across all occupied cube slots
    private Map<String, Integer> buildProvidedMap() {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < CUBE_SLOT_COUNT; i++) {
            if (this.cubeSlotItems[i] == null) continue;
            map.merge(this.cubeSlotItems[i].getId(), this.cubeSlotQuantities[i], Integer::sum);
        }
        return map;
    }

    // how many full recipe runs the provided quantities support — bottlenecked by the scarcest ingredient
    private int computeMultiplier(@Nonnull CubeCombineRecipeList.Recipe recipe, @Nonnull Map<String, Integer> provided) {
        int multiplier = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> req : recipe.inputs().entrySet()) {
            multiplier = Math.min(multiplier, provided.getOrDefault(req.getKey(), 0) / req.getValue());
        }
        return multiplier == Integer.MAX_VALUE ? 0 : multiplier;
    }

    // update the output slot icon, qty label, name, description, and combine button based on current grid contents
    private void refreshOutputPreview(@Nonnull UICommandBuilder cmd) {
        Map<String, Integer> provided = buildProvidedMap();
        CubeCombineRecipeList.Recipe best = provided.isEmpty() ? null : CubeCombineRecipeList.findBestMatch(provided);
        boolean satisfied  = best != null && best.isSatisfied(provided);
        int     multiplier = satisfied ? computeMultiplier(best, provided) : 0;

        // no match or requirements not fully met — clear the output panel
        if (best == null || !satisfied) {
            cmd.setNull("#CubeOutputItem.ItemId");
            cmd.set("#CubeOutputQty.Visible", false);
            cmd.set("#CubeOutputItemStats.Visible", false);
            cmd.set("#CombineButton.Disabled", true);
            return;
        }

        // show the output item icon — recipe is fully satisfied
        cmd.set("#CubeOutputItem.ItemId", best.outputItemId());

        // look up display name and description from item translation properties
        Item   outputItem  = Item.getAssetMap().getAsset(best.outputItemId());
        String nameKey     = outputItem != null ? outputItem.getTranslationProperties().getName() : null;
        String descKey     = outputItem != null ? outputItem.getTranslationProperties().getDescription() : null;
        String displayName = nameKey != null ? Message.translation(nameKey).getAnsiMessage() : best.outputItemId();
        String description = descKey != null ? Message.translation(descKey).getAnsiMessage() : "";
        cmd.set("#CubeOutputItemName.Text", displayName);
        cmd.set("#CubeOutputItemDescription.Text", description);
        cmd.set("#CubeOutputItemStats.Visible", true);

        // show scaled qty label only when output is more than 1
        int outputQty = best.outputQuantity() * multiplier;
        if (outputQty > 1) {
            cmd.set("#CubeOutputQty.Text", String.valueOf(outputQty));
            cmd.set("#CubeOutputQty.Visible", true);
        } else {
            cmd.set("#CubeOutputQty.Visible", false);
        }

        cmd.set("#CombineButton.Disabled", false);
    }

    // clear all cube slots, reset output panel, and refresh inventory after a successful combine
    private void finishCombine(@Nonnull UICommandBuilder cmd, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // clear every cube slot and its overlays
        for (int i = 0; i < CUBE_SLOT_COUNT; i++) {
            clearCubeSlot(cmd, i);
        }

        // clear selected slot overlay if any was left
        clearSelectedSlot(cmd);

        // reset the output panel
        cmd.setNull("#CubeOutputItem.ItemId");
        cmd.set("#CubeOutputQty.Visible", false);
        cmd.set("#CubeOutputItemStats.Visible", false);
        cmd.set("#CombineButton.Disabled", true);

        // re-push inventory so qty labels reflect consumed items
        pushInventoryState(ref, store, cmd);
        sendUpdate(cmd, false);
    }

    // push all inventory item ids and qty labels to the UI
    private void pushInventoryState(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        // push storage slots
        InventoryComponent.Storage storageComponent = (InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComponent != null) {
            ItemContainer storage = storageComponent.getInventory();
            for (short i = 0; i < Math.min(storage.getCapacity(), 36); i++) {
                ItemStack item = storage.getItemStack(i);
                if (item != null && !item.isEmpty()) {
                    cmd.set("#StorageItem" + i + ".ItemId", item.getItem().getId());
                    cmd.set("#StorageQty" + i + ".Text", String.valueOf(item.getQuantity()));
                    cmd.set("#StorageQty" + i + ".Visible", item.getQuantity() > 1);
                } else {
                    cmd.setNull("#StorageItem" + i + ".ItemId");
                    cmd.set("#StorageQty" + i + ".Visible", false);
                }
            }
        }

        // push hotbar slots
        InventoryComponent.Hotbar hotbarComponent = (InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbarComponent != null) {
            ItemContainer hotbar = hotbarComponent.getInventory();
            for (short i = 0; i < Math.min(hotbar.getCapacity(), 9); i++) {
                ItemStack item = hotbar.getItemStack(i);
                if (item != null && !item.isEmpty()) {
                    cmd.set("#HotbarItem" + i + ".ItemId", item.getItem().getId());
                    cmd.set("#HotbarQty" + i + ".Text", String.valueOf(item.getQuantity()));
                    cmd.set("#HotbarQty" + i + ".Visible", item.getQuantity() > 1);
                } else {
                    cmd.setNull("#HotbarItem" + i + ".ItemId");
                    cmd.set("#HotbarQty" + i + ".Visible", false);
                }
            }
        }
    }

    // clear the selected slot overlay and wipe selection state
    private void clearSelectedSlot(@Nonnull UICommandBuilder cmd) {
        if (this.selectedSlotId == null) return;
        cmd.set("#" + (isHotbarItem(this.selectedSlotId) ? "Hotbar" : "Storage") + "SelectedOverlay" + getSlotNumber(this.selectedSlotId) + ".Visible", false);
        this.selectedSlotId = null;
        this.selectedItem   = null;
    }

    private boolean isHotbarItem(@Nonnull String slotId) { return slotId.split(":")[0].equals("hotbar"); }
    private String  getSlotNumber(@Nonnull String slotId) { return slotId.split(":")[1]; }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
                .<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .build();

        public String action;
    }
}