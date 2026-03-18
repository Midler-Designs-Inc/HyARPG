package com.example.hyarpg.utils.abilities.juggernaut;

// Mod Imports
import com.example.hyarpg.utils.abilities.Ability;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Leap_Slam extends Ability {

    public Leap_Slam() {
        super("Ability_Rallying_Cry", DefaultEntityStatTypes.getSignatureEnergy(), 100f, true, 0);
    }

    @Override
    public void execute(Ref<EntityStore> ref) {}

}