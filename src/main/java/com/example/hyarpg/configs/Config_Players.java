package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Players {
    @Key("base_health_regen_per_second")
    @Comment("The starting base amount of health players regenerate per second. Default: 0")
    public float base_health_regen_per_second = 0f;

    @Key("base_mana_regen_per_second")
    @Comment("The starting base amount of mana players regenerate per second. Default: 1")
    public float base_mana_regen_per_second = 1f;

    @Key("base_stamina_regen_per_second")
    @Comment("The starting base amount of stamina players regenerate per second. Default: 1")
    public float base_stamina_regen_per_second = 1f;

    @Key("base_ammo_regen_per_second")
    @Comment("The starting base amount of ammo players regenerate per second. Default: 1")
    public float base_ammo_regen_per_second = 1f;
}