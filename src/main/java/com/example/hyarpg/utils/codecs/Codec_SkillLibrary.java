package com.example.hyarpg.utils.codecs;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.function.FunctionCodec;

// Mod Imports
import com.example.hyarpg.utils.skills.SkillLibrary;

// Java Imports
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Codec_SkillLibrary {
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()  // handles your @Nullable abilityId and statType fields
            .create();

    public static final Codec<SkillLibrary> SKILL_LIBRARY_CODEC = new FunctionCodec<>(
            Codec.STRING,
            json -> GSON.fromJson(json, SkillLibrary.class),
            lib  -> GSON.toJson(lib)
    );
}
