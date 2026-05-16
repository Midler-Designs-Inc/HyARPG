package com.example.hyarpg.ticking_systems;

// Hytale Imports
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.EffectOp;
import com.hypixel.hytale.protocol.EntityEffectUpdate;
import com.hypixel.hytale.protocol.EntityEffectsUpdate;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;

// Java Imports
import javax.annotation.Nonnull;
import java.util.Set;
import org.joml.Vector3d;

public class System_SoftTargeting extends EntityTickingSystem<EntityStore> {

    // max angular radius of the targeting cone in radians — widens or narrows the reticule feel
    private static final double MAX_ANGLE_RAD = Math.toRadians(5.0);

    // hard distance cutoff — NPCs beyond this range are never considered
    private static final float  MAX_TARGET_RANGE = 40f;

    // how long in ms the current target is held after aim drifts off it, prevents flicker
    private static final long TARGET_HOLD_MS = 100L;

    // highlight effect index resolved lazily once assets are loaded
    private int highlightEffectIndex = Integer.MIN_VALUE;

    private final ComponentType<EntityStore, Component_RPG_Player> componentType;

    public System_SoftTargeting(ComponentType<EntityStore, Component_RPG_Player> componentType) {
        this.componentType = componentType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() { return Query.and(this.componentType); }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        // resolve highlight effect index lazily once assets are loaded
        if (highlightEffectIndex == Integer.MIN_VALUE) highlightEffectIndex = EntityEffect.getAssetMap().getIndex("TargetedEffect");
        if (highlightEffectIndex == Integer.MIN_VALUE) return;

        // get the ref and applicable components
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Component_RPG_Player rpg = archetypeChunk.getComponent(index, Module_RPGSystem.componentTypeRPGPlayer);
        TransformComponent playerTransform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        HeadRotation headRotation = archetypeChunk.getComponent(index, HeadRotation.getComponentType());
        EntityTrackerSystems.EntityViewer viewer = archetypeChunk.getComponent(index, EntityTrackerSystems.EntityViewer.getComponentType());
        if (rpg == null || playerTransform == null || headRotation == null || viewer == null) return;

        // build eye-level origin and normalized look direction from head rotation
        Vector3d origin  = new Vector3d(playerTransform.getPosition()).add(0, 1.5, 0);
        Vector3d lookDir = new Vector3d(headRotation.getDirection());

        // scan visible entities for the closest NPC inside the aim cone
        Ref<EntityStore> bestTarget = null;
        double bestDist = Double.MAX_VALUE;
        for (Ref<EntityStore> candidate : viewer.visible) {
            // bail if no valid NPC candidate
            if (candidate == null || !candidate.isValid() || store.getComponent(candidate, NPCEntity.getComponentType()) == null) continue;

            // get the NPC transform component or bail
            TransformComponent npcTransform = store.getComponent(candidate, TransformComponent.getComponentType());
            if (npcTransform == null) continue;

            // get the NPC model bounding box and compute its true world-space center point
            ModelComponent modelComponent = store.getComponent(candidate, ModelComponent.getComponentType());
            Vector3d npcPos;
            if (modelComponent != null && modelComponent.getModel() != null) {
                var bb = modelComponent.getModel().getBoundingBox();
                npcPos = new Vector3d(npcTransform.getPosition()).add(bb.middleX(), bb.middleY(), bb.middleZ());
            } else {
                // fallback center estimate if no model available
                npcPos = new Vector3d(npcTransform.getPosition()).add(0, 1, 0);
            }
            Vector3d toNPC  = new Vector3d(npcPos).sub(origin);

            // reject if beyond max range
            double dist = toNPC.length();
            if (dist > MAX_TARGET_RANGE) continue;

            // compute the angle between look direction and the vector to the NPC — this is the screen-space cone test
            double cosAngle = Math.max(-1.0, Math.min(1.0, toNPC.dot(lookDir) / dist));
            double angle    = Math.acos(cosAngle);

            // widen the cone at close range to compensate for angular distortion when near large enemies
            double effectiveMaxAngle = MAX_ANGLE_RAD * Math.max(1.0, 8.0 / dist);

            // reject if outside the cone, otherwise track closest valid candidate
            if (angle > effectiveMaxAngle) continue;
            if (dist < bestDist) {
                bestTarget = candidate;
                bestDist   = dist;
            }
        }

        // apply target hold so the highlight doesn't flicker when aim briefly drifts
        long now = System.currentTimeMillis();
        Ref<EntityStore> previous = rpg.currentTarget;
        if (bestTarget != null) {
            rpg.currentTarget     = bestTarget;
            rpg.targetLastUpdated = now;
        } else if (rpg.currentTarget != null) {
            if (!rpg.currentTarget.isValid()) {
                rpg.currentTarget = null;
            } else if (now - rpg.targetLastUpdated > TARGET_HOLD_MS) {
                rpg.currentTarget = null;
            }
        }

        // send highlight packets only when the target actually changes
        if (rpg.currentTarget == previous) return;
        if (previous != null && previous.isValid()) sendHighlightPacket(ref, previous, store, false);
        if (rpg.currentTarget != null) sendHighlightPacket(ref, rpg.currentTarget, store, true);
    }

    // push an add or remove highlight effect to a single player's viewer queue
    private void sendHighlightPacket(@Nonnull Ref<EntityStore> ref, @Nonnull Ref<EntityStore> npcRef, @Nonnull Store<EntityStore> store, boolean apply) {
        // get the NPC's visible component so we can find this player's viewer
        EntityTrackerSystems.Visible visibleComponent = store.getComponent(npcRef, EntityTrackerSystems.Visible.getComponentType());
        if (visibleComponent == null) return;

        // get the specific viewer entry for this player and guard against stale state
        EntityTrackerSystems.EntityViewer targetViewer = visibleComponent.visibleTo.get(ref);
        if (targetViewer == null) return;
        if (!targetViewer.visible.contains(npcRef)) return;

        // build and queue the per-player effect update — SendPackets flushes it this tick
        targetViewer.queueUpdate(npcRef, new EntityEffectsUpdate(new EntityEffectUpdate[]{
            new EntityEffectUpdate(apply ? EffectOp.Add : EffectOp.Remove, highlightEffectIndex, apply ? 1.0f : 0.0f, apply, false, null)
        }));
    }

    // load order dependencies
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(
                new SystemGroupDependency(Order.AFTER,  EntityTrackerSystems.FIND_VISIBLE_ENTITIES_GROUP),
                new SystemGroupDependency(Order.BEFORE, EntityStore.SEND_PACKET_GROUP)
        );
    }
}