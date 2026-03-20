package com.example.hyarpg.utils.abilities;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class Ability {
    // static properties
    public final String abilityId;
    public final int abilityResourceStatIndex;
    public final float abilityResourceCost;
    public final boolean ultimateAbility;
    public final int cooldownSeconds;
    public final boolean isChanneled;
    public final List<String> requiredWeapons;

    // instantiated properties
    private long lastUse;

    // constructor
    public Ability(String abilityId, int abilityResourceStatIndex, float abilityResourceCost, boolean ultimateAbility, int cooldownSeconds, boolean isChanneled, List<String> requiredWeapons) {
        this.abilityId = abilityId;
        this.abilityResourceStatIndex = abilityResourceStatIndex;
        this.abilityResourceCost = abilityResourceCost;
        this.ultimateAbility = ultimateAbility;
        this.cooldownSeconds = cooldownSeconds;
        this.isChanneled = isChanneled;
        this.requiredWeapons = requiredWeapons;
    }

    // getter/setter for last user value
    public void setLastUse(long lastUse) { this.lastUse = lastUse; }
    public long getLastUse() { return lastUse; }

    // Execute fires when teh ability is triggered (override on children as needed)
    public void execute(Ref<EntityStore> ref) {}

    // Interaction vars changes parts of the ability's interaction (override on children as needed)
    @Nullable
    public Map<String, String> getInteractionVars(Ref<EntityStore> ref) { return null; }
}