package com.example.hyarpg.modules;

// Hytale Imports
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

// Mod Imports
import com.example.hyarpg.components.Component_RPG_Player;
import com.example.hyarpg.configs.ModConfig;
import com.example.hyarpg.utils.rooms.RoomFloodFill;
import com.example.hyarpg.utils.rooms.TerritoryData;

// Java Imports
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Module_RaidSystem {
    // NPC role IDs to spawn for each wave
    private static final List<List<String>> WAVE_NPC_IDS = List.of(
            List.of("skeleton", "skeleton", "skeleton", "skeleton"),                        // wave 1: 4 skeletons
            List.of("skeleton", "skeleton", "skeleton", "skeleton", "skeleton"),            // wave 2: 5 skeletons
            List.of("skeleton", "skeleton", "skeleton", "skeleton", "skeleton", "skeleton") // wave 3: 6 skeletons
    );

    // Wave parameters
    private static final int WAVE_COUNT = WAVE_NPC_IDS.size();
    private static final int SECONDS_BETWEEN_WAVES = 60;

    // Spawning parameters
    private static final int SPAWN_RING_RADIUS = TerritoryData.TERRITORY_HALF + 8; // ~36 blocks from center
    private static final int SPAWN_SCAN_HEIGHT_OFFSET = TerritoryData.TERRITORY_HALF + 8;
    private static final int SPAWN_SEARCH_ATTEMPTS = 8;

    // Explosion parameters — applied to surviving raid NPCs when the raid ends
//    private static final int POST_RAID_EXPLOSION_DELAY_SECONDS = 300; // 5 minutes after the final wave spawns
    private static final int POST_RAID_EXPLOSION_DELAY_SECONDS = 20; // 5 minutes after the final wave spawns
    private static final int EXPLOSION_BLOCK_RADIUS = 4;
    private static final float EXPLOSION_ENTITY_RADIUS = 6f;
    private static final float EXPLOSION_ENTITY_DAMAGE = 80f;

    // Inner tick parameters
    private static final int INNER_TICK_INTERVAL_SECONDS = 60;
    private Instant lastInnerTick;
    private final Random random;

    // Active raid groups — each tracks the spawned NPCs and their target player for one ongoing raid
    private final List<RaidGroup> activeRaids = new ArrayList<>();

    // Tracks a single active raid's spawned NPCs, target player, and end time
    private static class RaidGroup {
        final Ref<EntityStore> targetPlayerRef;
        final World world;
        final List<Ref<EntityStore>> npcRefs = new ArrayList<>();
        final long raidEndMs;

        RaidGroup(Ref<EntityStore> targetPlayerRef, World world) {
            this.targetPlayerRef = targetPlayerRef;
            this.world = world;
            // raid ends after all waves have had time to spawn plus the configured explosion delay
            this.raidEndMs = System.currentTimeMillis()
                    + ((long)(WAVE_COUNT - 1) * SECONDS_BETWEEN_WAVES * 1000L)
                    + ((long)POST_RAID_EXPLOSION_DELAY_SECONDS * 1000L);
        }

        void addNpc(Ref<EntityStore> npcRef) {
            npcRefs.add(npcRef);
        }

        // Prune dead/invalid NPC refs from this group
        void pruneDeadNpcs() {
            npcRefs.removeIf(ref -> !ref.isValid());
        }

        // Returns true if the group still has at least one living NPC
        boolean isAlive() {
            for (Ref<EntityStore> ref : npcRefs) {
                if (ref.isValid()) return true;
            }
            return false;
        }
    }

    public Module_RaidSystem() {
        this.random = new Random();
        this.lastInnerTick = Instant.now();
    }

    // Outer tick — called externally several times per second
    public void outerTick(){
        // If territory claiming is disabled, raiding as a whole is disabled — do nothing
        if (!ModConfig.get().building.allow_light_well_territory_claim) return;

        // check if the inner tick should fire or not
        long secondsSinceLastTick = Instant.now().getEpochSecond() - lastInnerTick.getEpochSecond();
        if (secondsSinceLastTick >= INNER_TICK_INTERVAL_SECONDS) innerTick();
    }

    // Inner tick — runs once per minute, evaluates every player independently
    private void innerTick() {
        // update the last inner tick time to now
        lastInnerTick = Instant.now();

        // get a time stamp and a raider timer seconds for comparison
        long nowEpochSeconds = Instant.now().getEpochSecond();
        long raidTimerSeconds = ModConfig.get().raids.raid_timer_in_minutes * 60L;

        // loop over each world and then on that worlds next execute loop over players
        for (World world : Universe.get().getWorlds().values().toArray(new World[0])) {
            world.execute(() -> {
                Store<EntityStore> store = world.getEntityStore().getStore();

                // loop over players
                for (PlayerRef playerRef : Universe.get().getPlayers()) {
                    try {
                        // player isn't valid ignore them
                        if (!playerRef.isValid()) continue;

                        // could not get the ref for this player ignore them
                        Ref<EntityStore> ref = playerRef.getReference();
                        if (ref == null) continue;

                        // this player does not have our main mod component ignore them
                        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
                        if (rpgPlayer == null) continue;

                        // check if base raiding is enabled and if so, check if the player's base should be raided
                        if (ModConfig.get().raids.allow_base_raids) {
                            long secondsSinceLastBaseRaid = nowEpochSeconds - rpgPlayer.lastBaseRaid;
                            if (secondsSinceLastBaseRaid >= raidTimerSeconds) {
                                if (random.nextInt(100) < ModConfig.get().raids.raid_chance) {
                                    startBaseRaid(playerRef, ref, store, world);
                                }
                            }
                        }

                        // check if player raiding is enabled and if so, check if hte player should be raided
                        if (ModConfig.get().raids.allow_player_raids) {
                            long secondsSinceLastPlayerRaid = nowEpochSeconds - rpgPlayer.lastPlayerRaid;
                            if (secondsSinceLastPlayerRaid >= raidTimerSeconds) {
                                if (random.nextInt(100) < ModConfig.get().raids.raid_chance) {
                                    startPlayerRaid(playerRef, ref, store, world);
                                }
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("[RaidSystem] Error evaluating raid for player: " + e.getMessage());
                    }
                }
            });
        }
    }

    // Base raid — targets the player's Light Well territory
    public void startBaseRaid(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, World world) {
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;

        // Stamp the timer immediately regardless of outcome
        rpgPlayer.lastBaseRaid = Instant.now().getEpochSecond();

        TerritoryData territory = rpgPlayer.territory;
        if (territory == null) {
            System.err.println("[RaidSystem] Base raid fired for " + playerRef.getUsername() + " but they have no territory — skipping.");
            return;
        }

        // Spawn center is the territory's light well center
        int cx = territory.getCenter().x;
        int cy = territory.getCenter().y;
        int cz = territory.getCenter().z;

        RaidGroup group = new RaidGroup(ref, world);
        spawnWaves(world, store, cx, cy, cz, true, group);
        activeRaids.add(group);
        System.out.println("[RaidSystem] Base raid started for " + playerRef.getUsername() + " at territory (" + cx + ", " + cy + ", " + cz + ")");
    }

    // Player raid — targets the player's current position
    public void startPlayerRaid(PlayerRef playerRef, Ref<EntityStore> ref, Store<EntityStore> store, World world) {
        Component_RPG_Player rpgPlayer = store.getComponent(ref, Module_RPGSystem.componentTypeRPGPlayer);
        if (rpgPlayer == null) return;

        // Stamp the timer immediately regardless of outcome
        rpgPlayer.lastPlayerRaid = Instant.now().getEpochSecond();

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) return;

        int cx = (int) Math.floor(transform.getPosition().getX());
        int cy = (int) Math.floor(transform.getPosition().getY());
        int cz = (int) Math.floor(transform.getPosition().getZ());

        RaidGroup group = new RaidGroup(ref, world);
        spawnWaves(world, store, cx, cy, cz, /* outsideTerritory */ false, group);
        activeRaids.add(group);
        System.out.println("[RaidSystem] Player raid started for " + playerRef.getUsername() + " at (" + cx + ", " + cy + ", " + cz + ")");
    }

    // Wave scheduling — fires each wave SECONDS_BETWEEN_WAVES apart, then schedules the raid end callback
    private void spawnWaves(World world, Store<EntityStore> store, int cx, int cy, int cz, boolean spawnOutsideTerritory, RaidGroup group) {
        for (int waveIndex = 0; waveIndex < WAVE_COUNT; waveIndex++) {
            final int wave = waveIndex;
            final long delayMs = (long) wave * SECONDS_BETWEEN_WAVES * 1000L;

            // Schedule each wave on a background thread, then re-enter the world executor for the actual spawn
            Thread.ofVirtual().start(() -> {
                try {
                    if (delayMs > 0) Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                world.execute(() -> spawnWave(world, store, cx, cy, cz, wave, spawnOutsideTerritory, group));
            });
        }

        // Schedule the raid end callback to fire after all waves plus the explosion delay
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(group.raidEndMs - System.currentTimeMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            world.execute(() -> onRaidEnd(group, world));
        });
    }

    // Single wave spawn
    private void spawnWave(World world, Store<EntityStore> store, int cx, int cy, int cz, int waveIndex, boolean spawnOutsideTerritory, RaidGroup group) {
        List<String> npcsToSpawn = WAVE_NPC_IDS.get(waveIndex);
        System.out.println("[RaidSystem] Spawning wave " + (waveIndex + 1) + " (" + npcsToSpawn.size() + " enemies)");

        for (String npcId : npcsToSpawn) {
            Vector3d spawnPos = findSafeSpawnPosition(world, cx, cy, cz, spawnOutsideTerritory);
            Vector3f rotation = new Vector3f(0f, (float)(random.nextDouble() * Math.PI * 2), 0f);

            var spawnResult = NPCPlugin.get().spawnNPC(store, npcId, null, spawnPos, rotation);
            if (spawnResult == null) continue;

            Ref<EntityStore> npcRef = spawnResult.first();
            NPCEntity npcEntity = (NPCEntity) spawnResult.second();

            // set leash point to the light well center so ReturnHome walks them toward the base
            npcEntity.saveLeashInformation(new Vector3d(cx, cy, cz), rotation);

            group.addNpc(npcRef);
        }
    }

    // Raid end callback — explodes any surviving NPCs and cleans up the group from the active raids list
    private void onRaidEnd(RaidGroup group, World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();

        group.pruneDeadNpcs();

        // explode any NPCs that are still alive
        if (group.isAlive()) {
            System.out.println("[RaidSystem] Raid ended with " + group.npcRefs.size() + " surviving NPCs — detonating.");
            for (Ref<EntityStore> npcRef : group.npcRefs) {
                if (!npcRef.isValid()) continue;
                try {
                    explodeNpc(npcRef, store, chunkStore);
                } catch (Exception e) {
                    System.err.println("[RaidSystem] Error exploding NPC: " + e.getMessage());
                }
            }
        }

        // remove this group from the active raids list
        activeRaids.remove(group);
        System.out.println("[RaidSystem] Raid group cleaned up.");
    }

    // Explodes a single surviving raid NPC — damages nearby blocks and players, then removes the NPC cleanly
    private static void explodeNpc(Ref<EntityStore> npcRef, Store<EntityStore> store, Store<ChunkStore> chunkStore) {
        TransformComponent transform = store.getComponent(npcRef, TransformComponent.getComponentType());
        if (transform == null) return;

        Vector3d pos = transform.getPosition();
        int cx = (int) Math.floor(pos.x);
        int cy = (int) Math.floor(pos.y);
        int cz = (int) Math.floor(pos.z);

        // damage blocks in a sphere around the NPC
        for (int x = cx - EXPLOSION_BLOCK_RADIUS; x <= cx + EXPLOSION_BLOCK_RADIUS; x++) {
            for (int y = cy - EXPLOSION_BLOCK_RADIUS; y <= cy + EXPLOSION_BLOCK_RADIUS; y++) {
                for (int z = cz - EXPLOSION_BLOCK_RADIUS; z <= cz + EXPLOSION_BLOCK_RADIUS; z++) {
                    if (Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy) + (z-cz)*(z-cz)) > EXPLOSION_BLOCK_RADIUS) continue;
                    try {
                        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
                        Ref<ChunkStore> chunkRef = ((ChunkStore) chunkStore.getExternalData()).getChunkReference(chunkIndex);
                        if (chunkRef == null || !chunkRef.isValid()) continue;

                        WorldChunk worldChunk = (WorldChunk) chunkStore.getComponent(chunkRef, WorldChunk.getComponentType());
                        if (worldChunk == null) continue;

                        int blockId = worldChunk.getBlock(x, y, z);
                        BlockType bt = BlockType.getAssetMap().getAsset(blockId);
                        if (bt == null || !RoomFloodFill.isStructural(bt)) continue;

                        // break the block suppressing drops
                        BlockHarvestUtils.performBlockBreak(null, null, new Vector3i(x, y, z), 2048, chunkRef, store, chunkStore);
                    } catch (Exception e) {
                        System.err.println("[RaidSystem] Error breaking block during explosion: " + e.getMessage());
                    }
                }
            }
        }

        // damage nearby players only — skip NPCs so they don't hurt each other or drop loot
        Damage.EnvironmentSource explosionSource = new Damage.EnvironmentSource("explosion");
        List<Ref<EntityStore>> nearby = TargetUtil.getAllEntitiesInBox(
                new Vector3d(pos.x - EXPLOSION_ENTITY_RADIUS, pos.y - EXPLOSION_ENTITY_RADIUS, pos.z - EXPLOSION_ENTITY_RADIUS),
                new Vector3d(pos.x + EXPLOSION_ENTITY_RADIUS, pos.y + EXPLOSION_ENTITY_RADIUS, pos.z + EXPLOSION_ENTITY_RADIUS),
                store
        );
        for (Ref<EntityStore> targetRef : nearby) {
            if (!targetRef.isValid() || targetRef.equals(npcRef)) continue;
            // skip NPCs — only damage players
            if (store.getComponent(targetRef, NPCEntity.getComponentType()) != null) continue;
            try {
                TransformComponent targetTransform = store.getComponent(targetRef, TransformComponent.getComponentType());
                if (targetTransform == null) continue;
                double distance = pos.distanceTo(targetTransform.getPosition());
                if (distance > EXPLOSION_ENTITY_RADIUS) continue;
                // scale damage by distance falloff
                float damage = EXPLOSION_ENTITY_DAMAGE * (1f - (float)(distance / EXPLOSION_ENTITY_RADIUS));
                if (damage > 0) DamageSystems.executeDamage(targetRef, store, new Damage(explosionSource, DamageCause.getAssetMap().getAsset("Environment"), damage));
            } catch (Exception e) {
                System.err.println("[RaidSystem] Error damaging entity during explosion: " + e.getMessage());
            }
        }

        // remove the NPC cleanly without triggering death loot or animations
        store.removeEntity(npcRef, RemoveReason.REMOVE);
    }

    // Safe spawn position search
    private Vector3d findSafeSpawnPosition(World world, int cx, int cy, int cz, boolean spawnOutsideTerritory) {
        int radius = spawnOutsideTerritory ? SPAWN_RING_RADIUS : (SPAWN_RING_RADIUS / 2);

        for (int attempt = 0; attempt < SPAWN_SEARCH_ATTEMPTS; attempt++) {
            // Spread attempts evenly around the ring, plus a small random jitter
            double angle = (2 * Math.PI / SPAWN_SEARCH_ATTEMPTS) * attempt + (random.nextDouble() * 0.4);
            int offsetX = (int) Math.round(Math.cos(angle) * radius);
            int offsetZ = (int) Math.round(Math.sin(angle) * radius);

            int scanX = cx + offsetX;
            int scanZ = cz + offsetZ;

            // Scan downward from cy + offset to find solid ground
            int groundY = findGroundY(world, scanX, cy + SPAWN_SCAN_HEIGHT_OFFSET, scanZ);
            if (groundY != Integer.MIN_VALUE) {
                // Spawn 1 block above the found ground block
                return new Vector3d(scanX + 0.5, groundY + 1.0, scanZ + 0.5);
            }
        }

        // Fallback: spawn at a fixed offset from center at original cy — better than nothing
        System.err.println("[RaidSystem] Could not find safe spawn ground after " + SPAWN_SEARCH_ATTEMPTS + " attempts, using fallback position.");
        return new Vector3d(cx + radius + 0.5, cy + 1.0, cz + 0.5);
    }

    // Scan down from (x, startY, z) and returns the Y of the first solid non-fluid block, or Integer.MIN_VALUE if none is found before hitting y=0
    private int findGroundY(World world, int x, int startY, int z) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
        if (chunk == null) return Integer.MIN_VALUE; // chunk not loaded, skip

        int clampedStart = Math.min(startY, 319);
        for (int y = clampedStart; y >= 1; y--) {
            int blockId = chunk.getBlock(x, y, z);
            BlockType bt = BlockType.getAssetMap().getAsset(blockId);
            if (bt != null && RoomFloodFill.isStructural(bt)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }
}