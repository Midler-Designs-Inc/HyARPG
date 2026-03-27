package com.example.hyarpg.utils.affixes;

// Java Mods
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ImplicitAffixPool {

    private record AffixDef(String stat, String display, float min, float max) {
        Affix build() { return new Affix(stat, display, min, max); }
    }

    private static AffixDef f(String stat, String display, float min, float max) { return new AffixDef(stat, display, min, max); }
    private static AffixDef f(String stat, String display, float value) { return new AffixDef(stat, display, value, value); }

    // ARMOR — +1-4% phys resist, +1-4% life per tier
    private static final List<List<AffixDef>> ARMOR = List.of(
        List.of(f("Stat_Increased_Physical_Resist", "Physical Resistance: +%s%%", 5f, 20f), f("Stat_Increased_Life", "Life: +%s%%", 5f, 20f)), // T1
        List.of(f("Stat_Increased_Physical_Resist", "Physical Resistance: +%s%%", 4f, 16f), f("Stat_Increased_Life", "Life: +%s%%", 4f, 16f)), // T2
        List.of(f("Stat_Increased_Physical_Resist", "Physical Resistance: +%s%%", 3f, 12f), f("Stat_Increased_Life", "Life: +%s%%", 3f, 12f)), // T3
        List.of(f("Stat_Increased_Physical_Resist", "Physical Resistance: +%s%%", 2f,  8f), f("Stat_Increased_Life", "Life: +%s%%", 2f,  8f)), // T4
        List.of(f("Stat_Increased_Physical_Resist", "Physical Resistance: +%s%%", 1f,  4f), f("Stat_Increased_Life", "Life: +%s%%", 1f,  4f))  // T5
    );

    // ARMOR — LEATHER — +1-4% elemental resist, +1-4% stamina per tier
    private static final List<List<AffixDef>> ARMOR_LEATHER = List.of(
        List.of(f("Stat_Increased_Elemental_Resist", "Elemental Resistance: +%s%%", 5f, 20f), f("Stat_Increased_Stamina", "Stamina: +%s%%", 5f, 20f)), // T1
        List.of(f("Stat_Increased_Elemental_Resist", "Elemental Resistance: +%s%%", 4f, 16f), f("Stat_Increased_Stamina", "Stamina: +%s%%", 4f, 16f)), // T2
        List.of(f("Stat_Increased_Elemental_Resist", "Elemental Resistance: +%s%%", 3f, 12f), f("Stat_Increased_Stamina", "Stamina: +%s%%", 3f, 12f)), // T3
        List.of(f("Stat_Increased_Elemental_Resist", "Elemental Resistance: +%s%%", 2f,  8f), f("Stat_Increased_Stamina", "Stamina: +%s%%", 2f,  8f)), // T4
        List.of(f("Stat_Increased_Elemental_Resist", "Elemental Resistance: +%s%%", 1f,  4f), f("Stat_Increased_Stamina", "Stamina: +%s%%", 1f,  4f))  // T5
    );

    // ARMOR — CLOTH — +1-4% magic resist, +1-4% mana per tier
    private static final List<List<AffixDef>> ARMOR_CLOTH = List.of(
        List.of(f("Stat_Increased_Magic_Resist", "Magic Resistance: +%s%%", 5f, 20f), f("Stat_Increased_Mana", "Mana: +%s%%", 5f, 20f)), // T1
        List.of(f("Stat_Increased_Magic_Resist", "Magic Resistance: +%s%%", 4f, 16f), f("Stat_Increased_Mana", "Mana: +%s%%", 4f, 16f)), // T2
        List.of(f("Stat_Increased_Magic_Resist", "Magic Resistance: +%s%%", 3f, 12f), f("Stat_Increased_Mana", "Mana: +%s%%", 3f, 12f)), // T3
        List.of(f("Stat_Increased_Magic_Resist", "Magic Resistance: +%s%%", 2f,  8f), f("Stat_Increased_Mana", "Mana: +%s%%", 2f,  8f)), // T4
        List.of(f("Stat_Increased_Magic_Resist", "Magic Resistance: +%s%%", 1f,  4f), f("Stat_Increased_Mana", "Mana: +%s%%", 1f,  4f))  // T5
    );

    // WEAPON — AXE — +1-3% dodge per tier; flat phys damage with explicit ranges per tier
    private static final List<List<AffixDef>> WEAPON_AXE = List.of(
        List.of(f("Stat_Increased_Dodge_Chance", "Dodge Chance: +%s%%", 5f, 15f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 5f, 10f)), // T1
        List.of(f("Stat_Increased_Dodge_Chance", "Dodge Chance: +%s%%", 4f, 12f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 4f,  6f)), // T2
        List.of(f("Stat_Increased_Dodge_Chance", "Dodge Chance: +%s%%", 3f,  9f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 3f,  4f)), // T3
        List.of(f("Stat_Increased_Dodge_Chance", "Dodge Chance: +%s%%", 2f,  6f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 1f,  2f)), // T4
        List.of(f("Stat_Increased_Dodge_Chance", "Dodge Chance: +%s%%", 1f,  3f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 1f,  2f))  // T5
    );

    // WEAPON — BATTLEAXE — +5-20% crit damage per tier
    private static final List<List<AffixDef>> WEAPON_BATTLEAXE = List.of(
        List.of(f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 25f, 100f)), // T1
        List.of(f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 20f,  80f)), // T2
        List.of(f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 15f,  60f)), // T3
        List.of(f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 10f,  40f)), // T4
        List.of(f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%",  5f,  20f))  // T5
    );

    // WEAPON — CLUB — +2-5% phys damage per tier
    private static final List<List<AffixDef>> WEAPON_CLUB = List.of(
        List.of(f("Stat_Increased_Physical_Damage", "Physical Damage: +%s%%", 10f, 25f)), // T1
        List.of(f("Stat_Increased_Physical_Damage", "Physical Damage: +%s%%",  8f, 20f)), // T2
        List.of(f("Stat_Increased_Physical_Damage", "Physical Damage: +%s%%",  6f, 15f)), // T3
        List.of(f("Stat_Increased_Physical_Damage", "Physical Damage: +%s%%",  4f, 10f)), // T4
        List.of(f("Stat_Increased_Physical_Damage", "Physical Damage: +%s%%",  2f,  5f))  // T5
    );

    // WEAPON — CROSSBOW — flat ammo with explicit values per tier
    private static final List<List<AffixDef>> WEAPON_CROSSBOW = List.of(
        List.of(f("Stat_Flat_Ammo", "Ammo: +%s", 2f, 3f)), // T1
        List.of(f("Stat_Flat_Ammo", "Ammo: +%s", 2f)),     // T2
        List.of(f("Stat_Flat_Ammo", "Ammo: +%s", 2f)),     // T3
        List.of(f("Stat_Flat_Ammo", "Ammo: +%s", 1f)),     // T4
        List.of(f("Stat_Flat_Ammo", "Ammo: +%s", 1f))      // T5
    );

    // WEAPON — DAGGERS — +5-20% crit chance per tier, +1-5% crit damage per tier
    private static final List<List<AffixDef>> WEAPON_DAGGER = List.of(
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%", 25f, 100f), f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 5f, 25f)), // T1
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%", 20f,  80f), f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 4f, 20f)), // T2
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%", 15f,  60f), f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 3f, 15f)), // T3
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%", 10f,  40f), f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 2f, 10f)), // T4
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%",  5f,  20f), f("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 1f,  5f))  // T5
    );

    // WEAPON — KUNAI — +1-3% poison damage per tier
    private static final List<List<AffixDef>> WEAPON_KUNAI = List.of(
        List.of(f("Stat_Increased_Poison_Damage", "Poison Damage: +%s%%", 5f, 15f)), // T1
        List.of(f("Stat_Increased_Poison_Damage", "Poison Damage: +%s%%", 4f, 12f)), // T2
        List.of(f("Stat_Increased_Poison_Damage", "Poison Damage: +%s%%", 3f,  9f)), // T3
        List.of(f("Stat_Increased_Poison_Damage", "Poison Damage: +%s%%", 2f,  6f)), // T4
        List.of(f("Stat_Increased_Poison_Damage", "Poison Damage: +%s%%", 1f,  3f))  // T5
    );

    // WEAPON — LONGSWORD — +2-5 flat stamina per tier, +1-3 flat phys damage per tier
    private static final List<List<AffixDef>> WEAPON_LONGSWORD = List.of(
        List.of(f("Stat_Flat_Stamina", "Stamina: +%s", 10f, 25f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 5f, 15f)), // T1
        List.of(f("Stat_Flat_Stamina", "Stamina: +%s",  8f, 20f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 4f, 12f)), // T2
        List.of(f("Stat_Flat_Stamina", "Stamina: +%s",  6f, 15f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 3f,  9f)), // T3
        List.of(f("Stat_Flat_Stamina", "Stamina: +%s",  4f, 10f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 2f,  6f)), // T4
        List.of(f("Stat_Flat_Stamina", "Stamina: +%s",  2f,  5f), f("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 1f,  3f))  // T5
    );

    // WEAPON — MACE — +1-4% stamina regen per tier; flat life regen with explicit values per tier
    private static final List<List<AffixDef>> WEAPON_MACE = List.of(
        List.of(f("Stat_Increased_Stamina_Regen", "Stamina Regen: +%s%%", 5f, 20f), f("Stat_Flat_Life_Regen", "Life Regen: +%s", 2f, 4f)), // T1
        List.of(f("Stat_Increased_Stamina_Regen", "Stamina Regen: +%s%%", 4f, 16f), f("Stat_Flat_Life_Regen", "Life Regen: +%s", 2f)),     // T2
        List.of(f("Stat_Increased_Stamina_Regen", "Stamina Regen: +%s%%", 3f, 12f), f("Stat_Flat_Life_Regen", "Life Regen: +%s", 2f)),     // T3
        List.of(f("Stat_Increased_Stamina_Regen", "Stamina Regen: +%s%%", 2f,  8f), f("Stat_Flat_Life_Regen", "Life Regen: +%s", 1f)),     // T4
        List.of(f("Stat_Increased_Stamina_Regen", "Stamina Regen: +%s%%", 1f,  4f), f("Stat_Flat_Life_Regen", "Life Regen: +%s", 1f))      // T5
    );

    // SHIELD — +5-10% stability per tier
    private static final List<List<AffixDef>> SHIELD = List.of(
        List.of(f("Stat_Increased_Stability", "Stability: +%s%%", 25f, 50f)), // T1
        List.of(f("Stat_Increased_Stability", "Stability: +%s%%", 20f, 40f)), // T2
        List.of(f("Stat_Increased_Stability", "Stability: +%s%%", 15f, 30f)), // T3
        List.of(f("Stat_Increased_Stability", "Stability: +%s%%", 10f, 20f)), // T4
        List.of(f("Stat_Increased_Stability", "Stability: +%s%%",  5f, 10f))  // T5
    );

    // WEAPON — SHORTBOW — +2-5% crit chance per tier, +0.1-1% run speed per tier
    private static final List<List<AffixDef>> WEAPON_SHORTBOW = List.of(
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%", 10f, 25f), f("Stat_Increased_Run_Speed", "Run Speed: +%s%%", 0.5f, 5f)),  // T1
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%",  8f, 20f), f("Stat_Increased_Run_Speed", "Run Speed: +%s%%", 0.4f, 4f)),  // T2
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%",  6f, 15f), f("Stat_Increased_Run_Speed", "Run Speed: +%s%%", 0.3f, 3f)),  // T3
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%",  4f, 10f), f("Stat_Increased_Run_Speed", "Run Speed: +%s%%", 0.2f, 2f)),  // T4
        List.of(f("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%",  2f,  5f), f("Stat_Increased_Run_Speed", "Run Speed: +%s%%", 0.1f, 1f))   // T5
    );

    // WEAPON — SWORD — +0.05-0.1 parry window per tier; flat fire damage with explicit ranges per tier
    private static final List<List<AffixDef>> WEAPON_SWORD = List.of(
        List.of(f("Stat_Flat_Parry_Window", "Parry Window: +%s", 0.25f, 0.5f), f("Stat_Flat_Fire_Damage", "Fire Damage: +%s", 5f, 10f)), // T1
        List.of(f("Stat_Flat_Parry_Window", "Parry Window: +%s", 0.2f,  0.4f), f("Stat_Flat_Fire_Damage", "Fire Damage: +%s", 4f,  6f)), // T2
        List.of(f("Stat_Flat_Parry_Window", "Parry Window: +%s", 0.15f, 0.3f), f("Stat_Flat_Fire_Damage", "Fire Damage: +%s", 3f,  4f)), // T3
        List.of(f("Stat_Flat_Parry_Window", "Parry Window: +%s", 0.1f,  0.2f), f("Stat_Flat_Fire_Damage", "Fire Damage: +%s", 1f,  2f)), // T4
        List.of(f("Stat_Flat_Parry_Window", "Parry Window: +%s", 0.05f, 0.1f), f("Stat_Flat_Fire_Damage", "Fire Damage: +%s", 1f,  2f))  // T5
    );

    // Registry
    private static final Map<String, List<List<AffixDef>>> REGISTRY = Map.ofEntries(
        Map.entry("Armor",            ARMOR),
        Map.entry("Armor_Cloth",      ARMOR_CLOTH),
        Map.entry("Armor_Leather",    ARMOR_LEATHER),
        Map.entry("Weapon_Axe",       WEAPON_AXE),
        Map.entry("Weapon_Battleaxe", WEAPON_BATTLEAXE),
        Map.entry("Weapon_Club",      WEAPON_CLUB),
        Map.entry("Weapon_Crossbow",  WEAPON_CROSSBOW),
        Map.entry("Weapon_Daggers",   WEAPON_DAGGER),
        Map.entry("Weapon_Kunai",     WEAPON_KUNAI),
        Map.entry("Weapon_Longsword", WEAPON_LONGSWORD),
        Map.entry("Weapon_Mace",      WEAPON_MACE),
        Map.entry("Weapon_Shield",    SHIELD),
        Map.entry("Weapon_Shortbow",  WEAPON_SHORTBOW),
        Map.entry("Weapon_Sword",     WEAPON_SWORD)
    );

    private ImplicitAffixPool() {}

    // Returns freshly rolled Affix instances every call — no cached values
    public static List<Affix> getImplicits(String itemType, int tier) {
        tier = Math.min(Math.max(tier, 1), 5);

        List<List<AffixDef>> tiers = REGISTRY.get(itemType);
        if (tiers == null) return List.of();

        return tiers.get(tier - 1).stream().map(AffixDef::build).collect(Collectors.toList());
    }
}