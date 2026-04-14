package com.example.hyarpg.utils.affixes;

public final class StatMapper {

    public static StatType fromAffixId(String affixId) {
        if (!affixId.startsWith("Stat_")) throw new IllegalArgumentException("Invalid affix id: " + affixId);

        String core = affixId.substring(5);
        String[] parts = core.split("_", 2);
        String modifierType = parts[0];
        String statPortion = parts[1].toUpperCase();

        String suffix = switch (modifierType) {
            case "Flat"      -> "_FLAT";
            case "Increased" -> "_PERCENT";
            default -> throw new IllegalArgumentException("Unknown modifier: " + modifierType);
        };

        // handle special cases where the enum name doesn't follow the simple pattern
        String enumName = switch (statPortion + suffix) {
            case "BARRIER_ON_BLOCK_PERCENT" -> "BARRIER_ON_BLOCK";
            case "SHIELD_STABILITY_PERCENT" -> "SHIELD_STABILITY_PERCENT";
            case "LIFE_LEECH_PERCENT"       -> "LIFE_LEECH_PERCENT";
            case "MANA_LEECH_PERCENT"       -> "MANA_LEECH_PERCENT";
            case "STAMINA_LEECH_PERCENT"    -> "STAMINA_LEECH_PERCENT";
            case "DAMAGE_TAKEN_FROM_MANA_PERCENT"    -> "DAMAGE_TAKEN_FROM_MANA_PERCENT";
            case "DAMAGE_TAKEN_FROM_STAMINA_PERCENT" -> "DAMAGE_TAKEN_FROM_STAMINA_PERCENT";
            case "AMMO_REGEN_PERCENT"       -> "AMMO_REGEN_PERCENT";
            case "AMMO_FLAT"                -> "AMMO_FLAT";
            default -> statPortion + suffix;
        };

        return StatType.valueOf(enumName);
    }
}