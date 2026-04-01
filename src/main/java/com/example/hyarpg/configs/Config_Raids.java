package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Raids {
    @Key("allow_base_raids")
    @Comment("Determines if players bases should be periodically raided by enemies. Disabling allow_light_well_territory_claim will disable base raiding regardless of this settings. Default: true")
    public boolean allow_base_raids = true;

    @Key("allow_player_raids")
    @Comment("Determines if players should be periodically raided by enemies. This is offered as an alternative to base raiding, but is not the intended as the default experience. This can also be enabled on top of base raiding if desired. Default: false")
    public boolean allow_player_raids = false;

    @Key("raid_timer_in_minutes")
    @Comment("Determines the amount of time (after a player has been raided) that must occur before a player is applicable to be raided again. Default: 45")
    public int raid_timer_in_minutes = 45;

    @Key("raid_chance")
    @Comment("Determines the chance a raid will occur. This is rolled once per minute for each player who's raid timer is up. On success the player is raided and their raid timer is reset.  Default: 10")
    public int raid_chance = 10;
}