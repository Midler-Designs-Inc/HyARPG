package com.example.hyarpg.utils.affixes;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class AffixPool {

    private static final List<Affix> AFFIXES = List.of(
            // Flat Damages
            new Affix("Stat_Flat_Fire_Damage",              "Fire Damage: +%s",               0.833f, 2.5f),
            new Affix("Stat_Flat_Ice_Damage",               "Ice Damage: +%s",                0.833f, 2.5f),
            new Affix("Stat_Flat_Lightning_Damage",         "Lightning Damage: +%s",          0.833f, 2.5f),
            new Affix("Stat_Flat_Poison_Damage",            "Poison Damage: +%s",             0.833f, 2.5f),
            new Affix("Stat_Flat_Physical_Damage",          "Physical Damage: +%s",           0.833f, 2.5f),
            new Affix("Stat_Flat_Magic_Damage",             "Magic Damage: +%s",              0.833f, 2.5f),

            // Increased Damages
            new Affix("Stat_Increased_Fire_Damage",         "Fire Damage: +%s%%",             3.33f,  10f),
            new Affix("Stat_Increased_Ice_Damage",          "Ice Damage: +%s%%",              3.33f,  10f),
            new Affix("Stat_Increased_Lightning_Damage",    "Lightning Damage: +%s%%",        3.33f,  10f),
            new Affix("Stat_Increased_Poison_Damage",       "Poison Damage: +%s%%",           3.33f,  10f),
            new Affix("Stat_Increased_Physical_Damage",     "Physical Damage: +%s%%",         3.33f,  10f),
            new Affix("Stat_Increased_Magic_Damage",        "Magic Damage: +%s%%",            3.33f,  10f),

            // Weapon Type Damages
            new Affix("Stat_Increased_Axe_Damage",          "Axe Damage: +%s%%",              3.33f,  10f),
            new Affix("Stat_Increased_Battleaxe_Damage",    "Battleaxe Damage: +%s%%",        3.33f,  10f),
            new Affix("Stat_Increased_Club_Damage",         "Club Damage: +%s%%",             3.33f,  10f),
            new Affix("Stat_Increased_Daggers_Damage",      "Daggers Damage: +%s%%",          3.33f,  10f),
            new Affix("Stat_Increased_Kunai_Damage",        "Kunai Damage: +%s%%",            3.33f,  10f),
            new Affix("Stat_Increased_Longsword_Damage",    "Longsword Damage: +%s%%",        3.33f,  10f),
            new Affix("Stat_Increased_Mace_Damage",         "Mace Damage: +%s%%",             3.33f,  10f),
            new Affix("Stat_Increased_Shortbow_Damage",     "Shortbow Damage: +%s%%",         3.33f,  10f),
            new Affix("Stat_Increased_Crossbow_Damage",     "Crossbow Damage: +%s%%",         3.33f,  10f),
            new Affix("Stat_Increased_Sword_Damage",        "Sword Damage: +%s%%",            3.33f,  10f),
            new Affix("Stat_Increased_Staff_Damage",        "Staff Damage: +%s%%",            3.33f,  10f),
            new Affix("Stat_Increased_Wand_Damage",         "Wand Damage: +%s%%",             3.33f,  10f),

            // Increased Resistances
            new Affix("Stat_Increased_Fire_Resist",         "Fire Resistance: +%s%%",         1.666f, 5f),
            new Affix("Stat_Increased_Ice_Resist",          "Ice Resistance: +%s%%",          1.666f, 5f),
            new Affix("Stat_Increased_Lightning_Resist",    "Lightning Resistance: +%s%%",    1.666f, 5f),
            new Affix("Stat_Increased_Poison_Resist",       "Poison Resistance: +%s%%",       1.666f, 5f),
            new Affix("Stat_Increased_Physical_Resist",     "Physical Resistance: +%s%%",     1.666f, 5f),
            new Affix("Stat_Increased_Magic_Resist",        "Magic Resistance: +%s%%",        1.666f, 5f),
            new Affix("Stat_Increased_Elemental_Resist",    "Elemental Resistance: +%s%%",    1.0f,   3f),
            new Affix("Stat_Increased_Fall_Resist",         "Fall Resistance: +%s%%",         1.666f, 5f),

            // Life
            new Affix("Stat_Flat_Life",                     "Life: +%s",                      8.333f, 25f),
            new Affix("Stat_Increased_Life",                "Life: +%s%%",                    1.666f, 5f),
            new Affix("Stat_Flat_Life_Regen",               "Life Regen: +%s",                0.166f, 0.5f),
            new Affix("Stat_Increased_Life_Regen",          "Life Regen: +%s%%",              3.33f,  10f),

            // Stamina
            new Affix("Stat_Flat_Stamina",                  "Stamina: +%s",                   0.833f, 2.5f),
            new Affix("Stat_Increased_Stamina",             "Stamina: +%s%%",                 1.666f, 5f),
            new Affix("Stat_Flat_Stamina_Regen",            "Stamina Regen: +%s",             0.166f, 0.5f),
            new Affix("Stat_Increased_Stamina_Regen",       "Stamina Regen: +%s%%",           3.33f,  10f),

            // Mana
            new Affix("Stat_Flat_Mana",                     "Mana: +%s",                      2.083f, 6.25f),
            new Affix("Stat_Increased_Mana",                "Mana: +%s%%",                    1.666f, 5f),
            new Affix("Stat_Flat_Mana_Regen",               "Mana Regen: +%s",                0.166f, 0.5f),
            new Affix("Stat_Increased_Mana_Regen",          "Mana Regen: +%s%%",              3.33f,  10f),

            // Critical Strikes
            new Affix("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%", 1.666f, 5f),
            new Affix("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 6.66f,  20f),

            // Defense
            new Affix("Stat_Increased_Dodge_Chance",        "Dodge Chance: +%s%%",            1.666f, 5f),
            new Affix("Stat_Increased_Stability",           "Stability: +%s%%",               2.2f,   6.7f),
            new Affix("Stat_Flat_Parry_Window",             "Parry Window: +%s",              0.033f, 0.1f),
            new Affix("Stat_Increased_Barrier_On_Block",    "Barrier on Block: +%s%%",        1.666f, 5f),
            new Affix("Stat_Increased_Shield_Stability",    "Shield Stability: +%s%%",        2.2f,   6.7f),

            // Leech — very tight, 6x T0 on 4 pieces would be ~2.4-7.2% which is still very strong
            new Affix("Stat_Increased_Life_Leech",          "Life Leech: +%s%%",              0.1f,   0.3f),
            new Affix("Stat_Increased_Mana_Leech",          "Mana Leech: +%s%%",              0.1f,   0.3f),
            new Affix("Stat_Increased_Stamina_Leech",       "Stamina Leech: +%s%%",           0.1f,   0.3f),

            // Damage taken from — conversion mechanics, tight range, 6x T0 max ~9-18%
            new Affix("Stat_Increased_Damage_Taken_From_Mana",    "Damage from Mana: +%s%%",    0.25f,  0.75f),
            new Affix("Stat_Increased_Damage_Taken_From_Stamina", "Damage from Stamina: +%s%%", 0.25f,  0.75f),

            // Ammo
            new Affix("Stat_Flat_Ammo",                     "Ammo: +%s",                      0.5f,   1.5f),
            new Affix("Stat_Increased_Ammo_Regen",          "Ammo Regen: +%s%%",              1.666f, 5f),

            // Misc
            new Affix("Stat_Increased_Run_Speed",           "Run Speed: +%s%%",               0.2f,   0.6f)
    );

    public static Affix randomAffix() { return AFFIXES.get(ThreadLocalRandom.current().nextInt(AFFIXES.size())); }

    public static Affix randomFlatDamageAffix() {
        List<Affix> filtered = AFFIXES.stream().filter(a -> a.stat().startsWith("Stat_Flat_") && a.stat().endsWith("_Damage")).toList();
        return filtered.isEmpty() ? null : filtered.get(ThreadLocalRandom.current().nextInt(filtered.size()));
    }

    public static Affix randomResistanceAffix() {
        List<Affix> filtered = AFFIXES.stream().filter(a -> a.stat().startsWith("Stat_Increased_") && a.stat().endsWith("_Resist")).toList();
        return filtered.isEmpty() ? null : filtered.get(ThreadLocalRandom.current().nextInt(filtered.size()));
    }

    public static List<Affix> randomAffixes(int count) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Set<Integer> indices = new HashSet<>();
        while (indices.size() < count) indices.add(r.nextInt(AFFIXES.size()));
        List<Affix> result = new ArrayList<>(count);
        for (int idx : indices) result.add(AFFIXES.get(idx));
        return result;
    }

    public static Affix getAffixByStatName(String statName) {
        for (Affix affix : AFFIXES) if (affix.stat().equals(statName)) return affix;
        return null;
    }
}