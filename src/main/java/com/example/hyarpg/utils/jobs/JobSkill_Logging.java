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
            new JobPerk("LeafFinder1",   "Leaf Finder I",          5,   "Small chance for extra leaves when felling trees."),
            new JobPerk("StickBundler1", "Stick Bundler I",        10,  "Small chance for extra sticks when felling trees."),
            new JobPerk("Yield1",        "Better Yield I",         15,  "Yield +1 log when felling a tree."),
            new JobPerk("LeafFinder2",   "Leaf Finder II",         20,  "High chance for extra leaves when felling trees."),
            new JobPerk("StickBundler2", "Stick Bundler II",       25,  "High chance for extra sticks when felling trees."),
            new JobPerk("Durability1",   "Careful Swing I",        30,  "Logging axes lose slightly less durability while chopping trees."),
            new JobPerk("Yield2",        "Better Yield II",        35,  "Yield +3 logs when felling a tree."),
            new JobPerk("Yield3",        "Better Yield III",       40,  "Yield +5 logs when felling a tree."),
            new JobPerk("Durability2",   "Careful Swing II",       45,  "Logging axes lose less durability while chopping trees."),
            new JobPerk("Yield4",        "Better Yield IV",        50,  "Yield +7 logs when felling a tree."),
            new JobPerk("Yield5",        "Better Yield V",         55,  "Yield +9 logs when felling a tree."),
            new JobPerk("Durability3",   "Careful Swing III",      60,  "Logging axes lose greatly reduced durability while chopping trees."),
            new JobPerk("Yield6",        "Better Yield VI",        65,  "Yield +11 logs when felling a tree."),
            new JobPerk("Yield7",        "Better Yield VII",       70,  "Yield +13 logs when felling a tree."),
            new JobPerk("Durability4",   "Careful Swing IV",       75,  "Logging axes lose minimal durability while chopping trees."),
            new JobPerk("Yield8",        "Better Yield VIII",      80,  "Yield +16 logs when felling a tree."),
            new JobPerk("Yield9",        "Better Yield IX",        85,  "Yield +18 logs when felling a tree."),
            new JobPerk("Yield10",       "Better Yield X",         90,  "Yield +20 logs when felling a tree."),
            new JobPerk("Yield11",       "Better Yield XI",        95,  "Yield +22 logs when felling a tree."),
            new JobPerk("InstantFell",   "Titan's Swing",          100, "10% chance to instantly fell a tree with each hit.")
        );
    }
}