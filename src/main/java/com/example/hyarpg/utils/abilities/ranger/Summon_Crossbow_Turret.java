package com.example.hyarpg.utils.abilities.ranger;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.utils.abilities.Ability;

import java.util.List;

public class Summon_Crossbow_Turret extends Ability {

    public Summon_Crossbow_Turret() {
        super("Ability_Summon_Crossbow_Turret", DefaultEntityStatTypes.getStamina(), 7f, false, 0, false, List.of(), false);
    }

}