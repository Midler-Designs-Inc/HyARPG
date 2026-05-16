package com.example.hyarpg.utils.abilities.knight;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

// Mod Imports
import com.example.hyarpg.utils.abilities.Ability;


// Java Imports
import java.util.List;
import org.joml.Vector3d;

public class Taunt extends Ability {

    private static final float RANGE = 15f;
    private static final float HEIGHT = 5f;

    public Taunt() {
        super("Ability_Taunt", DefaultEntityStatTypes.getStamina(), 5f, false, 3, false, List.of(), false);
    }

    @Override
    public void execute(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer) {
        Store<EntityStore> store = ref.getStore();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d origin = transform.getPosition();
        Vector3d position = new Vector3d(origin.x, origin.y, origin.z);
        Vector3d min = new Vector3d(position.x - RANGE, position.y - HEIGHT, position.z - RANGE);
        Vector3d max = new Vector3d(position.x + RANGE, position.y + HEIGHT, position.z + RANGE);

        for (Ref<EntityStore> targetRef : TargetUtil.getAllEntitiesInBox(min, max, store)) {
            if (!targetRef.isValid() || targetRef.equals(ref)) continue;

            TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTransform == null) continue;

            Vector3d targetPos = targetTransform.getPosition();
            double dx = targetPos.x - position.x;
            double dy = targetPos.y - position.y;
            double dz = targetPos.z - position.z;

            if (dx * dx + dz * dz > RANGE * RANGE) continue;
            if (dy > HEIGHT || dy < 0.0f) continue;

            NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npc == null) continue;

            Role role = npc.getRole();
            if (role == null) continue;

            if (!role.isFriendly(ref, store)) {
                role.setMarkedTarget("LockedTarget", ref);
                npc.onFlockSetState(targetRef, "Alerted", null, store);
            }
        }
    }

}