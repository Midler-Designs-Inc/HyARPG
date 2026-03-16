package com.example.hyarpg.utils.codecs;

// Hytale Imports
import com.example.hyarpg.utils.abilities.Ability;
import com.example.hyarpg.utils.abilities.knight.Rallying_Cry;
import com.example.hyarpg.utils.abilities.knight.Taunt;
import com.example.hyarpg.utils.affixes.StatType;
import com.example.hyarpg.utils.skills.Requirement;
import com.example.hyarpg.utils.skills.SkillNode;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.function.FunctionCodec;

// Mod Imports
import com.example.hyarpg.utils.skills.SkillLibrary;

// Java Imports
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;

import java.util.List;
import java.util.Map;

public class Codec_SkillLibrary {
    private static Map<String, Ability> abilityRegistry;

    private static Map<String, Ability> getAbilityRegistry() {
        if (abilityRegistry == null) {
            abilityRegistry = Map.of(
                    "Ability_Taunt", new Taunt(),
                    "Ability_Rallying_Cry", new Rallying_Cry()
            );
        }
        return abilityRegistry;
    }

    private static final Gson INNER_GSON = new Gson();

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(SkillNode.class, (JsonDeserializer<SkillNode>) (json, typeOfT, context) -> {
                JsonObject obj = json.getAsJsonObject();

                Ability ability = null;
                if (obj.has("ability") && !obj.get("ability").isJsonNull()) {
                    String abilityId = obj.getAsJsonObject("ability").get("abilityId").getAsString();
                    ability = getAbilityRegistry().get(abilityId);  // lazy lookup
                }

                String id = obj.get("id").getAsString();
                String displayName = obj.get("displayName").getAsString();
                String iconId = obj.get("iconId").getAsString();
                int cost = obj.get("cost").getAsInt();
                int maxRanks = obj.get("maxRanks").getAsInt();
                String version = obj.get("version").getAsString();
                List<Requirement> requirements = INNER_GSON.fromJson(obj.get("requirements"), new TypeToken<List<Requirement>>(){}.getType());

                SkillNode node;
                if (ability != null) {
                    node = new SkillNode(id, displayName, iconId, ability, cost, maxRanks, requirements, version);
                } else {
                    StatType statType = obj.get("statType").isJsonNull() ? null : INNER_GSON.fromJson(obj.get("statType"), StatType.class);
                    float statValuePerRank = obj.get("statValuePerRank").getAsFloat();
                    node = new SkillNode(id, displayName, iconId, statType, statValuePerRank, cost, maxRanks, requirements, version);
                }

                node.allocatedPoints = obj.has("allocatedPoints") ? obj.get("allocatedPoints").getAsInt() : 0;
                node.currentRank = obj.has("currentRank") ? obj.get("currentRank").getAsInt() : 0;
                return node;
            })
            .create();

    public static final Codec<SkillLibrary> SKILL_LIBRARY_CODEC = new FunctionCodec<>(
            Codec.STRING,
            json -> GSON.fromJson(json, SkillLibrary.class),
            lib  -> GSON.toJson(lib)
    );
}