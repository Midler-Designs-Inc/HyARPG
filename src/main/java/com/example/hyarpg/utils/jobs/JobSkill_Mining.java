package com.example.hyarpg.utils.jobs;

// Java Imports
import java.util.List;

public class JobSkill_Mining extends JobSkill {

    public static final JobSkill_Mining INSTANCE = new JobSkill_Mining();
    private JobSkill_Mining() {}

    @Override public String getId()          { return "Mining"; }
    @Override public String getDisplayName() { return "Mining"; }
    @Override public String getDescription() { return "The practice of mining ores. Higher levels improve yield and unlock new harvesting abilities."; }

    @Override
    public List<JobPerk> getPerks() {
        return List.of(
            new JobPerk("LeafFinder",    1,  5,   "Leaf Finder I",        ""),
            new JobPerk("StickBundler",  1,  10,  "Stick Bundler I",      ""),
            new JobPerk("Yield",         1,  15,  "Better Yield I",       "Gain +10% more ore when mining ore."),
            new JobPerk("LeafFinder",    2,  20,  "Leaf Finder II",       ""),
            new JobPerk("StickBundler",  2,  25,  "Stick Bundler II",     ""),
            new JobPerk("Durability",    1,  30,  "Careful Swing I",      "Pickaxes lose slightly less durability while mining ore."),
            new JobPerk("Yield",         2,  35,  "Better Yield II",      "Gain +20% more ore when mining ore."),
            new JobPerk("Yield",         3,  40,  "Better Yield III",     "Gain +30% more ore when mining ore."),
            new JobPerk("Durability",    2,  45,  "Careful Swing II",     "Pickaxes lose less durability while mining ore."),
            new JobPerk("Yield",         4,  50,  "Better Yield IV",      "Gain +40% more ore when mining ore."),
            new JobPerk("Yield",         5,  55,  "Better Yield V",       "Gain +50% more ore when mining ore."),
            new JobPerk("Durability",    3,  60,  "Careful Swing III",    "Pickaxes lose greatly reduced durability while mining ore."),
            new JobPerk("Yield",         6,  65,  "Better Yield VI",      "Gain +60% more ore when mining ore."),
            new JobPerk("Yield",         7,  70,  "Better Yield VII",     "Gain +70% more ore when mining ore."),
            new JobPerk("Durability",    4,  75,  "Careful Swing IV",     "Pickaxes lose minimal durability while mining ore."),
            new JobPerk("Yield",         8,  80,  "Better Yield VIII",    "Gain +80% more ore when mining ore."),
            new JobPerk("InstantFell",   1,  85,  "Instant Fell I",       "Gain +25% chance to instantly break ore nodes."),
            new JobPerk("Yield",         9, 90,  "Better Yield X",        "Gain +90% more ore when mining ore."),
            new JobPerk("Yield",         10, 95,  "Better Yield XI",      "Gain +100% more ore when mining ore."),
            new JobPerk("InstantFell",   2,  100, "Instant Fell II",      "Gain +50% chance to instantly break ore nodes.")
        );
    }
}