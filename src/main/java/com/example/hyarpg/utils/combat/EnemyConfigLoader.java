package com.example.hyarpg.utils.combat;

// Java Mods
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

public class EnemyConfigLoader {

    private static final Gson GSON = new Gson();

    public static class EnemyConfig {
        public String id;
        public String faction;
        public String damageType;
        public String operationalSpace;
        public String combatType;
        public String nature;
    }

    public static List<EnemyConfig> loadHostile() {
        return load("/data/EnemyClassifications.json");
    }

    public static List<EnemyConfig> loadNeutral() {
        return load("/data/NeutralsClassifications.json");
    }

    private static List<EnemyConfig> load(String resourcePath) {
        try (InputStream is = EnemyConfigLoader.class.getResourceAsStream(resourcePath);
             InputStreamReader reader = new InputStreamReader(is)) {

            Type listType = new TypeToken<List<EnemyConfig>>(){}.getType();
            return GSON.fromJson(reader, listType);

        } catch (Exception e) {
            throw new RuntimeException("Failed to load enemy config: " + resourcePath, e);
        }
    }
}