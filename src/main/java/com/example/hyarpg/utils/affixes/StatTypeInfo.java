package com.example.hyarpg.utils;

import com.example.hyarpg.utils.affixes.StatType;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Random;

public class StatTypeInfo {

    private static final Random RANDOM = new Random();

    // friendly display names for each stat type
    private static final Map<StatType, String> DISPLAY_NAMES = Map.ofEntries(
            // Primary Resources
            Map.entry(StatType.LIFE_FLAT,                     "+{value} Life"),
            Map.entry(StatType.LIFE_PERCENT,                  "+{value}% Life"),
            Map.entry(StatType.LIFE_REGEN_FLAT,               "+{value} Life Regen"),
            Map.entry(StatType.LIFE_REGEN_PERCENT,            "+{value}% Life Regen"),
            Map.entry(StatType.STAMINA_FLAT,                  "+{value} Stamina"),
            Map.entry(StatType.STAMINA_PERCENT,               "+{value}% Stamina"),
            Map.entry(StatType.STAMINA_REGEN_FLAT,            "+{value} Stamina Regen"),
            Map.entry(StatType.STAMINA_REGEN_PERCENT,         "+{value}% Stamina Regen"),
            Map.entry(StatType.MANA_FLAT,                     "+{value} Mana"),
            Map.entry(StatType.MANA_PERCENT,                  "+{value}% Mana"),
            Map.entry(StatType.MANA_REGEN_FLAT,               "+{value} Mana Regen"),
            Map.entry(StatType.MANA_REGEN_PERCENT,            "+{value}% Mana Regen"),

            // Damage
            Map.entry(StatType.FIRE_DAMAGE_FLAT,              "+{value} Fire Damage"),
            Map.entry(StatType.FIRE_DAMAGE_PERCENT,           "+{value}% Fire Damage"),
            Map.entry(StatType.ICE_DAMAGE_FLAT,               "+{value} Ice Damage"),
            Map.entry(StatType.ICE_DAMAGE_PERCENT,            "+{value}% Ice Damage"),
            Map.entry(StatType.LIGHTNING_DAMAGE_FLAT,         "+{value} Lightning Damage"),
            Map.entry(StatType.LIGHTNING_DAMAGE_PERCENT,      "+{value}% Lightning Damage"),
            Map.entry(StatType.POISON_DAMAGE_FLAT,            "+{value} Poison Damage"),
            Map.entry(StatType.POISON_DAMAGE_PERCENT,         "+{value}% Poison Damage"),
            Map.entry(StatType.PHYSICAL_DAMAGE_FLAT,          "+{value} Physical Damage"),
            Map.entry(StatType.PHYSICAL_DAMAGE_PERCENT,       "+{value}% Physical Damage"),
            Map.entry(StatType.MAGIC_DAMAGE_FLAT,             "+{value} Magic Damage"),
            Map.entry(StatType.MAGIC_DAMAGE_PERCENT,          "+{value}% Magic Damage"),

            // Weapon Damages
            Map.entry(StatType.AXE_DAMAGE_PERCENT,            "+{value}% Axe Damage"),
            Map.entry(StatType.BATTLEAXE_DAMAGE_PERCENT,      "+{value}% Battleaxe Damage"),
            Map.entry(StatType.CLUB_DAMAGE_PERCENT,           "+{value}% Club Damage"),
            Map.entry(StatType.DAGGERS_DAMAGE_PERCENT,        "+{value}% Daggers Damage"),
            Map.entry(StatType.KUNAI_DAMAGE_PERCENT,          "+{value}% Kunai Damage"),
            Map.entry(StatType.LONGSWORD_DAMAGE_PERCENT,      "+{value}% Longsword Damage"),
            Map.entry(StatType.MACE_DAMAGE_PERCENT,           "+{value}% Mace Damage"),
            Map.entry(StatType.SHORTBOW_DAMAGE_PERCENT,       "+{value}% Shortbow Damage"),
            Map.entry(StatType.CROSSBOW_DAMAGE_PERCENT,       "+{value}% Crossbow Damage"),
            Map.entry(StatType.SWORD_DAMAGE_PERCENT,          "+{value}% Sword Damage"),

            // Weapon Specific
            Map.entry(StatType.SHIELD_STABILITY_PERCENT,      "+{value}% Shield Stability"),

            // Main Hand Flat Damages — sets base weapon damage, not a bonus
            Map.entry(StatType.MAIN_HAND_FIRE_DAMAGE_FLAT,      "{value} Fire Damage"),
            Map.entry(StatType.MAIN_HAND_ICE_DAMAGE_FLAT,       "{value} Ice Damage"),
            Map.entry(StatType.MAIN_HAND_LIGHTNING_DAMAGE_FLAT, "{value} Lightning Damage"),
            Map.entry(StatType.MAIN_HAND_POISON_DAMAGE_FLAT,    "{value} Poison Damage"),
            Map.entry(StatType.MAIN_HAND_PHYSICAL_DAMAGE_FLAT,  "{value} Physical Damage"),
            Map.entry(StatType.MAIN_HAND_MAGIC_DAMAGE_FLAT,     "{value} Magic Damage"),

            // Off Hand Flat Damages — sets base weapon damage, not a bonus
            Map.entry(StatType.OFF_HAND_FIRE_DAMAGE_FLAT,       "{value} Fire Damage"),
            Map.entry(StatType.OFF_HAND_ICE_DAMAGE_FLAT,        "{value} Ice Damage"),
            Map.entry(StatType.OFF_HAND_LIGHTNING_DAMAGE_FLAT,  "{value} Lightning Damage"),
            Map.entry(StatType.OFF_HAND_POISON_DAMAGE_FLAT,     "{value} Poison Damage"),
            Map.entry(StatType.OFF_HAND_PHYSICAL_DAMAGE_FLAT,   "{value} Physical Damage"),
            Map.entry(StatType.OFF_HAND_MAGIC_DAMAGE_FLAT,      "{value} Magic Damage"),

            // Resistances
            Map.entry(StatType.FIRE_RESIST_PERCENT,           "+{value}% Fire Resistance"),
            Map.entry(StatType.ICE_RESIST_PERCENT,            "+{value}% Ice Resistance"),
            Map.entry(StatType.LIGHTNING_RESIST_PERCENT,      "+{value}% Lightning Resistance"),
            Map.entry(StatType.POISON_RESIST_PERCENT,         "+{value}% Poison Resistance"),
            Map.entry(StatType.PHYSICAL_RESIST_PERCENT,       "+{value}% Physical Resistance"),
            Map.entry(StatType.MAGIC_RESIST_PERCENT,          "+{value}% Magic Resistance"),
            Map.entry(StatType.ELEMENTAL_RESIST_PERCENT,      "+{value}% Elemental Resistance"),
            Map.entry(StatType.FALL_RESIST_PERCENT,           "+{value}% Fall Resistance"),

            // Critical
            Map.entry(StatType.CRITICAL_STRIKE_CHANCE_PERCENT, "+{value}% Critical Strike Chance"),
            Map.entry(StatType.CRITICAL_STRIKE_DAMAGE_PERCENT, "+{value}% Critical Strike Damage"),

            // Utility / Defense
            Map.entry(StatType.DODGE_CHANCE_PERCENT,          "+{value}% Dodge Chance"),
            Map.entry(StatType.STABILITY_PERCENT,             "+{value}% Stability"),
            Map.entry(StatType.PARRY_WINDOW_FLAT,             "+{value} Parry Window"),
            Map.entry(StatType.BARRIER_ON_BLOCK,              "+{value}% Max Life as Barrier on Block"),

            // Ammo
            Map.entry(StatType.AMMO_FLAT,                     "+{value} Ammo"),
            Map.entry(StatType.AMMO_REGEN_PERCENT,            "+{value}% Ammo Regen"),

            // Leech — percent of damage dealt
            Map.entry(StatType.LIFE_LEECH_PERCENT,            "{value}% of Damage Dealt Leeched as Life"),
            Map.entry(StatType.MANA_LEECH_PERCENT,            "{value}% of Damage Dealt Leeched as Mana"),
            Map.entry(StatType.STAMINA_LEECH_PERCENT,         "{value}% of Damage Dealt Leeched as Stamina"),

            // Damage Taken From
            Map.entry(StatType.DAMAGE_TAKEN_FROM_MANA_PERCENT,    "{value}% Damage Taken from Mana"),
            Map.entry(StatType.DAMAGE_TAKEN_FROM_STAMINA_PERCENT, "{value}% Damage Taken from Stamina"),

            // Misc
            Map.entry(StatType.RUN_SPEED_PERCENT,             "+{value}% Run Speed")
    );

    // damage flat stat types that identify a weapon's base damage on the head/blade slot
    private static final java.util.Set<StatType> MAIN_HAND_DAMAGE_FLATS = java.util.Set.of(
            StatType.MAIN_HAND_PHYSICAL_DAMAGE_FLAT,
            StatType.MAIN_HAND_FIRE_DAMAGE_FLAT,
            StatType.MAIN_HAND_ICE_DAMAGE_FLAT,
            StatType.MAIN_HAND_LIGHTNING_DAMAGE_FLAT,
            StatType.MAIN_HAND_POISON_DAMAGE_FLAT,
            StatType.MAIN_HAND_MAGIC_DAMAGE_FLAT
    );

    private static final java.util.Set<StatType> OFF_HAND_DAMAGE_FLATS = java.util.Set.of(
            StatType.OFF_HAND_PHYSICAL_DAMAGE_FLAT,
            StatType.OFF_HAND_FIRE_DAMAGE_FLAT,
            StatType.OFF_HAND_ICE_DAMAGE_FLAT,
            StatType.OFF_HAND_LIGHTNING_DAMAGE_FLAT,
            StatType.OFF_HAND_POISON_DAMAGE_FLAT,
            StatType.OFF_HAND_MAGIC_DAMAGE_FLAT
    );

    // returns a friendly display string for the given stat and value e.g. "+5 Physical Damage"
    @Nonnull
    public static String getDisplay(@Nonnull StatType stat, float min, float max) {
        String template = DISPLAY_NAMES.getOrDefault(stat, stat.name());
        String minFormatted = min == (int) min ? String.valueOf((int) min) : String.format("%.2f", min);
        String maxFormatted = max == (int) max ? String.valueOf((int) max) : String.format("%.2f", max);
        String range = minFormatted.equals(maxFormatted) ? maxFormatted : (minFormatted + "-" + maxFormatted);
        return template.replace("{value}", range);
    }

    // rolls a float value inclusively between min and max
    public static float rollValue(float min, float max) {
        if (min == max) return min;
        double result = (double) min + RANDOM.nextDouble() * ((double) max - (double) min);
        return (float) result;
    }

    // returns true if this stat is a base weapon damage flat (main or off hand)
    public static boolean isWeaponDamageStat(@Nonnull StatType stat) {
        return MAIN_HAND_DAMAGE_FLATS.contains(stat) || OFF_HAND_DAMAGE_FLATS.contains(stat);
    }
}