package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Thirst {
    @Key("enabled")
    @Comment("Whether the thirst system is active. Default: true")
    public boolean enabled = true;

    @Key("min_drain_rate")
    @Comment("Minimum amount thirst depletes per tick. Default: 0.01")
    public double min_drain_rate = 0.01;

    @Key("max_drain_rate")
    @Comment("Maximum amount thirst depletes per tick. Default: 0.03")
    public double max_drain_rate = 0.03;

    @Key("seconds_till_death")
    @Comment("Seconds until death once thirst is fully depleted. Default: 60")
    public double seconds_till_death = 60;

    @Key("restore_t1_percent")
    @Comment("Percent of thirst restored by tier 1 food (0.0 to 1.0). Default: 0.10")
    public double restore_t1_percent = 0.10;

    @Key("restore_t2_percent")
    @Comment("Percent of thirst restored by tier 2 food (0.0 to 1.0). Default: 0.25")
    public double restore_t2_percent = 0.25;

    @Key("restore_t3_percent")
    @Comment("Percent of thirst restored by tier 3 food (0.0 to 1.0). Default: 0.66")
    public double restore_t3_percent = 0.66;
}