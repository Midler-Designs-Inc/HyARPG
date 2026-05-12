package com.example.hyarpg.ticking_systems;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

// Mod Imports
import com.example.hyarpg.components.Component_HomingMissile;
import com.example.hyarpg.components.Component_Simulacrum;

// Java Imports
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class System_Simulacrum extends EntityTickingSystem<EntityStore> {

    private static final String PROJECTILE_CONFIG = "ProjectileConfig_Arcane_Missiles";
    private static final int MISSILE_COUNT = 5;
    private static final long STAGGER_MS = 100L;
    private static final float ARM_TIME = 0.5f;
    private static final float TURN_RATE = 15f;
    private static final float AGGRO_RANGE = 20f;
    private static final float[] YAW_OFFSETS  = { -20f, -10f, 0f, 10f, 20f };
    private static final float[] PITCH_OFFSETS = {  55f,  60f, 65f, 60f, 55f };

    private final ComponentType<EntityStore, Component_Simulacrum> componentType;
    private final ComponentType<EntityStore, Component_HomingMissile> homingComponentType;

    public System_Simulacrum(ComponentType<EntityStore, Component_Simulacrum> componentType, ComponentType<EntityStore, Component_HomingMissile> homingComponentType) {
        this.componentType = componentType;
        this.homingComponentType = homingComponentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(this.componentType);
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Component_Simulacrum simulacrum = archetypeChunk.getComponent(index, this.componentType);
        if (simulacrum == null) return;

        // bail if caster is gone
        if (simulacrum.casterRef == null || !simulacrum.casterRef.isValid()) {
            commandBuffer.removeComponent(ref, this.componentType);
            return;
        }

        // count down missile timer
        simulacrum.missileTimer -= dt;
        if (simulacrum.missileTimer > 0f) return;
        simulacrum.missileTimer = 5.0f;

        // get simulacrum position
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;
        Vector3d pos = transform.getPosition();

        // scan for nearby hostile NPCs
        Vector3d min = new Vector3d(pos.x - AGGRO_RANGE, pos.y - 5, pos.z - AGGRO_RANGE);
        Vector3d max = new Vector3d(pos.x + AGGRO_RANGE, pos.y + 5, pos.z + AGGRO_RANGE);
        Ref<EntityStore> nearestTarget = null;
        double nearestDist = Double.MAX_VALUE;

        // loop over nearby entities to re-assert aggro and get the closest
        for (Ref<EntityStore> nearbyRef : TargetUtil.getAllEntitiesInBox(min, max, store)) {
            if (!nearbyRef.isValid() || nearbyRef.equals(ref)) continue;
            NPCEntity npc = store.getComponent(nearbyRef, NPCEntity.getComponentType());
            if (npc == null) continue;
            Role role = npc.getRole();
            if (role == null || role.isFriendly(ref, store)) continue;

            // re-assert aggro
            role.setMarkedTarget("LockedTarget", ref);
            npc.onFlockSetState(nearbyRef, "Alerted", null, store);

            // track nearest for missile targeting
            TransformComponent targetTransform = store.getComponent(nearbyRef, TransformComponent.getComponentType());
            if (targetTransform == null) continue;
            double dist = pos.distanceTo(targetTransform.getPosition());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestTarget = nearbyRef;
            }
        }

        // bail if no valid target found
        if (nearestTarget == null) return;

        // resolve projectile config
        ProjectileConfig config = ProjectileConfig.getAssetMap().getAsset(PROJECTILE_CONFIG);
        if (config == null) return;

        // launch origin slightly above simulacrum
        float simulacrumYaw = transform.getRotation().getYaw();
        final Ref<EntityStore> capturedTarget = nearestTarget;
        final Ref<EntityStore> capturedCaster = simulacrum.casterRef;

        // stagger missile launches via virtual threads
        for (int i = 0; i < MISSILE_COUNT; i++) {
            final int missileIndex = i;
            Thread.ofVirtual().start(() -> {
                try { Thread.sleep(STAGGER_MS * missileIndex); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                float yawRad = simulacrumYaw + (float) Math.toRadians(YAW_OFFSETS[missileIndex]);
                double upAmount = Math.sin(Math.toRadians(PITCH_OFFSETS[missileIndex]));
                double forwardAmount = Math.cos(Math.toRadians(PITCH_OFFSETS[missileIndex]));
                Vector3d dir = new Vector3d(
                    Math.sin(yawRad) * forwardAmount,
                    upAmount,
                    Math.cos(yawRad) * forwardAmount
                );
                dir.normalize();

                // offset forward in the launch direction so missile spawns outside simulacrum hitbox
                Vector3d launchOrigin = pos.clone().add(0, 1.6, 0).add(dir.x * 1.5, 0, dir.z * 1.5);

                Ref<EntityStore> missileRef = ProjectileModule.get().spawnProjectile(null, capturedCaster, commandBuffer, config, launchOrigin.clone(), dir);
                commandBuffer.putComponent(missileRef, homingComponentType, new Component_HomingMissile(capturedCaster, capturedTarget, TURN_RATE, ARM_TIME, "MainHand_Magic_Scalar", 0.1f));
            });
        }
    }
}