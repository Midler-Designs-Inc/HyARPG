package com.example.hyarpg.utils.abilities.juggernaut;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.utils.abilities.Ability;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

// Java Imports
import java.util.List;

public class Chain_Pull extends Ability {

    private static final float RANGE = 15f;
    private static final float HEIGHT = 5f;
    private static final float UP_FORCE = 25f;
    private static final float PULL_STRENGTH_MULTIPLIER = 5f;

    public Chain_Pull() {
        super("Ability_Chain_Pull", DefaultEntityStatTypes.getStamina(), 5f, false, 3, false, List.of());
    }

    @Override
    public void execute(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer) {
        Store<EntityStore> store = ref.getStore();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d origin = transform.getPosition();
        Vector3d min = new Vector3d(origin.x - RANGE, origin.y - HEIGHT, origin.z - RANGE);
        Vector3d max = new Vector3d(origin.x + RANGE, origin.y + HEIGHT, origin.z + RANGE);

        for (Ref<EntityStore> targetRef : TargetUtil.getAllEntitiesInBox(min, max, store)) {
            if (!targetRef.isValid() || targetRef.equals(ref)) continue;

            TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTransform == null) continue;

            Vector3d targetPos = targetTransform.getPosition();
            double dx = targetPos.x - origin.x;
            double dy = targetPos.y - origin.y;
            double dz = targetPos.z - origin.z;

            if (dx * dx + dz * dz > RANGE * RANGE) continue;
            if (dy > HEIGHT || dy < -HEIGHT) continue;

            NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npc == null) continue;

            Role role = npc.getRole();
            if (role == null || role.isFriendly(ref, store)) continue;

            Velocity vc = store.getComponent(targetRef, Velocity.getComponentType());
            if (vc == null) continue;

            // Launch upward first
            Vector3d upward = new Vector3d(0, UP_FORCE, 0);
            vc.addInstruction(upward, new VelocityConfig(), ChangeVelocityType.Add);

            // Pull toward player, scaled by distance so they stop near player
            double distance = Math.sqrt(dx * dx + dz * dz);
            Vector3d pull = new Vector3d(origin.x - targetPos.x, 0, origin.z - targetPos.z);
            pull.normalize();
            pull.scale(distance * PULL_STRENGTH_MULTIPLIER);
            vc.addInstruction(pull, new VelocityConfig(), ChangeVelocityType.Add);
        }
    }

}