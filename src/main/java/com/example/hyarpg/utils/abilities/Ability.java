package com.example.hyarpg.utils.abilities;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Ability {
    // static properties
    public final String abilityId;
    public final int abilityResourceStatIndex;
    public final float abilityResourceCost;
    public final boolean ultimateAbility;
    public final int cooldownSeconds;

    // instantiated properties
    private long lastUse;

    // constructor
    public Ability(String abilityId, int abilityResourceStatIndex, float abilityResourceCost, boolean ultimateAbility, int cooldownSeconds) {
        this.abilityId = abilityId;
        this.abilityResourceStatIndex = abilityResourceStatIndex;
        this.abilityResourceCost = abilityResourceCost;
        this.ultimateAbility = ultimateAbility;
        this.cooldownSeconds = cooldownSeconds;
    }

    // getter/setter for last user value
    public void setLastUse(long lastUse) { this.lastUse = lastUse; }
    public long getLastUse() { return lastUse; }

    /**
     * Default implementation does nothing.
     * Subclasses can override.
     */
    public void execute(Ref<EntityStore> ref) {
        // no-op
    }
}