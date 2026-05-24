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
    public int xp_to_first_job_level = 100;

    @Key("xp_increase_per_job_level_modifier")
    @Comment("Percentage increase in XP required per job level. 0.03 = 3% more XP required. Default: 0.03")
    public float xp_increase_per_job_level_modifier = 0.03f;

    @Key("xp_increase_from_minor_activity")
    @Comment("How much XP a player should get from performing a minor activity related to a job skill. Ex: Damaging an ore block awards minor XP. Default: 1")
    public int xp_increase_from_minor_activity = 1;

    @Key("xp_increase_from_major_activity")
    @Comment("How much XP a player should get from performing a major activity related to a job skill. Ex: Breaking an ore block awards major XP. Default: 10")
    public int xp_increase_from_major_activity = 10;
}