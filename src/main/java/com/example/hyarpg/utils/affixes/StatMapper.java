package com.example.hyarpg.utils.affixes;

public final class StatMapper {

    public static StatType fromAffixId(String affixId) {

        if (!affixId.startsWith("Stat_"))
            throw new IllegalArgumentException("Invalid affix id: " + affixId);

        String core = affixId.substring(5);
        String[] parts = core.split("_", 2);

        String modifierType = parts[0];
        String statPortion = parts[1];

        String enumBase = statPortion.toUpperCase();
        String suffix;

        switch (modifierType) {
            case "Flat": suffix = "_FLAT"; break;
            case "Increased": suffix = "_PERCENT"; break;
            default:
                throw new IllegalArgumentException("Unknown modifier: " + modifierType);
        }

        return StatType.valueOf(enumBase + suffix);
    }

}