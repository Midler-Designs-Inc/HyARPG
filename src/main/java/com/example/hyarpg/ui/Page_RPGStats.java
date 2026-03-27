package com.example.hyarpg.ui;

// Hytale Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPG_System;
import com.example.hyarpg.utils.affixes.Affix;
import com.example.hyarpg.utils.affixes.AffixPool;
import com.example.hyarpg.utils.affixes.ImplicitAffixPool;
import com.example.hyarpg.utils.affixes.EntityStats;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// HyUI Imports
import au.ellie.hyui.builders.PageBuilder;

// Java Imports
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Page_RPGStats {

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store) {
        // get rpg player component
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPG_System.componentTypeRPGPlayer);

        // get armor container directly from component
        InventoryComponent.Armor armorComp = store.getComponent(ref, InventoryComponent.Armor.getComponentType());
        ItemContainer armor = armorComp != null ? armorComp.getInventory() : null;

        // get main hand and off hand
        ItemStack mainHandStack = InventoryComponent.getItemInHand(store, ref);
        InventoryComponent.Utility utilityComp = store.getComponent(ref, InventoryComponent.Utility.getComponentType());
        ItemStack offHandStack = utilityComp != null ? utilityComp.getActiveItem() : null;

        // --- Gear tab: item IDs ---
        String headItem     = armor != null ? getItemId(armor.getItemStack((short) ItemArmorSlot.Head.ordinal()))  : "";
        String chestItem    = armor != null ? getItemId(armor.getItemStack((short) ItemArmorSlot.Chest.ordinal())) : "";
        String handsItem    = armor != null ? getItemId(armor.getItemStack((short) ItemArmorSlot.Hands.ordinal())) : "";
        String legsItem     = armor != null ? getItemId(armor.getItemStack((short) ItemArmorSlot.Legs.ordinal()))  : "";
        String mainHandItem = getItemId(mainHandStack);
        String offHandItem  = getItemId(offHandStack);

        // --- Gear tab: affix + implicit HTML ---
        String headAffixHTML     = armor != null ? getGearSlotHTML(getAffixes(armor.getItemStack((short) ItemArmorSlot.Head.ordinal())),  getImplicits(armor.getItemStack((short) ItemArmorSlot.Head.ordinal())))  : "";
        String chestAffixHTML    = armor != null ? getGearSlotHTML(getAffixes(armor.getItemStack((short) ItemArmorSlot.Chest.ordinal())), getImplicits(armor.getItemStack((short) ItemArmorSlot.Chest.ordinal()))) : "";
        String handsAffixHTML    = armor != null ? getGearSlotHTML(getAffixes(armor.getItemStack((short) ItemArmorSlot.Hands.ordinal())), getImplicits(armor.getItemStack((short) ItemArmorSlot.Hands.ordinal()))) : "";
        String legsAffixHTML     = armor != null ? getGearSlotHTML(getAffixes(armor.getItemStack((short) ItemArmorSlot.Legs.ordinal())),  getImplicits(armor.getItemStack((short) ItemArmorSlot.Legs.ordinal())))  : "";
        String mainHandAffixHTML = getGearSlotHTML(getAffixes(mainHandStack), getImplicits(mainHandStack));
        String offHandAffixHTML  = getGearSlotHTML(getAffixes(offHandStack),  getImplicits(offHandStack));

        // get relevant info
        EntityStats playerStats = rpgPlayer.stats;
        int gearScore           = rpgPlayer.gearScore;
        int playerLevel         = rpgPlayer.level;
        boolean usingShield     = !ItemStack.isEmpty(offHandStack) && offHandItem.contains("Weapon_Shield");
        String statsHTML        = buildStatsHTML(playerStats, playerLevel, gearScore, usingShield);

        String html = """
        <div class="page-overlay">
            <button id="closeBtn" style="anchor-bottom: 10; anchor-width: 900; anchor-height: 40;">Close</button>

            <div class="container"
                 data-hyui-title="RPG Stats"
                 style="anchor-width: 900; anchor-height: 700;">

                <div class="container-contents" style="layout-mode: top; padding: 6;">

                    <!-- Tab navigation -->
                    <nav id="rpg-tabs" class="tabs"
                         data-tabs="gear:Gear:gear-content,stats:Stats:stats-content"
                         data-selected="gear">
                    </nav>

                    <!-- TAB 1: Gear -->
                    <div id="gear-content" class="tab-content"
                         data-hyui-tab-id="gear"
                         data-hyui-tab-nav="rpg-tabs">
                        <div style="layout-mode: topscrolling; anchor-width: 870; anchor-height: 600;"
                             data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                            <div style="layout-mode: top; anchor-width: 850; margin-left: 15; margin-top: 10;">

                                <!-- ROW 1: Head | Chest -->
                                <div style="layout-mode: left; anchor-width: 850; margin-bottom: 20;">
                                    <div style="layout-mode: left; anchor-width: 425; padding: 10;">
                                        <span class="item-slot" data-hyui-item-id="${HEAD_ITEM}"
                                              data-hyui-show-quality-background="true"
                                              data-hyui-show-quantity="false"
                                              style="anchor-width: 96; anchor-height: 96;"></span>
                                        <div style="layout-mode: top; anchor-width: 295; margin-left: 10;">
                                            ${HEAD_AFFIXES}
                                        </div>
                                    </div>
                                    <div style="layout-mode: left; anchor-width: 425; padding: 10;">
                                        <span class="item-slot" data-hyui-item-id="${CHEST_ITEM}"
                                              data-hyui-show-quality-background="true"
                                              data-hyui-show-quantity="false"
                                              style="anchor-width: 96; anchor-height: 96;"></span>
                                        <div style="layout-mode: top; anchor-width: 295; margin-left: 10;">
                                            ${CHEST_AFFIXES}
                                        </div>
                                    </div>
                                </div>

                                <!-- ROW 2: Hands | Legs -->
                                <div style="layout-mode: left; anchor-width: 850; margin-bottom: 20;">
                                    <div style="layout-mode: left; anchor-width: 425; padding: 10;">
                                        <span class="item-slot" data-hyui-item-id="${HANDS_ITEM}"
                                              data-hyui-show-quality-background="true"
                                              data-hyui-show-quantity="false"
                                              style="anchor-width: 96; anchor-height: 96;"></span>
                                        <div style="layout-mode: top; anchor-width: 295; margin-left: 10;">
                                            ${HANDS_AFFIXES}
                                        </div>
                                    </div>
                                    <div style="layout-mode: left; anchor-width: 425; padding: 10;">
                                        <span class="item-slot" data-hyui-item-id="${LEGS_ITEM}"
                                              data-hyui-show-quality-background="true"
                                              data-hyui-show-quantity="false"
                                              style="anchor-width: 96; anchor-height: 96;"></span>
                                        <div style="layout-mode: top; anchor-width: 295; margin-left: 10;">
                                            ${LEGS_AFFIXES}
                                        </div>
                                    </div>
                                </div>

                                <!-- ROW 3: Main Hand | Off Hand -->
                                <div style="layout-mode: left; anchor-width: 850; margin-bottom: 20;">
                                    <div style="layout-mode: left; anchor-width: 425; padding: 10;">
                                        <span class="item-slot" data-hyui-item-id="${MAINHAND_ITEM}"
                                              data-hyui-show-quality-background="true"
                                              data-hyui-show-quantity="true"
                                              style="anchor-width: 96; anchor-height: 96;"></span>
                                        <div style="layout-mode: top; anchor-width: 295; margin-left: 10;">
                                            ${MAINHAND_AFFIXES}
                                        </div>
                                    </div>
                                    <div style="layout-mode: left; anchor-width: 425; padding: 10;">
                                        <span class="item-slot" data-hyui-item-id="${OFFHAND_ITEM}"
                                              data-hyui-show-quality-background="true"
                                              data-hyui-show-quantity="true"
                                              style="anchor-width: 96; anchor-height: 96;"></span>
                                        <div style="layout-mode: top; anchor-width: 295; margin-left: 10;">
                                            ${OFFHAND_AFFIXES}
                                        </div>
                                    </div>
                                </div>

                            </div>
                        </div>
                    </div>

                    <!-- TAB 2: Stats -->
                    <div id="stats-content" class="tab-content"
                         data-hyui-tab-id="stats"
                         data-hyui-tab-nav="rpg-tabs">
                        <div style="layout-mode: topscrolling; anchor-width: 870; anchor-height: 600;"
                             data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                            ${STATS_HTML}
                        </div>
                    </div>

                </div>
            </div>
        </div>
        """;

        html = replaceStringTokens(html, Map.ofEntries(
                Map.entry("HEAD_ITEM",        headItem),
                Map.entry("HEAD_AFFIXES",     headAffixHTML),
                Map.entry("CHEST_ITEM",       chestItem),
                Map.entry("CHEST_AFFIXES",    chestAffixHTML),
                Map.entry("HANDS_ITEM",       handsItem),
                Map.entry("HANDS_AFFIXES",    handsAffixHTML),
                Map.entry("LEGS_ITEM",        legsItem),
                Map.entry("LEGS_AFFIXES",     legsAffixHTML),
                Map.entry("MAINHAND_ITEM",    mainHandItem),
                Map.entry("MAINHAND_AFFIXES", mainHandAffixHTML),
                Map.entry("OFFHAND_ITEM",     offHandItem),
                Map.entry("OFFHAND_AFFIXES",  offHandAffixHTML),
                Map.entry("STATS_HTML",       statsHTML)
        ));

        // Player is still needed here solely for getPageManager()
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        PageBuilder.pageForPlayer(playerRef)
                .fromHtml(html)
                .addEventListener("closeBtn", CustomUIEventBindingType.Activating, (ctx) -> {
                    player.getPageManager().setPage(ref, store, Page.None);
                })
                .open(store);
    }

    // -------------------------------------------------------------------------
    // Stats tab
    // -------------------------------------------------------------------------
    private static String buildStatsHTML(EntityStats s, int playerLevel, int gearScore, boolean usingShield) {
        StringBuilder sb = new StringBuilder();

        // ---- Two-column layout ----
        sb.append("<div style=\"layout-mode: left; anchor-width: 700;\">");

        // Left column
        sb.append("<div style=\"layout-mode: top; anchor-width: 340;\">");

        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Overview");
        addStat(sb, "Player Level", String.valueOf(playerLevel));
        addStat(sb, "Gear Score",   String.valueOf(gearScore));
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Offense");
        addStat(sb, "Physical Damage",   "+" + fmt(s.getFlatDamage("Physical")));
        addStat(sb, "Physical Damage",   "+" + fmt(s.getIncreasedDamage("Physical")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Magic Damage",      "+" + fmt(s.getFlatDamage("Magic")));
        addStat(sb, "Magic Damage",      "+" + fmt(s.getIncreasedDamage("Magic")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Fire Damage",       "+" + fmt(s.getFlatDamage("Fire")));
        addStat(sb, "Fire Damage",       "+" + fmt(s.getIncreasedDamage("Fire")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Ice Damage",        "+" + fmt(s.getFlatDamage("Ice")));
        addStat(sb, "Ice Damage",        "+" + fmt(s.getIncreasedDamage("Ice")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Lightning Damage",  "+" + fmt(s.getFlatDamage("Lightning")));
        addStat(sb, "Lightning Damage",  "+" + fmt(s.getIncreasedDamage("Lightning")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Poison Damage",     "+" + fmt(s.getFlatDamage("Poison")));
        addStat(sb, "Poison Damage",     "+" + fmt(s.getIncreasedDamage("Poison")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Crit Chance",    fmt(s.getCriticalStrikeChance()) + "%");
        addStat(sb, "Crit Damage",    fmt(s.getCriticalStrikeDamage()) + "x");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Axe Damage",    fmt(s.getIncreasedDamage("Axe")) + "%");
        addStat(sb, "Battleaxe Damage",    fmt(s.getIncreasedDamage("Battleaxe")) + "%");
        addStat(sb, "Club Damage",    fmt(s.getIncreasedDamage("Club")) + "%");
        addStat(sb, "Daggers Damage",    fmt(s.getIncreasedDamage("Daggers")) + "%");
        addStat(sb, "Kunai Damage",    fmt(s.getIncreasedDamage("Kunai")) + "%");
        addStat(sb, "Longsword Damage",    fmt(s.getIncreasedDamage("Longsword")) + "%");
        addStat(sb, "Mace Damage",    fmt(s.getIncreasedDamage("Mace")) + "%");
        addStat(sb, "Shortbow Damage",    fmt(s.getIncreasedDamage("Shortbow")) + "%");
        addStat(sb, "Crossbow Damage",    fmt(s.getIncreasedDamage("Crossbow")) + "%");
        addStat(sb, "Sword Damage",    fmt(s.getIncreasedDamage("Sword")) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Defense");
        addStat(sb, "Dodge Chance", fmt(s.getDodgeChance()) + "%");
        addStat(sb, "Stability", fmt(s.getStabilityPercent(usingShield)) + "%");
        addStat(sb, "Parry Window", "+" + fmt(s.getParryWindow()) + "(s)");
        addStat(sb, "Barrier on Block", fmt(s.getBarrierOnBlock()) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        sb.append("</div>"); // end left column

        // -- Right column --
        sb.append("<div style=\"layout-mode: top; anchor-width: 340;margin-left: 15;\">");

        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Resources");
        addStat(sb, "Life",         "+" + fmt(s.getFlatResource("Life")));
        addStat(sb, "Life",         "+" + fmt(s.getIncreasedResource("Life")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Stamina",      "+" + fmt(s.getFlatResource("Stamina")));
        addStat(sb, "Stamina",      "+" + fmt(s.getIncreasedResource("Stamina")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Mana",         "+" + fmt(s.getFlatResource("Mana")));
        addStat(sb, "Mana",         "+" + fmt(s.getIncreasedResource("Mana")) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Regeneration");
        addStat(sb, "Life Regen",   "+" + fmt(s.getFlatResourceRegen("Life")) + "s");
        addStat(sb, "Life Regen",   "+" + fmt(s.getIncreasedResourceRegen("Life")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Stamina Regen","+" + fmt(s.getFlatResourceRegen("Stamina")) + "s");
        addStat(sb, "Stamina Regen","+" + fmt(s.getIncreasedResourceRegen("Stamina")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Mana Regen",   "+" + fmt(s.getFlatResourceRegen("Mana")) + "s");
        addStat(sb, "Mana Regen",   "+" + fmt(s.getIncreasedResourceRegen("Mana")) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Resistances");
        addStat(sb, "Physical Resist", fmt(s.getResistance("Physical")) + "%");
        addStat(sb, "Magic Resist", fmt(s.getResistance("Magic")) + "%");
        addStat(sb, "Elemental Resist", fmt(s.getResistance("Elemental")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Fire Resist", fmt(s.getResistance("Fire")) + "%");
        addStat(sb, "Ice Resist", fmt(s.getResistance("Ice")) + "%");
        addStat(sb, "Lightning Resist", fmt(s.getResistance("Lightning")) + "%");
        addStat(sb, "Poison Resist", fmt(s.getResistance("Poison")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Fall Resist", fmt(s.getResistance("Fall")) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Utility");
        addStat(sb, "Run Speed",   "+" + fmt(s.getRunSpeedPercent()) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        addStat(sb, "Ammo",         "+" + fmt(s.getAddedAmmo()));
        addStat(sb, "Ammo Regen",   "+" + fmt(s.getAmmoRegenPercent()) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        sb.append("</div>"); // end right column
        sb.append("</div>"); // end two-column row

        return sb.toString();
    }

    private static void addSectionHeader(StringBuilder sb, String title) {
        sb.append("<p style=\"font-size: 20;margin-left: 5;margin-top: 3;color: #888888;\">")
                .append("<span data-hyui-bold=\"true\">")
                .append(title)
                .append("</span></p>");
    }

    private static void addStat(StringBuilder sb, String label, String value) {
        sb.append("<div style=\"layout-mode: left; anchor-width: 300; margin-left: 10; margin-bottom: 2;\">")
                .append("<p style=\"anchor-width: 200;\">").append(label).append("</p>")
                .append("<p style=\"anchor-width: 90; text-align: right;\">").append(value).append("</p>")
                .append("</div>");
    }

    private static String fmt(float value) {
        float rounded = Math.round(value * 10) / 10f;
        return rounded == (int) rounded
                ? String.valueOf((int) rounded)
                : String.valueOf(rounded);
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private static String getItemId(@Nullable ItemStack itemStack) {
        if (itemStack == null || itemStack.getItem() == null) return "";
        return itemStack.getItem().getId();
    }

    private static String replaceStringTokens(String template, Map<String, String> values) {
        String result = template;
        for (var entry : values.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    private static List<String> getAffixes(@Nullable ItemStack stack) {
        List<String> empty = new ArrayList<>();
        if (stack == null) return empty;
        String[] affixes = stack.getFromMetadataOrNull("affixes", Codec.STRING_ARRAY);
        return affixes == null ? empty : Arrays.asList(affixes);
    }

    private static List<String> getImplicits(@Nullable ItemStack stack) {
        List<String> empty = new ArrayList<>();
        if (stack == null) return empty;
        String[] implicits = stack.getFromMetadataOrNull("implicits", Codec.STRING_ARRAY);
        return implicits == null ? empty : Arrays.asList(implicits);
    }

    private static String getGearSlotHTML(List<String> affixes, List<String> implicits) {
        StringBuilder html = new StringBuilder();

        // implicits — format: "Stat_Increased_Stamina|4.4|display:Stamina: +%s%%"
        for (String str : implicits) {
            String[] parts = str.split("\\|");
            if (parts.length < 3) continue;
            float value = Float.parseFloat(parts[1]);
            String display = parts[2];
            html.append("<p style=\"font-size: 11; color: #c8a84b;\">")
                    .append(display.formatted(Math.round(value * 10) / 10f))
                    .append("</p>");
        }

        // divider — only shown when there are both implicits and affixes
        if (!implicits.isEmpty() && !affixes.isEmpty()) {
            html.append("<div style=\"anchor-width: 260; anchor-height: 1; margin-top: 3; margin-bottom: 3; background-color: #444444;\"></div>");
        }

        // affixes
        for (String str : affixes) {
            String[] parts = str.split("\\|");
            if (parts.length < 2) continue;
            String id = parts[0];
            Affix affix = AffixPool.getAffixByStatName(id);
            if (affix == null) continue;
            float value = Float.parseFloat(parts[1]);
            html.append("<p style=\"font-size: 11;\">T").append((int) affix.tier())
                    .append(" ").append(affix.display().formatted(Math.round(value * 10) / 10f))
                    .append("</p>");
        }

        return html.toString();
    }
}