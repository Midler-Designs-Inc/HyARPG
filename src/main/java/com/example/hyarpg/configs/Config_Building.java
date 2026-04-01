package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_Building {
    @Key("allow_light_well_territory_claim")
    @Comment("Determines if players are allowed to use Light Well benches to claim territory for base building. Disabling this will disable base raiding regardless of other settings. Default: true")
    public boolean allow_light_well_territory_claim = true;

    @Key("allow_bed_placement_outside_territory")
    @Comment("Determines if players are allowed to place beds outside a player's Light Well base territory. Default: false")
    public boolean allow_bed_placement_outside_territory = false;

    @Key("allow_bench_placement_outside_territory")
    @Comment("Determines if players are allowed to place benches outside a player's Light Well base territory. Default: false")
    public boolean allow_bench_placement_outside_territory = false;
}