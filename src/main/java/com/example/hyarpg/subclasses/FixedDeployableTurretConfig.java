package com.example.hyarpg.subclasses;

// Hytale Imports
import com.hypixel.hytale.builtin.deployables.DeployablesUtils;
import com.hypixel.hytale.builtin.deployables.component.DeployableComponent;
import com.hypixel.hytale.builtin.deployables.component.DeployableComponent.DeployableFlag;
import com.hypixel.hytale.builtin.deployables.component.DeployableProjectileComponent;
import com.hypixel.hytale.builtin.deployables.component.DeployableProjectileShooterComponent;
import com.hypixel.hytale.builtin.deployables.config.DeployableTurretConfig;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.Opacity;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.knockback.KnockbackComponent;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider.STATE;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

// Java Imports
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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

        // Tick and clean up active projectiles
        Vector3d pos = Vector3d.add(spawnPos, transformComponent.getPosition());
        updateProjectiles(ref, store, commandBuffer, shooterComponent);

        // Check if current active target is still valid and in range
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

        // Rotate head toward target
        Vector3f lookRotation = Vector3f.ZERO;
        if (hasTarget) {
            TransformComponent targetTransform = store.getComponent(target, TransformComponent.getComponentType());
            assert targetTransform != null;
            Vector3d targetPos = calculatedTargetPosition(targetTransform.getPosition().clone());
            Vector3d relativeOffset = new Vector3d(pos.x - targetPos.x, pos.y - targetPos.y, pos.z - targetPos.z);
            lookRotation = Vector3f.lerpAngle(headRotationComponent.getRotation(), Vector3f.lookAt(relativeOffset.negate()), rotationSpeed * dt);
        }
        headRotationComponent.setRotation(lookRotation);

        // Determine if burst timing allows firing
        int shotsFired = component.getFlag(DeployableFlag.BURST_SHOTS);
        float timeSinceLastAttack = component.getTimeSinceLastAttack();
        boolean canFire = false;
        if (shotsFired < burstCount && timeSinceLastAttack >= shotInterval) {
            component.setFlag(DeployableFlag.BURST_SHOTS, shotsFired + 1);
            canFire = true;
        } else if (shotsFired >= burstCount && timeSinceLastAttack >= burstCooldown) {
            component.setFlag(DeployableFlag.BURST_SHOTS, 1);
            canFire = true;
        }

        // Spawn projectile toward target, null creator avoids cross-store player component lookup crash
        if (canFire && hasTarget) {
            Vector3d fwdDirection = new Vector3d().assign((double) lookRotation.getYaw(), (double) lookRotation.getPitch());
            Vector3d projectileSpawnPos = transformComponent.getPosition().clone();
            projectileSpawnPos.y += 0.5;
            projectileSpawnPos.add(fwdDirection.clone().normalize());

            world.execute(() -> {
                Ref<EntityStore> projectileRef = ProjectileModule.get().spawnProjectile(null, null, commandBuffer, projectileConfig, projectileSpawnPos, fwdDirection.clone());
                System.out.println("[Turret] fired in world.execute, projectileRef null=" + (projectileRef == null) + " valid=" + (projectileRef != null && projectileRef.isValid()));
                if (projectileRef != null) {
                    commandBuffer.addComponent(projectileRef, DeployableProjectileComponent.getComponentType(), new DeployableProjectileComponent(projectileSpawnPos));
                    shooterComponent.getProjectiles().add(projectileRef);
                }
            });

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

    // Tick all tracked projectiles and process removals
    // in updateProjectiles, add ref parameter
    private void updateProjectiles(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DeployableProjectileShooterComponent shooterComponent) {
        List<Ref<EntityStore>> projectiles = shooterComponent.getProjectiles();
        List<Ref<EntityStore>> projectilesForRemoval = shooterComponent.getProjectilesForRemoval();
        projectiles.removeAll(Collections.singleton(null));
        for (Ref<EntityStore> projectile : projectiles) updateProjectile(ref, projectile, shooterComponent, store, commandBuffer);
        for (Ref<EntityStore> projectile : projectilesForRemoval) {
            if (projectile.isValid()) commandBuffer.removeEntity(projectile, RemoveReason.REMOVE);
            projectiles.remove(projectile);
        }
        projectilesForRemoval.clear();
    }

    // Scan projectile path for entity hits and flag inactive projectiles for removal
    private void updateProjectile(@Nonnull Ref<EntityStore> ref, @Nonnull Ref<EntityStore> projectileRef, @Nonnull DeployableProjectileShooterComponent shooterComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!projectileRef.isValid()) { shooterComponent.getProjectilesForRemoval().add(projectileRef); return; }

        TransformComponent projTransform = store.getComponent(projectileRef, TransformComponent.getComponentType());
        if (projTransform == null) { shooterComponent.getProjectilesForRemoval().add(projectileRef); return; }

        DeployableProjectileComponent deployableProjectileComponent = store.getComponent(projectileRef, DeployableProjectileComponent.getComponentType());
        if (deployableProjectileComponent == null) { shooterComponent.getProjectilesForRemoval().add(projectileRef); return; }

        Vector3d projPos = projTransform.getPosition();
        Vector3d prevPos = deployableProjectileComponent.getPreviousTickPosition();
        Vector3d increment = new Vector3d((projPos.x - prevPos.x) * 0.1, (projPos.y - prevPos.y) * 0.1, (projPos.z - prevPos.z) * 0.1);
        AtomicReference<Boolean> hit = new AtomicReference<>(Boolean.FALSE);

        for (int j = 0; j < 10; ++j) {
            if (hit.get()) break;
            Vector3d scanPos = deployableProjectileComponent.getPreviousTickPosition().clone();
            scanPos.x += increment.x * j;
            scanPos.y += increment.y * j;
            scanPos.z += increment.z * j;
            if (getDebugVisuals()) DebugUtils.addSphere(store.getExternalData().getWorld(), scanPos, new Vector3f(1.0F, 1.0F, 1.0F), 0.1, 5.0F);
            for (Ref<EntityStore> targetEntityRef : TargetUtil.getAllEntitiesInSphere(scanPos, 0.1, store)) {
                if (hit.get()) return;
                if (targetEntityRef.equals(ref)) continue; // skip the turret itself
                if (!isValidTarget(ref, store, targetEntityRef)) continue; // skip invalid targets
                projectileHit(targetEntityRef, projectileRef, shooterComponent, store, commandBuffer);
                hit.set(Boolean.TRUE);
            }
        }

        deployableProjectileComponent.setPreviousTickPosition(projPos);

        // Remove projectile if it has stopped moving
        if (!hit.get()) {
            StandardPhysicsProvider physicsComponent = store.getComponent(projectileRef, StandardPhysicsProvider.getComponentType());
            if (physicsComponent != null && physicsComponent.getState() != STATE.ACTIVE) shooterComponent.getProjectilesForRemoval().add(projectileRef);
        }
    }

    // Apply damage, knockback, and sound on entity hit then flag projectile for removal
    private void projectileHit(@Nonnull Ref<EntityStore> ref, @Nonnull Ref<EntityStore> projectileRef, @Nonnull DeployableProjectileShooterComponent shooterComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        DamageSystems.executeDamage(ref, commandBuffer, new Damage(new Damage.EntitySource(ref), DamageCause.PHYSICAL, projectileDamage));

        TransformComponent projectileTransform = store.getComponent(projectileRef, TransformComponent.getComponentType());
        assert projectileTransform != null;
        Vector3d projectilePosition = projectileTransform.getPosition().clone();

        if (projectileKnockback != null) {
            float projectileRotationYaw = projectileTransform.getRotation().getYaw();
            store.getExternalData().getWorld().execute(() -> {
                if (ref.isValid()) applyKnockback(ref, projectilePosition, projectileRotationYaw, store);
            });
        }

        DeployablesUtils.playSoundEventsAtEntity(ref, commandBuffer, projectileHitLocalSoundEventIndex, projectileHitWorldSoundEventIndex, projectilePosition);
        shooterComponent.getProjectilesForRemoval().add(projectileRef);
    }

    // Calculate and apply knockback velocity to the hit entity
    private void applyKnockback(@Nonnull Ref<EntityStore> targetRef, @Nonnull Vector3d attackerPos, float attackerYaw, @Nonnull Store<EntityStore> store) {
        KnockbackComponent knockbackComponent = store.ensureAndGetComponent(targetRef, KnockbackComponent.getComponentType());
        TransformComponent transformComponent = store.getComponent(targetRef, TransformComponent.getComponentType());
        assert transformComponent != null;
        knockbackComponent.setVelocity(projectileKnockback.calculateVector(attackerPos, attackerYaw, transformComponent.getPosition()));
        knockbackComponent.setVelocityType(projectileKnockback.getVelocityType());
        knockbackComponent.setVelocityConfig(projectileKnockback.getVelocityConfig());
        knockbackComponent.setDuration(projectileKnockback.getDuration());
    }
}