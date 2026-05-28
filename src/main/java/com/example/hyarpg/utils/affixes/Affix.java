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

        // determine which tier bracket this level falls into (0–5), capped at 5
        int bracket = Math.min(level / 20, 5);

        // higher bracket = lower minimum tier (better rolls available)
        int minTier = 5 - bracket;

        // roll a tier between the minimum and 5 inclusive
        int rolledTier = r.nextInt(minTier, 6);

        // at high levels, tier 1 rolls have a 25% chance to upgrade to the best tier (0)
        if (level >= 500 && rolledTier == 1) {
            if (r.nextFloat() < 0.25f) {
                rolledTier = 0;
            }
        }

        // store the final rolled tier
        this.tier = rolledTier;

        // roll a fresh base value and scale it by tier multiplier
        this.value = rollValue() * (6 - rolledTier);
    }
}