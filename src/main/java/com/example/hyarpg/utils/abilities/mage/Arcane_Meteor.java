package com.example.hyarpg.utils.abilities.mage;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

// Mod Imports
import com.example.hyarpg.components.Component_HomingMissile;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.abilities.Ability;

// Java Imports
import java.awt.Color;
import java.util.List;

public class Arcane_Meteor extends Ability {

    private static final String PROJECTILE_CONFIG = "ProjectileConfig_Arcane_Meteor";
    private static final float SPAWN_HEIGHT = 40f;
    private static final float AOE_RANGE = 8f;
    private static final float AOE_HEIGHT = 6f;
    private static final float TURN_RATE = 20f;
    private static final float ARM_TIME = 0f;

    public Arcane_Meteor() {
        super("Ability_Arcane_Meteor", DefaultEntityStatTypes.getSignatureEnergy(), 100f, true, 0, false, List.of());
    }

    @Override
    public void execute(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer) {
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        // get required components
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null || playerRef == null) return;

        // get last target — bail with message if none
        Ref<EntityStore> targetRef = rpgPlayer.lastEnemyHit;
        if (targetRef == null || !targetRef.isValid()) {
            playerRef.sendMessage(Message.raw("No target — hit an enemy to acquire one.").color(Color.RED));
            return;
        }

        // resolve projectile config — bail if not found
        ProjectileConfig config = ProjectileConfig.getAssetMap().getAsset(PROJECTILE_CONFIG);
        if (config == null) return;

        // get target position and spawn meteor far above it
        TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
        if (targetTransform == null) return;
        Vector3d targetPos = targetTransform.getPosition().clone();
        Vector3d spawnPos = targetPos.clone().add(0, SPAWN_HEIGHT, 0);

        // direction straight down toward target
        Vector3d dir = new Vector3d(0, -1, 0);

        // spawn the projectile on next world execute
        final Ref<EntityStore> capturedTargetRef = targetRef;
        world.execute(() -> {
            Ref<EntityStore> meteorRef = ProjectileModule.get().spawnProjectile(null, ref, commandBuffer, config, spawnPos, dir);
            commandBuffer.putComponent(meteorRef, Component_HomingMissile.getComponentType(), new Component_HomingMissile(ref, capturedTargetRef, TURN_RATE, ARM_TIME, "MainHand_Magic_Scalar", 15f, AOE_RANGE, AOE_HEIGHT));
        });
    }
}