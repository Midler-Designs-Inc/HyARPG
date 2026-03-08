package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Combat {
    @Key("level_diff_damage_multiplier")
    @Comment("Damage will scale by this multiplier, per level difference, between the attacker and defender. Default: 1.15")
    public float level_diff_damage_multiplier = 1.15f;

    @Key("rarity_diff_damage_multiplier")
    @Comment("Damage will scale by this multiplier, per rarity difference, between the attacker and defender Default: 1.33")
    public float rarity_diff_damage_multiplier = 1.33f;
}