package com.example.hyarpg.utils.skills;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.utils.affixes.StatType;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;

// Java Imports
import javax.annotation.Nullable;
import java.util.List;

public class SkillNode {

    // Initial Class Properties (if these change, version has to change and node has to be refunded)
    public final String id;
    public final String displayName;
    public final String iconId;
    public final int cost;
    public final int maxRanks;
    public final List<Requirement> requirements;
    public final String version;
    public final String abilityId;
    public final int abilityResourceStatIndex;
    public final float abilityResourceCost;
    public final boolean ultimateAbility;
    public final StatType statType;
    public final float statValuePerRank;

    // Internal Class Properties
    private int allocatedPoints = 0;
    private int currentRank = 0;
    private boolean isLocked = false;

    public SkillNode(String id, String displayName, String iconId, String abilityId, int abilityResourceStatIndex, float abilityResourceCost, boolean ultimateAbility, int cost, int maxRanks, List<Requirement> requirements, String version) {
        this.id = id;
        this.displayName = displayName;
        this.iconId = iconId;
        this.abilityId = abilityId;
        this.abilityResourceStatIndex = abilityResourceStatIndex;
        this.abilityResourceCost = abilityResourceCost;
        this.ultimateAbility = ultimateAbility;
        this.statType = null;
        this.statValuePerRank = 0;
        this.cost = cost;
        this.maxRanks = maxRanks;
        this.requirements = requirements;
        this.version = version;
    }

    public SkillNode(String id, String displayName, String iconId, @Nullable StatType statType, float statValuePerRank, int cost, int maxRanks, List<Requirement> requirements, String version) {
        this.id = id;
        this.displayName = displayName;
        this.iconId = iconId;
        this.abilityId = null;
        this.abilityResourceStatIndex = DefaultEntityStatTypes.getStamina();
        this.abilityResourceCost = 0;
        this.statType = statType;
        this.statValuePerRank = statValuePerRank;
        this.cost = cost;
        this.maxRanks = maxRanks;
        this.requirements = requirements;
        this.ultimateAbility = false;
        this.version = version;
    }

    // get the internal properties
    public int getAllocatedPoints () { return allocatedPoints; }
    public int getCurrentRank () { return currentRank; }
    public boolean getIsLocked () { return isLocked; }
    public String getVersion() { return version; }

    // allocate points
    public int allocatePoints(int availablePoints) {
        // if the rank is maxed, or the passed points are less than cost, return false
        if(currentRank >= maxRanks || availablePoints < cost) return availablePoints;

        // otherwise allocate the points and increment the rank
        allocatedPoints += cost;
        currentRank++;

        // return true
        return availablePoints - cost;
    }

    // refund this node
    public int refund(Component_RPG_Player comp) {
        // Get a refence to the amount of points allocated into this node
        int refundPoints = allocatedPoints;

        this.allocatedPoints = 0;
        this.currentRank = 0;

        // check if this node is an equipped ability and if so, remove it
        if (id.equals(comp.ultimateAbility)) {
            comp.ultimateAbility = null;
            comp.ultimateAbilityIcon = null;

        } else if (id.equals(comp.primaryAbility)) {
            comp.primaryAbility = null;
            comp.primaryAbilityIcon = null;

        } else if (id.equals(comp.secondaryAbility)) {
            comp.secondaryAbility = null;
            comp.secondaryAbilityIcon = null;
        }

        return refundPoints;
    }

    // lock this node
    public void lock() { this.isLocked = true; }

    // try to unlock this node
    public void unlock() {
        this.isLocked = false;
    }
}
