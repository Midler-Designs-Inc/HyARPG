package com.example.hyarpg.utils.affixes;

// Java Imports
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class AffixPool {

    // global list of applicable affixes
    private static final List<Affix> AFFIXES = List.of(
        // Flat Damages
        new Affix("Stat_Flat_Fire_Damage", "Fire Damage: +%s", 0.833f, 2.5f),
        new Affix("Stat_Flat_Cold_Damage", "Cold Damage: +%s", 0.833f, 2.5f),
        new Affix("Stat_Flat_Lightning_Damage", "Lightning Damage: +%s", 0.833f, 2.5f),
        new Affix("Stat_Flat_Poison_Damage", "Poison Damage: +%s", 0.833f, 2.5f),
        new Affix("Stat_Flat_Physical_Damage", "Physical Damage: +%s", 0.833f, 2.5f),
        new Affix("Stat_Flat_Magic_Damage", "Magic Damage: +%s", 0.833f, 2.5f),

        // Increased Damages
        new Affix("Stat_Increased_Fire_Damage", "Fire Damage: +%s%%", 3.33f, 10),
        new Affix("Stat_Increased_Cold_Damage", "Cold Damage: +%s%%", 3.33f, 10),
        new Affix("Stat_Increased_Lightning_Damage", "Lightning Damage: +%s%%", 3.33f, 10),
        new Affix("Stat_Increased_Poison_Damage", "Poison Damage: +%s%%", 3.33f, 10),
        new Affix("Stat_Increased_Physical_Damage", "Physical Damage: +%s%%", 3.33f, 10),
        new Affix("Stat_Increased_Magic_Damage", "Magic Damage: +%s%%", 3.33f, 10),

        // Increased Resistances
        new Affix("Stat_Increased_Fire_Resist", "Fire Resistance: +%s%%", 1.666f, 5),
        new Affix("Stat_Increased_Cold_Resist", "Cold Resistance: +%s%%", 1.666f, 5),
        new Affix("Stat_Increased_Lightning_Resist", "Lightning Resistance: +%s%%", 1.666f, 5),
        new Affix("Stat_Increased_Poison_Resist", "Poison Resistance: +%s%%", 1.666f, 5),
        new Affix("Stat_Increased_Physical_Resist", "Physical Resistance: +%s%%", 1.666f, 5),
        new Affix("Stat_Increased_Magic_Resist", "Magic Resistance: +%s%%", 1.666f, 5),

        // HP
        new Affix("Stat_Flat_Life", "Life: +%s", 8.333f, 25),
        new Affix("Stat_Increased_Life", "Life: +%s%%", 1.666f, 5),
        new Affix("Stat_Flat_Life_Regen", "Life Regen: +%s", 0.166f, 0.5f),
        new Affix("Stat_Increased_Life_Regen", "Life Regen: +%s%%", 3.33f, 10),

        // Stamina
        new Affix("Stat_Flat_Stamina", "Stamina: +%s", 0.833f, 2.5f),
        new Affix("Stat_Increased_Stamina", "Stamina: +%s%%", 1.666f, 5),
        new Affix("Stat_Flat_Stamina_Regen", "Stamina Regen: +%s", 0.166f, 0.5f),
        new Affix("Stat_Increased_Stamina_Regen", "Stamina Regen: +%s%%", 3.33f, 10),

        // Mana
        new Affix("Stat_Flat_Mana", "Mana: +%s", 2.083f, 6.25f),
        new Affix("Stat_Increased_Mana", "Mana: +%s%%", 1.666f, 5),
        new Affix("Stat_Flat_Mana_Regen", "Mana Regen: +%s", 0.166f, 0.5f),
        new Affix("Stat_Increased_Mana_Regen", "Mana Regen: +%s%%", 3.33f, 10),

        // Critical Strikes
        new Affix("Stat_Increased_Critical_Strike_Chance", "Critical Strike Chance: +%s%%", 1.666f, 5),
        new Affix("Stat_Increased_Critical_Strike_Damage", "Critical Strike Damage: +%s%%", 6.66f, 20),

        // Dodging
        new Affix("Stat_Increased_Dodge_Chance", "Dodge Chance: +%s%%", 1.666f, 5),

        // Parrying & Blocking
        new Affix("Stat_Increased_Stability", "Stability: +%s%%", 2.2f, 6.7f),
        new Affix("Stat_Flat_Parry_Window", "Parry Window: +%s", 0.033f, .1f),

        // Misc
        new Affix("Stat_Increased_Run_Speed", "Run Speed: +%s%%", 0.2f, 0.6f)
    ); // Affixes are T0-T5. Things are balanced around T1-T5, T0 will be a rare random 6th level of stat increase
    // Affix tiers increase every 100 levels on monsters/gear

    // get a single random affix
    public static Affix randomAffix() {
        return AFFIXES.get(
                ThreadLocalRandom.current().nextInt(AFFIXES.size())
        );
    }

    // get a single random flat damage affix
    public static Affix randomFlatDamageAffix() {
        ThreadLocalRandom r = ThreadLocalRandom.current();

        // Filter to flat damage affixes
        List<Affix> flatDamageAffixes = AFFIXES.stream()
            .filter(a -> a.stat().startsWith("Stat_Flat_") && a.stat().endsWith("_Damage"))
            .toList();

        // If none found, bail
        if (flatDamageAffixes.isEmpty()) return null;

        // Return the randomly selected flat damage affix
        return flatDamageAffixes.get(r.nextInt(flatDamageAffixes.size()));
    }

    // get a single random flat resistance affix
    public static Affix randomResistanceAffix() {
        ThreadLocalRandom r = ThreadLocalRandom.current();

        // Filter to resistance affixes
        List<Affix> resistAffixes = AFFIXES.stream()
            .filter(a -> a.stat().startsWith("Stat_Increased_") && a.stat().endsWith("_Resist"))
            .toList();

        // If none found, bail
        if (resistAffixes.isEmpty()) return null;

        // return the randomly selected resistance affix
        return resistAffixes.get(ThreadLocalRandom.current().nextInt(resistAffixes.size()));
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