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
            new JobPerk("Yield",         1,  5,   "Better Yield I",         "10% chance to gain an additional ore when mining ore."),
            new JobPerk("Durability",    1,  10,  "Careful Swing I",        "Pickaxes lose slightly less durability while mining ore."),
            new JobPerk("Yield",         2,  15,  "Better Yield II",        "20% chance to gain an additional ore when mining ore."),
            new JobPerk("ShardFinder1",  1,  20,  "Shard Finder Uncommon",  "25% chance to gain an Uncommon shard when mining ore."),
            new JobPerk("Yield",         3,  25,  "Better Yield III",       "30% chance to gain an additional ore when mining ore."),
            new JobPerk("Durability",    2,  30,  "Careful Swing II",       "Pickaxes lose less durability while mining ore."),
            new JobPerk("Yield",         4,  35,  "Better Yield IV",        "40% chance to gain an additional ore when mining ore."),
            new JobPerk("Yield",         5,  40,  "Better Yield V",         "50% chance to gain an additional ore when mining ore."),
            new JobPerk("ShardFinder2",  2,  45,  "Shard Finder Rare",      "15% chance to gain a Rare shard when mining ore."),
            new JobPerk("Durability",    3,  50,  "Careful Swing III",      "Pickaxes lose greatly reduced durability while mining ore."),
            new JobPerk("Yield",         6,  55,  "Better Yield VI",        "60% chance to gain an additional ore when mining ore."),
            new JobPerk("Yield",         7,  60,  "Better Yield VII",       "70% chance to gain an additional ore when mining ore."),
            new JobPerk("ShardFinder3",  3,  65,  "Shard Finder Epic",      "7% chance to gain an Epic shard when mining ore."),
            new JobPerk("Durability",    4,  70,  "Careful Swing IV",       "Pickaxes lose minimal durability while mining ore."),
            new JobPerk("Yield",         8,  75,  "Better Yield VIII",      "80% chance to gain an additional ore when mining ore."),
            new JobPerk("Yield",         9,  80,  "Better Yield XI",        "90% chance to gain an additional ore when mining ore."),
            new JobPerk("Yield",         10, 85,  "Better Yield X",         "100% chance to gain an additional ore when mining ore."),
            new JobPerk("ShardFinder4",  4,  90,  "Shard Finder Legendary", "3% chance to gain a Legendary shard when mining ore."),
            new JobPerk("Yahtzee",       1,  95,  "Yahtzee I",              "50% chance to gain a second additional ore when mining ore."),
            new JobPerk("BonusYahtzee",  1,  95,  "Bonus Yahtzee I",        "50% chance to gain a third additional ore when mining ore.")
        );
    }
}