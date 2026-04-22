package com.example.hyarpg.ui;

// Hytale Imports
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

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.affixes.Affix;
import com.example.hyarpg.utils.affixes.AffixPool;
import com.example.hyarpg.utils.affixes.EntityStats;
import com.example.hyarpg.utils.affixes.StatType;
import com.example.hyarpg.utils.StatTypeInfo;

// HyUI Imports
import au.ellie.hyui.builders.PageBuilder;

// Java Imports
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Page_RPGStats {

    // damage type colors
    private static final Map<String, String> DAMAGE_COLORS = Map.of(
            "Fire",     "#e8472a",
            "Lightning","#2a6be8",
            "Ice",      "#a8d8ea",
            "Poison",   "#4caf50",
            "Physical", "#ffffff",
            "Magic",    "#cc44cc"
    );

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store) {
        // get rpg player component
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);

        // get armor container directly from component
        InventoryComponent.Armor armorComp = store.getComponent(ref, InventoryComponent.Armor.getComponentType());
        ItemContainer armor = armorComp != null ? armorComp.getInventory() : null;

        // get main hand and off hand
        ItemStack mainHandStack = InventoryComponent.getItemInHand(store, ref);
        InventoryComponent.Utility utilityComp = store.getComponent(ref, InventoryComponent.Utility.getComponentType());
        ItemStack offHandStack = utilityComp != null ? utilityComp.getActiveItem() : null;

        // gear tab item ids
        String headItem     = armor != null ? getItemId(armor.getItemStack((short) ItemArmorSlot.Head.ordinal()))  : "";
        String chestItem    = armor != null ? getItemId(armor.getItemStack((short) ItemArmorSlot.Chest.ordinal())) : "";
        String handsItem    = armor != null ? getItemId(armor.getItemStack((short) ItemArmorSlot.Hands.ordinal())) : "";
        String legsItem     = armor != null ? getItemId(armor.getItemStack((short) ItemArmorSlot.Legs.ordinal()))  : "";
        String mainHandItem = getItemId(mainHandStack);
        String offHandItem  = getItemId(offHandStack);

        // gear tab affix + implicit html
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
        String statsHTML        = buildStatsHTML(playerStats, playerLevel, gearScore, usingShield, mainHandStack, offHandStack);

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
                                              style="anchor-width: 160; anchor-height: 160;"></span>
                                        <div style="layout-mode: top; anchor-width: 230; margin-left: 10;">
                                            ${HEAD_AFFIXES}
                                        </div>
                                    </div>
                                    <div style="layout-mode: left; anchor-width: 425; padding: 10;">
                                        <span class="item-slot" data-hyui-item-id="${CHEST_ITEM}"
                                              data-hyui-show-quality-background="true"
                                              data-hyui-show-quantity="false"
                                              style="anchor-width: 160; anchor-height: 160;"></span>
                                        <div style="layout-mode: top; anchor-width: 230; margin-left: 10;">
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
                                              style="anchor-width: 160; anchor-height: 160;"></span>
                                        <div style="layout-mode: top; anchor-width: 230; margin-left: 10;">
                                            ${HANDS_AFFIXES}
                                        </div>
                                    </div>
                                    <div style="layout-mode: left; anchor-width: 425; padding: 10;">
                                        <span class="item-slot" data-hyui-item-id="${LEGS_ITEM}"
                                              data-hyui-show-quality-background="true"
                                              data-hyui-show-quantity="false"
                                              style="anchor-width: 160; anchor-height: 160;"></span>
                                        <div style="layout-mode: top; anchor-width: 230; margin-left: 10;">
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
                                              style="anchor-width: 160; anchor-height: 160;"></span>
                                        <div style="layout-mode: top; anchor-width: 230; margin-left: 10;">
                                            ${MAINHAND_AFFIXES}
                                        </div>
                                    </div>
                                    <div style="layout-mode: left; anchor-width: 425; padding: 10;">
                                        <span class="item-slot" data-hyui-item-id="${OFFHAND_ITEM}"
                                              data-hyui-show-quality-background="true"
                                              data-hyui-show-quantity="true"
                                              style="anchor-width: 160; anchor-height: 160;"></span>
                                        <div style="layout-mode: top; anchor-width: 230; margin-left: 10;">
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

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        PageBuilder.pageForPlayer(playerRef)
                .fromHtml(html)
                .addEventListener("closeBtn", CustomUIEventBindingType.Activating, (ctx) -> {
                    player.getPageManager().setPage(ref, store, Page.None);
                })
                .open(store);
    }

    // returns the hex color for a damage type
    private static String getDamageTypeColor(@Nonnull StatType stat) {
        if (stat == StatType.MAIN_HAND_FIRE_DAMAGE_FLAT || stat == StatType.OFF_HAND_FIRE_DAMAGE_FLAT) return DAMAGE_COLORS.get("Fire");
        if (stat == StatType.MAIN_HAND_LIGHTNING_DAMAGE_FLAT || stat == StatType.OFF_HAND_LIGHTNING_DAMAGE_FLAT) return DAMAGE_COLORS.get("Lightning");
        if (stat == StatType.MAIN_HAND_ICE_DAMAGE_FLAT || stat == StatType.OFF_HAND_ICE_DAMAGE_FLAT) return DAMAGE_COLORS.get("Ice");
        if (stat == StatType.MAIN_HAND_POISON_DAMAGE_FLAT || stat == StatType.OFF_HAND_POISON_DAMAGE_FLAT) return DAMAGE_COLORS.get("Poison");
        if (stat == StatType.MAIN_HAND_MAGIC_DAMAGE_FLAT || stat == StatType.OFF_HAND_MAGIC_DAMAGE_FLAT) return DAMAGE_COLORS.get("Magic");
        return DAMAGE_COLORS.get("Physical");
    }

    // -------------------------------------------------------------------------
    // Stats tab
    // -------------------------------------------------------------------------
    private static String buildStatsHTML(EntityStats s, int playerLevel, int gearScore, boolean usingShield, @Nullable ItemStack mainHandStack, @Nullable ItemStack offHandStack) {
        StringBuilder sb = new StringBuilder();

        sb.append("<div style=\"layout-mode: left; anchor-width: 700;\">");

        // left column
        sb.append("<div style=\"layout-mode: top; anchor-width: 340;\">");

        // overview
        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Overview");
        addStat(sb, "Player Level", String.valueOf(playerLevel));
        addStat(sb, "Gear Score",   String.valueOf(gearScore));
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        // weapon damage section — only show non-zero damage stats from main/off hand
        String weaponDamageHTML = buildWeaponDamageHTML(mainHandStack, offHandStack);
        if (!weaponDamageHTML.isEmpty()) {
            sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
            addSectionHeader(sb, "Weapon Damage");
            sb.append(weaponDamageHTML);
            sb.append("<div style=\"margin-bottom: 5;\"></div>");
            sb.append("</div>");
        }

        // offense
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
        addStat(sb, "Axe Damage",        fmt(s.getIncreasedDamage("Axe")) + "%");
        addStat(sb, "Battleaxe Damage",  fmt(s.getIncreasedDamage("Battleaxe")) + "%");
        addStat(sb, "Club Damage",       fmt(s.getIncreasedDamage("Club")) + "%");
        addStat(sb, "Daggers Damage",    fmt(s.getIncreasedDamage("Daggers")) + "%");
        addStat(sb, "Kunai Damage",      fmt(s.getIncreasedDamage("Kunai")) + "%");
        addStat(sb, "Longsword Damage",  fmt(s.getIncreasedDamage("Longsword")) + "%");
        addStat(sb, "Mace Damage",       fmt(s.getIncreasedDamage("Mace")) + "%");
        addStat(sb, "Shortbow Damage",   fmt(s.getIncreasedDamage("Shortbow")) + "%");
        addStat(sb, "Crossbow Damage",   fmt(s.getIncreasedDamage("Crossbow")) + "%");
        addStat(sb, "Sword Damage",      fmt(s.getIncreasedDamage("Sword")) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        // defense
        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Defense");
        addStat(sb, "Dodge Chance",     fmt(s.getDodgeChance()) + "%");
        addStat(sb, "Stability",        fmt(s.getStabilityPercent(usingShield)) + "%");
        addStat(sb, "Parry Window",     "+" + fmt(s.getParryWindow()) + "(s)");
        addStat(sb, "Barrier on Block", fmt(s.getBarrierOnBlock()) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        sb.append("</div>"); // end left column

        // right column
        sb.append("<div style=\"layout-mode: top; anchor-width: 340;margin-left: 15;\">");

        // resources
        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Resources");
        addStat(sb, "Life",    "+" + fmt(s.getFlatResource("Life")));
        addStat(sb, "Life",    "+" + fmt(s.getIncreasedResource("Life")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Stamina", "+" + fmt(s.getFlatResource("Stamina")));
        addStat(sb, "Stamina", "+" + fmt(s.getIncreasedResource("Stamina")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Mana",    "+" + fmt(s.getFlatResource("Mana")));
        addStat(sb, "Mana",    "+" + fmt(s.getIncreasedResource("Mana")) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        // regeneration
        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Regeneration");
        addStat(sb, "Life Regen",    "+" + fmt(s.getFlatResourceRegen("Life")) + "s");
        addStat(sb, "Life Regen",    "+" + fmt(s.getIncreasedResourceRegen("Life")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Stamina Regen", "+" + fmt(s.getFlatResourceRegen("Stamina")) + "s");
        addStat(sb, "Stamina Regen", "+" + fmt(s.getIncreasedResourceRegen("Stamina")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Mana Regen",    "+" + fmt(s.getFlatResourceRegen("Mana")) + "s");
        addStat(sb, "Mana Regen",    "+" + fmt(s.getIncreasedResourceRegen("Mana")) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        // resistances
        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Resistances");
        addStat(sb, "Physical Resist",  fmt(s.getResistance("Physical")) + "%");
        addStat(sb, "Magic Resist",     fmt(s.getResistance("Magic")) + "%");
        addStat(sb, "Elemental Resist", fmt(s.getResistance("Elemental")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Fire Resist",      fmt(s.getResistance("Fire")) + "%");
        addStat(sb, "Ice Resist",       fmt(s.getResistance("Ice")) + "%");
        addStat(sb, "Lightning Resist", fmt(s.getResistance("Lightning")) + "%");
        addStat(sb, "Poison Resist",    fmt(s.getResistance("Poison")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Fall Resist",      fmt(s.getResistance("Fall")) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        // Advanced
        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Advanced");
        addStat(sb, "Life Leech",              fmt(s.getLeech("Life")) + "%");
        addStat(sb, "Mana Leech",              fmt(s.getLeech("Mana")) + "%");
        addStat(sb, "Stamina Leech",           fmt(s.getLeech("Stamina")) + "%");
        sb.append("<div style=\"margin-bottom: 10;\"></div>");
        addStat(sb, "Damage from Mana",        fmt(s.getDamageTakenFrom("Mana")) + "%");
        addStat(sb, "Damage from Stamina",     fmt(s.getDamageTakenFrom("Stamina")) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        // utility
        sb.append("<div style=\"layout-mode: top; margin-top: 10;margin-bottom: 20;background-color: #111a24;margin-left: 15;anchor-width: 310;\">");
        addSectionHeader(sb, "Utility");
        addStat(sb, "Run Speed",  "+" + fmt(s.getRunSpeedPercent()) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        addStat(sb, "Ammo",       "+" + fmt(s.getAddedAmmo()));
        addStat(sb, "Ammo Regen", "+" + fmt(s.getAmmoRegenPercent()) + "%");
        sb.append("<div style=\"margin-bottom: 5;\"></div>");
        sb.append("</div>");

        sb.append("</div>"); // end right column
        sb.append("</div>"); // end two-column row

        return sb.toString();
    }

    // builds weapon damage rows for main and off hand from implicit metadata — only non-zero stats shown
    private static String buildWeaponDamageHTML(@Nullable ItemStack mainHandStack, @Nullable ItemStack offHandStack) {
        StringBuilder sb = new StringBuilder();

        appendWeaponDamageRows(sb, mainHandStack, "Main Hand");
        appendWeaponDamageRows(sb, offHandStack, "Off Hand");

        return sb.toString();
    }

    // reads implicits from a stack and appends any weapon damage flat stats as colored stat rows
    private static void appendWeaponDamageRows(StringBuilder sb, @Nullable ItemStack stack, String label) {
        if (ItemStack.isEmpty(stack)) return;
        String[] implicits = stack.getFromMetadataOrNull("implicits", Codec.STRING_ARRAY);
        if (implicits == null) return;

        for (String str : implicits) {
            String[] parts = str.split("\\|");
            if (parts.length < 3) continue;
            StatType stat;
            try { stat = StatType.valueOf(parts[0]); } catch (Exception e) { continue; }
            if (!StatTypeInfo.isWeaponDamageStat(stat)) continue;
            float value = Float.parseFloat(parts[1]);
            if (value == 0f) continue;
            String color = getDamageTypeColor(stat);
            String damageType = getDamageTypeName(stat);
            sb.append("<div style=\"layout-mode: left; anchor-width: 300; margin-left: 10; margin-bottom: 2;\">")
                    .append("<p style=\"anchor-width: 200; color: #ffffff;\">").append(label).append("</p>")
                    .append("<p style=\"anchor-width: 90; text-align: right; color: ").append(color).append(";\"><span data-hyui-bold=\"true\">").append(fmt(value)).append("</span> (").append(damageType).append(")</p>")
                    .append("</div>");
        }
    }

    // returns the display name of the damage type for a weapon damage stat
    private static String getDamageTypeName(@Nonnull StatType stat) {
        if (stat == StatType.MAIN_HAND_FIRE_DAMAGE_FLAT      || stat == StatType.OFF_HAND_FIRE_DAMAGE_FLAT)      return "Fire";
        if (stat == StatType.MAIN_HAND_LIGHTNING_DAMAGE_FLAT || stat == StatType.OFF_HAND_LIGHTNING_DAMAGE_FLAT) return "Lightning";
        if (stat == StatType.MAIN_HAND_ICE_DAMAGE_FLAT       || stat == StatType.OFF_HAND_ICE_DAMAGE_FLAT)       return "Ice";
        if (stat == StatType.MAIN_HAND_POISON_DAMAGE_FLAT    || stat == StatType.OFF_HAND_POISON_DAMAGE_FLAT)    return "Poison";
        if (stat == StatType.MAIN_HAND_MAGIC_DAMAGE_FLAT     || stat == StatType.OFF_HAND_MAGIC_DAMAGE_FLAT)     return "Magic";
        return "Physical";
    }

    private static void addSectionHeader(StringBuilder sb, String title) {
        sb.append("<p style=\"font-size: 20;margin-left: 5;margin-top: 3;color: #888888;\">")
                .append("<span data-hyui-bold=\"true\">").append(title).append("</span></p>");
    }

    private static void addStat(StringBuilder sb, String label, String value) {
        sb.append("<div style=\"layout-mode: left; anchor-width: 300; margin-left: 10; margin-bottom: 2;\">")
                .append("<p style=\"anchor-width: 200;\">").append(label).append("</p>")
                .append("<p style=\"anchor-width: 90; text-align: right;\">").append(value).append("</p>")
                .append("</div>");
    }

    private static String fmt(float value) {
        if (value == (int) value) return String.valueOf((int) value);
        if (value < 0.01f && value > 0f) return String.format("%.4f", value);
        return String.format("%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
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
        for (var entry : values.entrySet()) result = result.replace("${" + entry.getKey() + "}", entry.getValue());
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

        // weapon damage line — first implicit from slot 0 that is a weapon damage stat
        String weaponDamageLine = "";
        String weaponDamageColor = "#ffffff";
        List<String> remainingImplicits = new ArrayList<>();

        for (String str : implicits) {
            String[] parts = str.split("\\|");
            if (parts.length < 3) continue;
            StatType stat;
            try { stat = StatType.valueOf(parts[0]); } catch (Exception e) { remainingImplicits.add(str); continue; }

            if (weaponDamageLine.isEmpty() && StatTypeInfo.isWeaponDamageStat(stat)) {
                weaponDamageColor = getDamageTypeColor(stat);
                weaponDamageLine = parts[2];
            } else {
                remainingImplicits.add(str);
            }
        }

        // weapon damage header + value
        if (!weaponDamageLine.isEmpty()) {
            html.append("<p style=\"font-size: 9; color: #888888;\">Weapon Damage</p>");
            html.append("<p style=\"font-size: 13; color: ").append(weaponDamageColor).append("; font-weight: bold;\">").append(weaponDamageLine).append("</p>");
            html.append("<div style=\"anchor-width: 200; anchor-height: 1; margin-top: 3; margin-bottom: 3; background-color: #333333;\"></div>");
        }

        // remaining implicits
        for (String str : remainingImplicits) {
            String[] parts = str.split("\\|");
            if (parts.length < 3) continue;
            html.append("<p style=\"font-size: 11; color: #c8a84b;\">").append(parts[2]).append("</p>");
        }

        // separator between implicits and affixes
        if (!remainingImplicits.isEmpty() && !affixes.isEmpty()) {
            html.append("<div style=\"anchor-width: 200; anchor-height: 1; margin-top: 3; margin-bottom: 3; background-color: #444444;\"></div>");
        }

        // affixes
        for (String str : affixes) {
            String[] parts = str.split("\\|");
            if (parts.length < 3) continue;
            Affix affix = AffixPool.getAffixByStatName(parts[0]);
            if (affix == null) continue;
            float value = Float.parseFloat(parts[1]);
            int tier = (int) Float.parseFloat(parts[2]);
            html.append("<p style=\"font-size: 11;\">T").append(tier)
                    .append(" ").append(affix.display().formatted(Math.round(value * 10) / 10f))
                    .append("</p>");
        }

        return html.toString();
    }
}