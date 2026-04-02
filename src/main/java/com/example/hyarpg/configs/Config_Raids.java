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

    @Key("raid_chance")
    @Comment("Determines the chance a raid will occur. This is rolled once per minute for each player who's raid timer is up. On success the player is raided and their raid timer is reset.  Default: 10")
    public int raid_chance = 10;

    @Key("raid_cooldown_in_minutes")
    @Comment("Determines the amount of time (after a player has been raided) that must occur before a player is applicable to be raided again. Default: 45")
    public int raid_cooldown_in_minutes = 45;

    @Key("seconds_before_first_wave")
    @Comment("Determines how many seconds should pass before the first wave is spawned. Default: 30")
    public int seconds_before_first_wave = 30;

    @Key("seconds_between_waves")
    @Comment("Determines how many seconds should pass between each wave spawn. Default: 60")
    public int seconds_between_waves = 60;

    @Key("seconds_after_last_wave_before_raid_end")
    @Comment("Determines how many seconds should pass after the last wave is spawned before the raid should end. If unkilled_raid_enemies_explode is true, any enemies spawned for the raid, that are left alive when the raid ends, will explode damaging nearby players and blocks.  Default: 300")
    public int seconds_after_last_wave_before_raid_end = 300;

    @Key("unkilled_raid_enemies_explode")
    @Comment("Determines if unkilled raid enemies should explode when the raid ends. Explosions damage blocks and entities. This is intended to add a real level of danger to the raids encouraging players not to ignore them. Default: true")
    public boolean unkilled_raid_enemies_explode = true;

    @Key("explosion_hit_radius_blocks")
    @Comment("Determines the radius (measured in blocks) the unkilled enemy explosions should hit blocks. Default: 4")
    public int explosion_hit_radius_blocks = 4;

    @Key("explosion_hit_radius_entities")
    @Comment("Determines the radius (measured in ??) the unkilled enemy explosions should hit players. Default: 6")
    public float explosion_hit_radius_entities = 6;

    @Key("explosion_hit_damage_blocks")
    @Comment("Determines the damage the unkilled enemy explosions should do to blocks. Default: 40")
    public float explosion_hit_damage_blocks = 40;

    @Key("explosion_hit_damage_entities")
    @Comment("Determines the damage the unkilled enemy explosions should do to players. Default: 40")
    public float explosion_hit_damage_entities = 40;
}