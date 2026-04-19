package com.example.hyarpg.ui;

// Hytale imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// HyUI imports
import au.ellie.hyui.builders.PageBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public class Page_HowToPlay {

    // -------------------------------------------------------------------------
    // Page dimensions — match Page_SkillTree's sizing
    // -------------------------------------------------------------------------

    private static final int CONTAINER_WIDTH  = 1100;
    private static final int CONTAINER_HEIGHT = 700;

    private static final int INNER_WIDTH  = CONTAINER_WIDTH  - 30;
    private static final int INNER_HEIGHT = CONTAINER_HEIGHT - 50;

    private static final int COL_HEIGHT   = INNER_HEIGHT - 20;

    // Left ToC column
    private static final int NAV_WIDTH    = 220;
    private static final int NAV_SCROLL_H = COL_HEIGHT - 28;

    // Right content column
    private static final int CONTENT_WIDTH       = INNER_WIDTH - NAV_WIDTH - 10;
    private static final int CONTENT_SCROLL_H    = COL_HEIGHT - 28;
    // Inner wrapper: column minus left margin (12), scrollbar gutter (~16), and safety buffer (40)
    private static final int CONTENT_INNER_WIDTH = CONTENT_WIDTH - 68;

    // -------------------------------------------------------------------------
    // Section registry
    // -------------------------------------------------------------------------

    private static final LinkedHashMap<String, String> SECTIONS = new LinkedHashMap<>();

    static {
        SECTIONS.put("getting_started", "Getting Started");
        SECTIONS.put("survival",        "Survival");
        SECTIONS.put("progression",     "Levels & Skill Trees");
        SECTIONS.put("gear",            "Gear & Loot");
        SECTIONS.put("combat",          "Combat");
        SECTIONS.put("base_building",   "Base Building & Rooms");
        SECTIONS.put("raids",           "Raids");
        SECTIONS.put("commands",        "Commands");
        SECTIONS.put("configuration",   "Configuration");
    }

    // -------------------------------------------------------------------------
    // Entry points
    // -------------------------------------------------------------------------

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store) {
        open(ref, store, "getting_started");
    }

    public static void open(Ref<EntityStore> ref, Store<EntityStore> store, String selectedSectionId) {
        Player    player    = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        String activeSectionId = (selectedSectionId != null && SECTIONS.containsKey(selectedSectionId))
                ? selectedSectionId : SECTIONS.keySet().iterator().next();

        // --- Left ToC nav ---
        StringBuilder navHtml = new StringBuilder();
        for (Map.Entry<String, String> e : SECTIONS.entrySet()) {
            boolean isActive = e.getKey().equals(activeSectionId);
            String  btnId    = "toc_btn_" + e.getKey();
            String  activeBg = isActive ? "background-color: #1e3a5f;" : "";

            navHtml.append("<button id=\"").append(btnId).append("\"")
                    .append(" style=\"anchor-width: ").append(NAV_WIDTH - 16).append(";")
                    .append(" anchor-height: 36;")
                    .append(" margin-bottom: 4;")
                    .append(activeBg).append("\">")
                    .append(e.getValue())
                    .append("</button>");
        }

        String sectionTitle   = SECTIONS.get(activeSectionId);
        String sectionContent = buildSectionContent(activeSectionId);

        String html = """
        <div class="page-overlay">
            <button id="closeBtn"
                    style="anchor-bottom: 10; anchor-width: ${CONTAINER_WIDTH}; anchor-height: 40;">
                Close
            </button>

            <div class="container"
                 data-hyui-title="How To Play"
                 style="anchor-width: ${CONTAINER_WIDTH}; anchor-height: ${CONTAINER_HEIGHT};">

                <div class="container-contents">
                    <div style="layout-mode: left; anchor-width: ${INNER_WIDTH}; anchor-height: ${INNER_HEIGHT};">

                        <!-- ===== LEFT TOC ===== -->
                        <div style="layout-mode: top;
                                    anchor-width: ${NAV_WIDTH};
                                    anchor-height: ${COL_HEIGHT};
                                    background-color: #0d141c;">

                            <p style="margin-left: 8; margin-top: 8; margin-bottom: 4;">
                                <span data-hyui-bold="true">Contents</span>
                            </p>

                            <div style="layout-mode: topscrolling;
                                        anchor-width: ${NAV_WIDTH};
                                        anchor-height: ${NAV_SCROLL_H};"
                                 data-hyui-keep-scroll-position="true"
                                 data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                                <div style="layout-mode: top; margin-left: 8; margin-top: 4;">
                                    ${NAV_HTML}
                                </div>
                            </div>
                        </div>

                        <!-- ===== RIGHT CONTENT ===== -->
                        <div style="layout-mode: top;
                                    anchor-width: ${CONTENT_WIDTH};
                                    anchor-height: ${COL_HEIGHT};
                                    margin-left: 10;">

                            <p style="margin-left: 8; margin-top: 8; margin-bottom: 4;">
                                <span data-hyui-bold="true">${SECTION_TITLE}</span>
                            </p>

                            <div style="layout-mode: topscrolling;
                                        anchor-width: ${CONTENT_WIDTH};
                                        anchor-height: ${CONTENT_SCROLL_H};"
                                 data-hyui-keep-scroll-position="true"
                                 data-hyui-scrollbar-style="&quot;Common.ui&quot; &quot;DefaultScrollbarStyle&quot;">
                                <div style="layout-mode: top; anchor-width: ${CONTENT_INNER_WIDTH}; margin-left: 12; margin-top: 8;">
                                    ${SECTION_CONTENT}
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </div>
        """;

        html = html
                .replace("${CONTAINER_WIDTH}",    String.valueOf(CONTAINER_WIDTH))
                .replace("${CONTAINER_HEIGHT}",   String.valueOf(CONTAINER_HEIGHT))
                .replace("${INNER_WIDTH}",         String.valueOf(INNER_WIDTH))
                .replace("${INNER_HEIGHT}",        String.valueOf(INNER_HEIGHT))
                .replace("${COL_HEIGHT}",          String.valueOf(COL_HEIGHT))
                .replace("${NAV_WIDTH}",           String.valueOf(NAV_WIDTH))
                .replace("${NAV_SCROLL_H}",        String.valueOf(NAV_SCROLL_H))
                .replace("${CONTENT_WIDTH}",       String.valueOf(CONTENT_WIDTH))
                .replace("${CONTENT_SCROLL_H}",    String.valueOf(CONTENT_SCROLL_H))
                .replace("${CONTENT_INNER_WIDTH}", String.valueOf(CONTENT_INNER_WIDTH))
                .replace("${NAV_HTML}",            navHtml.toString())
                .replace("${SECTION_TITLE}",       sectionTitle)
                .replace("${SECTION_CONTENT}",     sectionContent);

        PageBuilder builder = PageBuilder.pageForPlayer(playerRef).fromHtml(html);

        builder.addEventListener("closeBtn", CustomUIEventBindingType.Activating, (ctx) -> {
            player.getPageManager().setPage(ref, store, Page.None);
        });

        for (String sectionId : SECTIONS.keySet()) {
            String btnId = "toc_btn_" + sectionId;
            builder.addEventListener(btnId, CustomUIEventBindingType.Activating, (ctx) -> {
                player.getWorld().execute(() -> Page_HowToPlay.open(ref, store, sectionId));
            });
        }

        builder.open(store);
    }

    // -------------------------------------------------------------------------
    // Section dispatcher
    // -------------------------------------------------------------------------

    private static String buildSectionContent(String sectionId) {
        return switch (sectionId) {
            case "getting_started" -> buildGettingStarted();
            case "survival"        -> buildSurvival();
            case "progression"     -> buildProgression();
            case "gear"            -> buildGear();
            case "combat"          -> buildCombat();
            case "base_building"   -> buildBaseBuilding();
            case "raids"           -> buildRaids();
            case "commands"        -> buildCommands();
            case "configuration"   -> buildConfiguration();
            default                -> p("Section not found.");
        };
    }

    // =========================================================================
    // HTML primitive helpers
    //
    // Every helper returns one or more COMPLETE <p> or <div> elements.
    // Nothing is concatenated as raw text into another element.
    // =========================================================================

    /** A plain body paragraph. */
    private static String p(String content) {
        return "<p style=\"margin-bottom: 8; white-space: wrap;\">" + content + "</p>";
    }

    /** A gold section heading. */
    private static String heading(String text) {
        return "<p style=\"margin-top: 10; margin-bottom: 6; white-space: wrap;\">"
                + "<span data-hyui-bold=\"true\" data-hyui-color=\"#c8a84b\">" + text + "</span>"
                + "</p>";
    }

    /** A 1px coloured horizontal rule. */
    private static String divider() {
        return "<div style=\"anchor-height: 1; background-color: #2a3a4a; margin-top: 4; margin-bottom: 12;\"></div>";
    }

    /** A bullet point as its own standalone <p>. Never concatenate — one call per bullet. */
    private static String bullet(String text) {
        return "<p style=\"margin-bottom: 4; margin-left: 4; white-space: wrap;\">"
                + "<span data-hyui-color=\"#aaaaaa\">- </span>"
                + text
                + "</p>";
    }

    /**
     * A command entry shown in the orange player-command style.
     * Name on its own <p>, description indented below it on the next <p>.
     */
    private static String cmd(String nameColor, String name, String description) {
        return "<p style=\"margin-top: 6; margin-bottom: 2; white-space: wrap;\">"
                + "<span data-hyui-bold=\"true\" data-hyui-color=\"" + nameColor + "\">" + name + "</span>"
                + "</p>"
                + "<p style=\"margin-bottom: 8; margin-left: 12; white-space: wrap;\">"
                + "<span data-hyui-color=\"#aaaaaa\">" + description + "</span>"
                + "</p>";
    }

    /** Inline bold span — use only inside p() or bullet() text arguments. */
    private static String bold(String text) {
        return "<span data-hyui-bold=\"true\">" + text + "</span>";
    }

    /** Inline coloured span — use only inside p() or bullet() text arguments. */
    private static String col(String color, String text) {
        return "<span data-hyui-color=\"" + color + "\">" + text + "</span>";
    }

    /** Inline plain white text span — use inside bullet() or p() to ensure colour is explicit. */
    private static String txt(String text) {
        return "<span data-hyui-color=\"#ffffff\">" + text + "</span>";
    }

    // =========================================================================
    // Section content builders
    // =========================================================================

    private static String buildGettingStarted() {
        return heading("Welcome to the Survival ARPG Overhaul")
                + p("This mod transforms Hytale into a full-scale Action RPG with new progression, combat, and building systems layered on top of the vanilla experience. Starting a fresh world is recommended for the best experience.")

                + divider()

                + heading("Your HUD")
                + bullet(bold("Hunger & Thirst") + txt(" — Bars that drain over time. Eat food and drink water to maintain them."))
                + bullet(bold("Health / Mana / Stamina") + txt(" — Core combat resources. See the Combat section for details on how each works."))
                + bullet(bold("Ammo") + txt(" — Appears on your HUD when you have a weapon equipped that uses it."))
                + bullet(bold("Signature Energy") + txt(" — Builds over time. Used to cast your equipped Ultimate ability."))
                + bullet(bold("Player Level & XP") + txt(" — Your current level and progress toward the next. Kill enemies to earn XP."))
                + bullet(bold("Gear Score") + txt(" — The average score of your equipped gear. Scales all damage dealt and received."))
                + bullet(bold("Ability Slots") + txt(" — E (Primary), R (Secondary), Q (Ultimate). Unlocked and assigned from your skill trees."))

                + divider()

                + heading("Things To Do")
                + p("There is no strict order — explore at your own pace. Some good early goals:")
                + bullet(col("#ffffff", "Pick up ingredients and materials to discover gear recipes."))
                + bullet(col("#ffffff", "Gather berries and fruit early — they restore both hunger and thirst."))
                + bullet(col("#ffffff", "Field-craft starter weapons from your discovered Crude weapon recipes."))
                + bullet(col("#ffffff", "Kill enemies to level up and earn better gear drops."))
                + bullet("Invest skill points into a skill tree using " + col("#f0c060", "/skills") + txt("."))
                + bullet(col("#ffffff", "Place a Light Well to claim territory so you can start placing benches and building a base — but make sure you are ready to defend it."))
                + divider();
    }

    private static String buildSurvival() {
        return heading("Hunger")
                + p("Hunger drains passively over time. It is restored by eating vanilla foods, which carry T1-T3 hunger restore buffs. Most cooked and crafted foods will restore hunger.")
                + divider()

                + heading("Thirst")
                + p("Thirst also drains passively over time. Fruit such as berries and apples are great early resources — they restore both thirst and hunger at the same time.")
                + divider()

                + heading("Water")
                + p("You cannot drink water directly from the world. You need a " + bold("Water Bottle") + " to collect dirty water, which must then be cooked at a " + bold("Cooking Station") + " to clean it before it can be consumed.")
                + divider();
    }

    private static String buildProgression() {
        return heading("Levelling Up")
                + p("Defeat enemies to earn XP. Your level and XP progress are always visible on your HUD. Use " + col("#f0c060", "/stats") + " to see your current level and full stats at any time.")
                + p("XP scales based on the enemy's level compared to yours. Enemies more than 10 levels below you give no XP. Enemies at your level give standard XP, and enemies up to 10 levels above you give progressively more — up to triple XP. This encourages pushing further into the world to fight tougher enemies and keep levelling efficiently.")
                + divider()

                + heading("Skill Trees")
                + p("Open your skill trees with " + col("#f0c060", "/skills") + ". Skill points can be spent in any tree — you are not locked into a single class and are encouraged to mix and match across trees to build a playstyle that suits you.")
                + p("Each tree contains stat nodes and active abilities. Some skill trees may not appear until certain requirements are met, so keep exploring and levelling to discover what is available.")
                + divider()

                + heading("Abilities")
                + p("Abilities can be unlocked and equipped directly from their respective skill trees. Once an ability is available to equip, assign it to one of your ability slots:")
                + bullet(bold("E") + txt(" — Primary Ability slot."))
                + bullet(bold("R") + txt(" — Secondary Ability slot."))
                + bullet(bold("Q") + txt(" — Ultimate Ability slot. Requires Signature Energy to cast."))
                + p("Some abilities require specific weapon types to use. Requirements are shown in the skill tree tooltip. If the wrong weapon is equipped when you use an ability, you will receive a chat notification.")
                + p("Assigned abilities may also have cooldowns that appear on the skill icon as a countdown timer after use.")
                + divider();
    }

    private static String buildGear() {
        return heading("Item Rarities")
                + p("All weapons and armour use a five-tier rarity system:")
                + p(col("#ffffff", "Common") + "  ->  " + col("#55ff55", "Uncommon") + "  ->  " + col("#5588ff", "Rare") + "  ->  " + col("#cc44cc", "Epic") + "  ->  " + col("#ffaa00", "Legendary"))
                + p("Higher rarity items roll more affixes (1-4) and have better base stats.")
                + divider()

                + heading("Item Discovery")
                + p("Weapons and armour must be discovered before they can be crafted or received as kill drops. Chest drops in the world are not restricted — they use a distance-based tier system that scales with how far you have explored, matching the ore distance configuration.")
                + bullet(bold("Common") + txt(" variants — unlocked by collecting the required crafting ingredients."))
                + bullet(bold("Uncommon and above") + txt(" — unlocked by finding recipe drops in the world."))
                + bullet("View all your discovered recipes at any time with " + col("#f0c060", "/discovered") + txt("."))
                + divider()

                + heading("Affixes & Implicits")
                + p("Every non-common item rolls bonus modifiers called " + bold("affixes") + ". The number of affixes scales with rarity (1-4). Affixes range from T0 (extremely rare) to T5 (most common).")
                + p("Items also have " + bold("implicit modifiers") + " — built-in stats tied to the item base itself, separate from affixes. Affixes and implicits do not appear on item tooltips. View them all on your " + col("#f0c060", "/stats") + " page.")
                + divider()

                + heading("Gear Score")
                + p("Your Gear Score is the " + bold("average score of all equipped items") + ". It directly scales every damage calculation — both outgoing and incoming.")
                + bullet("Crafted gear matches " + bold("your player level") + txt("."))
                + bullet("Dropped gear matches the " + bold("enemy's level") + txt("."))
                + bullet("Always keep your Gear Score current as you level. Use " + col("#f0c060", "/stats") + txt(" to check it."))
                + divider()

                + heading("Salvaging")
                + p("Break down unwanted gear at the " + bold("Salvage Bench") + " to reclaim materials. All gear salvages for some of its primary material back:")
                + bullet(bold("Common gear") + txt(" — Primary material and a Blue Magic Shard."))
                + bullet(bold("Uncommon and above") + txt(" — Primary material and Rarity Shards."))
                + divider()

                + heading("Ore Progression")
                + p("Higher ore tiers start to appear the further you explore into the world. Ore types wax and wane, peaking at their mid-point range before fading. The ranges below are default values and are configurable.")
                + bullet(bold("Copper") + txt(" — Close to spawn (0-20k blocks, peaks ~10k)."))
                + bullet(bold("Iron") + txt(" — Mid range (10-30k blocks, peaks ~20k)."))
                + bullet(bold("Thorium") + txt(" — Further out (20-40k blocks, peaks ~30k)."))
                + bullet(bold("Cobalt") + txt(" — Deep exploration (30-50k blocks, peaks ~40k)."))
                + bullet(bold("Adamantite") + txt(" — The furthest tier (40-60k blocks, peaks ~50k)."))
                + bullet(bold("Mithril") + txt(" — The furthest tier (50-70k blocks, peaks ~60k)."))
                + divider();
    }

    private static String buildCombat() {
        return heading("Damage")
                + p("All damage runs through a custom ARPG calculation pipeline. Your Gear Score is the primary driver — keep it up to date.")
                + p("Abilities that scale off " + bold("Weapon") + " damage type use your " + bold("main hand") + " weapon's damage as their base. Off-hand weapon damage does not contribute to ability scaling.")
                + divider()

                + heading("Blocking")
                + p("Most weapons support blocking, not just shields. When a hit comes in and is handled by the block pipeline (block is checked before dodge), the damage is processed in this order:")
                + bullet("Incoming damage is reduced by your " + bold("Stability") + txt(" percent, up to its cap."))
                + bullet("The remaining damage is subtracted from your " + bold("Stamina") + txt("."))
                + bullet(col("#ffffff", "Whatever is left then continues through the rest of the pipeline, including dodge, before being applied as health damage."))
                + divider()

                + heading("Parrying")
                + p("Time your block precisely to trigger a parry. A successful parry checks the full incoming damage against your " + bold("current Stamina") + txt(" — you cannot parry a hit that would exceed your current stamina pool, so stacking stamina helps. A failed parry is treated as a regular blocked hit."))
                + bullet(bold("Stability") + txt(" — Also improves parry effectiveness."))
                + bullet(bold("Parry Window") + txt(" — An affix that widens the timing window for a successful parry."))
                + divider()

                + heading("Resources")
                + p(bold("Health") + " has no natural passive regeneration. You must acquire health regen through skill tree nodes or gear affixes.")
                + p(bold("Stamina") + " regenerates passively. It is consumed by blocking and by some abilities.")
                + p(bold("Mana") + " regenerates passively. It is used by abilities and may have additional uses in the future.")
                + p(bold("Ammo") + " recharges automatically and is no longer a consumed item. The ammo HUD element appears when you have a weapon equipped that uses it.")
                + p(bold("Signature Energy") + " builds over time and is persisted at the player level. It will build and display in the normal vanilla location. You can only cast a Ultimate ability if one is equipped from the skill tree.")
                + divider()

                + heading("Enemy Rarities")
                + p("Enemies spawn with different rarity tiers that affect their power and the affixes they carry:")
                + bullet(bold("Common") + txt(" — 1 affix."))
                + bullet(bold("Magical") + txt(" — 2 affixes."))
                + bullet(bold("Rare") + txt(" — 3 affixes."))
                + bullet(bold("Elite") + txt(" — 4 affixes."))
                + p("Higher rarity enemies deal more damage, take less damage, and grant an XP bonus on kill. Loot drop rates are not affected by enemy rarity.")
                + divider()

                + heading("Kill Participation & Loot")
                + p("Any player who hits an enemy within the last " + bold("30 seconds") + " before it dies is considered a kill participant and is eligible for XP and loot drops.")
                + p("Each player only sees their own damage notifications. Loot drop notifications are broadcast to all players who participated in the kill and were eligible for drops.")
                + divider();
    }

    private static String buildBaseBuilding() {
        return heading("Light Wells")
                + p("The " + bold("Light Well") + " is your base anchor. Placing one claims the surrounding territory as yours.")
                + bullet(col("#ffffff", "Benches and beds can only be placed inside an active Light Well territory."))
                + bullet(col("#ffffff", "Each player can only have one Light Well. You cannot place one that would overlap another player's territory."))
                + bullet(col("#ffffff", "Placing a Light Well makes your base a potential raid target."))
                + bullet("Use " + col("#f0c060", "/home") + txt(" to teleport back to your Light Well from anywhere in the world."))
                + divider()

                + heading("The Room System")
                + p("The Room System rewards creative building. Build rooms inside your territory and experiment with their " + bold("size") + ", " + bold("decorations") + ", " + bold("benches") + ", and " + bold("contents") + " to discover hidden room recipes that unlock new bonuses.")
                + p("There is no recipe book to read upfront — experimentation is the key. View everything you have discovered so far with " + col("#f0c060", "/discovered") + ".")
                + divider();
    }

    private static String buildRaids() {
        return heading("What Are Raids?")
                + p("Once you have an active Light Well, your base can be raided. There is a good variety of curated raid events that can occur, each bringing different enemy types and challenges to your base.")
                + divider()

                + heading("How Raids Work")
                + p("When a raid begins, a " + bold("raid HUD icon") + " appears on your screen showing the current state of the raid. There is a short grace window before the first wave starts spawning, and time between each subsequent wave — all of this is visible on the HUD icon.")
                + p("After the last wave spawns, there is a " + bold("5-minute window") + " to clear all remaining enemies before the raid ends.")
                + p(col("#ff9944", "Important: ") + "Any raid enemies still alive when the raid ends will " + bold("explode") + ", destroying chunks of your base around them.")
                + divider()

                + heading("Your Base When You Are Away")
                + p("Your base remains active and can be raided even if you are offline. It is strongly encouraged to " + col("#f0c060", "/home") + " when a raid begins so you can handle it directly.")
                + p("If your " + bold("Light Well") + " is destroyed during a raid, all benches and beds in your territory will also break and drop in place. This includes any upgrades you have invested into those benches, so protecting your Light Well should be your top priority during a raid.")
                + divider();
    }

    private static String buildCommands() {
        // Derived from command class names and constructors
        return heading("Player Commands")
                + cmd("#f0c060", "/skills",
                "Open your skill trees. Browse, invest skill points, and equip unlocked abilities.")
                + cmd("#f0c060", "/stats",
                "Open your stats page. View your current level, Gear Score, affixes, and implicit modifiers.")
                + cmd("#f0c060", "/discovered",
                "Open your recipe book. Shows all gear and room recipes you have discovered, split into tabs.")
                + cmd("#f0c060", "/home",
                "Teleport to your Light Well. Works from anywhere in the world.")
                + cmd("#f0c060", "/HyARPG_Player_Settings_ShowCombatMessages <true|false>",
                "Toggle whether combat damage messages are shown to you in chat.")
                + cmd("#f0c060", "/HyARPG_Player_Settings_ShowLootDropMessages <true|false>",
                "Toggle whether loot drop messages are shown to you in chat.")
                + divider()

                + heading("Admin Commands")
                + cmd("#ff9944", "/HyARPG_Add_Player_Levels <player> <amount>",
                "Add levels directly to a player by awarding the required XP.")
                + cmd("#ff9944", "/HyARPG_Set_Skill_Points <player> <amount>",
                "Set a player's available skill point total to the given value.")
                + cmd("#ff9944", "/HyARPG_Refund_Skills <player>",
                "Refund all spent skill points for a player, resetting their skill trees.")
                + cmd("#ff9944", "/HyARPG_Reset_Discovered_Ingredient <player>",
                "Reset all discovered crafting ingredients for a player, allowing recipe discoveries to re-fire.")
                + cmd("#ff9944", "/HyARPG_Reset_Discovered_Rooms <player>",
                "Reset all discovered room recipes for a player, allowing room discoveries to re-fire.")
                + cmd("#ff9944", "/HyARPG_Hunger_TickEnabled <true|false>",
                "Enable or disable the hunger system server-wide. Requires a relog for the HUD bar to update.")
                + cmd("#ff9944", "/HyARPG_Thirst_TickEnabled <true|false>",
                "Enable or disable the thirst system server-wide. Requires a relog for the HUD bar to update.")
                + cmd("#ff9944", "/HyARPG_Trigger_Raid <player> <base|player>",
                "Manually trigger a raid on a player's base or the player directly. Used for testing.")
                + cmd("#ff9944", "/HyARPG_Clear_Current_Territory",
                "Clears the territory at the executing admin's current location. Used to unstick broken Light Wells.")
                + divider();
    }

    private static String buildConfiguration() {
        return heading("Configuration File")
                + p("The configuration file is generated automatically on first run and can be found at:")
                + p(col("#f0c060", "Hytale\\UserData\\Saves\\world_name\\mods\\HyARPG"))
                + p("Any missing entries are regenerated on startup, so you can safely delete individual keys to reset them to their defaults. Each entry in the file includes a comment describing what it does and its default value.")
                + divider()

                + heading("What Can Be Configured")
                + bullet(bold("Combat") + txt(" — Damage multipliers for level and rarity differences, parry window timing, global player damage in/out multipliers, and combat log broadcasting."))
                + bullet(bold("Enemies") + txt(" — Level scaling rate per distance, random level variance, and nameplate display options."))
                + bullet(bold("Experience") + txt(" — Base XP to level up, XP scaling per level, and XP gained from kills."))
                + bullet(bold("Hunger & Thirst") + txt(" — Enable/disable each system independently, drain rates, death timers, and food restore values per tier."))
                + bullet(bold("Loot") + txt(" — Drop chance modifiers for gear and recipes, and loot notification broadcasting."))
                + bullet(bold("Players") + txt(" — Base regeneration rates for Health, Mana, Stamina, and Ammo."))
                + bullet(bold("Building") + txt(" — Light Well territory claiming and whether benches and beds can be placed outside of claimed territory."))
                + bullet(bold("Raids") + txt(" — Enable/disable base and player raids, raid chance, cooldowns, wave timing, and explosion damage settings."))
                + bullet(bold("World") + txt(" — Ore spawn distances, vein sizes, vein counts, and Y level ranges per ore tier. Note: ore settings only apply to newly generated chunks."))
                + divider();
    }
}