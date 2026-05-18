package com.example.hyarpg.utils.jobs;

// Java Imports
import java.util.List;

public abstract class JobSkill {

    // pure data record for a job perk — immutable and concise
    public record JobPerk(String id, String displayName, int unlockLevel, String description) {}

    // job identity and display
    public abstract String getId();
    public abstract String getDisplayName();
    public abstract String getDescription();

    // perks unlockable in this job, ordered by unlock level
    public abstract List<JobPerk> getPerks();

    // return all perks the player has unlocked at the given job level
    public List<JobPerk> getUnlockedPerks(int level) {
        return getPerks().stream().filter(p -> level >= p.unlockLevel()).toList();
    }

    // check if the player has a specific perk by id at the given job level
    public boolean hasPerk(int level, String perkId) {
        return getPerks().stream().anyMatch(p -> p.id().equals(perkId) && level >= p.unlockLevel());
    }
}