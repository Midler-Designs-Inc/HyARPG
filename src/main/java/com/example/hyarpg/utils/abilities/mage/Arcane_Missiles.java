package com.example.hyarpg.utils.abilities.mage;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_HomingMissile;
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.abilities.Ability;

// Java Imports
import java.util.List;
import org.joml.Vector3d;

public class Arcane_Missiles extends Ability {

    private static final String PROJECTILE_CONFIG = "ProjectileConfig_Arcane_Missiles";
    private static final int MISSILE_COUNT = 5;
    private static final long STAGGER_MS = 100L;
    private static final float ARM_TIME = 0.5f;
    private static final float TURN_RATE = 15f;

    // yaw and pitch offsets per missile so they fan out on launch
    private static final float[] YAW_OFFSETS   = { -20f, -10f,  0f, 10f, 20f };
    private static final float[] PITCH_OFFSETS = { 55f, 60f, 65f, 60f, 55f };

    public Arcane_Missiles() {
        super("Ability_Arcane_Missiles", DefaultEntityStatTypes.getMana(), 10f, false, 5, false, List.of(), true);
    }

    @Override
    public void execute(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer) {
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        // get required components
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null || playerRef == null) return;

        // get the currently targeted enemy or bail
        Ref<EntityStore> target = rpgPlayer.currentTarget;
        if (target == null || !target.isValid()) return;

        // resolve projectile config — bail if not found
        ProjectileConfig config = ProjectileConfig.getAssetMap().getAsset(PROJECTILE_CONFIG);
        if (config == null) return;

        // get player transform for launch origin and yaw
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;
        Vector3d launchOrigin = new Vector3d(transform.getPosition()).add(0, 1.6, 0);
        float playerYaw = transform.getRotation().yaw();

        // capture target ref for lambda
        final Ref<EntityStore> capturedTargetRef = target;

        // stagger each missile launch on a virtual thread like raid wave spawning
        for (int i = 0; i < MISSILE_COUNT; i++) {
            final int missileIndex = i;
            Thread.ofVirtual().start(() -> {
                try { Thread.sleep(STAGGER_MS * missileIndex); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                world.execute(() -> {
                    float yawRad = playerYaw + (float) Math.toRadians(YAW_OFFSETS[missileIndex]);

                    // build upward launch direction directly — positive Y is up in Hytale
                    double upAmount = Math.sin(Math.toRadians(PITCH_OFFSETS[missileIndex]));
                    double forwardAmount = Math.cos(Math.toRadians(PITCH_OFFSETS[missileIndex]));

                    Vector3d dir = new Vector3d(
                        Math.sin(yawRad) * forwardAmount,
                        upAmount,
                        Math.cos(yawRad) * forwardAmount
                    );
                    dir.normalize();

                    Ref<EntityStore> missileRef = ProjectileModule.get().spawnProjectile(null, ref, commandBuffer, config, new Vector3d(launchOrigin), dir);
                    commandBuffer.putComponent(missileRef, Component_HomingMissile.getComponentType(), new Component_HomingMissile(ref, capturedTargetRef, TURN_RATE, ARM_TIME, "MainHand_Magic_Scalar", 0.5f));
                });
            });
        }
    }
}