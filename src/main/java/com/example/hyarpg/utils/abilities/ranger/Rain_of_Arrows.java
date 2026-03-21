package com.example.hyarpg.utils.abilities.ranger;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.utils.abilities.Ability;

import java.util.List;

public class Rain_of_Arrows extends Ability {

    public Rain_of_Arrows() {
        super("Ability_Rain_of_Arrows", DefaultEntityStatTypes.getSignatureEnergy(), 100f, true, 0, false, List.of("Shortbow", "Crossbow", "Longbow"));
    }

    @Override
    public void execute(Ref<EntityStore> ref) {}

}