package com.example.hyarpg.utils.affixes;

import java.util.concurrent.ThreadLocalRandom;

public final class Affix {

    private final String stat;
    private final String display;
    private final float minValue;
    private final float maxValue;
    private float value;
    private float tier = 5;

    // Constructor with setting set value
    public Affix(String stat, String display, float minValue, float maxValue) {
        this.stat = stat;
        this.display = display;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = rollValue();
    }

    public String stat() { return stat; }
    public String display() { return display; }
    public float value() { return value; }
    public float tier() { return tier; }

    // roll a random value between the min and max values (inclusive)
    private float rollValue() {
        return minValue + ThreadLocalRandom.current().nextFloat(0f, maxValue - minValue + 1f);
    }

    // roll the tier for this affix based on the item/enemy level passed in, 25% chance for T1 rolls to become T0 rolls
    public void rollTier(int level) {
        ThreadLocalRandom r = ThreadLocalRandom.current();

        // Determine bracket (0–5 max)
        int bracket = Math.min(level / 100, 5);

        // Lowest possible tier based on bracket
        int minTier = 5 - bracket;

        // Roll tier between minTier and 5 inclusive
        int rolledTier = r.nextInt(minTier, 6);

        // Special rule: if tier 1 is rolled AND level >= 500, 25% chance to upgrade to 0
        if (level >= 500 && rolledTier == 1) {
            if (r.nextFloat() < 0.25f) {
                rolledTier = 0;
            }
        }

        // update the tier on the affix
        this.tier = rolledTier;

        // multiply the value by the rolled tier
        this.value *= (6 - rolledTier);
    }
}