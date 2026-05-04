package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Enemies {
    @Key("blocks_per_level_threshold")
    @Comment("Enemy levels will increase by 1 every X blocks away from XYZ[0, 100, 0]. Default: 500")
    public int blocks_per_level_threshold = 500;

    @Key("random_level_offset")
    @Comment("The +/- random level variance of enemies (base level determined by distance). Default: 5")
    public int random_level_offset = 5;

    @Key("prefix_chance")
    @Comment("The chance an enemy will spawn with 1 of 6 prefixes. Prefixes grant additional flat damage of a type determined by the rolled prefix. Default: 0.25")
    public float prefix_chance = 0.25f;

    @Key("show_enemy_nameplates")
    @Comment("Rather or not to display enemy names and level above their head. Default: true")
    public boolean show_enemy_nameplates = true;

    @Key("clear_enemy_nameplates")
    @Comment("Remove existing enemy nameplates. WARNING: This may interfere with other mods. Use it if needed, then turn it off again. Default: false")
    public boolean clear_enemy_nameplates = false;
}