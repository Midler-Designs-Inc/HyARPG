package com.example.hyarpg.ui;

// Hytale Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.utils.StatTypeInfo;
import com.example.hyarpg.utils.affixes.StatType;
import com.example.hyarpg.utils.items.ItemFactory;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonDocument;
import org.bson.BsonValue;

// Java Imports
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.example.hyarpg.modules.Module_RPGSystem.componentTypeRPGPlayer;

public class CustomForgeCraftingPage extends InteractiveCustomUIPage<CustomForgeCraftingPage.PageData> implements ItemContainerWindow {

    // cache of item id to raw CraftingComponent bson document — populated lazily, never changes at runtime
    private static final Map<String, BsonDocument> CRAFTING_COMPONENT_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    // known shard item ids — identified by asset id rather than CraftingComponent block
    private static final java.util.Set<String> SHARD_IDS = java.util.Set.of(
        "Uncommon_Shards", "Rare_Shards", "Epic_Shards", "Legendary_Shards"
    );

    // category tab id constants
    public static final String CAT_1H = "1h";
    public static final String CAT_2H = "2h";
    public static final String CAT_RANGED = "ranged";
    public static final String CAT_MAGIC = "magic";
    public static final String CAT_ARMOR = "armor";
    public static final String CAT_LEATHER = "leather";
    public static final String CAT_CLOTH = "cloth";

    // all diagram group ids matching exactly the ui file
    private static final String[] ALL_DIAGRAMS = {
        "#DiagramAxe", "#DiagramClub", "#DiagramShield", "#DiagramSpear", "#DiagramSword",
        "#DiagramBattleaxe", "#DiagramClaws", "#DiagramDaggers", "#DiagramLongsword", "#DiagramMace", "#DiagramScythe", "#DiagramSickles",
        "#DiagramCrossbow", "#DiagramKunai", "#DiagramLongbow", "#DiagramShortbow",
        "#DiagramSpellbook", "#DiagramStaff", "#DiagramWand",
        "#DiagramArmorHelmet", "#DiagramArmorChest", "#DiagramArmorGloves", "#DiagramArmorPants",
        "#DiagramLeatherHelmet", "#DiagramLeatherChest", "#DiagramLeatherGloves", "#DiagramLeatherPants",
        "#DiagramClothHelmet", "#DiagramClothChest", "#DiagramClothGloves", "#DiagramClothPants"
    };

    // all icon row group ids
    private static final String[] ALL_ICON_ROWS = {
        "#ItemCategories1H", "#ItemCategories2H", "#ItemCategoriesRanged", "#ItemCategoriesMagic",
        "#ItemCategoriesArmor", "#ItemCategoriesLeather", "#ItemCategoriesCloth"
    };

    // tab overlays paired with categories — must match ui file exactly
    private static final String[] ALL_TAB_OVERLAYS = {
        "#Tab1HOverlay", "#Tab2HOverlay", "#TabRangedOverlay", "#TabMagicOverlay",
        "#TabMetalArmorOverlay", "#TabLeatherArmorOverlay", "#TabClothArmorOverlay"
    };
    private static final String[] ALL_TAB_CATEGORIES = {
        CAT_1H, CAT_2H, CAT_RANGED, CAT_MAGIC, CAT_ARMOR, CAT_LEATHER, CAT_CLOTH
    };

    // icon overlays paired with item names — overlay visible means INACTIVE, hidden means ACTIVE
    private static final String[] ALL_ICON_OVERLAYS = {
        "#IconAxeOverlay", "#IconClubOverlay", "#IconShieldOverlay", "#IconSpearOverlay", "#IconSwordOverlay",
        "#IconBattleaxeOverlay", "#IconClawsOverlay", "#IconDaggersOverlay", "#IconLongswordOverlay", "#IconMaceOverlay", "#IconScytheOverlay", "#IconSicklesOverlay",
        "#IconCrossbowOverlay", "#IconKunaiOverlay", "#IconLongbowOverlay", "#IconShortbowOverlay",
        "#IconSpellbookOverlay", "#IconStaffOverlay", "#IconWandOverlay",
        "#IconArmorHelmetOverlay", "#IconArmorChestOverlay", "#IconArmorGlovesOverlay", "#IconArmorPantsOverlay",
        "#IconLeatherHelmetOverlay", "#IconLeatherChestOverlay", "#IconLeatherGlovesOverlay", "#IconLeatherPantsOverlay",
        "#IconClothHelmetOverlay", "#IconClothChestOverlay", "#IconClothGlovesOverlay", "#IconClothPantsOverlay"
    };
    private static final String[] ALL_ICON_ITEMS = {
        "Axe", "Club", "Shield", "Spear", "Sword",
        "Battleaxe", "Claws", "Daggers", "Longsword", "Mace", "Scythe", "Sickles",
        "Crossbow", "Kunai", "Longbow", "Shortbow",
        "Spellbook", "Staff", "Wand",
        "Helmet", "Chest", "Gloves", "Pants",
        "Helmet", "Chest", "Gloves", "Pants",
        "Helmet", "Chest", "Gloves", "Pants"
    };

    // containers backing the input and output item slots
    private final SimpleItemContainer inputContainer;
    private final SimpleItemContainer outputContainer;
    private final CombinedItemContainer combinedItemContainer;

    // currently selected category and item within it
    private String activeCategory = CAT_1H;
    private String activeItem = "Axe";

    // currently hovered diagram slot (-1 means none)
    private int hoveredSlot = -1;

    // tracks item ids placed in each input slot (index 0-3 maps to slots 1-4)
    public record SlotEntry (String slotN, Item item) {};
    private final SlotEntry[] inputSlotItems = new SlotEntry[4];

    // currently selected inventory slot — null means nothing selected, format: "storage:N" or "hotbar:N"
    private String selectedSlotId = null;
    private Item selectedItem = null;

    // maps each item sub-category name to the set of component types valid in slots 1-4
    private static final Map<String, List<String>> ALLOWED_COMPONENTS = Map.ofEntries(
        // 1H weapons
        Map.entry("Axe",       List.of("Axe Head", "Shaft", "Handle", "Shard")),
        Map.entry("Club",      List.of("Club Head", "Shaft", "Handle", "Shard")),
        Map.entry("Shield",    List.of("Shield Frame", "Shield Body", "Shield Core", "Shard")),
        Map.entry("Spear",     List.of("Spear Head", "Shaft", "Handle", "Shard")),
        Map.entry("Sword",     List.of("Blade", "Hilt", "Handle", "Shard")),

        // 2H weapons
        Map.entry("Battleaxe", List.of("Battleaxe Head", "Shaft", "Handle", "Shard")),
        Map.entry("Claws",     List.of("Claw Blades", "Hilt", "Handle", "Shard")),
        Map.entry("Daggers",   List.of("Short Blade", "Hilt", "Handle", "Shard")),
        Map.entry("Longsword", List.of("Long Blade", "Hilt", "Handle", "Shard")),
        Map.entry("Mace",      List.of("Mace Head", "Shaft", "Handle", "Shard")),
        Map.entry("Scythe",    List.of("Scythe Blade", "Shaft", "Handle", "Shard")),
        Map.entry("Sickles",   List.of("Curved Blade", "Shaft", "Handle", "Shard")),

        // ranged weapons
        Map.entry("Crossbow",  List.of("Crossbow Head", "String", "Crossbow Stock", "Shard")),
        Map.entry("Kunai",     List.of("Kunai Blade", "Hilt", "Handle", "Shard")),
        Map.entry("Longbow",   List.of("Longbow Body", "String", "Handle", "Shard")),
        Map.entry("Shortbow",  List.of("Shortbow Body", "String", "Handle", "Shard")),

        // magic weapons
        Map.entry("Spellbook", List.of("Book Binding", "Book Pages", "Magic Core", "Shard")),
        Map.entry("Staff",     List.of("Staff Head", "Shaft", "Magic Core", "Shard")),
        Map.entry("Wand",      List.of("Wand Body", "Handle", "Magic Core", "Shard"))
    );

    public CustomForgeCraftingPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);

        // 4 input slots for crafting components
        this.inputContainer = new SimpleItemContainer((short) 4);

        // 1 output slot, deny player interaction so only server can write to it
        this.outputContainer = new SimpleItemContainer((short) 1);
        this.outputContainer.setGlobalFilter(FilterType.DENY_ALL);

        // combine containers so the window system binds them to the item slots
        this.combinedItemContainer = new CombinedItemContainer(new ItemContainer[]{ this.inputContainer, this.outputContainer });
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load the UI file
        cmd.append("CustomForgeCraftingPanel.ui");

        // bind category tab clicks
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Tab1H", EventData.of("Action", "category:1h"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Tab2H", EventData.of("Action", "category:2h"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabRanged", EventData.of("Action", "category:ranged"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabMagic", EventData.of("Action", "category:magic"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabMetalArmor", EventData.of("Action", "category:armor"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabLeatherArmor", EventData.of("Action", "category:leather"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TabClothArmor", EventData.of("Action", "category:cloth"));

        // bind item icon clicks for 1H weapons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconAxe", EventData.of("Action", "item:Axe"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconClub", EventData.of("Action", "item:Club"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconShield", EventData.of("Action", "item:Shield"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconSpear", EventData.of("Action", "item:Spear"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconSword", EventData.of("Action", "item:Sword"));

        // bind item icon clicks for 2H weapons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconBattleaxe", EventData.of("Action", "item:Battleaxe"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconClaws", EventData.of("Action", "item:Claws"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconDaggers", EventData.of("Action", "item:Daggers"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconLongsword", EventData.of("Action", "item:Longsword"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconMace", EventData.of("Action", "item:Mace"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconScythe", EventData.of("Action", "item:Scythe"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconSickles", EventData.of("Action", "item:Sickles"));

        // bind item icon clicks for ranged weapons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconCrossbow", EventData.of("Action", "item:Crossbow"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconKunai", EventData.of("Action", "item:Kunai"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconLongbow", EventData.of("Action", "item:Longbow"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconShortbow", EventData.of("Action", "item:Shortbow"));

        // bind item icon clicks for magic weapons
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconSpellbook", EventData.of("Action", "item:Spellbook"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconStaff", EventData.of("Action", "item:Staff"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconWand", EventData.of("Action", "item:Wand"));

        // bind item icon clicks for metal armor
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconArmorHelmet", EventData.of("Action", "item:Helmet"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconArmorChest", EventData.of("Action", "item:Chest"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconArmorGloves", EventData.of("Action", "item:Gloves"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconArmorPants", EventData.of("Action", "item:Pants"));

        // bind item icon clicks for leather armor
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconLeatherHelmet", EventData.of("Action", "item:Helmet"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconLeatherChest", EventData.of("Action", "item:Chest"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconLeatherGloves", EventData.of("Action", "item:Gloves"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconLeatherPants", EventData.of("Action", "item:Pants"));

        // bind item icon clicks for cloth armor
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconClothHelmet", EventData.of("Action", "item:Helmet"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconClothChest", EventData.of("Action", "item:Chest"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconClothGloves", EventData.of("Action", "item:Gloves"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#IconClothPants", EventData.of("Action", "item:Pants"));

        // bind slot hover — locksInterface false so events are never blocked by acknowledgments
        events.addEventBinding(CustomUIEventBindingType.MouseEntered, "#InputSlot1", EventData.of("Action", "hover:1"), false);
        events.addEventBinding(CustomUIEventBindingType.MouseEntered, "#InputSlot2", EventData.of("Action", "hover:2"), false);
        events.addEventBinding(CustomUIEventBindingType.MouseEntered, "#InputSlot3", EventData.of("Action", "hover:3"), false);

        // bind slot unhover with slot index so we can ignore stale exits
        events.addEventBinding(CustomUIEventBindingType.MouseExited, "#InputSlot1", EventData.of("Action", "unhover:1"), false);
        events.addEventBinding(CustomUIEventBindingType.MouseExited, "#InputSlot2", EventData.of("Action", "unhover:2"), false);
        events.addEventBinding(CustomUIEventBindingType.MouseExited, "#InputSlot3", EventData.of("Action", "unhover:3"), false);

        // bind input slot clicks — places the currently selected inventory item into this slot
        events.addEventBinding(CustomUIEventBindingType.Activating, "#InputSlot1", EventData.of("Action", "place:1"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#InputSlot2", EventData.of("Action", "place:2"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#InputSlot3", EventData.of("Action", "place:3"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#InputSlot4", EventData.of("Action", "place:4"));

        // bind storage slot clicks — selects the item in that inventory slot
        for (int i = 0; i < 36; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#StorageSlot" + i, EventData.of("Action", "select:storage:" + i));
        }

        // bind hotbar slot clicks — selects the item in that hotbar slot
        for (int i = 0; i < 9; i++) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#HotbarSlot" + i, EventData.of("Action", "select:hotbar:" + i));
        }

        // bind craft button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CraftButton", EventData.of("Action", "craft"));

        // apply initial state using the framework-provided cmd
        applyState(cmd);

        // push inventory items to slots on open
        pushInventoryState(ref, store, cmd);
    }

    @Nonnull
    @Override
    public ItemContainer getItemContainer() {
        return this.combinedItemContainer;
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        // route incoming actions — action encodes type and value as "type:value"
        if (data.action == null) { sendUpdate((UICommandBuilder) null, false); return; }
        if (data.action.startsWith("category:")) {
            handleCategoryChanged(ref, store, data.action.substring("category:".length()));
        } else if (data.action.startsWith("item:")) {
            handleItemChanged(ref, store, data.action.substring("item:".length()));
        } else if (data.action.startsWith("hover:")) {
            handleSlotHover(Integer.parseInt(data.action.substring("hover:".length())), ref, store);
        } else if (data.action.startsWith("unhover:")) {
            handleSlotUnhover(Integer.parseInt(data.action.substring("unhover:".length())), ref, store);
        } else if (data.action.startsWith("select:")) {
            handleInventorySelect(data.action.substring("select:".length()), ref, store);
        } else if (data.action.startsWith("place:")) {
            handleInputPlace(Integer.parseInt(data.action.substring("place:".length())), ref, store);
        } else if (data.action.equals("craft")) {
            handleCraft(ref, store);
        } else {
            sendUpdate((UICommandBuilder) null, false);
        }
    }

    private void handleCategoryChanged(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String category) {
        // switch active category and reset to first item in that category
        this.activeCategory = category;
        this.activeItem = getDefaultItemForCategory(category);
        this.hoveredSlot = -1;
        UICommandBuilder cmd = new UICommandBuilder();
        clearSelectedSlot(cmd);
        clearInputSlots(cmd, false);
        setOutputSlotState(cmd);
        applyState(cmd);
        applyInventoryUsabilityOverlays(ref, store, cmd);
        sendUpdate(cmd, false);
    }

    private void handleItemChanged(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String item) {
        // switch active item within current category
        this.activeItem = item;
        this.hoveredSlot = -1;
        UICommandBuilder cmd = new UICommandBuilder();
        clearSelectedSlot(cmd);
        clearInputSlots(cmd, false);
        setOutputSlotState(cmd);
        applyState(cmd);
        applyInventoryUsabilityOverlays(ref, store, cmd);
        sendUpdate(cmd, false);
    }

    private void handleSlotHover(int slot, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        String base = getActiveDiagramId().substring(1);

        // restore previously hovered slot part back to default
        if (this.hoveredSlot != -1 && this.hoveredSlot != slot) {
            cmd.set("#" + base + "P" + this.hoveredSlot + "Default.Visible", true);
            cmd.set("#" + base + "P" + this.hoveredSlot + "Glow.Visible", false);
        }

        // set new hovered slot part to glow
        cmd.set("#" + base + "P" + slot + "Default.Visible", false);
        cmd.set("#" + base + "P" + slot + "Glow.Visible", true);

        this.hoveredSlot = slot;
        sendHoverUpdate(cmd);
    }

    private void handleSlotUnhover(int slot, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // if hoveredSlot has already moved to a different slot, MouseEntered already handled the reset
        if (this.hoveredSlot != slot) { sendUpdate((UICommandBuilder) null, false); return; }
        UICommandBuilder cmd = new UICommandBuilder();
        String base = getActiveDiagramId().substring(1);
        cmd.set("#" + base + "P" + slot + "Default.Visible", true);
        cmd.set("#" + base + "P" + slot + "Glow.Visible", false);
        this.hoveredSlot = -1;
        sendHoverUpdate(cmd);
    }

    private void handleInventorySelect(@Nonnull String slotId, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // parse "storage:N" or "hotbar:N" to find the item at that slot
        String[] parts = slotId.split(":");
        boolean isHotbar = parts[0].equals("hotbar");
        int index = Integer.parseInt(parts[1]);

        // init the command builder
        UICommandBuilder cmd = new UICommandBuilder();

        // try to get the clicked inventory slot/item or bail
        ItemContainer inv = isHotbar
                ? ((InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())).getInventory()
                : ((InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType())).getInventory();
        if (inv == null) { sendUpdate((UICommandBuilder) null, false); return; }

        // if slot is empty bail
        ItemStack stack = inv.getItemStack((short) index);
        if (stack == null || stack.isEmpty()) { sendUpdate((UICommandBuilder) null, false); return; }

        // loop over input slots and check if this item is in there
        for (int i = 0; i < inputSlotItems.length; i++) {
            SlotEntry entry = inputSlotItems[i];
            if (entry == null) continue;

            if(entry.slotN().equals(slotId)) {
                cmd.set("#" + (isHotbarItem(this.inputSlotItems[i].slotN) ? "Hotbar" : "Storage") + "InUseOverlay" + getSlotNumber(this.inputSlotItems[i].slotN) + ".Visible", false);
                cmd.setNull("#InputItem" + (i + 1) + ".ItemId");
                this.inputSlotItems[i] = null;

                sendUpdate(cmd, false);
                return;
            }
        }

        // if the item is not usable bail
        boolean usable = isComponentAllowed(stack.getItem().getId(), null);
        if (!usable) { sendUpdate((UICommandBuilder) null, false); return; }

        // deselect previously selected slot if any
        if (this.selectedSlotId != null) {
            cmd.set("#" + (isHotbarItem(this.selectedSlotId) ? "Hotbar" : "Storage") + "SelectedOverlay" + getSlotNumber(this.selectedSlotId) + ".Visible", false);
        }

        // if clicking the already selected slot, deselect it
        if (slotId.equals(this.selectedSlotId)) {
            this.selectedSlotId = null;
            this.selectedItem = null;
            clearInputSlots(cmd, true);
            sendUpdate(cmd, false);
            return;
        }

        // select the new slot — fade its ItemSlot to indicate it is "in use"
        this.selectedSlotId = slotId;
        this.selectedItem = stack.getItem();
        cmd.set("#" + (isHotbarItem(slotId) ? "Hotbar" : "Storage") + "SelectedOverlay" + getSlotNumber(slotId) + ".Visible", true);

        // update the input slots to indicate they can be used
        for (int i = 0; i < inputSlotItems.length; i++) {
            final Integer indx = i;
            boolean slotValid = isComponentAllowed(stack.getItem().getId(), indx);
            cmd.set("#InputSlotOverlay" + (i + 1) + ".Visible", !slotValid);
        }

        // send the updates
        sendUpdate(cmd, false);
    }

    private void handleInputPlace(int slot, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        // init the command builder
        UICommandBuilder cmd = new UICommandBuilder();

        // if there is a selected item, validate it can go in this slot before any other change
        if (this.selectedItem != null) {
            // check the selected item against this slot type, don't let components go into slots that dont support their type
            boolean slotValid = isComponentAllowed(this.selectedItem.getId(), slot - 1);
            if (!slotValid) {
                sendUpdate(cmd, false);
                return;
            }
        }

        // No matter what the input slot was clicked so we are either clearing or swaping an existing value if applicable
        if (this.inputSlotItems[slot - 1] != null) {
            cmd.set("#" + (isHotbarItem(this.inputSlotItems[slot - 1].slotN) ? "Hotbar" : "Storage") + "InUseOverlay" + getSlotNumber(this.inputSlotItems[slot - 1].slotN) + ".Visible", false);
            cmd.setNull("#InputItem" + slot + ".ItemId");
            this.inputSlotItems[slot - 1] = null;

            // if slots 1-3 were cleared
            if (slot - 1 < 3) {
                cmd.set("#CraftButton.Disabled", true);
            }
        }

        // if nothing was selected it's just a clear and we are good.
        if (this.selectedItem == null) {
            setOutputSlotState(cmd);
            sendUpdate(cmd, false);
            return;
        }

        // otherwise place the selected item id into the input slot array
        this.inputSlotItems[slot - 1] = new SlotEntry(this.selectedSlotId, this.selectedItem);

        // update the clicked input slot with the selected item icon
        cmd.set("#InputItem" + slot + ".ItemId", this.selectedItem.getId());

        // enable the in use overlay for the slotted item
        cmd.set("#" + (isHotbarItem(this.selectedSlotId) ? "Hotbar" : "Storage") + "InUseOverlay" + getSlotNumber(this.selectedSlotId) + ".Visible", true);

        // clear the selection overlay and data
        clearSelectedSlot(cmd);

        // clear input hilighting
        clearInputSlots(cmd, true);

        // toggle the output slot state
        setOutputSlotState(cmd);

        // send the updates
        sendUpdate(cmd, false);
    }

    private void handleCraft(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();

        // validate player still has all three components in their inventory
        for (int i = 0; i < 3; i++) {
            SlotEntry entry = inputSlotItems[i];
            if (entry == null) { sendUpdate((UICommandBuilder) null, false); return; }
            ItemContainer inv = isHotbarItem(entry.slotN())
                    ? ((InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())).getInventory()
                    : ((InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType())).getInventory();
            if (inv == null) { sendUpdate((UICommandBuilder) null, false); return; }
            short slot = Short.parseShort(getSlotNumber(entry.slotN()));
            ItemStack stack = inv.getItemStack(slot);
            if (stack == null || stack.isEmpty() || !stack.getItem().getId().equals(entry.item().getId())) {
                sendUpdate((UICommandBuilder) null, false);
                return;
            }
        }

        // determine rarity from shard slot — no shard means Common
        String rarity = "Common";
        if (inputSlotItems[3] != null) {
            String shardId = inputSlotItems[3].item().getId();
            rarity = shardId.substring(0, shardId.indexOf('_'));
        }

        // get player level for gear score
        Component_RPG_Player rpgPlayer = store.getComponent(ref, componentTypeRPGPlayer);
        int playerLevel = rpgPlayer == null ? 1 : rpgPlayer.level;

        // build the output item via item factory
        String outputItemId = ItemFactory.readCraftingComponent(inputSlotItems[0].item().getId())
                .get("outputAssetID").asString().getValue() + "_" + rarity;
        ItemStack output = ItemFactory.createItem(
                outputItemId,
                playerLevel,
                rarity,
                inputSlotItems[0].item().getId(),
                inputSlotItems[1].item().getId(),
                inputSlotItems[2].item().getId()
        );
        if (output == null) { sendUpdate((UICommandBuilder) null, false); return; }

        // remove components from inventory — one of each, last thing before giving item
        for (int i = 0; i < 3; i++) {
            SlotEntry entry = inputSlotItems[i];
            ItemContainer inv = isHotbarItem(entry.slotN())
                    ? ((InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())).getInventory()
                    : ((InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType())).getInventory();
            short slot = Short.parseShort(getSlotNumber(entry.slotN()));
            ItemStack current = inv.getItemStack(slot);
            inv.replaceItemStackInSlot(slot, current, current.withQuantity(current.getQuantity() - 1));
        }

        // remove shard if present
        if (inputSlotItems[3] != null) {
            SlotEntry shardEntry = inputSlotItems[3];
            ItemContainer inv = isHotbarItem(shardEntry.slotN())
                    ? ((InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType())).getInventory()
                    : ((InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType())).getInventory();
            short slot = Short.parseShort(getSlotNumber(shardEntry.slotN()));
            ItemStack current = inv.getItemStack(slot);
            inv.replaceItemStackInSlot(slot, current, current.withQuantity(current.getQuantity() - 1));
        }

        // give the player the crafted item
        InventoryComponent.Storage storage = (InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storage != null) storage.getInventory().addItemStack(output);

        // clear the UI
        clearSelectedSlot(cmd);
        clearInputSlots(cmd, false);
        setOutputSlotState(cmd);
        pushInventoryState(ref, store, cmd);
        applyState(cmd);
        applyInventoryUsabilityOverlays(ref, store, cmd);
        sendUpdate(cmd, false);
    }

    // send a UI update that bypasses PageManager acknowledgment counter so hover events are never blocked
    private void sendHoverUpdate(@Nonnull UICommandBuilder cmd) {
        Ref<EntityStore> ref = this.playerRef.getReference();
        if (ref == null) return;
        Store<EntityStore> store = ref.getStore();
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;
        this.playerRef.getPacketHandler().writeNoCache(new CustomPage(
                this.getClass().getName(), false, false, this.lifetime,
                cmd.getCommands(), UIEventBuilder.EMPTY_EVENT_BINDING_ARRAY
        ));
    }

    // converts a slotId like "storage:5" or "hotbar:2" to its ItemSlot element id e.g. "StorageItem5"
    private String getItemSlotId(@Nonnull String slotId) {
        String[] parts = slotId.split(":");
        boolean isHotbar = parts[0].equals("hotbar");
        return isHotbar ? "HotbarItem" + parts[1] : "StorageItem" + parts[1];
    }

    // determines if is hotbar or not
    private boolean isHotbarItem(@Nonnull String slotId) {
        String[] parts = slotId.split(":");
        return parts[0].equals("hotbar");
    }

    // determines if is hotbar or not
    private String getSlotNumber(@Nonnull String slotId) {
        String[] parts = slotId.split(":");
        return parts[1];
    }

    // apply all visibility and label state to a UICommandBuilder
    private void applyState(@Nonnull UICommandBuilder cmd) {
        // hide all icon rows then show only the active one
        for (String row : ALL_ICON_ROWS) cmd.set(row + ".Visible", false);
        cmd.set(getActiveIconRow() + ".Visible", true);

        // hide all diagrams then show only the active one
        for (String diagram : ALL_DIAGRAMS) cmd.set(diagram + ".Visible", false);
        cmd.set(getActiveDiagramId() + ".Visible", true);

        // show overlay only on active tab, hide all others
        for (int i = 0; i < ALL_TAB_OVERLAYS.length; i++) {
            cmd.set(ALL_TAB_OVERLAYS[i] + ".Visible", ALL_TAB_CATEGORIES[i].equals(this.activeCategory));
        }

        // overlay visible = inactive state, overlay hidden = active state
        for (int i = 0; i < ALL_ICON_OVERLAYS.length; i++) {
            cmd.set(ALL_ICON_OVERLAYS[i] + ".Visible", !ALL_ICON_ITEMS[i].equals(this.activeItem));
        }

        // update the category and sub category label text
        cmd.set("#CategoryLabel.TextSpans", Message.raw(getCategoryLabel(this.activeCategory)));
        cmd.set("#SubCategoryLabel.TextSpans", Message.raw(this.activeItem));

        // restore any items already placed in input slots
        for (int i = 0; i < inputSlotItems.length; i++) {
            if (inputSlotItems[i] != null) {
                cmd.set("#InputItem" + (i + 1) + ".ItemId", inputSlotItems[i].item.getId());
            }
        }

        // restore selected slot fade state if a selection is active
        if (this.selectedSlotId != null) {
            cmd.set("#" + getItemSlotId(this.selectedSlotId) + ".Visible", false);
        }
    }

    // returns the diagram group id for the current category and item
    private String getActiveDiagramId() {
        return switch (this.activeCategory) {
            case CAT_ARMOR -> "#DiagramArmor" + this.activeItem;
            case CAT_LEATHER -> "#DiagramLeather" + this.activeItem;
            case CAT_CLOTH -> "#DiagramCloth" + this.activeItem;
            default -> "#Diagram" + this.activeItem;
        };
    }

    // maps active category to its icon row group id
    private String getActiveIconRow() {
        return switch (this.activeCategory) {
            case CAT_2H -> "#ItemCategories2H";
            case CAT_RANGED -> "#ItemCategoriesRanged";
            case CAT_MAGIC -> "#ItemCategoriesMagic";
            case CAT_ARMOR -> "#ItemCategoriesArmor";
            case CAT_LEATHER -> "#ItemCategoriesLeather";
            case CAT_CLOTH -> "#ItemCategoriesCloth";
            default -> "#ItemCategories1H";
        };
    }

    // maps category id to display label text
    private String getCategoryLabel(@Nonnull String category) {
        return switch (category) {
            case CAT_2H -> "2H WEAPONS";
            case CAT_RANGED -> "RANGED WEAPONS";
            case CAT_MAGIC -> "MAGIC WEAPONS";
            case CAT_ARMOR -> "METAL ARMOR";
            case CAT_LEATHER -> "LEATHER ARMOR";
            case CAT_CLOTH -> "CLOTH ARMOR";
            default -> "1H WEAPONS";
        };
    }

    // returns the first item for a given category on initial load or category switch
    private String getDefaultItemForCategory(@Nonnull String category) {
        return switch (category) {
            case CAT_2H -> "Battleaxe";
            case CAT_RANGED -> "Crossbow";
            case CAT_MAGIC -> "Spellbook";
            case CAT_ARMOR, CAT_LEATHER, CAT_CLOTH -> "Helmet";
            default -> "Axe";
        };
    }

    // extracts the component type from the raw asset json e.g. "Axe Head"
    private static String extractComponentType(@Nonnull String itemId) {
        if (SHARD_IDS.contains(itemId)) return "Shard";
        BsonDocument component = readCraftingComponent(itemId);
        if (component == null) return null;
        BsonValue type = component.get("type");
        return (type != null && type.isString()) ? type.asString().getValue() : null;
    }

    // extracts the component tier from the raw asset json e.g. 1
    private static int extractComponentTier(@Nonnull String itemId) {
        BsonDocument component = readCraftingComponent(itemId);
        if (component == null) return -1;
        BsonValue tier = component.get("tier");
        return (tier != null && tier.isInt32()) ? tier.asInt32().getValue() : -1;
    }

    // checks if this item is allowed for the active weapon (and/or in this slot as applicable)
    public boolean isComponentAllowed(@Nonnull String itemId, @Nullable Integer slot) {
        String type = extractComponentType(itemId);
        if (type == null) return false;

        List<String> allowed = ALLOWED_COMPONENTS.get(this.activeItem);
        if (allowed == null) return false;

        if (slot == null) return allowed.contains(type);
        return allowed.get(slot).equals(type);
    }

    // Enable/disable inventory items based on the selected category
    private void applyInventoryUsabilityOverlays(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        // loop storage slots and set overlay based on whether the item is usable for the active weapon
        InventoryComponent.Storage storageComponent = (InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComponent != null) {
            ItemContainer storage = storageComponent.getInventory();
            for (short i = 0; i < Math.min(storage.getCapacity(), 36); i++) {
                ItemStack stack = storage.getItemStack(i);
                if (stack == null || stack.isEmpty()) continue;
                boolean usable = isComponentAllowed(stack.getItem().getId(), null);
                cmd.set("#StorageInvalidOverlay" + i + ".Visible", !usable);
            }
        }

        // loop hotbar slots and set overlay based on whether the item is usable for the active weapon
        InventoryComponent.Hotbar hotbarComponent = (InventoryComponent.Hotbar) store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        if (hotbarComponent != null) {
            ItemContainer hotbar = hotbarComponent.getInventory();
            for (short i = 0; i < Math.min(hotbar.getCapacity(), 9); i++) {
                ItemStack stack = hotbar.getItemStack(i);
                if (stack == null || stack.isEmpty()) continue;
                boolean usable = isComponentAllowed(stack.getItem().getId(), null);
                cmd.set("#HotbarInvalidOverlay" + i + ".Visible", !usable);
            }
        }
    }

    // push all inventory item ids to the UI slots
    private void pushInventoryState(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull UICommandBuilder cmd) {
        Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) return;

        // push storage slots
        InventoryComponent.Storage storageComponent = (InventoryComponent.Storage) store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        if (storageComponent != null) {
            ItemContainer storage = storageComponent.getInventory();
            for (short i = 0; i < Math.min(storage.getCapacity(), 36); i++) {
                ItemStack item = storage.getItemStack(i);
                String itemId = (item != null && !item.isEmpty()) ? item.getItem().getId() : null;
                if (itemId != null) {
                    cmd.set("#StorageItem" + i + ".ItemId", itemId);
                }
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
                if (itemId != null) {
                    cmd.set("#HotbarItem" + i + ".ItemId", itemId);
                }
                else cmd.setNull("#HotbarItem" + i + ".ItemId");
            }
        }

        // apply usability overlays based on default active item
        applyInventoryUsabilityOverlays(ref, store, cmd);
    }

    // clear the selected slot overlays and data
    private void clearSelectedSlot(UICommandBuilder cmd) {
        if(this.selectedSlotId == null || this.selectedItem == null) return;

        cmd.set("#" + (isHotbarItem(this.selectedSlotId) ? "Hotbar" : "Storage") + "SelectedOverlay" + getSlotNumber(this.selectedSlotId) + ".Visible", false);
        this.selectedSlotId = null;
        this.selectedItem = null;
    }

    // clear the input slot overlays and data
    private void clearInputSlots(UICommandBuilder cmd, boolean onlyClearOverlays) {
        // update the input slots to indicate they can be used
        for (int i = 0; i < inputSlotItems.length; i++) {
            cmd.set("#InputSlotOverlay" + (i + 1) + ".Visible", false);
            if (!onlyClearOverlays && inputSlotItems[i] != null) {
                cmd.set("#" + (isHotbarItem(this.inputSlotItems[i].slotN) ? "Hotbar" : "Storage") + "InUseOverlay" + getSlotNumber(this.inputSlotItems[i].slotN) + ".Visible", false);
                cmd.setNull("#InputItem" + (i + 1) + ".ItemId");
                this.inputSlotItems[i] = null;
            }
        }
    }

    // update teh crafting result panel/window
    private void setOutputSlotState(@Nonnull UICommandBuilder cmd) {
        if (this.inputSlotItems[0] != null && this.inputSlotItems[1] != null && this.inputSlotItems[2] != null) {
            cmd.set("#CraftButton.Disabled", false);

            // determine rarity suffix from shard slot — default to Common if empty
            String rarity = "Common";
            if (this.inputSlotItems[3] != null) {
                String shardId = this.inputSlotItems[3].item().getId();
                rarity = shardId.substring(0, shardId.indexOf('_'));
            }

            // read output asset id from slot 0's CraftingComponent and set output item
            BsonDocument slot0Component = readCraftingComponent(this.inputSlotItems[0].item().getId());
            if (slot0Component != null) {
                BsonValue outputId = slot0Component.get("outputAssetID");
                if (outputId != null && outputId.isString()) {
                    cmd.set("#OutputItem.ItemId", outputId.asString().getValue() + "_" + rarity);
                }
            }

            // collect weapon type label e.g. "Axe (1H)"
            String handedness = List.of(CAT_2H, CAT_RANGED, CAT_MAGIC).contains(this.activeCategory) ? "2H" : "1H";
            cmd.set("#WeaponType.Text", this.activeItem + " (" + handedness + ")");

            // collect base weapon damage from slot 0 implicits and remaining implicits from all slots
            String weaponDamageText = "";
            List<String> implicitLines = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                if (this.inputSlotItems[i] == null) continue;
                BsonDocument comp = readCraftingComponent(this.inputSlotItems[i].item().getId());
                if (comp == null) continue;
                BsonValue implicitsVal = comp.get("implicits");
                if (implicitsVal == null || !implicitsVal.isArray()) continue;

                for (BsonValue entry : implicitsVal.asArray()) {
                    if (!entry.isDocument()) continue;
                    BsonDocument implicit = entry.asDocument();
                    BsonValue statVal = implicit.get("stat");
                    BsonValue minVal = implicit.get("min");
                    BsonValue maxVal = implicit.get("max");
                    if (statVal == null || minVal == null || maxVal == null) continue;

                    StatType stat;
                    try { stat = StatType.valueOf(statVal.asString().getValue()); }
                    catch (Exception e) { continue; }

                    float min = minVal.isDouble() ? (float) minVal.asDouble().getValue() : (float) minVal.asInt32().getValue();
                    float max = maxVal.isDouble() ? (float) maxVal.asDouble().getValue() : (float) maxVal.asInt32().getValue();

                    // slot 0's first main/off hand damage flat is the weapon damage — shown separately
                    if (i == 0 && weaponDamageText.isEmpty() && StatTypeInfo.isWeaponDamageStat(stat)) {
                        weaponDamageText = StatTypeInfo.getDisplay(stat, min, max);
                    } else {
                        implicitLines.add(StatTypeInfo.getDisplay(stat, min, max));
                    }
                }
            }

            // populate weapon stats section
            cmd.set("#WeaponDamage.Text", weaponDamageText);
            cmd.set("#WeaponStats.Visible", true);

            // populate implicit lines 1-5 — clear any beyond what we have
            for (int i = 0; i < 5; i++) {
                cmd.set("#ImplicitLine" + (i + 1) + ".Text", i < implicitLines.size() ? implicitLines.get(i) : "");
            }

            // populate affix count line based on rarity
            int affixCount = switch (rarity) {
                case "Uncommon" -> 1;
                case "Rare" -> 2;
                case "Epic" -> 3;
                case "Legendary" -> 4;
                default -> 0;
            };
            cmd.set("#ImplicitLine5Empty.Text", "");
            cmd.set("#AffixCount.Text", affixCount == 1 ? "+1 random stat" : "+" + affixCount + " random stats");

        } else {
            // clear output state
            cmd.setNull("#OutputItem.ItemId");
            cmd.set("#CraftButton.Disabled", true);
            cmd.set("#WeaponStats.Visible", false);
        }
    }

    // reads and caches the CraftingComponent block from a raw asset json — returns null if not present
    private static BsonDocument readCraftingComponent(@Nonnull String itemId) {
        return CRAFTING_COMPONENT_CACHE.computeIfAbsent(itemId, id -> {
            Path assetPath = Item.getAssetMap().getPath(id);
            if (assetPath == null) return null;
            try {
                BsonDocument doc = BsonDocument.parse(Files.readString(assetPath));
                BsonValue component = doc.get("CraftingComponent");
                return (component != null && component.isDocument()) ? component.asDocument() : null;
            } catch (Exception e) {
                return null;
            }
        });
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
                .<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .append(new KeyedCodec<>("ItemStackId", Codec.STRING), (d, v) -> d.itemStackId = v, d -> d.itemStackId).add()
                .append(new KeyedCodec<>("DragItemStackId", Codec.STRING), (d, v) -> d.dragItemStackId = v, d -> d.dragItemStackId).add()
                .append(new KeyedCodec<>("SourceSlotId", Codec.INTEGER), (d, v) -> d.sourceSlotId = v, d -> d.sourceSlotId).add()
                .append(new KeyedCodec<>("SourceInventorySectionId", Codec.INTEGER), (d, v) -> d.sourceInventorySectionId = v, d -> d.sourceInventorySectionId).add()
                .append(new KeyedCodec<>("SlotIndex", Codec.INTEGER), (d, v) -> d.slotIndex = v, d -> d.slotIndex).add()
                .build();

        public String action;
        public String itemStackId;
        public String dragItemStackId;
        public Integer sourceSlotId;
        public Integer sourceInventorySectionId;
        public Integer slotIndex;
    }
}