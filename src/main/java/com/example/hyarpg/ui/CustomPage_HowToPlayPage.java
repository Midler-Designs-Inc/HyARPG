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

// Mod Imports
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.configs.Config_World;

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
            { "cube_combine",    "HTPBtnCubeCombine"    },
            { "salvaging",       "HTPBtnSalvaging"      },
            { "combat",          "HTPBtnCombat"         },
            { "base_building",   "HTPBtnBaseBuilding"   },
            { "raids",           "HTPBtnRaids"          },
            { "prefabs",         "HTPBtnPrefabs"        },
            { "wayward_shrines", "HTPBtnWaywardShrines" },
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
        cmd.append("HowToPlayPanel.ui");
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
        for (String[] section : SECTIONS)
            events.addEventBinding(CustomUIEventBindingType.Activating, "#" + section[1], EventData.of("Action", "section:" + section[0]));
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

    private void applySection(@Nonnull UICommandBuilder cmd, @Nonnull String sectionId) {
        cmd.set("#HTPSectionTitle.Text", getSectionTitle(sectionId));
        List<Line> lines = buildSectionContent(sectionId);
        for (int i = 0; i < TOTAL_LINES; i++) {
            String element = "#HTPLine" + (i + 1);
            if (i < lines.size()) {
                cmd.set(element + ".TextSpans", lines.get(i).message);
                cmd.set(element + ".Visible", true);
            } else {
                cmd.set(element + ".Text", "");
                cmd.set(element + ".Visible", false);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Section builders
    // -------------------------------------------------------------------------

    private List<Line> buildSectionContent(@Nonnull String sectionId) {
        return switch (sectionId) {
            case "getting_started" -> buildGettingStarted();
            case "survival"        -> buildSurvival();
            case "progression"     -> buildProgression();
            case "gear"            -> buildGear();
            case "crafting"        -> buildCrafting();
            case "cube_combine"    -> buildCubeCombine();
            case "salvaging"       -> buildSalvaging();
            case "combat"          -> buildCombat();
            case "base_building"   -> buildBaseBuilding();
            case "raids"           -> buildRaids();
            case "prefabs"         -> buildPrefabs();
            case "wayward_shrines" -> buildWaywardShrines();
            case "commands"        -> buildCommands();
            case "configuration"   -> buildConfiguration();
            default                -> List.of(plain("Section not found."));
        };
    }

    private List<Line> buildGettingStarted() {
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
        lines.add(heading("Death"));
        lines.add(plain("Dying is inevitable, but it doesn't have to be the end of the world. When you die you will drop a gravestone at the location of your death."));
        lines.add(bullet("Allies near your grave can revive you by channeling your grave uninterrupted for 10 seconds."));
        lines.add(bullet("Your items will be stored in your grave as a lootable container for as long as the gravestone exists. Currently any player is able to loot your gravestone, we might lock it down in the future."));
        lines.add(bullet("Dying and creating a new gravestone (with your current items), while an old gravestone exists, will cause the old gravestone to break and drop its items on the ground around it."));
        lines.add(spacer());
        lines.add(heading("Things To Do"));
        lines.add(plain("There is no strict order — explore at your own pace. Some good early goals:"));
        lines.add(bullet("Gather berries and fruit early — they restore both hunger and thirst."));
        lines.add(bullet("Craft a Water Bottle and purify dirty water at a Campfire."));
        lines.add(bullet("Kill enemies to level up and earn gear drops."));
        lines.add(bullet("Invest skill points into a skill tree using /skills."));
        lines.add(bullet("Place a Light Well to claim territory and start building a base."));
        lines.add(bullet("Look for Shard Dust — find it in the world or from enemies, then craft it into Rarity Shards at a Furnace to enhance your gear."));
        lines.add(bullet("Look for Runes of Powering — find it in the world or from enemies, use them to enhance your gear's gear score to your player level."));
        lines.add(bullet("Craft a Wayward Compass to help you find Wayward Shrines while exploring. Use the located shrines to get around and travel long distances."));
        return lines;
    }

    private List<Line> buildSurvival() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Death"));
        lines.add(plain("Dying is inevitable, but it doesn't have to be the end of the world. When you die you will drop a gravestone at the location of your death."));
        lines.add(bullet("Allies near your grave can revive you by channeling your grave uninterrupted for 10 seconds."));
        lines.add(bullet("Your items will be stored in your grave as a lootable container for as long as the gravestone exists. Currently any player is able to loot your gravestone, we might lock it down in the future."));
        lines.add(bullet("Dying and creating a new gravestone (with your current items), while an old gravestone exists, will cause the old gravestone to break and drop its items on the ground around it."));
        lines.add(bullet("Dead Mans Chest", "Craft this item and consume it to instantly recover all items from your gravestone, breaking it in the process."));
        lines.add(spacer());
        lines.add(heading("Hunger"));
        lines.add(plain("Hunger drains passively over time. It is restored by eating vanilla foods, which carry T1-T3 hunger restore buffs."));
        lines.add(spacer());
        lines.add(heading("Thirst"));
        lines.add(plain("Thirst also drains passively over time. Fruit such as berries and apples restore both thirst and hunger at the same time."));
        lines.add(spacer());
        lines.add(heading("Water"));
        lines.add(plain("You cannot drink water directly from the world. Craft a Water Bottle to collect dirty water, then purify it at a Campfire before consuming it."));
        return lines;
    }

    private List<Line> buildProgression() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Levelling Up"));
        lines.add(plain("Defeat enemies to earn XP."));
        lines.add(plain("XP scales based on enemy level vs yours. Enemies 10+ levels below give no XP. Enemies up to 10 levels above give up to triple XP."));
        lines.add(bullet("Players gain +" + ModConfig.get().players.base_health_per_level + " Max HP per level. (configurable)"));
        lines.add(spacer());
        lines.add(heading("Skill Trees"));
        lines.add(plain("Open your skill trees with /skills. You are not locked into a single class — mix and match across trees freely."));
        lines.add(plain("Each tree contains stat nodes and active abilities. Some trees may not appear until certain requirements are met."));
        lines.add(bullet("Sphere of Regret", "Craft these if you want to respec your skills."));
        lines.add(spacer());
        lines.add(heading("Abilities"));
        lines.add(plain("Abilities are unlocked and equipped directly from their skill trees. Assign them to:"));
        lines.add(bullet("E", "Primary Ability slot."));
        lines.add(bullet("R", "Secondary Ability slot."));
        lines.add(bullet("Q", "Ultimate Ability slot. Requires Signature Energy to cast."));
        lines.add(plain("Some abilities require specific weapon types. If the wrong weapon is equipped you will receive a chat notification."));
        return lines;
    }

    private List<Line> buildGear() {
        Config_World world = ModConfig.get().world;
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
        lines.add(bullet("Runes of Powering", "Use these in the Horradrix Cube to raise the gear score of gear."));
        lines.add(spacer());
        lines.add(heading("World Tiers & Material Progression"));
        lines.add(plain("Materials like metals, leather, and cloth are gated by world tiers T0–T6. The tier of an area increases the further you travel from the world origin (" + world.origin_spawn_point_x + ", " + world.origin_spawn_point_z + ")."));
        lines.add(plain("Enemies and loot in each zone reflect the tier. Push further out to access higher-tier crafting materials. Ore frequency ramps up and peaks near the middle of each range."));
        lines.add(bullet("T0 — Crude",      "0 – " + (int) world.min_distance_for_copper_spawn + " blocks from origin. Starter materials, no ore, no leather, no fabric."));
        lines.add(bullet("T1 — Copper",     (int) world.min_distance_for_copper_spawn + " – " + (int) world.max_distance_for_copper_spawn + " blocks. Copper, Soft Leather and Wool Fabric."));
        lines.add(bullet("T2 — Iron",       (int) world.min_distance_for_iron_spawn + " – " + (int) world.max_distance_for_iron_spawn + " blocks. Iron, Light Leather and Linen Fabric."));
        lines.add(bullet("T3 — Thorium",    (int) world.min_distance_for_thorium_spawn + " – " + (int) world.max_distance_for_thorium_spawn + " blocks. Thorium, Medium Leather and Cotton Fabric."));
        lines.add(bullet("T4 — Cobalt",     (int) world.min_distance_for_cobalt_spawn + " – " + (int) world.max_distance_for_cobalt_spawn + " blocks. Cobalt, Heavy Leather and Silk Fabric."));
        lines.add(bullet("T5 — Adamantite", (int) world.min_distance_for_adamantite_spawn + " – " + (int) world.max_distance_for_adamantite_spawn + " blocks. Adamantite, Storm Leather and Cindercloth Fabric."));
        lines.add(bullet("T6 — Mithril",    (int) world.min_distance_for_mithril_spawn + " – " + (int) world.max_distance_for_mithril_spawn + " blocks. Mithril, Dark Leather and Shadoweave Fabric."));
        return lines;
    }

    private List<Line> buildCrafting() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("The Horradrix Cube"));
        lines.add(plain("An ancient puzzle cube with magical transmutational properties. Use it to combine weapon and armor components into completed gear, combine items into new ones, and salvage gear back into components."));
        lines.add(spacer());
        lines.add(heading("Components"));
        lines.add(plain("Components are the building blocks of gear. Each has a type (e.g. Axe Head, Handle, Shaft) and a tier (T0-T5)."));
        lines.add(plain("Components are crafted at their respective benches from raw ingredients. Each component carries implicit stats through to the finished item."));
        lines.add(spacer());
        lines.add(heading("Rarity"));
        lines.add(plain("Use a Rarity Shard when combining components to influence the rarity of the resulting gear."));
        lines.add(bullet("Common (no shard)", "+0 random stat affix."));
        lines.add(bullet("Uncommon", "+1 random stat affix."));
        lines.add(bullet("Rare", "+2 random stat affixes."));
        lines.add(bullet("Epic", "+3 random stat affixes."));
        lines.add(bullet("Legendary", "+4 random stat affixes."));
        lines.add(spacer());
        lines.add(heading("Shard Dust"));
        lines.add(plain("Shard Dust can be found in the world or dropped by enemies. Collect enough and craft it into Rarity Shards at the Furnace — giving a reliable path to higher-rarity gear without relying purely on drops."));
        lines.add(spacer());
        lines.add(heading("Output Panel"));
        lines.add(plain("As you slot components the output panel updates to show the weapon type, base damage, implicit stats, and affix count before you commit to crafting."));
        return lines;
    }

    private List<Line> buildCubeCombine() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Cube Combine"));
        lines.add(plain("The Horradrix Cube has another trick — Cube Combine. Place items into the combine slots and experiment to discover new recipes. More recipes will be added over time."));
        lines.add(spacer());
        lines.add(heading("How It Works"));
        lines.add(plain("Slot items into the Cube. If the combination of items match a known recipe the output item appears in the output slot. Hit the combine button to confirm the transaction consuming the appropriate amount of input items and returning an appropriate amount of output items."));
        lines.add(plain("There is no recipe book — experimentation is the point. Trade knowledge with other players or discover it yourself."));
        lines.add(spacer());
        lines.add(heading("Crafting Runes"));
        lines.add(plain("Crafting Runes can be found in the world or dropped by enemies and are used in the Horradrix Cube to modify gear in specific ways. Runes provide deterministic ways to modify pre-existing crafted or looted gear."));
        lines.add(spacer());
        lines.add(heading("Known Recipes (Starter Hints)"));
        lines.add(bullet("3x Shard Dust (same tier)", "Combines into 1 higher-tier Shard Dust. A reliable way to upgrade your dust stockpile toward better shards."));
        lines.add(bullet("3x Broken Pickaxe", "Combines into 1 new working Pickaxe. Salvaging broken tools pays off."));
        return lines;
    }

    private List<Line> buildSalvaging() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("The Horradrix Cube"));
        lines.add(plain("An ancient puzzle cube with magical transmutational properties. Use it to break down gear into components and components into raw materials."));
        lines.add(spacer());
        lines.add(heading("Salvaging Gear"));
        lines.add(plain("Place a weapon or armour piece into the input slot. The output slots show the components and shard that could be returned."));
        lines.add(plain("Salvaging randomly returns one of the three components or the rarity shard (except for common items)."));
        lines.add(spacer());
        lines.add(heading("Salvaging Components"));
        lines.add(plain("Components can also be salvaged back into raw ingredients. Place a component in the input slot to see what ingredients it could return."));
        lines.add(plain("Salvaging randomly returns one of the up to 4 materials used to craft the component. The amount returned is random — anywhere from 1 to the full crafting cost."));
        lines.add(spacer());
        lines.add(heading("Tips"));
        lines.add(bullet("Salvage low-quality gear to fund crafting of higher-tier items."));
        lines.add(bullet("A component with good implicits is worth keeping — salvaging loses those rolls."));
        lines.add(bullet("Check the output panel before salvaging to see exactly what you might get back."));
        return lines;
    }

    private List<Line> buildCombat() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Damage"));
        lines.add(plain("All damage runs through a custom ARPG pipeline. Your Gear Score is the primary driver."));
        lines.add(plain("Abilities that scale off Weapon damage use your main hand weapon's damage as their base."));
        lines.add(bullet("Enemy Base Damage", "All enemies will do a base damage of " + ModConfig.get().combat.enemy_base_damage + " which is then multiplied by their level. (configurable)"));
        lines.add(bullet("Enemy Prefix", "Enemies have a " + (ModConfig.get().enemies.prefix_chance * 100) + "% (configurable) chance to spawn with a randomly rolled prefix from 1 of 6 different choices based on the mods 6 damage types. The rolled prefix adds flat damage to the enemy's attacks equal to +" + ModConfig.get().combat.enemy_prefix_damage + " (configurable) which is then multiplied by the enemy's level."));
        lines.add(bullet("Enemy Rarity", "Enemies deal " + ((ModConfig.get().combat.rarity_diff_damage_multiplier - 1) * 100) + "% (configurable) more damage per rarity and take " + ((ModConfig.get().combat.rarity_diff_damage_multiplier - 1) * 100) + " (configurable) less damage per rarity."));
        lines.add(spacer());
        lines.add(heading("Enemy Combat Types"));
        lines.add(plain("All enemies belong to one of 8 combat types. Each type changes how much damage they deal, how tough they are, and how they behave in combat. Learning these patterns will help you react faster and choose better targets."));
        lines.add(bullet("Zerg", "Very fragile but extremely fast. Deals low damage individually, but dangerous in groups due to their speed and numbers."));
        lines.add(bullet("Skirmisher", "Fast and evasive. Hard to hit and constantly moving, but deals slightly less damage and isn’t very durable."));
        lines.add(bullet("Fighter", "Balanced in all areas. Average health, average damage, and no major weaknesses or strengths."));
        lines.add(bullet("Berserker", "Very high damage with frequent critical hits. Low durability, but can quickly overwhelm you if ignored."));
        lines.add(bullet("Bruiser", "High health and solid damage. Can take a beating and deal consistent damage, especially against physical attacks."));
        lines.add(bullet("Juggernaut", "Extremely tough with the highest health and strong resistances. Hits hard and is slow to bring down."));
        lines.add(bullet("Sniper", "Low health but very high damage from critical hits. Can deal heavy burst damage if left unchecked."));
        lines.add(bullet("Caster", "Fragile but dangerous magic users. Deal high magical damage and are more resistant to magic attacks."));
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
        lines.add(bullet("Common",  "1 affix.",   "#ffffff"));
        lines.add(bullet("Magical", "2 affixes.",  "#0000FF"));
        lines.add(bullet("Rare",    "3 affixes.",  "#FF00CC"));
        lines.add(bullet("Elite",   "4 affixes.",  "#FFFF00"));
        return lines;
    }

    private List<Line> buildBaseBuilding() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Light Wells"));
        lines.add(plain("The Light Well is your base anchor. Placing one claims the surrounding territory as yours and sets it as your spawn point. Breaking your active Light Well resets your spawn back to world spawn."));
        lines.add(bullet("Benches can be placed inside an active Light Well territory. Beds can be placed anywhere."));
        lines.add(bullet("Each player can only have one Light Well."));
        lines.add(bullet("Placing a Light Well makes your base a potential raid target."));
        lines.add(plain("Use Homestones (crafted from inventory) to teleport back to your Light Well from anywhere in the world."));
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

    private List<Line> buildRaids() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("What Are Raids?"));
        lines.add(plain("Once you have an active Light Well your base can be raided. There is a variety of curated raid events, each bringing different enemy types and challenges."));
        lines.add(spacer());
        lines.add(heading("How Raids Work"));
        lines.add(bullet("When a raid begins a raid HUD icon appears showing the current state."));
        lines.add(bullet("There is a short grace window before the first wave, and a grace window between each subsequent wave."));
        lines.add(bullet("Enemies spawned during the raid will have an enemy level equal to the average level of all online owners/co-owners for base raids, and equal to the player level for player raids."));
        lines.add(bullet("After the last wave spawns there is a 5-minute window to clear all remaining enemies. Raids will end successfully when all enemies are cleared."));
        lines.add(bullet("Important:", "Any raid enemies still alive when the raid ends will EXPLODE, destroying chunks of your base around them. (configurable)"));
        lines.add(spacer());
        lines.add(heading("Raid Cooldown"));
        lines.add(plain("After a raid concludes there is a cooldown before your base can be targeted again. By default this is " + ModConfig.get().raids.raid_cooldown_in_minutes + " minutes. (configurable)"));
        lines.add(spacer());
        lines.add(heading("Your Base When Away"));
        lines.add(plain("Your base can be raided even when you are not nearby. When a raid begins, its critical to use a Homestone or get back quickly to prevent your base from being destroyed."));
        lines.add(plain("If your Light Well is destroyed all benches and beds in your territory will also break and drop in place. This will cause you to lose any bench upgrades you may have invested in."));
        return lines;
    }

    private List<Line> buildPrefabs() {
        Config_World world = ModConfig.get().world;
        List<Line> lines = new ArrayList<>();
        lines.add(heading("World Prefabs"));
        lines.add(plain("The world generates prefab structures automatically as you explore. These are hand-crafted builds placed into the world at regular intervals as you push into new regions."));
        lines.add(plain("All prefabs are stripped of crafting benches before placement, but chests are left intact and may contain loot."));
        lines.add(spacer());
        lines.add(heading("Prefab Types"));
        lines.add(bullet("Surface",             "Above-ground structures with no enemies. Appear every " + world.prefabSurfaceRegionSize + " blocks, " + (int)(world.prefabSurfaceSpawnChance * 100) + "% chance per region."));
        lines.add(bullet("Surface Dungeon",      "Above-ground structures with enemy spawners. More dangerous. Appear every " + world.prefabSurfaceDungeonRegionSize + " blocks, " + (int)(world.prefabSurfaceDungeonSpawnChance * 100) + "% chance per region."));
        lines.add(bullet("Underground",          "Below-ground structures with no enemies. Appear every " + world.prefabUndergroundRegionSize + " blocks, " + (int)(world.prefabUndergroundSpawnChance * 100) + "% chance per region."));
        lines.add(bullet("Underground Dungeon",  "Below-ground structures with enemy spawners. Expect a fight. Appear every " + world.prefabUndergroundDungeonRegionSize + " blocks, " + (int)(world.prefabUndergroundDungeonSpawnChance * 100) + "% chance per region."));
        lines.add(spacer());
        lines.add(heading("Adding Custom Prefabs"));
        lines.add(plain("Drop your own '*.prefab.json' files into any of the four prefab folders. The game will seed them randomly across your world. Only newly generated chunks are affected."));
        lines.add(bullet("mods/HyARPG/prefabs/surface"));
        lines.add(bullet("mods/HyARPG/prefabs/surface_dungeon"));
        lines.add(bullet("mods/HyARPG/prefabs/underground"));
        lines.add(bullet("mods/HyARPG/prefabs/underground_dungeon"));
        return lines;
    }

    private List<Line> buildWaywardShrines() {
        Config_World world = ModConfig.get().world;
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Wayward Shrines"));
        lines.add(plain("Exploring far from home is risky — Wayward Shrines make it more approachable. These teleporter shrines appear throughout the world at regular intervals and let you warp between them. Pro Tip: Build your base near one and you will have instant access anytime you are ready to head out!"));
        lines.add(spacer());
        lines.add(heading("How They Work"));
        lines.add(plain("Interact with a Wayward Shrine, give it a name (or leave the default) and hit save to active it. From that point forward that shrine will now appear as a warp point, by name, from any other shrine you find."));
        lines.add(plain("Shrines are a one-way safety net for deep exploration — push further out knowing you can always get back."));
        lines.add(spacer());
        lines.add(heading("Shrine Spacing"));
        lines.add(plain("Shrines appear every " + world.prefabWaywardShrineRegionSize + " blocks with a " + (int)(world.prefabWaywardShrineSpawnChance * 100) + "% spawn chance per region. Both values are configurable."));
        lines.add(spacer());
        lines.add(heading("Tips"));
        lines.add(bullet("Find a shrine before pushing into a new tier zone — it gives you a safe return point."));
        lines.add(bullet("Shrines are surface structures. Look above ground, not underground."));
        lines.add(bullet("If you are having a hard time finding a shrine, craft a Wayward Compass. It can guide you to your heart's desire."));
        return lines;
    }

    private List<Line> buildCommands() {
        List<Line> lines = new ArrayList<>();
        lines.add(heading("Player Commands"));
        lines.add(command("/skills", "Open your skill trees. Browse, invest skill points, and equip abilities."));
        lines.add(command("/stats", "Open character/gear management page. View and equip mod gear/items."));
        lines.add(command("/discovered", "Open your recipe book. Shows all discovered room recipes."));
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

    private List<Line> buildConfiguration() {
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
        lines.add(bullet("World", "Ore spawn distances, vein sizes and counts, Y level ranges, prefab region sizes and spawn chances, Wayward Shrine spacing."));
        return lines;
    }

    // -------------------------------------------------------------------------
    // Line factory helpers
    // -------------------------------------------------------------------------

    private static final Message EMPTY = Message.raw("");

    private static Line spacer() { return new Line(EMPTY); }

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

    private static String getSectionTitle(@Nonnull String sectionId) {
        return switch (sectionId) {
            case "getting_started" -> "Getting Started";
            case "survival"        -> "Survival";
            case "progression"     -> "Levels & Skill Trees";
            case "gear"            -> "Gear & Loot";
            case "crafting"        -> "Crafting";
            case "cube_combine"    -> "Cube Combine";
            case "salvaging"       -> "Salvaging";
            case "combat"          -> "Combat";
            case "base_building"   -> "Base Building & Rooms";
            case "raids"           -> "Raids";
            case "prefabs"         -> "World Prefabs";
            case "wayward_shrines" -> "Wayward Shrines";
            case "commands"        -> "Commands";
            case "configuration"   -> "Configuration";
            default                -> sectionId;
        };
    }

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