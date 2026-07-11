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
import com.hypixel.hytale.server.npc.role.support.CombatSupport;

// Mod Imports
import com.example.hyarpg.utils.abilities.Ability;

// Java Imports
import java.util.List;
import org.joml.Vector3d;

public class Taunt extends Ability {

    private static final float RANGE = 15f;
    private static final float HEIGHT = 5f;

    // register this ability with its name, resource cost, cooldown and flags
    public Taunt() {
        super("Ability_Taunt", DefaultEntityStatTypes.getStamina(), 5f, false, 3, false, List.of(), false);
    }

    @Override
    public void execute(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer) {
        // grab the store and the caster's transform so we know where the taunt originates
        Store<EntityStore> store = ref.getStore();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        // define a box around the caster to scan for tauntable targets
        Vector3d origin = transform.getPosition();
        Vector3d position = new Vector3d(origin.x, origin.y, origin.z);
        Vector3d min = new Vector3d(position.x - RANGE, position.y - HEIGHT, position.z - RANGE);
        Vector3d max = new Vector3d(position.x + RANGE, position.y + HEIGHT, position.z + RANGE);

        // loop over every entity inside that box
        for (Ref<EntityStore> targetRef : TargetUtil.getAllEntitiesInBox(min, max, store)) {
            // skip invalid entities and the caster itself
            if (!targetRef.isValid() || targetRef.equals(ref)) continue;

            // fetch the target's position, bail if it has none
            TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
            if (targetTransform == null) continue;

            // work out how far away the target actually is
            Vector3d targetPos = targetTransform.getPosition();
            double dx = targetPos.x - position.x;
            double dy = targetPos.y - position.y;
            double dz = targetPos.z - position.z;

            // skip anything outside the taunt radius or vertical height band
            if (dx * dx + dz * dz > RANGE * RANGE) continue;
            if (dy > HEIGHT || dy < 0.0f) continue;

            // fetch the target's NPC/role data so we can check hostility
            NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npc == null) continue;

            Role role = npc.getRole();
            if (role == null) continue;

            // only taunt NPCs that are actually hostile to the caster
            if (CombatSupport.get(targetRef, store).getCanCauseDamage(targetRef, ref, store)) {
                role.setMarkedTarget(targetRef, store, "LockedTarget", ref);
                npc.onFlockSetState(targetRef, "Alerted", null, store);
            }
        }
    }

}