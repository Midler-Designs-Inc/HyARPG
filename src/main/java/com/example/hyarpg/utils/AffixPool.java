package com.example.hyarpg.utils;

// Java Imports
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class AffixPool {

    // global list of applicable affixes
    private static final List<Affix> AFFIXES = List.of(
        // Flat Damages
        new Affix("Stat_Flat_Fire_Damage", "Fire Damage: +%s", 1, 3, true),
        new Affix("Stat_Flat_Cold_Damage", "Cold Damage: +%s", 1, 3, true),
        new Affix("Stat_Flat_Lightning_Damage", "Lightning Damage: +%s", 1, 3, true),
        new Affix("Stat_Flat_Poison_Damage", "Poison Damage: +%s", 1, 3, true),
        new Affix("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 1, 3, true),
        new Affix("Stat_Flat_Magical_Damage", "Magical Damage: +%s", 1, 3, true),
        new Affix("Stat_Flat_Projectile_Damage", "Projectile Damage: +%s", 1, 3, true),

        // Increased Damages
        new Affix("Stat_Increased_Fire_Damage", "Fire Damage: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Cold_Damage", "Cold Damage: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Lightning_Damage", "Lightning Damage: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Poison_Damage", "Poison Damage: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Physical_Damage", "Physical Damage: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Magical_Damage", "Magical Damage: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Projectile_Damage", "Projectile Damage: +%s%%", 1, 3, true),

        // Increased Resistances
        new Affix("Stat_Increased_Fire_Resist", "Fire Resistance: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Cold_Resist", "Cold Resistance: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Lightning_Resist", "Lightning Resistance: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Poison_Resist", "Poison Resistance: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Physical_Resist", "Physical Resistance: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Magical_Resist", "Magical Resistance: +%s%%", 1, 3, true),

        // Critical Strikes
        new Affix("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%", 1, 3, true),
        new Affix("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 1, 3, true),

        // HP
        new Affix("Stat_Flat_Life", "Life: +%s", 1, 3, true),
        new Affix("Stat_Increased_Life", "Life: +%s%%", 1, 3, true),
        new Affix("Stat_Flat_Life_Regen", "Life Regen: +%s", 1, 3, true),
        new Affix("Stat_Increased_Life_Regen", "Life Regen: +%s%%", 1, 3, true),

        // Stamina
        new Affix("Stat_Flat_Stamina", "Stamina: +%s", 1, 3, true),
        new Affix("Stat_Increased_Stamina", "Stamina: +%s%%", 1, 3, true),
        new Affix("Stat_Flat_Stamina_Regen", "Stamina Regen: +%s", 1, 3, true),
        new Affix("Stat_Increased_Stamina_Regen", "Stamina Regen: +%s%%", 1, 3, true),

        // Mana
        new Affix("Stat_Flat_Mana", "Mana: +%s", 1, 3, true),
        new Affix("Stat_Increased_Mana", "Mana: +%s%%", 1, 3, true),
        new Affix("Stat_Flat_Mana_Regen", "Mana Regen: +%s", 1, 3, true),
        new Affix("Stat_Increased_Mana_Regen", "Mana Regen: +%s%%", 1, 3, true),

        // Dodging
        new Affix("Stat_Increased_Dodge_Chance", "Dodge Chance: +%s%%", 1, 3, true),

        // Parrying & Blocking
        new Affix("Stat_Increased_Stability", "Stability: +%s%%", 1, 3, true),
        new Affix("Stat_Flat_Increase_Parry_Window", "Parry Window: +%s", 1, 3, true),

        // Misc
        new Affix("Stat_Jumps", "Jumps: +%s", 1, 3, false),
        new Affix("Stat_Run_Speed", "Run Speed: +%s%%", 1, 3, false)
    );

    // get a single random affix
    public static Affix randomAffix() {
        return AFFIXES.get(
                ThreadLocalRandom.current().nextInt(AFFIXES.size())
        );
    }

    // get n random affixes
    public static List<Affix> randomAffixes(int count) {
        // Obtain the thread-local random number generator (fast, thread-safe RNG)
        ThreadLocalRandom r = ThreadLocalRandom.current();

        // Track randomly chosen indices; Set ensures uniqueness (no duplicates)
        Set<Integer> indices = new HashSet<>();

        // Keep sampling random indices until we have the requested number of unique entries
        while (indices.size() < count) {
            indices.add(r.nextInt(AFFIXES.size()));
        }

        // Build result list from sampled indices
        List<Affix> result = new ArrayList<>(count);
        for (int idx : indices) result.add(AFFIXES.get(idx));

        // return the result
        return result;
    }

    // get affix by name
    public static Affix getAffixByStatName(String statName) {
        for (Affix affix : AFFIXES) {
            if (affix.stat().equals(statName)) return affix;
        }
        return null;
    }
}