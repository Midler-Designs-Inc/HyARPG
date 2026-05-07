package com.example.hyarpg.subclasses;

// Hytale Imports
import com.hypixel.hytale.builtin.deployables.component.DeployableComponent;
import com.hypixel.hytale.builtin.deployables.component.DeployableComponent.DeployableFlag;
import com.hypixel.hytale.builtin.deployables.component.DeployableProjectileShooterComponent;
import com.hypixel.hytale.builtin.deployables.config.DeployableTurretConfig;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Opacity;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.*;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.LaunchProjectileInteraction;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

// Java Imports
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.annotation.Nonnull;

public class FixedDeployableTurretConfig extends DeployableTurretConfig {

    @Nonnull
    public static final BuilderCodec<FixedDeployableTurretConfig> CODEC = BuilderCodec.builder(FixedDeployableTurretConfig.class, FixedDeployableTurretConfig::new, DeployableTurretConfig.CODEC).build();

    // Route tick to the correct state handler
    @Override
    public void tick(@Nonnull DeployableComponent deployableComponent, float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> entityRef = archetypeChunk.getReferenceTo(index);
        switch (deployableComponent.getFlag(DeployableFlag.STATE)) {
            case 0 -> tickInitState(entityRef, deployableComponent, store, commandBuffer);
            case 1 -> tickStartDeployState(entityRef, deployableComponent, store);
            case 2 -> tickAwaitDeployState(entityRef, deployableComponent, store);
            case 3 -> tickAttackState(entityRef, deployableComponent, dt, store, commandBuffer);
        }
    }

    // Add shooter component and begin deploy animation
    private void tickInitState(@Nonnull Ref<EntityStore> entityRef, @Nonnull DeployableComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        component.setFlag(DeployableFlag.STATE, 1);
        commandBuffer.addComponent(entityRef, DeployableProjectileShooterComponent.getComponentType());
        playAnimation(store, entityRef, this, "Deploy");
    }

    // Advance to await state and continue deploy animation
    private void tickStartDeployState(@Nonnull Ref<EntityStore> ref, @Nonnull DeployableComponent component, @Nonnull Store<EntityStore> store) {
        component.setFlag(DeployableFlag.STATE, 2);
        playAnimation(store, ref, this, "Deploy");
    }

    // Wait for deploy delay to expire before entering attack state
    private void tickAwaitDeployState(@Nonnull Ref<EntityStore> ref, @Nonnull DeployableComponent component, @Nonnull Store<EntityStore> store) {
        Instant now = store.getResource(TimeResource.getResourceType()).getNow();
        Instant readyTime = component.getSpawnInstant().plus((long) deployDelay, ChronoUnit.SECONDS);
        if (now.isAfter(readyTime)) {
            component.setFlag(DeployableFlag.STATE, 3);
            playAnimation(store, ref, this, "Loop");
        }
    }

    private void tickAttackState(@Nonnull Ref<EntityStore> ref, @Nonnull DeployableComponent component, float dt, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        component.setTimeSinceLastAttack(component.getTimeSinceLastAttack() + dt);

        World world = commandBuffer.getExternalData().getWorld();
        DeployableProjectileShooterComponent shooterComponent = store.getComponent(ref, DeployableProjectileShooterComponent.getComponentType());

        // Compute base spawn position from face offset
        Vector3d spawnPos = Vector3d.ZERO.clone();
        if (projectileSpawnOffsets != null) {
            Vector3d spawnOffset = projectileSpawnOffsets.get(component.getSpawnFace());
            if (spawnOffset != null) spawnPos.add(spawnOffset);
        }

        // If shooter component is missing, immediately despawn the turret
        if (shooterComponent == null) {
            world.execute(() -> {
                if (!ref.isValid()) return;
                DespawnComponent despawn = store.ensureAndGetComponent(ref, DespawnComponent.getComponentType());
                despawn.setDespawn(commandBuffer.getResource(WorldTimeResource.getResourceType()).getGameTime());
            });
            return;
        }

        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        assert transformComponent != null;
        HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
        assert headRotationComponent != null;

        // Check if current active target is still valid and in range
        Vector3d pos = Vector3d.add(spawnPos, transformComponent.getPosition());
        boolean hasTarget = false;
        Ref<EntityStore> target = shooterComponent.getActiveTarget();
        if (target != null && target.isValid()) {
            TransformComponent targetTransform = store.getComponent(target, TransformComponent.getComponentType());
            assert targetTransform != null;
            Vector3d targetPos = calculatedTargetPosition(targetTransform.getPosition());
            Vector3d direction = Vector3d.directionTo(pos, targetPos);
            if (targetPos.distanceTo(pos) <= trackableRadius && testLineOfSight(pos, targetPos, direction, commandBuffer)) hasTarget = true;
        }

        // Scan for a new closest valid target if we don't have one
        if (!hasTarget) {
            Ref<EntityStore> closestTarget = null;
            Vector3d closestTargetPos = Vector3d.MAX;
            for (Ref<EntityStore> potentialTargetRef : TargetUtil.getAllEntitiesInSphere(pos, detectionRadius, commandBuffer)) {
                if (potentialTargetRef == null || !potentialTargetRef.isValid()) continue;
                TransformComponent targetTransform = store.getComponent(potentialTargetRef, TransformComponent.getComponentType());
                assert targetTransform != null;
                Vector3d targetPosition = calculatedTargetPosition(targetTransform.getPosition());
                Vector3d direction = Vector3d.directionTo(pos, targetPosition);
                if (testLineOfSight(pos, targetPosition, direction, commandBuffer) && isValidTarget(ref, store, potentialTargetRef) && pos.distanceTo(targetPosition) < pos.distanceTo(closestTargetPos)) {
                    closestTargetPos = targetPosition;
                    closestTarget = potentialTargetRef;
                }
            }
            if (closestTarget != null) {
                shooterComponent.setActiveTarget(closestTarget);
                target = closestTarget;
                hasTarget = true;
            }
        }

        // Rotate head toward target using exact look-at rotation
        Vector3f lookRotation = Vector3f.ZERO;
        Vector3f firingRotation = Vector3f.ZERO;
        if (hasTarget) {
            TransformComponent targetTransform = store.getComponent(target, TransformComponent.getComponentType());
            assert targetTransform != null;
            Vector3d targetPos = calculatedTargetPosition(targetTransform.getPosition().clone());
            Vector3d relativeOffset = new Vector3d(pos.x - targetPos.x, pos.y - targetPos.y, pos.z - targetPos.z);
            firingRotation = Vector3f.lookAt(relativeOffset.negate());
            lookRotation = Vector3f.lerpAngle(headRotationComponent.getRotation(), firingRotation, rotationSpeed * dt);
        }
        headRotationComponent.setRotation(lookRotation);

        // Determine if burst timing allows firing
        int shotsFired = component.getFlag(DeployableFlag.BURST_SHOTS);
        float timeSinceLastAttack = component.getTimeSinceLastAttack();
        boolean canFire = false;
        if (shotsFired < burstCount && timeSinceLastAttack >= shotInterval) {
            component.setFlag(DeployableFlag.BURST_SHOTS, shotsFired + 1);
            canFire = true;
        }
        else if (shotsFired >= burstCount && timeSinceLastAttack >= burstCooldown) {
            component.setFlag(DeployableFlag.BURST_SHOTS, 1);
            canFire = true;
        }

        // Spawn projectile with owner (player) as creator so ProjectileHit/Miss interactions
        // fire via the player's LivingEntity context — ownerRef retrieved before lambda,
        // used inside world.execute where the world thread store context is correct
        if (canFire && hasTarget) {
            Vector3d fwdDirection = new Vector3d().assign((double) firingRotation.getYaw(), (double) firingRotation.getPitch());
            Vector3d projectileSpawnPos = transformComponent.getPosition().clone();
            projectileSpawnPos.y += 0.5;
            projectileSpawnPos.add(fwdDirection.clone().normalize());

            DeployableComponent dc = commandBuffer.getComponent(ref, DeployableComponent.getComponentType());
//            ProjectileModule.get().spawnProjectile(null, ref, commandBuffer, projectileConfig, projectileSpawnPos, fwdDirection.clone());
            ProjectileModule.get().spawnProjectile(null, dc.getOwner(), commandBuffer, projectileConfig, projectileSpawnPos, fwdDirection.clone());

            playAnimation(store, ref, this, "Shoot");
            component.setTimeSinceLastAttack(0.0F);
        }
    }

    // Add target offset to get the aimed position on the entity
    private Vector3d calculatedTargetPosition(@Nonnull Vector3d original) {
        return Vector3d.add(original.clone(), targetOffset);
    }

    // Only target hostile NPCs, exclude players, deployables, and the owner
    private boolean isValidTarget(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> targetRef) {
        if (targetRef.equals(ref)) return false;
        if (store.getComponent(targetRef, Player.getComponentType()) != null) return false;
        if (store.getComponent(targetRef, DeployableComponent.getComponentType()) != null) return false;
        NPCEntity npcEntity = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npcEntity == null || !npcEntity.getCanCauseDamage(ref, store)) return false;
        DeployableComponent deployableComponent = store.getComponent(ref, DeployableComponent.getComponentType());
        if (deployableComponent == null) return true;
        return canShootOwner || !targetRef.equals(deployableComponent.getOwner());
    }

    // Raycast between turret and target to check for obstructing opaque blocks
    private boolean testLineOfSight(@Nonnull Vector3d attackerPos, @Nonnull Vector3d targetPos, @Nonnull Vector3d direction, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!doLineOfSightTest) return true;

        com.hypixel.hytale.protocol.Vector3f spawnOffset = projectileConfig.getSpawnOffset();
        Vector3d testFromPos = attackerPos.clone().add((double) spawnOffset.x, (double) (spawnOffset.y + generatedModel.getEyeHeight()), (double) spawnOffset.z);
        double distance = testFromPos.distanceTo(targetPos);
        World world = commandBuffer.getExternalData().getWorld();

        if (getDebugVisuals()) {
            Vector3d increment = direction.scale(distance);
            Vector3f whiteColor = new Vector3f(1.0F, 1.0F, 1.0F);
            for (int i = 0; i < 10; ++i) {
                Vector3d p = testFromPos.clone();
                p.addScaled(increment, (double) ((float) i / 10.0F));
                DebugUtils.addSphere(world, p, whiteColor, 0.1, 0.5F);
            }
        }

        Vector3i blockPosition = TargetUtil.getTargetBlock(world, (id, fluid_id) -> {
            if (id == 0) return false;
            BlockType blockType = BlockType.getAssetMap().getAsset(id);
            if (blockType == null) return false;
            BlockMaterial material = blockType.getMaterial();
            if (material != null && material != BlockMaterial.Empty) return blockType.getOpacity() != Opacity.Transparent;
            return false;
        }, attackerPos.x, attackerPos.y, attackerPos.z, direction.x, direction.y, direction.z, distance);

        if (blockPosition == null) return true;
        double entityDistance = attackerPos.distanceSquaredTo(targetPos);
        double blockDistance = attackerPos.distanceSquaredTo((double) blockPosition.x + 0.5, (double) blockPosition.y + 0.5, (double) blockPosition.z + 0.5);
        return entityDistance < blockDistance;
    }
}