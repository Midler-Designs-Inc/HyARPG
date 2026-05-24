package com.example.hyarpg.utils.jobs;

// Java Imports
import java.util.List;

public class JobSkill_Logging extends JobSkill {

    public static final JobSkill_Logging INSTANCE = new JobSkill_Logging();
    private JobSkill_Logging() {}

    @Override public String getId()          { return "Logging"; }
    @Override public String getDisplayName() { return "Logging"; }
    @Override public String getDescription() { return "The practice of chopping trees. Higher levels improve yield and unlock new harvesting abilities."; }

    @Override
    public List<JobPerk> getPerks() {
        return List.of(
            new JobPerk("LeafFinder",    1,  5,   "Leaf Finder I",        "Acquire a small amount of plant fiber when picking up chopped logs."),
            new JobPerk("StickBundler",  1,  10,  "Stick Bundler I",      "Acquire a small amount of sticks when picking up chopped logs."),
            new JobPerk("Yield",         1,  15,  "Better Yield I",       "You have a 10% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("LeafFinder",    2,  20,  "Leaf Finder II",       "Acquire a large amount of plant fiber when picking up chopped logs."),
            new JobPerk("StickBundler",  2,  25,  "Stick Bundler II",     "Acquire a large amount of sticks when picking up chopped logs."),
            new JobPerk("Durability",    1,  30,  "Careful Swing I",      "Hatchets lose slightly less durability while chopping trees."),
            new JobPerk("Yield",         2,  35,  "Better Yield II",      "You have a 20% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("Yield",         3,  40,  "Better Yield III",     "You have a 30% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("Durability",    2,  45,  "Careful Swing II",     "Hatchets lose less durability while chopping trees."),
            new JobPerk("Yield",         4,  50,  "Better Yield IV",      "You have a 40% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("Yield",         5,  55,  "Better Yield V",       "You have a 50% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("Durability",    3,  60,  "Careful Swing III",    "Hatchets lose greatly reduced durability while chopping trees."),
            new JobPerk("Yield",         6,  65,  "Better Yield VI",      "You have a 60% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("Yield",         7,  70,  "Better Yield VII",     "You have a 70% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("Durability",    4,  75,  "Careful Swing IV",     "Hatchets lose minimal durability while chopping trees."),
            new JobPerk("Yield",         8,  80,  "Better Yield VIII",    "You have a 80% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("InstantFell",   1,  85,  "Instant Fell I",       "You have a 25% chance to instantly break wood trunks."),
            new JobPerk("Yield",         9, 90,  "Better Yield X",        "You have a 90% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("Yield",         10, 95,  "Better Yield XI",      "You have a 100% chance to gain extra logs when picking up chopped logs."),
            new JobPerk("InstantFell",   2,  100, "Instant Fell II",      "You have a 50% chance to instantly break wood trunks.")
        );
    }
}