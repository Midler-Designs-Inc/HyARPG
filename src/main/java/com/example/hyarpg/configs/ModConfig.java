package com.example.hyarpg.configs;

// Java Imports
import de.bsommerfeld.jshepherd.annotation.Comment;
import de.bsommerfeld.jshepherd.annotation.Key;
import de.bsommerfeld.jshepherd.annotation.Section;
import de.bsommerfeld.jshepherd.core.ConfigurablePojo;
import de.bsommerfeld.jshepherd.core.ConfigurationLoader;

import java.nio.file.Path;
import java.nio.file.Paths;

@Comment("HyARPG Configuration")
public class ModConfig extends ConfigurablePojo<ModConfig> {
    // Global instance — accessible anywhere via ModConfig.get()
    private static ModConfig INSTANCE;

    // File path
    private static final Path CONFIG_PATH = Paths.get("").toAbsolutePath().resolve("mods/HyARPG/HyARPG_Config.toml");

    @Key("building")
    @Section("building")
    public Config_Building building = new Config_Building();

    @Key("combat")
    @Section("combat")
    public Config_Combat combat = new Config_Combat();

    @Key("enemies")
    @Section("enemies")
    public Config_Enemies enemies = new Config_Enemies();

    @Key("experience")
    @Section("experience")
    public Config_Experience experience = new Config_Experience();

    @Key("hunger")
    @Section("hunger")
    public Config_Hunger hunger = new Config_Hunger();

    @Key("players")
    @Section("players")
    public Config_Players players = new Config_Players();

    @Key("raids")
    @Section("raids")
    public Config_Raids raids = new Config_Raids();

    @Key("thirst")
    @Section("thirst")
    public Config_Thirst thirst = new Config_Thirst();

    @Key("world")
    @Section("world")
    public Config_World world = new Config_World();

    public static void load() {
        Thread.currentThread().setContextClassLoader(ModConfig.class.getClassLoader());
        INSTANCE = ConfigurationLoader.from(CONFIG_PATH)
                .withComments()
                .load(ModConfig::new);
        INSTANCE.save();
    }

    public static ModConfig get() {
        if (INSTANCE == null) throw new IllegalStateException("ModConfig not loaded yet");
        return INSTANCE;
    }

}