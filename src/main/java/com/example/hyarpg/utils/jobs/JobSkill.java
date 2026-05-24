package com.example.hyarpg.utils.jobs;

// Java Imports
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class JobSkill {

    // pure data record for a job perk — immutable and concise
    public record JobPerk(String id, int tier,  int unlockLevel, String displayName, String description) {}

    // job identity and display
    public abstract String getId();
    public abstract String getDisplayName();
    public abstract String getDescription();

    // perks unlockable in this job, ordered by unlock level
    public abstract List<JobPerk> getPerks();

    // return only the highest tier unlocked perk per unique ID
    public Map<String, JobPerk> getUnlockedPerks(int level) {
        // create an empty map to store the filtered perks into
        Map<String, JobPerk> highestPerTier = new HashMap<>();

        // loop over perks
        for (JobPerk perk : getPerks()) {
            // filter out those that are not unlocked by level yet
            if (level < perk.unlockLevel()) continue;

            // check if this perk already exists in the map,
            // if not add it or replace it with a higher tier
            JobPerk existing = highestPerTier.get(perk.id());

            if (existing == null || perk.tier() > existing.tier()) {
                highestPerTier.put(perk.id(), perk);
            }
        }

        // return the filtered map
        return highestPerTier;
    }

}