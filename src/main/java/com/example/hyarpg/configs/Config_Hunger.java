package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Hunger {
    @Key("enabled")
    @Comment("Whether the hunger system is active. Default: true")
    public boolean enabled = true;

    @Key("drain_rate")
    @Comment("How fast hunger depletes per tick. Default: 0.02")
    public double drain_rate = 0.02;

    @Key("seconds_till_death")
    @Comment("Seconds until death once hunger is fully depleted. Default: 60")
    public double seconds_till_death = 60;

    @Key("restore_t1_percent")
    @Comment("Percent of hunger restored by tier 1 food (0.0 to 1.0). Default: 0.10")
    public double restore_t1_percent = 0.10;

    @Key("restore_t2_percent")
    @Comment("Percent of hunger restored by tier 2 food (0.0 to 1.0). Default: 0.25")
    public double restore_t2_percent = 0.25;

    @Key("restore_t3_percent")
    @Comment("Percent of hunger restored by tier 3 food (0.0 to 1.0). Default: 0.66")
    public double restore_t3_percent = 0.66;
}