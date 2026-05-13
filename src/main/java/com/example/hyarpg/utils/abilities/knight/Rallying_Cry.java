package com.example.hyarpg.utils.abilities.knight;

// Mod Imports
import com.example.hyarpg.utils.abilities.Ability;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public class Rallying_Cry extends Ability {

    public Rallying_Cry() {
        super("Ability_Rallying_Cry", DefaultEntityStatTypes.getSignatureEnergy(), 100f, true, 0, false, List.of(), false);
    }

}