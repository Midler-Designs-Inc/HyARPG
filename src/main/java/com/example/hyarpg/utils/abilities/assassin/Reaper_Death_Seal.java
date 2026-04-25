package com.example.hyarpg.utils.abilities.assassin;

// Hytale Imports
import com.example.hyarpg.modules.Module_RPGSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.utils.abilities.Ability;

// Java Imports
import java.awt.*;
import java.util.List;

public class Reaper_Death_Seal extends Ability {

    private static final double DAMAGE_PER_MARK = 0.10; // 10% of target max health per mark

    public Reaper_Death_Seal() {
        super("Ability_Reaper_Death_Seal", DefaultEntityStatTypes.getSignatureEnergy(), 100f, true, 300, false, List.of());
    }

    @Override
    public void execute(Ref<EntityStore> ref) {
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        // get the rpg player component
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null || playerRef == null) return;

        // get the last hit target — bail if none
        Ref<EntityStore> target = rpgPlayer.marks.getLastHitTarget();
        if (target == null || !target.isValid()) {
            playerRef.sendMessage(Message.raw("You must have a valid target to use this ability. Hit an enemy to acquire them as a target.").color(Color.RED));
            return;
        }

        world.execute(() -> {
            // get the target's stat map to read max health
            EntityStatMap targetStatMap = store.getComponent(target, EntityStatsModule.get().getEntityStatMapComponentType());
            if (targetStatMap == null) return;

            // get the target's max health
            EntityStatValue healthStat = targetStatMap.get(DefaultEntityStatTypes.getHealth());
            if (healthStat == null) return;
            float maxHealth = healthStat.getMax();

            // consume all assassin marks on the target
            int marksConsumed = rpgPlayer.marks.count("ASSASSIN");
            if (marksConsumed > 0) rpgPlayer.marks.clear("ASSASSIN");

            // calculate final damage — 10% of target max health per mark consumed
            float finalDamage = (float) (maxHealth * DAMAGE_PER_MARK * marksConsumed);

            // apply the damage directly via command cause, bypassing the swing pipeline
            DamageSystems.executeDamage(target, store,
                new Damage(
                    new Damage.EntitySource(ref),
                    DamageCause.getAssetMap().getAsset("Command"),
                    finalDamage
                )
            );
        });
    }
}