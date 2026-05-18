package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Experience {
    @Key("xp_to_first_level")
    @Comment("Base XP required to reach level 2. All subsequent levels scale from this value. Default: 10")
    public int xp_to_first_level = 10;

    @Key("xp_increase_per_level_modifier")
    @Comment("Percentage increase in XP required per level. 0.15 = 15% more XP each level (e.g. 10, 11.5, 13.2...). Default: 0.15")
    public float xp_increase_per_level_modifier = .15f;

    @Key("xp_gained_from_equal_level_kill")
    @Comment("Base XP awarded for killing an enemy at the same level as the player. Scales up or down based on level difference. Default: 1")
    public int xp_gained_from_equal_level_kill = 1;

    @Key("xp_to_first_job_level")
    @Comment("Base XP required to reach job level 2. All subsequent job levels scale from this value. Default: 20")
    public int xp_to_first_job_level = 20;

    @Key("xp_increase_per_job_level_modifier")
    @Comment("Percentage increase in XP required per job level. 0.50 = 50% more XP each level (e.g. 20, 30, 45, 67...). Default: 0.50")
    public float xp_increase_per_job_level_modifier = 0.50f;
}