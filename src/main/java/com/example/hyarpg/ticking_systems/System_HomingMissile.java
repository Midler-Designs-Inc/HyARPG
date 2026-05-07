package com.example.hyarpg.ticking_systems;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.collision.BlockCollisionProvider;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_HomingMissile;

// Java Imports
import javax.annotation.Nonnull;

public class System_HomingMissile extends EntityTickingSystem<EntityStore> {

    public final ComponentType<EntityStore, Component_HomingMissile> componentType;

    public System_HomingMissile(ComponentType<EntityStore, Component_HomingMissile> componentType) {
        this.componentType = componentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(this.componentType);
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Component_HomingMissile homing = archetypeChunk.getComponent(index, this.componentType);
        if (homing == null) return;

        // count down arm time — do nothing until the arc has played out
        if (homing.armTime > 0f) {
            homing.armTime -= dt;
            return;
        }

        // bail if target is gone
        if (homing.targetRef == null || !homing.targetRef.isValid()) {
            commandBuffer.removeComponent(ref, this.componentType);
            commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            return;
        }

        // get missile and target positions
        TransformComponent missileTransform = store.getComponent(ref, TransformComponent.getComponentType());
        TransformComponent targetTransform = store.getComponent(homing.targetRef, TransformComponent.getComponentType());
        if (missileTransform == null || targetTransform == null) return;

        // compute direction from missile to target center mass
        Vector3d missilePos = missileTransform.getPosition();
        Vector3d targetPos = targetTransform.getPosition().clone().add(0, 1.0, 0);

        // get current velocity and speed so we preserve it while steering
        Velocity velocity = store.getComponent(ref, Velocity.getComponentType());
        if (velocity == null) return;

        Vector3d vel = velocity.getVelocity().clone();
        double speed = vel.length();

        // missile has stopped — it hit something, apply damage and clean up
        if (speed < 5) {
            homing.slowTime += dt;
            if (homing.slowTime > 0.1f) {
                DamageSystems.executeDamage(homing.targetRef, commandBuffer,
                    new Damage(
                        new Damage.EntitySource(homing.casterRef),
                        DamageCause.getAssetMap().getAsset("MainHand"),
                        1f
                    )
                );
                commandBuffer.removeComponent(ref, this.componentType);
                commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            }
            return;
        } else {
            homing.slowTime = 0f;
        }

        // turn off collision if we haven't already
        if (!homing.blockCollisionDisabled) {
            StandardPhysicsProvider spp = store.getComponent(ref, StandardPhysicsProvider.getComponentType());
            if (spp != null) {
                spp.getBlockCollisionProvider().setRequestedCollisionMaterials(0);
                homing.blockCollisionDisabled = true;
            }
        }


        // get the current direction and direction to target
        Vector3d currentDir = vel.clone().normalize();
        Vector3d toTarget = Vector3d.directionTo(missilePos, targetPos);

        // blend current direction toward target direction by turn rate — this gives the curve
        Vector3d newDir = new Vector3d(
            currentDir.x + (toTarget.x - currentDir.x) * homing.turnRate * dt,
            currentDir.y + (toTarget.y - currentDir.y) * homing.turnRate * dt,
            currentDir.z + (toTarget.z - currentDir.z) * homing.turnRate * dt
        ).normalize();

        // write back at original speed
        velocity.getVelocity().assign(newDir.scale(35));
    }
}