package com.example.hyarpg.utils;

// Java Imports
import java.util.EnumMap;
import java.util.Map;

public final class PlayerStats {

    /* Base Stats */
    private static final float BASE_LIFE = 100f;
    private static final float BASE_STAMINA = 10f;
    private static final float BASE_MANA = 25f;

    private static final float BASE_CRIT_DAMAGE = 1.5f;

    /* Caps */
    private static final float MAX_RESIST = 75f;
    private static final float MAX_DODGE = 75f;
    private static final float MAX_CRIT_CHANCE = 100f;

    private final Map<StatType, Float> stats = new EnumMap<>(StatType.class);

    public PlayerStats() {
        for (StatType type : StatType.values())
            stats.put(type, 0f);
    }

    public void add(StatType type, float value) {
        stats.put(type, stats.get(type) + value);
    }

    public float getRaw(StatType type) {
        return stats.get(type);
    }

    private float applyIncreased(float basePlusFlat, float percent) {
        return basePlusFlat * (1f + percent / 100f);
    }

    private float clamp(float value, float cap) {
        return Math.min(value, cap);
    }

    /* Resources */
    public float getLife() {
        return applyIncreased(
                BASE_LIFE + getRaw(StatType.LIFE_FLAT),
                getRaw(StatType.LIFE_PERCENT)
        );
    }
    public float getLifeRegen() {
        return applyIncreased(
                getRaw(StatType.LIFE_REGEN_FLAT),
                getRaw(StatType.LIFE_REGEN_PERCENT)
        );
    }
    public float getMana() {
        return applyIncreased(
                BASE_MANA + getRaw(StatType.MANA_FLAT),
                getRaw(StatType.MANA_PERCENT)
        );
    }
    public float getManaRegen() {
        return applyIncreased(
                getRaw(StatType.MANA_REGEN_FLAT),
                getRaw(StatType.MANA_REGEN_PERCENT)
        );
    }
    public float getStamina() {
        return applyIncreased(
                BASE_STAMINA + getRaw(StatType.STAMINA_FLAT),
                getRaw(StatType.STAMINA_PERCENT)
        );
    }
    public float getStaminaRegen() {
        return applyIncreased(
                getRaw(StatType.STAMINA_REGEN_FLAT),
                getRaw(StatType.STAMINA_REGEN_PERCENT)
        );
    }

    /* Damage */
    public float getFireDamage() {
        return applyIncreased(
                getRaw(StatType.FIRE_DAMAGE_FLAT),
                getRaw(StatType.FIRE_DAMAGE_PERCENT)
        );
    }
    public float getColdDamage() {
        return applyIncreased(
                getRaw(StatType.COLD_DAMAGE_FLAT),
                getRaw(StatType.COLD_DAMAGE_PERCENT)
        );
    }
    public float getLightningDamage() {
        return applyIncreased(
                getRaw(StatType.LIGHTNING_DAMAGE_FLAT),
                getRaw(StatType.LIGHTNING_DAMAGE_PERCENT)
        );
    }
    public float getPhysicalDamage() {
        return applyIncreased(
                getRaw(StatType.PHYSICAL_DAMAGE_FLAT),
                getRaw(StatType.PHYSICAL_DAMAGE_PERCENT)
        );
    }
    public float getMagicalDamage() {
        return applyIncreased(
                getRaw(StatType.MAGICAL_DAMAGE_FLAT),
                getRaw(StatType.MAGICAL_DAMAGE_PERCENT)
        );
    }

    /* Crit */
    public float getCriticalStrikeChance() {
        return clamp(getRaw(StatType.CRITICAL_STRIKE_CHANCE_PERCENT), MAX_CRIT_CHANCE);
    }
    public float getCriticalStrikeDamage() {
        return BASE_CRIT_DAMAGE * (1f +
                getRaw(StatType.CRITICAL_STRIKE_DAMAGE_PERCENT) / 100f);
    }

    /* Resistances */
    public float getFireResistance() {
        return clamp(getRaw(StatType.FIRE_RESIST_PERCENT), MAX_RESIST);
    }
    public float getColdResistance() {
        return clamp(getRaw(StatType.COLD_RESIST_PERCENT), MAX_RESIST);
    }
    public float getLightningResistance() {
        return clamp(getRaw(StatType.LIGHTNING_RESIST_PERCENT), MAX_RESIST);
    }
    public float getPoisonResistance() {
        return clamp(getRaw(StatType.POISON_RESIST_PERCENT), MAX_RESIST);
    }

    /* Utility */
    public float getDodgeChance() {
        return clamp(getRaw(StatType.DODGE_CHANCE_PERCENT), MAX_DODGE);
    }
    public float getRunSpeedMultiplier() {
        return 1f + getRaw(StatType.RUN_SPEED_PERCENT) / 100f;
    }
    public int getBonusJumps() {
        return Math.round(getRaw(StatType.JUMPS_FLAT));
    }
}