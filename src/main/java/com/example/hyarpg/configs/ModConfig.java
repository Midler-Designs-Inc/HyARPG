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

    @Key("hunger")
    @Section("hunger")
    public Config_Hunger hunger = new Config_Hunger();

    @Key("thirst")
    @Section("thirst")
    public Config_Thirst thirst = new Config_Thirst();

    @Key("enemies")
    @Section("enemies")
    public Config_Enemies enemies = new Config_Enemies();

    @Key("combat")
    @Section("combat")
    public Config_Combat combat = new Config_Combat();

    @Key("experience")
    @Section("experience")
    public Config_Experience experience = new Config_Experience();

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