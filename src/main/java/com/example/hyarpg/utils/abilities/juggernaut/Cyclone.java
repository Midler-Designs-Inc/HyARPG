package com.example.hyarpg.utils.abilities.juggernaut;

// Hytale Imports
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;

// Mod Imports
import com.example.hyarpg.utils.abilities.Ability;

// Java Imports
import java.util.List;

public class Cyclone extends Ability {

    public Cyclone() {
        super("Ability_Cyclone", DefaultEntityStatTypes.getStamina(), 1f, false, 0, true, List.of("Battleaxe", "Mace", "Longsword"), false);
    }

}