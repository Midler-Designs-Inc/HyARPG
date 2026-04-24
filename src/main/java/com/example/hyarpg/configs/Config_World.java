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

    // ── Copper ────────────────────────────────────────────────────────────────

    @Key("min_distance_for_copper_spawn")
    @Comment("Determines the minimum distance from origin before copper starts spawning.")
    public double min_distance_for_copper_spawn = 2_000;

    @Key("max_distance_for_copper_spawn")
    @Comment("Determines the maximum distance from origin before copper stops spawning.")
    public double max_distance_for_copper_spawn = 22_000;

    @Key("copper_veins_per_chunk")
    @Comment("Number of copper veins that can generate per chunk. Default: 8")
    public int copper_veins_per_chunk = 8;

    @Key("copper_min_vein_size")
    @Comment("Minimum number of blocks in a single copper vein. Default: 3")
    public int copper_min_vein_size = 3;

    @Key("copper_max_vein_size")
    @Comment("Maximum number of blocks in a single copper vein. Default: 8")
    public int copper_max_vein_size = 8;

    @Key("copper_min_y")
    @Comment("Minimum Y level copper can spawn at. Default: 10")
    public int copper_min_y = 10;

    @Key("copper_max_y")
    @Comment("Maximum Y level copper can spawn at. Default: 80")
    public int copper_max_y = 80;

    // ── Iron ──────────────────────────────────────────────────────────────────

    @Key("min_distance_for_iron_spawn")
    @Comment("Determines the minimum distance from origin before iron starts spawning.")
    public double min_distance_for_iron_spawn = 12_000;

    @Key("max_distance_for_iron_spawn")
    @Comment("Determines the maximum distance from origin before iron stops spawning.")
    public double max_distance_for_iron_spawn = 32_000;

    @Key("iron_veins_per_chunk")
    @Comment("Number of iron veins that can generate per chunk. Default: 6")
    public int iron_veins_per_chunk = 6;

    @Key("iron_min_vein_size")
    @Comment("Minimum number of blocks in a single iron vein. Default: 3")
    public int iron_min_vein_size = 3;

    @Key("iron_max_vein_size")
    @Comment("Maximum number of blocks in a single iron vein. Default: 7")
    public int iron_max_vein_size = 7;

    @Key("iron_min_y")
    @Comment("Minimum Y level iron can spawn at. Default: 5")
    public int iron_min_y = 5;

    @Key("iron_max_y")
    @Comment("Maximum Y level iron can spawn at. Default: 60")
    public int iron_max_y = 60;

    @Key("iron_cracked_veins_per_chunk")
    @Comment("Number of cracked iron veins that can generate per chunk. Default: 3")
    public int iron_cracked_veins_per_chunk = 3;

    @Key("iron_cracked_min_vein_size")
    @Comment("Minimum number of blocks in a single cracked iron vein. Default: 2")
    public int iron_cracked_min_vein_size = 2;

    @Key("iron_cracked_max_vein_size")
    @Comment("Maximum number of blocks in a single cracked iron vein. Default: 5")
    public int iron_cracked_max_vein_size = 5;

    @Key("iron_cracked_min_y")
    @Comment("Minimum Y level cracked iron can spawn at. Default: 5")
    public int iron_cracked_min_y = 5;

    @Key("iron_cracked_max_y")
    @Comment("Maximum Y level cracked iron can spawn at. Default: 60")
    public int iron_cracked_max_y = 60;

    // ── Thorium ───────────────────────────────────────────────────────────────

    @Key("min_distance_for_thorium_spawn")
    @Comment("Determines the minimum distance from origin before thorium starts spawning.")
    public double min_distance_for_thorium_spawn = 22_000;

    @Key("max_distance_for_thorium_spawn")
    @Comment("Determines the maximum distance from origin before thorium stops spawning.")
    public double max_distance_for_thorium_spawn = 42_000;

    @Key("thorium_veins_per_chunk")
    @Comment("Number of thorium veins that can generate per chunk. Default: 5")
    public int thorium_veins_per_chunk = 5;

    @Key("thorium_min_vein_size")
    @Comment("Minimum number of blocks in a single thorium vein. Default: 3")
    public int thorium_min_vein_size = 3;

    @Key("thorium_max_vein_size")
    @Comment("Maximum number of blocks in a single thorium vein. Default: 7")
    public int thorium_max_vein_size = 7;

    @Key("thorium_min_y")
    @Comment("Minimum Y level thorium can spawn at. Default: 5")
    public int thorium_min_y = 5;

    @Key("thorium_max_y")
    @Comment("Maximum Y level thorium can spawn at. Default: 70")
    public int thorium_max_y = 70;

    @Key("thorium_cracked_veins_per_chunk")
    @Comment("Number of cracked thorium veins that can generate per chunk. Default: 3")
    public int thorium_cracked_veins_per_chunk = 3;

    @Key("thorium_cracked_min_vein_size")
    @Comment("Minimum number of blocks in a single cracked thorium vein. Default: 2")
    public int thorium_cracked_min_vein_size = 2;

    @Key("thorium_cracked_max_vein_size")
    @Comment("Maximum number of blocks in a single cracked thorium vein. Default: 5")
    public int thorium_cracked_max_vein_size = 5;

    @Key("thorium_cracked_min_y")
    @Comment("Minimum Y level cracked thorium can spawn at. Default: 5")
    public int thorium_cracked_min_y = 5;

    @Key("thorium_cracked_max_y")
    @Comment("Maximum Y level cracked thorium can spawn at. Default: 70")
    public int thorium_cracked_max_y = 70;

    // ── Cobalt ────────────────────────────────────────────────────────────────

    @Key("min_distance_for_cobalt_spawn")
    @Comment("Determines the minimum distance from origin before cobalt starts spawning.")
    public double min_distance_for_cobalt_spawn = 32_000;

    @Key("max_distance_for_cobalt_spawn")
    @Comment("Determines the maximum distance from origin before cobalt stops spawning.")
    public double max_distance_for_cobalt_spawn = 52_000;

    @Key("cobalt_veins_per_chunk")
    @Comment("Number of cobalt veins that can generate per chunk. Default: 5")
    public int cobalt_veins_per_chunk = 5;

    @Key("cobalt_min_vein_size")
    @Comment("Minimum number of blocks in a single cobalt vein. Default: 3")
    public int cobalt_min_vein_size = 3;

    @Key("cobalt_max_vein_size")
    @Comment("Maximum number of blocks in a single cobalt vein. Default: 7")
    public int cobalt_max_vein_size = 7;

    @Key("cobalt_min_y")
    @Comment("Minimum Y level cobalt can spawn at. Default: 5")
    public int cobalt_min_y = 5;

    @Key("cobalt_max_y")
    @Comment("Maximum Y level cobalt can spawn at. Default: 60")
    public int cobalt_max_y = 60;

    @Key("cobalt_cracked_veins_per_chunk")
    @Comment("Number of cracked cobalt veins that can generate per chunk. Default: 3")
    public int cobalt_cracked_veins_per_chunk = 3;

    @Key("cobalt_cracked_min_vein_size")
    @Comment("Minimum number of blocks in a single cracked cobalt vein. Default: 2")
    public int cobalt_cracked_min_vein_size = 2;

    @Key("cobalt_cracked_max_vein_size")
    @Comment("Maximum number of blocks in a single cracked cobalt vein. Default: 5")
    public int cobalt_cracked_max_vein_size = 5;

    @Key("cobalt_cracked_min_y")
    @Comment("Minimum Y level cracked cobalt can spawn at. Default: 5")
    public int cobalt_cracked_min_y = 5;

    @Key("cobalt_cracked_max_y")
    @Comment("Maximum Y level cracked cobalt can spawn at. Default: 60")
    public int cobalt_cracked_max_y = 60;

    // ── Adamantite ────────────────────────────────────────────────────────────

    @Key("min_distance_for_adamantite_spawn")
    @Comment("Determines the minimum distance from origin before adamantite starts spawning.")
    public double min_distance_for_adamantite_spawn = 42_000;

    @Key("max_distance_for_adamantite_spawn")
    @Comment("Determines the maximum distance from origin before adamantite stops spawning.")
    public double max_distance_for_adamantite_spawn = 62_000;

    @Key("adamantite_veins_per_chunk")
    @Comment("Number of adamantite veins that can generate per chunk. Default: 4")
    public int adamantite_veins_per_chunk = 4;

    @Key("adamantite_min_vein_size")
    @Comment("Minimum number of blocks in a single adamantite vein. Default: 3")
    public int adamantite_min_vein_size = 3;

    @Key("adamantite_max_vein_size")
    @Comment("Maximum number of blocks in a single adamantite vein. Default: 7")
    public int adamantite_max_vein_size = 7;

    @Key("adamantite_min_y")
    @Comment("Minimum Y level adamantite can spawn at. Default: 1")
    public int adamantite_min_y = 1;

    @Key("adamantite_max_y")
    @Comment("Maximum Y level adamantite can spawn at. Default: 50")
    public int adamantite_max_y = 50;

    @Key("adamantite_cracked_veins_per_chunk")
    @Comment("Number of cracked adamantite veins that can generate per chunk. Default: 3")
    public int adamantite_cracked_veins_per_chunk = 3;

    @Key("adamantite_cracked_min_vein_size")
    @Comment("Minimum number of blocks in a single cracked adamantite vein. Default: 2")
    public int adamantite_cracked_min_vein_size = 2;

    @Key("adamantite_cracked_max_vein_size")
    @Comment("Maximum number of blocks in a single cracked adamantite vein. Default: 5")
    public int adamantite_cracked_max_vein_size = 5;

    @Key("adamantite_cracked_min_y")
    @Comment("Minimum Y level cracked adamantite can spawn at. Default: 1")
    public int adamantite_cracked_min_y = 1;

    @Key("adamantite_cracked_max_y")
    @Comment("Maximum Y level cracked adamantite can spawn at. Default: 50")
    public int adamantite_cracked_max_y = 50;

    // ── Mithril ───────────────────────────────────────────────────────────────

    @Key("min_distance_for_mithril_spawn")
    @Comment("Determines the minimum distance from origin before mithril starts spawning.")
    public double min_distance_for_mithril_spawn = 52_000;

    @Key("max_distance_for_mithril_spawn")
    @Comment("Determines the maximum distance from origin before mithril stops spawning.")
    public double max_distance_for_mithril_spawn = 72_000;

    @Key("mithril_veins_per_chunk")
    @Comment("Number of mithril veins that can generate per chunk. Default: 3")
    public int mithril_veins_per_chunk = 3;

    @Key("mithril_min_vein_size")
    @Comment("Minimum number of blocks in a single mithril vein. Default: 2")
    public int mithril_min_vein_size = 2;

    @Key("mithril_max_vein_size")
    @Comment("Maximum number of blocks in a single mithril vein. Default: 5")
    public int mithril_max_vein_size = 5;

    @Key("mithril_min_y")
    @Comment("Minimum Y level mithril can spawn at. Default: 1")
    public int mithril_min_y = 1;

    @Key("mithril_max_y")
    @Comment("Maximum Y level mithril can spawn at. Default: 30")
    public int mithril_max_y = 30;

}