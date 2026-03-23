package com.example.hyarpg.subclasses;

import com.hypixel.hytale.builtin.deployables.component.DeployableComponent;
import com.hypixel.hytale.builtin.deployables.config.DeployableAoeConfig;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;

public class FixedDeployableAoeConfig extends DeployableAoeConfig {

    public static final BuilderCodec<FixedDeployableAoeConfig> CODEC = BuilderCodec.builder(
            FixedDeployableAoeConfig.class, FixedDeployableAoeConfig::new, DeployableAoeConfig.CODEC).build();

    @Override
    protected void attackTarget(@Nonnull Ref<EntityStore> targetRef, @Nonnull Ref<EntityStore> ownerRef, @Nonnull DamageCause damageCause, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // Get the real player ref from the deployable's owner
        DeployableComponent dc = commandBuffer.getComponent(ownerRef, DeployableComponent.getComponentType());
        Ref<EntityStore> playerRef = dc != null ? dc.getOwner() : ownerRef;
        Ref<EntityStore> damageSource = (playerRef != null && playerRef.isValid()) ? playerRef : ownerRef;

        if (damageAmount <= 0.0F) return;
        Damage damageEntry = new Damage(new Damage.EntitySource(damageSource), damageCause, damageAmount);
        if (targetRef.equals(damageSource)) damageEntry.setSource(Damage.NULL_SOURCE);
        DamageSystems.executeDamage(targetRef, commandBuffer, damageEntry);
    }

    @Override
    protected void handleDetection(@Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> deployableRef, @Nonnull DeployableComponent deployableComponent, @Nonnull Vector3d position, float radius, @Nonnull DamageCause damageCause) {
        DamageCause resolvedCause = getDamageCause();
        Ref<EntityStore> ownerRef = deployableComponent.getOwner();

        for (Ref<EntityStore> targetRef : TargetUtil.getAllEntitiesInSphere(position, (double) radius, store)) {
            if (targetRef == null || !targetRef.isValid()) continue;
            if (targetRef.equals(deployableRef)) continue;
            if (targetRef.equals(ownerRef)) continue;

            // Skip players
            if (store.getComponent(targetRef, Player.getComponentType()) != null) continue;

            // Skip other deployables
            if (store.getComponent(targetRef, DeployableComponent.getComponentType()) != null) continue;

            // Only attack hostile NPCs
            NPCEntity npcEntity = store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npcEntity == null || !npcEntity.getCanCauseDamage(deployableRef, store)) continue;

            attackTarget(targetRef, deployableRef, resolvedCause, commandBuffer);
        }
    }
}