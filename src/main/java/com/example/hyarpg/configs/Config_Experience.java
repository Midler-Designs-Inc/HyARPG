package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Experience {
    @Key("xp_to_first_level")
    @Comment("The amount of XP required to reach go from level 1 to level 2. Used as the foundation for XP scaling. Default: 10")
    public int xp_to_first_level = 10;

    @Key("xp_increase_per_level_modifier")
    @Comment("The amount of XP required to reach the next level will increase each level by the modifier amount. Default: 1.33")
    public float xp_increase_per_level_modifier = 0.1f;

    @Key("xp_gained_from_equal_level_kill")
    @Comment("The amount of XP gained from killing an enemy at the same level as you. Default: 0.2 seconds")
    public int xp_gained_from_equal_level_kill = 1;
}