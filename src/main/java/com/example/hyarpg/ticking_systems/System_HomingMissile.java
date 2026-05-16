package com.example.hyarpg.ticking_systems;

// Hytale Imports
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_HomingMissile;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

// Java Imports
import javax.annotation.Nonnull;
import org.joml.Vector3d;

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
        Vector3d targetPos = new Vector3d(targetTransform.getPosition()).add(0, 1.0, 0);

        // get current velocity and speed
        Velocity velocity = store.getComponent(ref, Velocity.getComponentType());
        if (velocity == null) return;

        Vector3d vel = new Vector3d(velocity.getVelocity());
        double speed = vel.length();

        // missile has stopped — apply damage and clean up
        if (speed < 5) {
            homing.slowTime += dt;
            if (homing.slowTime > 0.1f) {
                if (homing.aoeDamageRange > 0) {
                    // AOE damage — hit all hostile and neutral NPCs in range of impact
                    Vector3d impactPos = missileTransform.getPosition();
                    Vector3d min = new Vector3d(impactPos.x - homing.aoeDamageRange, impactPos.y - homing.aoeDamageHeight, impactPos.z - homing.aoeDamageRange);
                    Vector3d max = new Vector3d(impactPos.x + homing.aoeDamageRange, impactPos.y + homing.aoeDamageHeight, impactPos.z + homing.aoeDamageRange);
                    for (Ref<EntityStore> aoeTarget : TargetUtil.getAllEntitiesInBox(min, max, store)) {
                        if (!aoeTarget.isValid() || aoeTarget.equals(ref)) continue;
                        NPCEntity npc = store.getComponent(aoeTarget, NPCEntity.getComponentType());
                        if (npc == null) continue;
                        Role role = npc.getRole();
                        if (role == null) continue;
                        if (!role.isFriendly(homing.casterRef, store)) {
                            DamageSystems.executeDamage(aoeTarget, commandBuffer,
                                new Damage(
                                    new Damage.EntitySource(homing.casterRef),
                                    DamageCause.getAssetMap().getAsset(homing.damageType),
                                    homing.damageValue
                                )
                            );
                        }
                    }
                } else {
                    // single target damage
                    DamageSystems.executeDamage(homing.targetRef, commandBuffer,
                        new Damage(
                            new Damage.EntitySource(homing.casterRef),
                            DamageCause.getAssetMap().getAsset(homing.damageType),
                            homing.damageValue
                        )
                    );
                }
                commandBuffer.removeComponent(ref, this.componentType);
                commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            }
            return;
        } else {
            homing.slowTime = 0f;
        }

        // disable block collision once per missile
        if (!homing.blockCollisionDisabled) {
            StandardPhysicsProvider spp = store.getComponent(ref, StandardPhysicsProvider.getComponentType());
            if (spp != null) {
                spp.getBlockCollisionProvider().setRequestedCollisionMaterials(0);
                homing.blockCollisionDisabled = true;
            }
        }

        // get the current direction and direction to target
        Vector3d currentDir = new Vector3d(vel).normalize();
        Vector3d toTarget = new Vector3d(targetPos).sub(missilePos).normalize();

        // blend current direction toward target direction by turn rate — this gives the curve
        Vector3d newDir = new Vector3d(
            currentDir.x + (toTarget.x - currentDir.x) * homing.turnRate * dt,
            currentDir.y + (toTarget.y - currentDir.y) * homing.turnRate * dt,
            currentDir.z + (toTarget.z - currentDir.z) * homing.turnRate * dt
        ).normalize();

        // write back at homing speed
        velocity.getVelocity().set(new Vector3d(newDir).mul(35));
    }
}