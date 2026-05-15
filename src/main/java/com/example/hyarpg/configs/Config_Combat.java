package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Combat {
    @Key("enemy_base_damage")
    @Comment("This is the base damage an average enemy should do at level 1. This value is a baseline, enemies have their own combat type which will scale up or down based off this value. This value scales with the enemy level, x2 at level 2, x3 at level 3 and so on. Default: 3")
    public float enemy_base_damage = 3f;

    @Key("enemy_prefix_damage")
    @Comment("This is the additional flat damage amount, added to enemies that roll with a prefix, at level 1. Damage type is determined by the rolled prefix and this value scales with the enemy level, x2 at level 2, x3 at level 3 and so on. Default: 1")
    public float enemy_prefix_damage = 1f;

    @Key("level_diff_damage_multiplier")
    @Comment("Damage will scale by this multiplier, per level difference, between the attacker and defender. Default: 1.15")
    public float level_diff_damage_multiplier = 1.15f;

    @Key("rarity_diff_damage_multiplier")
    @Comment("Damage will scale by this multiplier, per rarity difference, between the attacker and defender. Default: 1.25")
    public float rarity_diff_damage_multiplier = 1.25f;

    @Key("base_parry_window_in_seconds")
    @Comment("Blocking within this time of the hit constitutes a parried hit. Default: 0.2 seconds")
    public double base_parry_window_in_seconds = 0.2;

    @Key("damage_to_player_multiplier")
    @Comment("A global multiplier changing how much damage players will take from enemies. Default: 1.0")
    public float damage_to_player_multiplier = 1.0f;

    @Key("damage_from_player_multiplier")
    @Comment("A global multiplier changing how much damage players will deal to enemies. Default: 1.0")
    public float damage_from_player_multiplier = 1.0f;

    @Key("broadcast_combat_logs_in_chat")
    @Comment("Rather or not to broadcast in chat when an entity damages another entity. Players can turn this off individually as well. Default: true")
    public boolean broadcast_combat_logs_in_chat = true;
}