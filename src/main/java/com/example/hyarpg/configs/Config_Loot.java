package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Loot {
    @Key("loot_drop_chance_modifier")
    @Comment("Modifies the chances for loot to drop for participating players when killing an enemy. Default: 1.0")
    public float loot_drop_chance_modifier = 1.0f;

    @Key("recipe_drop_chance_modifier")
    @Comment("Modifies the chances for recipes to drop for participating players when killing an enemy. Default: 1.0")
    public float recipe_drop_chance_modifier = 1.0f;

}