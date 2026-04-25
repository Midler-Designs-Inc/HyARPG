package com.example.hyarpg.utils.affixes;

// Java Imports
import java.util.EnumMap;
import java.util.Map;

public final class EntityStats {

    /* Base Stats */
    private static final float BASE_CRIT_DAMAGE = 1.5f;

    /* Caps */
    public static final float MAX_RESIST = 75f;
    public static final float MAX_DODGE = 75f;
    public static final float MAX_CRIT_CHANCE = 100f;
    public static final float MAX_STABILITY = 90f;
    public static final float MAX_BARRIER_ON_BLOCK = 100f;

    private final Map<StatType, Float> stats = new EnumMap<>(StatType.class);

    public EntityStats() {
        for (StatType type : StatType.values())
            stats.put(type, 0f);
    }

    public void add(StatType type, float value) {
        stats.put(type, stats.get(type) + value);
    }

    public float getRaw(StatType type) {
        return stats.get(type);
    }

    private float clamp(float value, float cap) {
        return Math.min(value, cap);
    }

    // function to merge two entity stat classes together
    public void merge(EntityStats other) {
        for (StatType type : StatType.values()) {
            float otherValue = other.getRaw(type);
            if (otherValue != 0f) add(type, otherValue);
        }
    }

    /* Resources */
    public float getFlatResource(String resourceId) {
        if (resourceId == null) return 0f;
        switch (resourceId) {
            case "Life": return getRaw(StatType.LIFE_FLAT);
            case "Mana": return getRaw(StatType.MANA_FLAT);
            case "Stamina": return getRaw(StatType.STAMINA_FLAT);
            default: return 0f;
        }
    }
    public float getIncreasedResource(String resourceId) {
        if (resourceId == null) return 0f;
        switch (resourceId) {
            case "Life": return getRaw(StatType.LIFE_PERCENT);
            case "Mana": return getRaw(StatType.MANA_PERCENT);
            case "Stamina": return getRaw(StatType.STAMINA_PERCENT);
            default: return 0f;
        }
    }

    /* Resource Regens */
    public float getFlatResourceRegen(String resourceId) {
        if (resourceId == null) return 0f;
        switch (resourceId) {
            case "Life": return getRaw(StatType.LIFE_REGEN_FLAT);
            case "Mana": return getRaw(StatType.MANA_REGEN_FLAT);
            case "Stamina": return getRaw(StatType.STAMINA_REGEN_FLAT);
            default: return 0f;
        }
    }
    public float getIncreasedResourceRegen(String resourceId) {
        if (resourceId == null) return 0f;
        switch (resourceId) {
            case "Life": return getRaw(StatType.LIFE_REGEN_PERCENT);
            case "Mana": return getRaw(StatType.MANA_REGEN_PERCENT);
            case "Stamina": return getRaw(StatType.STAMINA_REGEN_PERCENT);
            default: return 0f;
        }
    }

    /* Damage */
    public float getFlatDamage(String damageCause) {
        if (damageCause == null) return 0f;
        switch (damageCause) {
            case "Fire": return getRaw(StatType.FIRE_DAMAGE_FLAT);
            case "Ice": return getRaw(StatType.ICE_DAMAGE_FLAT);
            case "Lightning": return getRaw(StatType.LIGHTNING_DAMAGE_FLAT);
            case "Poison": return getRaw(StatType.POISON_DAMAGE_FLAT);
            case "Magic": return getRaw(StatType.MAGIC_DAMAGE_FLAT);
            case "Physical": return getRaw(StatType.PHYSICAL_DAMAGE_FLAT);
            default: return 0f;
        }
    }
    public float getIncreasedDamage(String damageCause) {
        if (damageCause == null) return 0f;
        switch (damageCause) {
            case "Fire": return getRaw(StatType.FIRE_DAMAGE_PERCENT);
            case "Ice": return getRaw(StatType.ICE_DAMAGE_PERCENT);
            case "Lightning": return getRaw(StatType.LIGHTNING_DAMAGE_PERCENT);
            case "Poison": return getRaw(StatType.POISON_DAMAGE_PERCENT);
            case "Magic": return getRaw(StatType.MAGIC_DAMAGE_PERCENT);
            case "Physical": return getRaw(StatType.PHYSICAL_DAMAGE_PERCENT);

            /* Weapon Damages */
            case "Axe": return getRaw(StatType.AXE_DAMAGE_PERCENT);
            case "Battleaxe": return getRaw(StatType.BATTLEAXE_DAMAGE_PERCENT);
            case "Club": return getRaw(StatType.CLUB_DAMAGE_PERCENT);
            case "Daggers": return getRaw(StatType.DAGGERS_DAMAGE_PERCENT);
            case "Kunai": return getRaw(StatType.KUNAI_DAMAGE_PERCENT);
            case "Longsword": return getRaw(StatType.LONGSWORD_DAMAGE_PERCENT);
            case "Mace": return getRaw(StatType.MACE_DAMAGE_PERCENT);
            case "Shortbow": return getRaw(StatType.SHORTBOW_DAMAGE_PERCENT);
            case "Crossbow": return getRaw(StatType.CROSSBOW_DAMAGE_PERCENT);
            case "Sword": return getRaw(StatType.SWORD_DAMAGE_PERCENT);
            default: return 0f;
        }
    }

    /* Resistances */
    public float getResistance(String damageCause) {
        if (damageCause == null) return 0f;
        switch (damageCause) {
            case "Fire": return clamp(
                getRaw(StatType.FIRE_RESIST_PERCENT) + getRaw(StatType.ELEMENTAL_RESIST_PERCENT),
                MAX_RESIST
            );
            case "Ice": return clamp(
                getRaw(StatType.ICE_RESIST_PERCENT) + getRaw(StatType.ELEMENTAL_RESIST_PERCENT),
                MAX_RESIST
            );
            case "Lightning": return clamp(
                getRaw(StatType.LIGHTNING_RESIST_PERCENT) + getRaw(StatType.ELEMENTAL_RESIST_PERCENT),
                MAX_RESIST
            );
            case "Poison": return clamp(getRaw(StatType.POISON_RESIST_PERCENT), MAX_RESIST);
            case "Magic": return clamp(getRaw(StatType.MAGIC_RESIST_PERCENT), MAX_RESIST);
            case "Physical": return clamp(getRaw(StatType.PHYSICAL_RESIST_PERCENT), MAX_RESIST);
            case "Elemental": return clamp(getRaw(StatType.ELEMENTAL_RESIST_PERCENT), MAX_RESIST);
            case "Fall": return clamp(getRaw(StatType.FALL_RESIST_PERCENT), 100);
            default: return 0f;
        }
    }

    // Crit
    public float getCriticalStrikeChance() {
        return clamp(getRaw(StatType.CRITICAL_STRIKE_CHANCE_PERCENT), MAX_CRIT_CHANCE);
    }
    public float getCriticalStrikeDamage() {
        return BASE_CRIT_DAMAGE * (1f +
                getRaw(StatType.CRITICAL_STRIKE_DAMAGE_PERCENT) / 100f);
    }

    // Ammo
    public float getAddedAmmo() {
        return getRaw(StatType.AMMO_FLAT);
    }
    public float getAmmoRegenPercent() {
        return getRaw(StatType.AMMO_REGEN_PERCENT);
    }

    // Leech
    public float getLeech(String resource) {
        if (resource == null) return 0f;
        return switch (resource) {
            case "Life"    -> getRaw(StatType.LIFE_LEECH_PERCENT);
            case "Mana"    -> getRaw(StatType.MANA_LEECH_PERCENT);
            case "Stamina" -> getRaw(StatType.STAMINA_LEECH_PERCENT);
            default -> 0f;
        };
    }

    // Damage taken from
    public float getDamageTakenFrom(String resource) {
        if (resource == null) return 0f;
        return switch (resource) {
            case "Mana"    -> getRaw(StatType.DAMAGE_TAKEN_FROM_MANA_PERCENT);
            case "Stamina" -> getRaw(StatType.DAMAGE_TAKEN_FROM_STAMINA_PERCENT);
            default -> 0f;
        };
    }

    // Marks
    public int getFlatApplyMarks(String markType) {
        if (markType == null) return 0;

        switch (markType) {
            case "Assassin": return (int) getRaw(StatType.APPLY_ASSASSIN_MARK_FLAT);
            default: return 0;
        }
    }

    // Utility
    public float getDodgeChance() {
        return clamp(getRaw(StatType.DODGE_CHANCE_PERCENT), MAX_DODGE);
    }
    public float getRunSpeedPercent() {
        return getRaw(StatType.RUN_SPEED_PERCENT);
    }
    public float getStabilityPercent(boolean shieldEquipped) {
        // get base stability and apply the shield stability buff if applicable
        float stabilityBase = getRaw(StatType.STABILITY_PERCENT);
        if(shieldEquipped) stabilityBase += getRaw(StatType.SHIELD_STABILITY_PERCENT);

        return clamp(stabilityBase, MAX_STABILITY);
    }
    public float getParryWindow() {
        return getRaw(StatType.PARRY_WINDOW_FLAT);
    }
    public float getBarrierOnBlock() { return clamp(getRaw(StatType.BARRIER_ON_BLOCK), MAX_BARRIER_ON_BLOCK); }
}