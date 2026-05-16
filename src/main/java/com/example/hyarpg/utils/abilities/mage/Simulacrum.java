package com.example.hyarpg.utils.abilities.mage;

// Hytale Imports
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.modules.Module_RPGSystem;
import com.example.hyarpg.utils.abilities.Ability;
import com.example.hyarpg.utils.rooms.RoomFloodFill;
import com.example.hyarpg.components.Component_RPG_Enemy;
import com.example.hyarpg.components.Component_Simulacrum;

// Java Imports
import java.time.Duration;
import java.util.List;
import org.joml.Vector3f;
import org.joml.Vector3d;

public class Simulacrum extends Ability {

    private static final int MAX_BLINK_DISTANCE = 10;
    private static final String SIMULACRUM_ROLE = "Role_Simulacrum";

    public Simulacrum() {
        super("Ability_Simulacrum", DefaultEntityStatTypes.getMana(), 10f, false, 30, false, List.of(), false);
    }

    @Override
    public void execute(Ref<EntityStore> ref, CommandBuffer<EntityStore> commandBuffer) {
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        // get required components
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null || playerRef == null) return;

        // get player transform for spawn origin and look direction
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        // capture spawn origin — simulacrum spawns here
        Vector3d spawnOrigin = new Vector3d(transform.getPosition());

        // get player max mana — simulacrum HP will be set to this value
        EntityStatMap statMap = store.getComponent(ref, EntityStatsModule.get().getEntityStatMapComponentType());
        if (statMap == null) return;
        EntityStatValue manaStat = statMap.get(DefaultEntityStatTypes.getMana());
        if (manaStat == null) return;
        float simulacrumHp = manaStat.getMax();

        // get look transform for blink direction
        var look = com.hypixel.hytale.server.core.util.TargetUtil.getLook(ref, commandBuffer);
        Vector3d lookOrigin = look.getPosition();
        Vector3d lookDir = new Vector3d(look.getDirection()).normalize();

        world.execute(() -> {
            // raycast up to MAX_BLINK_DISTANCE blocks along look direction
            Vector3d blinkTarget = raycastToSolid(world, lookOrigin, lookDir, MAX_BLINK_DISTANCE);

            // find a safe 1x3x1 position near the blink target
            Vector3d teleportPos = findSafePosition(world, blinkTarget.x, blinkTarget.y, blinkTarget.z);

            // teleport the player to the safe position
            Teleport teleport = Teleport.createForPlayer(world, teleportPos, transform.getRotation().clone());
            store.addComponent(ref, Teleport.getComponentType(), teleport);

            // spawn the simulacrum NPC at the player's original position
            int roleIndex = NPCPlugin.get().getIndex(SIMULACRUM_ROLE);
            if (roleIndex < 0) return;

            NPCPlugin.get().spawnEntity(store, roleIndex, spawnOrigin, new Rotation3f(0, transform.getRotation().yaw(), 0), null, null,
                (npcEntity, simulacrumRef, s) -> {
                    // add the simulacrum component so the ticking system can fire the arcane missiles
                    s.addComponent(simulacrumRef, Component_Simulacrum.getComponentType(), new Component_Simulacrum(ref));

                    // add RPG enemy component at player's level so damage pipeline scales correctly
                    Component_RPG_Enemy simulacrumEnemy = new Component_RPG_Enemy(rpgPlayer.gearScore);
                    s.addComponent(simulacrumRef, Module_RPGSystem.componentTypeRPGEnemy, simulacrumEnemy);

                    // add the despawn component with a 30 second timer
                    TimeResource time = s.getResource(TimeResource.getResourceType());
                    s.addComponent(simulacrumRef, DespawnComponent.getComponentType(), new DespawnComponent(time.getNow().plus(Duration.ofSeconds(30L))));

                    // set simulacrum HP to player's max mana via stat modifier
                    EntityStatMap npcStatMap = s.getComponent(simulacrumRef, EntityStatsModule.get().getEntityStatMapComponentType());
                    if (npcStatMap == null) return;

                    int healthIndex = DefaultEntityStatTypes.getHealth();
                    EntityStatValue healthStat = npcStatMap.get(healthIndex);
                    if (healthStat == null) return;

                    // compute delta to reach target HP and apply as modifier
                    float currentMax = healthStat.getMax();
                    float delta = simulacrumHp - currentMax;
                    npcStatMap.putModifier(healthIndex, "SIMULACRUM_HP", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, delta));
                    npcStatMap.setStatValue(healthIndex, simulacrumHp);

                    // alert nearby hostile NPCs to the simulacrum's presence on spawn
                    float aggroRange = 20f;
                    Vector3d min = new Vector3d(spawnOrigin.x - aggroRange, spawnOrigin.y - 5, spawnOrigin.z - aggroRange);
                    Vector3d max = new Vector3d(spawnOrigin.x + aggroRange, spawnOrigin.y + 5, spawnOrigin.z + aggroRange);
                    for (Ref<EntityStore> nearbyRef : TargetUtil.getAllEntitiesInBox(min, max, s)) {
                        if (!nearbyRef.isValid() || nearbyRef.equals(simulacrumRef)) continue;
                        NPCEntity npc = s.getComponent(nearbyRef, NPCEntity.getComponentType());
                        if (npc == null) continue;
                        Role role = npc.getRole();
                        if (role == null || role.isFriendly(simulacrumRef, s)) continue;
                        role.setMarkedTarget("LockedTarget", simulacrumRef);
                        npc.onFlockSetState(nearbyRef, "Alerted", null, s);
                    }
                }
            );
        });
    }

    // steps along look direction until hitting a solid block or reaching max distance
    private Vector3d raycastToSolid(World world, Vector3d origin, Vector3d dir, int maxBlocks) {
        Vector3d pos = new Vector3d(origin);
        for (int i = 0; i < maxBlocks; i++) {
            pos.add(dir.x, dir.y, dir.z);
            long chunkIndex = ChunkUtil.indexChunkFromBlock((int) pos.x, (int) pos.z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            if (chunk == null) break;
            int blockId = chunk.getBlock((int) pos.x, (int) pos.y, (int) pos.z);
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            if (blockType != null && RoomFloodFill.isStructural(blockType)) {
                // hit a solid — step back one to stay in air
                pos.sub(dir.x, dir.y, dir.z);
                break;
            }
        }
        return pos;
    }

    // finds a safe 1x3x1 air gap position near the origin
    private Vector3d findSafePosition(World world, double x, double y, double z) {
        if (isPositionSafe(world, x, y, z)) return new Vector3d(x, y, z);

        for (int r = 1; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;
                    if (isPositionSafe(world, x + dx, y, z + dz)) return new Vector3d(x + dx, y, z + dz);
                    if (isPositionSafe(world, x + dx, y + 1, z + dz)) return new Vector3d(x + dx, y + 1, z + dz);
                }
            }
        }

        return new Vector3d(x, y, z);
    }

    // checks for a solid floor and 3 blocks of clear air above it
    private boolean isPositionSafe(World world, double x, double y, double z) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock((int) x, (int) z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            if (chunk == null) return false;

            int blockAtFeet = chunk.getBlock((int) x, (int) y,     (int) z);
            int blockAtHead = chunk.getBlock((int) x, (int) y + 1, (int) z);

            BlockType feet = BlockType.getAssetMap().getAsset(blockAtFeet);
            BlockType head = BlockType.getAssetMap().getAsset(blockAtHead);

            boolean feetClear = !RoomFloodFill.isStructural(feet);
            boolean headClear = !RoomFloodFill.isStructural(head);

            return feetClear && headClear;
        } catch (Exception e) {
            return false;
        }
    }
}