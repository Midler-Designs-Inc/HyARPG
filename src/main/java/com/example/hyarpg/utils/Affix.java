package com.example.hyarpg.utils;

import javax.annotation.Nullable;

public final class Affix {

    private final String stat;
    private final String display;
    private final float minValue;
    private final float maxValue;
    private final float value;
    private final boolean scaleValues;

    // Constructor with setting set value
    public Affix(String stat, String display, float minValue, float maxValue, boolean scaleValues) {
        this.stat = stat;
        this.display = display;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.scaleValues = scaleValues;
        this.value = rollValue();
    }

    public String stat() { return stat; }
    public String display() { return display; }
    public float minValue() { return minValue; }
    public float maxValue() { return maxValue; }
    public boolean scaleValues() { return scaleValues; }
    public float value() { return value; }

    private float rollValue() {
        var r = java.util.concurrent.ThreadLocalRandom.current();
        return minValue + r.nextFloat() * (maxValue - minValue);
    }
}