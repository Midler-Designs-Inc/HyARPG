package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Combat {
    @Key("level_diff_damage_multiplier")
    @Comment("Damage will scale by this multiplier, per level difference, between the attacker and defender. Default: 1.15")
    public float level_diff_damage_multiplier = 1.15f;

    @Key("rarity_diff_damage_multiplier")
    @Comment("Damage will scale by this multiplier, per rarity difference, between the attacker and defender. Default: 1.33")
    public float rarity_diff_damage_multiplier = 1.33f;

    @Key("base_parry_window_in_seconds")
    @Comment("Blocking within this time of the hit constitutes a parried hit. Default: 0.2 seconds")
    public long base_parry_window_in_seconds = 200_000_000L;

    @Key("damage_to_player_multiplier")
    @Comment("A multiplier changing how much damage players will take from enemies. Default: 1.0")
    public float damage_to_player_multiplier = 1.0f;

    @Key("damage_from_player_multiplier")
    @Comment("A multiplier changing how much damage players will deal to enemies. Default: 1.0")
    public float damage_from_player_multiplier = 1.0f;
}