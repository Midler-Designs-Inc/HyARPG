package com.example.hyarpg.configs;

import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;

public class Config_World {

    @Key("read_me")
    @Comment("IMPORTANT: This section only works for chunks that are newly generated. To use this section of the config, you must place it in a new world folder before making that world. Just go into your directory where your world saves are, make a new folder after the name of your new world and place this file in that directory under mods/HyARPG. Keep in mind ore spawn frequency ramps up and down peaking near the middle point. So if you set min distance to 0 and max distance to say 100k you will see very little (basically none) of that ore around 0 and 100k and a lot of that ore around 50k.")
    public boolean read_me = true;

    @Key("origin_spawn_point_x")
    @Comment("Sets the x coordinate of the origin point. Origin point is used when determining distance from start/spawn. Default: 0")
    public int origin_spawn_point_x = 0;

    @Key("origin_spawn_point_y")
    @Comment("Sets the y coordinate of the origin point. Origin point is used when determining distance from start/spawn. Default: 100")
    public int origin_spawn_point_y = 100;

    @Key("origin_spawn_point_z")
    @Comment("Sets the z coordinate of the origin point. Origin point is used when determining distance from start/spawn. Default: 0")
    public int origin_spawn_point_z = 0;

    @Key("min_distance_for_copper_spawn")
    @Comment("Determines the minimum distance from origin before copper starts spawning. Default: 0")
    public double min_distance_for_copper_spawn = 0;

    @Key("max_distance_for_copper_spawn")
    @Comment("Determines the maximum distance from origin before copper stops spawning. Default: 20000")
    public double max_distance_for_copper_spawn = 20_000;

    @Key("min_distance_for_iron_spawn")
    @Comment("Determines the minimum distance from origin before iron starts spawning. Default: 10000")
    public double min_distance_for_iron_spawn = 10_000;

    @Key("max_distance_for_iron_spawn")
    @Comment("Determines the maximum distance from origin before iron stops spawning. Default: 30000")
    public double max_distance_for_iron_spawn = 30_000;

    @Key("min_distance_for_thorium_spawn")
    @Comment("Determines the minimum distance from origin before thorium starts spawning. Default: 20000")
    public double min_distance_for_thorium_spawn = 20_000;

    @Key("max_distance_for_thorium_spawn")
    @Comment("Determines the maximum distance from origin before thorium stops spawning. Default: 40000")
    public double max_distance_for_thorium_spawn = 40_000;

    @Key("min_distance_for_cobalt_spawn")
    @Comment("Determines the minimum distance from origin before cobalt starts spawning. Default: 20000")
    public double min_distance_for_cobalt_spawn = 20_000;

    @Key("max_distance_for_cobalt_spawn")
    @Comment("Determines the maximum distance from origin before cobalt stops spawning. Default: 40000")
    public double max_distance_for_cobalt_spawn = 40_000;

    @Key("min_distance_for_adamantite_spawn")
    @Comment("Determines the minimum distance from origin before adamantite starts spawning. Default: 30000")
    public double min_distance_for_adamantite_spawn = 30_000;

    @Key("max_distance_for_adamantite_spawn")
    @Comment("Determines the maximum distance from origin before adamantite stops spawning. Default: 50000")
    public double max_distance_for_adamantite_spawn = 50_000;

    @Key("min_distance_for_mithril_spawn")
    @Comment("Determines the minimum distance from origin before mithril starts spawning. Default: 40000")
    public double min_distance_for_mithril_spawn = 40_000;

    @Key("max_distance_for_mithril_spawn")
    @Comment("Determines the maximum distance from origin before mithril stops spawning. Default: 60000")
    public double max_distance_for_mithril_spawn = 60_000;

}