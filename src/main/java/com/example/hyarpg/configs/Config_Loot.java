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

    @Key("broadcast_drops_in_chat")
    @Comment("Rather or not to broadcast in chat when a player gets a rolled drop off an enemy. Will only broadcast to other players that participated in the kill. Players can turn off individually as well. Default: true")
    public boolean broadcast_drops_in_global_chat = true;

}