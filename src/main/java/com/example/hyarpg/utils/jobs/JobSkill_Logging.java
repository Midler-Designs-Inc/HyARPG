package com.example.hyarpg.utils.jobs;

// Java Imports
import java.util.List;

public class JobSkill_Logging extends JobSkill {

    public static final JobSkill_Logging INSTANCE = new JobSkill_Logging();
    private JobSkill_Logging() {}

    @Override public String getId()          { return "Logging"; }
    @Override public String getDisplayName() { return "Logging"; }
    @Override public String getDescription() { return "The art of felling trees. Higher levels improve yield and unlock new harvesting abilities."; }

    @Override
    public List<JobPerk> getPerks() {
        return List.of(
            new JobPerk("LeafFinder",    1,  5,   "Leaf Finder I",        "Yield +10 plant fiber when felling trees."),
            new JobPerk("StickBundler",  1,  10,  "Stick Bundler I",      "Yield +10 sticks when felling trees."),
            new JobPerk("Yield",         1,  15,  "Better Yield I",       "Yield +1 log when felling a tree."),
            new JobPerk("LeafFinder",    2,  20,  "Leaf Finder II",       "Yield +20 plant fiber when felling trees."),
            new JobPerk("StickBundler",  2,  25,  "Stick Bundler II",     "Yield +20 sticks when felling trees."),
            new JobPerk("Durability",    1,  30,  "Careful Swing I",      "Logging axes lose slightly less durability while chopping trees."),
            new JobPerk("Yield",         2,  35,  "Better Yield II",      "Yield +2 logs when felling a tree."),
            new JobPerk("Yield",         3,  40,  "Better Yield III",     "Yield +3 logs when felling a tree."),
            new JobPerk("Durability",    2,  45,  "Careful Swing II",     "Logging axes lose less durability while chopping trees."),
            new JobPerk("Yield",         4,  50,  "Better Yield IV",      "Yield +4 logs when felling a tree."),
            new JobPerk("Yield",         5,  55,  "Better Yield V",       "Yield +5 logs when felling a tree."),
            new JobPerk("Durability",    3,  60,  "Careful Swing III",    "Logging axes lose greatly reduced durability while chopping trees."),
            new JobPerk("Yield",         6,  65,  "Better Yield VI",      "Yield +6 logs when felling a tree."),
            new JobPerk("Yield",         7,  70,  "Better Yield VII",     "Yield +7 logs when felling a tree."),
            new JobPerk("Durability",    4,  75,  "Careful Swing IV",     "Logging axes lose minimal durability while chopping trees."),
            new JobPerk("Yield",         8,  80,  "Better Yield VIII",    "Yield +8 logs when felling a tree."),
            new JobPerk("Yield",         9,  85,  "Better Yield IX",      "Yield +9 logs when felling a tree."),
            new JobPerk("Yield",         10, 90,  "Better Yield X",       "Yield +10 logs when felling a tree."),
            new JobPerk("Yield",         11, 95,  "Better Yield XI",      "Yield +11 logs when felling a tree."),
            new JobPerk("InstantFell",   1,  100, "Titan's Swing",        "10% chance to instantly break")
        );
    }
}