package com.example.hyarpg.ui;

// Hytale Imports
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Java Imports
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class CustomPage_HowToPlayPage extends InteractiveCustomUIPage<CustomPage_HowToPlayPage.PageData> {

    // total number of content lines defined in the ui file
    private static final int TOTAL_LINES = 40;

    // all section ids in nav order
    private static final String[][] SECTIONS = {
        { "getting_started", "HTPBtnGettingStarted" },
        { "survival",        "HTPBtnSurvival"       },
        { "progression",     "HTPBtnProgression"    },
        { "gear",            "HTPBtnGear"           },
        { "crafting",        "HTPBtnCrafting"       },
        { "salvaging",       "HTPBtnSalvaging"      },
        { "combat",          "HTPBtnCombat"         },
        { "base_building",   "HTPBtnBaseBuilding"   },
        { "raids",           "HTPBtnRaids"          },
        { "commands",        "HTPBtnCommands"       },
        { "configuration",   "HTPBtnConfiguration"  }
    };

    // currently active section
    private String activeSection = "getting_started";

    public CustomPage_HowToPlayPage() {
        super(null, CustomPageLifetime.CanDismiss, PageData.CODEC);
    }

    public CustomPage_HowToPlayPage(@Nonnull com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load the UI file
        cmd.append("HowToPlayPanel.ui");

        // bind close button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));

        // bind section nav buttons
        for (String[] section : SECTIONS) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#" + section[1], EventData.of("Action", "section:" + section[0]));
        }

        // apply initial section content
        applySection(cmd, this.activeSection);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null) { sendUpdate((UICommandBuilder) null, false); return; }

        if (data.action.equals("close")) {
            com.hypixel.hytale.server.core.entity.entities.Player player = store.getComponent(ref, com.hypixel.hytale.server.core.entity.entities.Player.getComponentType());
            if (player != null) player.getPageManager().setPage(ref, store, com.hypixel.hytale.protocol.packets.interface_.Page.None);
        } else if (data.action.startsWith("section:")) {
            String sectionId = data.action.substring("section:".length());
            this.activeSection = sectionId;
            UICommandBuilder cmd = new UICommandBuilder();
            applySection(cmd, sectionId);
            sendUpdate(cmd, false);
        } else {
            sendUpdate((UICommandBuilder) null, false);
        }
    }

    // apply section title and content lines to the UI
    private void applySection(@Nonnull UICommandBuilder cmd, @Nonnull String sectionId) {
        // set section title
        cmd.set("#HTPSectionTitle.Text", getSectionTitle(sectionId));

        // build content lines for this section
        List<Line> lines = buildSectionContent(sectionId);

        // populate lines — set used ones, clear the rest
        for (int i = 0; i < TOTAL_LINES; i++) {
            String element = "#HTPLine" + (i + 1);
            if (i < lines.size()) {
                Line line = lines.get(i);
                cmd.set(element + ".TextSpans", line.message);
                cmd.set(element + ".Visible", true);
            } else {
                cmd.set(element + ".Text", "");
                cmd.set(element + ".Visible", false);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Line builders — each method returns a list of Lines for a section
    // -------------------------------------------------------------------------

    private List<Line> buildSectionContent(@Nonnull String sectionId) {
        return switch (sectionId) {
            case "getting_started" -> buildGettingStarted();
            case "survival"        -> buildSurvival();
            case "progression"     -> buildProgression();
            case "gear"            -> buildGear();
            case "crafting"        -> buildCrafting();
            case "salvaging"       -> buildSalvaging();
            case "combat"          -> buildCombat();
            case "base_building"   -> buildBaseBuilding();
            case "raids"           -> buildRaids();
            case "commands"        -> buildCommands();
            case "configuration"   -> buildConfiguration();
            default                -> List.of(plain("Section not found."));
        };
    }

    private static List<Line> buildGettingStarted() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Welcome to the Survival ARPG Overhaul"));
        lines.add(plain("This mod transforms Hytale into a full-scale Action RPG with new progression, combat, and building systems. Starting a fresh world is recommended."));
        lines.add(spacer());
        lines.add(heading("Your HUD"));
        lines.add(bullet("Hunger & Thirst", "Bars that drain over time. Eat food and drink water to maintain them."));
        lines.add(bullet("Health / Mana / Stamina", "Core combat resources. See the Combat section for details."));
        lines.add(bullet("Ammo", "Appears on your HUD when you have a ranged weapon equipped."));
        lines.add(bullet("Signature Energy", "Builds over time. Used to cast your equipped Ultimate ability."));
        lines.add(bullet("Player Level & XP", "Your current level and progress toward the next. Kill enemies to earn XP."));
        lines.add(bullet("Gear Score", "The average score of your equipped gear. Scales all damage dealt and received."));
        lines.add(bullet("Ability Slots", "E (Primary), R (Secondary), Q (Ultimate). Unlocked from your skill trees."));
        lines.add(spacer());
        lines.add(heading("Things To Do"));
        lines.add(plain("There is no strict order — explore at your own pace. Some good early goals:"));
        lines.add(bullet("Gather ingredients to discover gear component recipes."));
        lines.add(bullet("Gather berries and fruit early — they restore both hunger and thirst."));
        lines.add(bullet("Kill enemies to level up and earn gear drops."));
        lines.add(bullet("Invest skill points into a skill tree using /skills."));
        lines.add(bullet("Place a Light Well to claim territory and start building a base."));
        return lines;
    }

    private static List<Line> buildSurvival() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Hunger"));
        lines.add(plain("Hunger drains passively over time. It is restored by eating vanilla foods, which carry T1-T3 hunger restore buffs."));
        lines.add(spacer());
        lines.add(heading("Thirst"));
        lines.add(plain("Thirst also drains passively over time. Fruit such as berries and apples restore both thirst and hunger at the same time."));
        lines.add(spacer());
        lines.add(heading("Water"));
        lines.add(plain("You cannot drink water directly from the world. You need a Water Bottle to collect dirty water, which must be cooked at a Cooking Station before it can be consumed."));
        return lines;
    }

    private static List<Line> buildProgression() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Levelling Up"));
        lines.add(plain("Defeat enemies to earn XP."));
        lines.add(plain("XP scales based on enemy level vs yours. Enemies 10+ levels below give no XP. Enemies up to 10 levels above give up to triple XP."));
        lines.add(spacer());
        lines.add(heading("Skill Trees"));
        lines.add(plain("Open your skill trees with /skills. You are not locked into a single class — mix and match across trees freely."));
        lines.add(plain("Each tree contains stat nodes and active abilities. Some trees may not appear until certain requirements are met."));
        lines.add(spacer());
        lines.add(heading("Abilities"));
        lines.add(plain("Abilities are unlocked and equipped directly from their skill trees. Assign them to:"));
        lines.add(bullet("E", "Primary Ability slot."));
        lines.add(bullet("R", "Secondary Ability slot."));
        lines.add(bullet("Q", "Ultimate Ability slot. Requires Signature Energy to cast."));
        lines.add(plain("Some abilities require specific weapon types. If the wrong weapon is equipped you will receive a chat notification."));
        return lines;
    }

    private static List<Line> buildGear() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Item Rarities"));
        lines.add(plain("All weapons and armour use a five-tier rarity system: Common -> Uncommon -> Rare -> Epic -> Legendary."));
        lines.add(plain("Higher rarity items roll more affixes (1-4) and have better base stats."));
        lines.add(spacer());
        lines.add(heading("Item Discovery"));
        lines.add(plain("Weapon and armour components must be discovered before they can be crafted. They can also come from salvaging gear found in the wild."));
        lines.add(spacer());
        lines.add(heading("Affixes & Implicits"));
        lines.add(plain("Every non-common item rolls bonus modifiers called affixes. Affixes range from T0 (extremely rare) to T5 (most common)."));
        lines.add(plain("Items will also get implicit modifiers that come from the components used to craft them."));
        lines.add(spacer());
        lines.add(heading("Gear Score"));
        lines.add(plain("Your Gear Score is the average score of all equipped items. It directly scales every damage calculation."));
        lines.add(bullet("Crafted gear", "Matches your player level."));
        lines.add(bullet("Dropped gear", "Matches the killed enemy's level."));
        lines.add(bullet("Found gear", "Matches the enemy level of the area."));
        lines.add(spacer());
        lines.add(heading("Ore Progression"));
        lines.add(plain("Higher ore tiers appear the further you explore. Ranges below are defaults and are configurable."));
        lines.add(bullet("Crude Tier", "0k-2k blocks, starter/basic materials drops, no ore spawns."));
        lines.add(bullet("Copper Tier", "2k-22k blocks, peaks ~12k. Copper gear/materials drop and spawn."));
        lines.add(bullet("Iron Tier", "12k-32k blocks, peaks ~22k. Iron gear/materials drop and spawn."));
        lines.add(bullet("Thorium Tier", "22k-42k blocks, peaks ~32k. Thorium gear/materials drop and spawn."));
        lines.add(bullet("Cobalt Tier", "32k-52k blocks, peaks ~42k. Cobalt gear/materials drop and spawn."));
        lines.add(bullet("Adamantite Tier", "42k-62k blocks, peaks ~52k. Adamantite gear/materials drop and spawn."));
        lines.add(bullet("Mithril Tier", "52k-72k blocks, peaks ~62k. Mithril gear/materials drop and spawn."));
        return lines;
    }

    private static List<Line> buildCrafting() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("The Horradrix Cube"));
        lines.add(plain("An ancient puzzle cube that has magical transmutational properties. You can use this cube to combine weapon and armor components into completed pieces of gear."));
        lines.add(spacer());
        lines.add(heading("Components"));
        lines.add(plain("Components are the building blocks of gear. Each has a type (e.g. Axe Head, Handle, Shaft) and a tier (T0-T5)."));
        lines.add(plain("Components are crafted from various respective benches (weapon components at the weapon bench, armor at the armor bench, etc.) from raw ingredients found in the world. Each component has its own implicit stats that carry through to the finished item."));
        lines.add(spacer());
        lines.add(heading("Rarity"));
        lines.add(plain("Use a Rarity Shard when combining components to influence the rarity of the resulting gear."));
        lines.add(bullet("Common (no shard)", "+0 random stat affix."));
        lines.add(bullet("Uncommon", "+1 random stat affix."));
        lines.add(bullet("Rare", "+2 random stat affixes."));
        lines.add(bullet("Epic", "+3 random stat affixes."));
        lines.add(bullet("Legendary", "+4 random stat affixes."));
        lines.add(spacer());
        lines.add(heading("Output Panel"));
        lines.add(plain("As you slot components the output panel updates to show the weapon type, base damage, implicit stats, and affix count before you commit to crafting."));
        return lines;
    }

    private static List<Line> buildSalvaging() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("The Horradrix Cube"));
        lines.add(plain("An ancient puzzle cube that has magical transmutational properties. You can use it to break down gear into components and break down components into material."));
        lines.add(spacer());
        lines.add(heading("Salvaging Gear"));
        lines.add(plain("Place a weapon or armour piece into the input slot. The output slots show the components and shard that could be returned."));
        lines.add(plain("Salvaging randomly returns one of the three components or the rarity shard (except for with common items)"));
        lines.add(spacer());
        lines.add(heading("Salvaging Components"));
        lines.add(plain("Components can also be salvaged back into raw ingredients. Place a component in the input slot to see what ingredients it could return."));
        lines.add(plain("Salvaging randomly returns one of the up to 4 materials used to craft the component. The amount of material returned is random — you could get anywhere from 1 to the full crafting cost."));
        lines.add(spacer());
        lines.add(heading("Tips"));
        lines.add(bullet("Salvage low-quality gear to fund crafting of higher-tier items."));
        lines.add(bullet("A component that rolled good implicits is worth keeping — salvaging loses those rolls."));
        lines.add(bullet("Check the output panel before salvaging to see exactly what you might get back."));
        return lines;
    }

    private static List<Line> buildCombat() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Damage"));
        lines.add(plain("All damage runs through a custom ARPG pipeline. Your Gear Score is the primary driver."));
        lines.add(plain("Abilities that scale off Weapon damage use your main hand weapon's damage as their base."));
        lines.add(spacer());
        lines.add(heading("Blocking"));
        lines.add(plain("Most weapons support blocking. When a blocked hit comes in:"));
        lines.add(bullet("Stability", "Reduces incoming damage by your Stability percent."));
        lines.add(bullet("Stamina", "The remaining damage is subtracted from your Stamina."));
        lines.add(plain("Whatever is left continues through the pipeline and may be applied as health damage."));
        lines.add(spacer());
        lines.add(heading("Parrying"));
        lines.add(plain("Time your block precisely to parry. A successful parry checks the full incoming damage against your current Stamina — you cannot parry a hit that would exceed your stamina pool."));
        lines.add(spacer());
        lines.add(heading("Resources"));
        lines.add(bullet("Health", "No passive regeneration. Acquire health regen through skill trees or gear."));
        lines.add(bullet("Stamina", "Regenerates passively. Consumed by blocking and some abilities."));
        lines.add(bullet("Mana", "Regenerates passively. Used by abilities."));
        lines.add(bullet("Ammo", "Recharges automatically. No longer a consumed item."));
        lines.add(bullet("Signature Energy", "Builds over time. Required to cast Ultimate abilities."));
        lines.add(spacer());
        lines.add(heading("Enemy Rarities"));
        lines.add(bullet("Common",  "1 affix.",  "#ffffff"));
        lines.add(bullet("Magical", "2 affixes.", "#0000FF"));
        lines.add(bullet("Rare",    "3 affixes.", "#FF00CC"));
        lines.add(bullet("Elite",   "4 affixes.", "#FFFF00"));
        return lines;
    }

    private static List<Line> buildBaseBuilding() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Light Wells"));
        lines.add(plain("The Light Well is your base anchor. Placing one claims the surrounding territory as yours and sets it as your spawn point. Breaking your active Light Well resets your spawn back to world spawn."));
        lines.add(bullet("Benches can be placed inside an active Light Well territory. Beds can be placed anywhere."));
        lines.add(bullet("Each player can only have one Light Well."));
        lines.add(bullet("Placing a Light Well makes your base a potential raid target."));
        lines.add(plain("Use /home to teleport back to your Light Well from anywhere in the world."));
        lines.add(spacer());
        lines.add(heading("Co-Ownership"));
        lines.add(plain("You can share ownership of your Light Well territory with other players. Co-owners can place and remove benches inside your territory and are included in raid defense."));
        lines.add(bullet("Open the Light Well UI to manage co-owners."));
        lines.add(bullet("Co-owners do not consume their own Light Well slot — they can still place their own base."));
        lines.add(spacer());
        lines.add(heading("The Room System"));
        lines.add(plain("The Room System rewards creative building. Build rooms inside your territory and experiment with size, decorations, benches, and contents to discover hidden room recipes that unlock new bonuses."));
        lines.add(plain("There is no recipe book to read upfront — experimentation is the key. View discovered rooms with /discovered."));
        return lines;
    }

    private static List<Line> buildRaids() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("What Are Raids?"));
        lines.add(plain("Once you have an active Light Well your base can be raided. There is a variety of curated raid events, each bringing different enemy types and challenges."));
        lines.add(spacer());
        lines.add(heading("How Raids Work"));
        lines.add(plain("When a raid begins a raid HUD icon appears showing the current state. There is a short grace window before the first wave, and time between each subsequent wave."));
        lines.add(plain("After the last wave spawns there is a 5-minute window to clear all remaining enemies."));
        lines.add(plain("Important: Any raid enemies still alive when the raid ends will EXPLODE, destroying chunks of your base around them."));
        lines.add(spacer());
        lines.add(heading("Your Base When Away"));
        lines.add(plain("Your base can be raided even while you are offline. Use /home when a raid begins so you can handle it directly."));
        lines.add(plain("If your Light Well is destroyed all benches and beds in your territory will also break and drop in place."));
        return lines;
    }

    private static List<Line> buildCommands() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Player Commands"));
        lines.add(command("/skills", "Open your skill trees. Browse, invest skill points, and equip abilities."));
        lines.add(command("/stats", "Open character/gear management page. View and equip mod gear/items"));
        lines.add(command("/discovered", "Open your recipe book. Shows all discovered component and room recipes."));
        lines.add(command("/home", "Teleport to your Light Well from anywhere in the world."));
        lines.add(command("/HyARPG_Player_Settings_ShowCombatMessages <true|false>", "Toggle combat damage messages."));
        lines.add(spacer());
        lines.add(heading("Admin Commands"));
        lines.add(command("/HyARPG_Add_Player_Levels <player> <amount>", "Add levels to a player."));
        lines.add(command("/HyARPG_Set_Skill_Points <player> <amount>", "Set a player's skill point total."));
        lines.add(command("/HyARPG_Refund_Skills <player>", "Refund all spent skill points for a player."));
        lines.add(command("/HyARPG_Reset_Discovered_Ingredient <player>", "Reset discovered crafting ingredients."));
        lines.add(command("/HyARPG_Reset_Discovered_Rooms <player>", "Reset discovered room recipes."));
        lines.add(command("/HyARPG_Hunger_TickEnabled <true|false>", "Enable or disable the hunger system."));
        lines.add(command("/HyARPG_Thirst_TickEnabled <true|false>", "Enable or disable the thirst system."));
        lines.add(command("/HyARPG_Trigger_Raid <player> <base|player>", "Manually trigger a raid for testing."));
        lines.add(command("/HyARPG_Clear_Current_Territory", "Clear territory at your current location."));
        return lines;
    }

    private static List<Line> buildConfiguration() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Configuration File"));
        lines.add(plain("The configuration file is generated automatically on first run at:"));
        lines.add(plain("Hytale/UserData/Saves/world_name/mods/HyARPG"));
        lines.add(plain("Missing entries are regenerated on startup. Each entry includes a comment describing what it does."));
        lines.add(spacer());
        lines.add(heading("What Can Be Configured"));
        lines.add(bullet("Combat", "Damage multipliers, parry window timing, global player damage in/out multipliers, combat log broadcasting."));
        lines.add(bullet("Enemies", "Level scaling rate per distance, random level variance, nameplate display options."));
        lines.add(bullet("Experience", "Base XP to level up, XP scaling per level, XP gained from kills."));
        lines.add(bullet("Hunger & Thirst", "Enable/disable each system, drain rates, death timers, food restore values per tier."));
        lines.add(bullet("Players", "Base regeneration rates for Health, Mana, Stamina, and Ammo."));
        lines.add(bullet("Building", "Light Well territory claiming and bench placement rules."));
        lines.add(bullet("Raids", "Enable/disable raids, raid chance, cooldowns, wave timing, explosion damage settings."));
        lines.add(bullet("World", "Ore spawn distances, vein sizes, vein counts, Y level ranges per tier."));
        return lines;
    }

    // -------------------------------------------------------------------------
    // Line factory helpers
    // -------------------------------------------------------------------------

    private static final Message EMPTY = Message.raw("");

    private static Line spacer() {
        return new Line(EMPTY);
    }

    private static Line plain(@Nonnull String text) {
        return new Line(Message.raw(text).color("#cccccc"));
    }

    private static Line heading(@Nonnull String text) {
        return new Line(Message.raw(text).bold(true).color("#c8a84b"));
    }

    private static Line bullet(@Nonnull String text) {
        return new Line(Message.join(
                Message.raw("- ").color("#888888"),
                Message.raw(text).color("#cccccc")
        ));
    }

    private static Line bullet(@Nonnull String label, @Nonnull String description) {
        return new Line(Message.join(
                Message.raw("- ").color("#888888"),
                Message.raw(label).bold(true).color("#ffffff"),
                Message.raw(" — ").color("#555555"),
                Message.raw(description).color("#aaaaaa")
        ));
    }

    private static Line bullet(@Nonnull String label, @Nonnull String description, @Nonnull String labelColor) {
        return new Line(Message.join(
                Message.raw("- ").color("#888888"),
                Message.raw(label).bold(true).color(labelColor),
                Message.raw(" — ").color("#555555"),
                Message.raw(description).color("#aaaaaa")
        ));
    }

    private static Line command(@Nonnull String cmd, @Nonnull String description) {
        return new Line(Message.join(
                Message.raw(cmd).bold(true).color("#f0c060"),
                Message.raw("  ").color("#cccccc"),
                Message.raw(description).color("#888888")
        ));
    }

    // maps section id to display title
    private static String getSectionTitle(@Nonnull String sectionId) {
        return switch (sectionId) {
            case "getting_started" -> "Getting Started";
            case "survival"        -> "Survival";
            case "progression"     -> "Levels & Skill Trees";
            case "gear"            -> "Gear & Loot";
            case "crafting"        -> "Crafting";
            case "salvaging"       -> "Salvaging";
            case "combat"          -> "Combat";
            case "base_building"   -> "Base Building & Rooms";
            case "raids"           -> "Raids";
            case "commands"        -> "Commands";
            case "configuration"   -> "Configuration";
            default                -> sectionId;
        };
    }

    // simple wrapper so we can store a Message per line
    private record Line(Message message) {}

    // -------------------------------------------------------------------------
    // PageData codec
    // -------------------------------------------------------------------------

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
                .<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .build();

        public String action;
    }
}
