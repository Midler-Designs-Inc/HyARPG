package com.example.hyarpg.utils.codecs;

// Hytale Imports
import com.example.hyarpg.utils.abilities.assassin.Reaper_Death_Seal;
import com.example.hyarpg.utils.abilities.assassin.Shadow_Strike;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import org.bson.BsonValue;

// Mod Imports
import com.example.hyarpg.utils.skills.SkillLibrary;
import com.example.hyarpg.utils.skills.SkillNode;
import com.example.hyarpg.utils.skills.Requirement;
import com.example.hyarpg.utils.abilities.Ability;
import com.example.hyarpg.utils.abilities.knight.*;
import com.example.hyarpg.utils.abilities.juggernaut.*;
import com.example.hyarpg.utils.abilities.ranger.Aerial_Maneuver;
import com.example.hyarpg.utils.abilities.ranger.Rain_Of_Arrows;
import com.example.hyarpg.utils.abilities.ranger.Summon_Crossbow_Turret;
import com.example.hyarpg.utils.affixes.StatType;

// Java / Gson Imports
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Codec_SkillLibrary {

    private static Map<String, Ability> abilityRegistry;

    private static Map<String, Ability> getAbilityRegistry() {
        if (abilityRegistry == null) {
            abilityRegistry = Map.of(
                "Ability_Taunt", new Taunt(),
                "Ability_Rallying_Cry", new Rallying_Cry(),
                "Ability_Chain_Pull", new Chain_Pull(),
                "Ability_Cyclone", new Cyclone(),
                "Ability_Leap_Slam", new Leap_Slam(),
                "Ability_Rain_Of_Arrows", new Rain_Of_Arrows(),
                "Ability_Summon_Crossbow_Turret", new Summon_Crossbow_Turret(),
                "Ability_Aerial_Maneuver", new Aerial_Maneuver(),
                "Ability_Shadow_Strike", new Shadow_Strike(),
                "Ability_Reaper_Death_Seal", new Reaper_Death_Seal()
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
                    ability = getAbilityRegistry().get(abilityId);
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

    public static final Codec<SkillLibrary> SKILL_LIBRARY_CODEC = new Codec<SkillLibrary>() {

        @Nonnull
        @Override
        public SkillLibrary decode(BsonValue bsonValue, ExtraInfo extraInfo) {
            String json = Codec.STRING.decode(bsonValue, extraInfo);
            return GSON.fromJson(json, SkillLibrary.class);
        }

        @Override
        public BsonValue encode(SkillLibrary lib, ExtraInfo extraInfo) {
            return Codec.STRING.encode(GSON.toJson(lib), extraInfo);
        }

        @Nonnull
        @Override
        public SkillLibrary decodeJson(@Nonnull RawJsonReader reader, ExtraInfo extraInfo) throws IOException {
            String json = Codec.STRING.decodeJson(reader, extraInfo);
            return GSON.fromJson(json, SkillLibrary.class);
        }

        @Nonnull
        @Override
        public Schema toSchema(@Nonnull SchemaContext context) {
            return Codec.STRING.toSchema(context);
        }
    };
}