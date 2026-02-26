package com.example.hyarpg.ui;

// Hytale imports
import com.example.hyarpg.utils.Affix;
import com.example.hyarpg.utils.AffixPool;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// HyUI imports
import au.ellie.hyui.builders.PageBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Page_RPGStats {

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store) {
        // get the player and their armor slots
        Player player = store.getComponent(ref, Player.getComponentType());
        Inventory inventory = player.getInventory();
        ItemContainer armor = inventory.getArmor();

        // Get equipped item IDs from player inventory
        String headItem = getItemId(armor.getItemStack((short) ItemArmorSlot.Head.ordinal()));
        String chestItem = getItemId(armor.getItemStack((short) ItemArmorSlot.Chest.ordinal()));
        String handsItem = getItemId(armor.getItemStack((short) ItemArmorSlot.Hands.ordinal()));
        String legsItem = getItemId(armor.getItemStack((short) ItemArmorSlot.Legs.ordinal()));

        // Main hand = active hotbar item, off hand = active utility item
        String mainHandItem = getItemId(inventory.getItemInHand());
        String offHandItem = getItemId(inventory.getUtilityItem());

        // get equipped item affixes
        List<String> headAffixes = getAffixes(armor.getItemStack((short) ItemArmorSlot.Head.ordinal()));
        List<String> chestAffixes = getAffixes(armor.getItemStack((short) ItemArmorSlot.Chest.ordinal()));
        List<String> handsAffixes = getAffixes(armor.getItemStack((short) ItemArmorSlot.Hands.ordinal()));
        List<String> legsAffixes = getAffixes(armor.getItemStack((short) ItemArmorSlot.Legs.ordinal()));
        List<String> mainHandAffixes = getAffixes(inventory.getItemInHand());
        List<String> offHandAffixes = getAffixes(inventory.getUtilityItem());

        // formulate the HTML
        String html = """
        <div class="page-overlay">
            <button id="closeBtn" style="anchor-bottom: 10; anchor-width: 750; anchor-height: 40;">Close</button>
    
            <div class="container"
                 data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;"
                 data-hyui-title="RPG Stats"
                 style="anchor-width: 750; anchor-height: 700;">
    
                <div class="container-contents" style="layout-mode: topscrolling;">
    
                    <!-- ROW 1: Head | Chest -->
                    <div style="layout-mode: left; anchor-width: 700; margin-bottom: 20;">
                        <div style="layout-mode: left; anchor-width: 350; padding: 10;">
                            <span class="item-slot" data-hyui-item-id="${HEAD_ITEM}"
                                  data-hyui-show-quality-background="true"
                                  data-hyui-show-quantity="false"
                                  style="anchor-width: 80; anchor-height: 80;"></span>
                            <div style="layout-mode: top; anchor-width: 240; anchor-left: 10;">
                                ${HEAD_AFFIXES}
                            </div>
                        </div>
                        <div style="layout-mode: left; anchor-width: 350; padding: 10;">
                            <span class="item-slot" data-hyui-item-id="${CHEST_ITEM}"
                                  data-hyui-show-quality-background="true"
                                  data-hyui-show-quantity="false"
                                  style="anchor-width: 80; anchor-height: 80;"></span>
                            <div style="layout-mode: top; anchor-width: 240; anchor-left: 10;">
                                ${CHEST_AFFIXES}
                            </div>
                        </div>
                    </div>
    
                    <!-- ROW 2: Hands | Legs -->
                    <div style="layout-mode: left; anchor-width: 700; margin-bottom: 20;">
                        <div style="layout-mode: left; anchor-width: 350; padding: 10;">
                            <span class="item-slot" data-hyui-item-id="${HANDS_ITEM}"
                                  data-hyui-show-quality-background="true"
                                  data-hyui-show-quantity="false"
                                  style="anchor-width: 80; anchor-height: 80;"></span>
                            <div style="layout-mode: top; anchor-width: 240; anchor-left: 10;">
                                ${HANDS_AFFIXES}
                            </div>
                        </div>
                        <div style="layout-mode: left; anchor-width: 350; padding: 10;">
                            <span class="item-slot" data-hyui-item-id="${LEGS_ITEM}"
                                  data-hyui-show-quality-background="true"
                                  data-hyui-show-quantity="false"
                                  style="anchor-width: 80; anchor-height: 80;"></span>
                            <div style="layout-mode: top; anchor-width: 240; anchor-left: 10;">
                                ${LEGS_AFFIXES}
                            </div>
                        </div>
                    </div>
    
                    <!-- ROW 3: Main Hand | Off Hand -->
                    <div style="layout-mode: left; anchor-width: 700; margin-bottom: 20;">
                        <div style="layout-mode: left; anchor-width: 350; padding: 10;">
                            <span class="item-slot" data-hyui-item-id="${MAINHAND_ITEM}"
                                  data-hyui-show-quality-background="true"
                                  data-hyui-show-quantity="true"
                                  style="anchor-width: 80; anchor-height: 80;"></span>
                            <div style="layout-mode: top; anchor-width: 240; anchor-left: 10;">
                                ${MAINHAND_AFFIXES}
                            </div>
                        </div>
                        <div style="layout-mode: left; anchor-width: 350; padding: 10;">
                            <span class="item-slot" data-hyui-item-id="${OFFHAND_ITEM}"
                                  data-hyui-show-quality-background="true"
                                  data-hyui-show-quantity="true"
                                  style="anchor-width: 80; anchor-height: 80;"></span>
                            <div style="layout-mode: top; anchor-width: 240; anchor-left: 10;">
                                ${OFFHAND_AFFIXES}
                            </div>
                        </div>
                    </div>
    
                </div>
            </div>
        </div>
        """;

        // generate blank affix HTML
        String headAffixHTML = getAffixSlotHTML(headAffixes);
        String chestAffixHTML = getAffixSlotHTML(chestAffixes);
        String handsAffixHTML = getAffixSlotHTML(handsAffixes);
        String legsAffixHTML = getAffixSlotHTML(legsAffixes);
        String mainHandAffixHTML = getAffixSlotHTML(mainHandAffixes);
        String offHandAffixHTML = getAffixSlotHTML(offHandAffixes);

        // replace the tokens in the html with our data
        Map<String, String> stringTokens = Map.ofEntries(
            Map.entry("HEAD_ITEM", headItem),
            Map.entry("HEAD_AFFIXES", headAffixHTML),
            Map.entry("CHEST_ITEM", chestItem),
            Map.entry("CHEST_AFFIXES", chestAffixHTML),
            Map.entry("HANDS_ITEM", handsItem),
            Map.entry("HANDS_AFFIXES", handsAffixHTML),
            Map.entry("LEGS_ITEM", legsItem),
            Map.entry("LEGS_AFFIXES", legsAffixHTML),
            Map.entry("MAINHAND_ITEM", mainHandItem),
            Map.entry("MAINHAND_AFFIXES", mainHandAffixHTML),
            Map.entry("OFFHAND_ITEM", offHandItem),
            Map.entry("OFFHAND_AFFIXES", offHandAffixHTML)
        );
        html = replaceStringTokens(html, stringTokens);

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        PageBuilder.pageForPlayer(playerRef)
                .fromHtml(html)
                .addEventListener("closeBtn", CustomUIEventBindingType.Activating, (ctx) -> {
                    player.getPageManager().setPage(ref, store, Page.None);
                })
                .open(store);
    }

    // Helper to safely get item ID string, returning empty string if slot is empty
    private static String getItemId(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getItem() == null) return "";
        return itemStack.getItem().getId();
    }

    // string token replacement for better string templating
    private static String replaceStringTokens(String template, Map<String, String> values) {
        String result = template;

        for (var entry : values.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }

    // function to try to get affix meta data or return empty array
    private static List<String> getAffixes(ItemStack stack) {
        // Make an empty list and return it if no item stack passed
        List<String> empty = new ArrayList<String>();
        if(stack == null) return empty;

        // try to get affixes and if not found return the empty list
        String[] affixes = stack.getFromMetadataOrNull("affixes", Codec.STRING_ARRAY);
        return affixes == null ? empty : Arrays.asList(affixes);
    }

    // funciton to compute affix slot HTML
    private static String getAffixSlotHTML(List<String> affixes) {
        // set a return string
        String html = "";

        // set head affixes HTML
        for (String str : affixes) {
            // split the affix string into parts and validate it
            var parts = str.split("\\|");
            if(parts[0] == null || parts[1] == null) continue;

            // get the affix id and a new affix
            String id = parts[0];
            Affix affix = AffixPool.getAffixByStatName(id);
            if(affix == null) continue;

            // formulate the HTML
            Float value = Float.parseFloat(parts[1]);
            html += "<p>* " + affix.display().formatted(Math.round(value * 10) / 10f) + "</p>";
        }

        // return the HTML string
        return html;
    }

}