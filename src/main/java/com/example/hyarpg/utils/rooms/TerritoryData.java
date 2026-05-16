package com.example.hyarpg.utils.rooms;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;


// Java
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.joml.Vector3i;

public class TerritoryData {

    // The light well is 1x3x1, centered in the 57x57x57 territory
    public static final int TERRITORY_HALF = 28;

    @SuppressWarnings("unchecked")
    public static final BuilderCodec<TerritoryData> CODEC = BuilderCodec
            .builder(TerritoryData.class, TerritoryData::new)
            .append(new KeyedCodec<>("CenterX", Codec.INTEGER), (d, v) -> d.centerX = v, d -> d.centerX).add()
            .append(new KeyedCodec<>("CenterY", Codec.INTEGER), (d, v) -> d.centerY = v, d -> d.centerY).add()
            .append(new KeyedCodec<>("CenterZ", Codec.INTEGER), (d, v) -> d.centerZ = v, d -> d.centerZ).add()
            .append(new KeyedCodec<>("OwnerUuid", Codec.STRING),
                    (d, v) -> d.ownerUuid = v != null ? UUID.fromString(v) : null,
                    d -> d.ownerUuid != null ? d.ownerUuid.toString() : null).add()
            .append(new KeyedCodec<>("CoOwners", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (d, v) -> {
                        d.coOwners = new ArrayList<>();
                        if (v != null) for (String entry : v) {
                            String[] parts = entry.split(":", 2);
                            if (parts.length == 2) {
                                try { d.coOwners.add(new CoOwnerEntry(UUID.fromString(parts[0]), parts[1])); } catch (Exception ignored) {}
                            }
                        }
                    },
                    d -> d.coOwners.stream().map(e -> e.uuid().toString() + ":" + e.username()).toArray(String[]::new)
            ).add()
            .append(new KeyedCodec<>("CoOwnerRequests", new ArrayCodec<>(Codec.STRING, String[]::new)),
                    (d, v) -> d.coOwnerRequests = v != null ? Arrays.stream(v).map(UUID::fromString).collect(Collectors.toCollection(ArrayList::new)) : new ArrayList<>(),
                    d -> d.coOwnerRequests.stream().map(UUID::toString).toArray(String[]::new)).add()
            .append(new KeyedCodec<>("LastBaseRaid", Codec.LONG),
                    (d, v) -> d.lastBaseRaid = v,
                    d -> d.lastBaseRaid).add()
            .append(new KeyedCodec<>("LastSkippedTime", Codec.LONG),
                    (d, v) -> d.lastSkippedTime = v,
                    d -> d.lastSkippedTime).add()
            .append(new KeyedCodec<>("NextRaid", Codec.STRING),
                    (d, v) -> d.nextRaid = v,
                    d -> d.nextRaid).add()
            .build();

    // Center of the territory — the base block of the light well (bottom of the 1x3x1)
    private int centerX, centerY, centerZ;

    // Ownership
    public record CoOwnerEntry(UUID uuid, String username) {}
    @Nullable private UUID ownerUuid;
    @Nonnull private List<CoOwnerEntry> coOwners = new ArrayList<>();
    private List<UUID> coOwnerRequests = new ArrayList<>();

    // Raid tracking
    private long lastBaseRaid;
    private long lastSkippedTime = 0;
    @Nullable public String nextRaid = null;

    // Required for codec deserialization
    private TerritoryData() {}

    public TerritoryData(Vector3i lightWellBase, UUID ownerUuid) {
        this.centerX = lightWellBase.x;
        this.centerY = lightWellBase.y;
        this.centerZ = lightWellBase.z;
        this.ownerUuid = ownerUuid;
        // start lastBaseRaid at now so the full cooldown window must pass before first raid
        this.lastBaseRaid = Instant.now().getEpochSecond();
    }

    // --- Spatial --- //
    public Vector3i getCenter() { return new Vector3i(centerX, centerY, centerZ); }
    public int getMinX() { return centerX - TERRITORY_HALF; }
    public int getMinY() { return centerY - TERRITORY_HALF + 1; }
    public int getMinZ() { return centerZ - TERRITORY_HALF; }
    public int getMaxX() { return centerX + TERRITORY_HALF; }
    public int getMaxY() { return centerY + TERRITORY_HALF + 1; }
    public int getMaxZ() { return centerZ + TERRITORY_HALF; }
    public boolean contains(int x, int y, int z) {
        return x >= getMinX() && x <= getMaxX()
                && y >= getMinY() && y <= getMaxY()
                && z >= getMinZ() && z <= getMaxZ();
    }
    public boolean isLightWellBlock(int x, int y, int z) {
        return x == centerX && z == centerZ && y >= centerY && y <= centerY + 2;
    }

    // --- Ownership --- //
    @Nullable public UUID getOwnerUuid() { return ownerUuid; }
    public List<CoOwnerEntry> getCoOwners() { return Collections.unmodifiableList(coOwners); }
    public boolean isCoOwner(UUID uuid) { return coOwners.stream().anyMatch(e -> e.uuid().equals(uuid)); }
    public boolean hasAccess(UUID uuid) { return uuid.equals(ownerUuid) || isCoOwner(uuid); }
    public void addCoOwner(UUID uuid, String username) {
        if (!isCoOwner(uuid)) coOwners.add(new CoOwnerEntry(uuid, username));
    }
    public void removeCoOwner(UUID uuid) { coOwners.removeIf(e -> e.uuid().equals(uuid)); }

    // --- Co-owner requests --- //
    public List<UUID> getCoOwnerRequests() { return Collections.unmodifiableList(coOwnerRequests); }
    public boolean hasPendingRequest(UUID uuid) { return coOwnerRequests.contains(uuid); }
    public boolean hasLightwellStake(UUID uuid) { return uuid.equals(ownerUuid) || isCoOwner(uuid); }
    public boolean requestCoOwnership(UUID uuid) {
        if (hasLightwellStake(uuid) || coOwnerRequests.contains(uuid)) return false;
        coOwnerRequests.add(uuid);
        return true;
    }
    public boolean approveCoOwner(UUID requesterUuid, UUID approverUuid, String requesterUsername) {
        if (!approverUuid.equals(ownerUuid)) return false;
        if (!coOwnerRequests.contains(requesterUuid)) return false;
        coOwnerRequests.remove(requesterUuid);
        addCoOwner(requesterUuid, requesterUsername);
        return true;
    }
    public boolean denyCoOwner(UUID requesterUuid, UUID approverUuid) {
        if (!approverUuid.equals(ownerUuid)) return false;
        return coOwnerRequests.remove(requesterUuid);
    }

    // --- Raid tracking --- //

    // call when no owners are online this tick — records when offline period started
    public void markSkipped(long nowEpochSeconds) {
        if (lastSkippedTime == 0) lastSkippedTime = nowEpochSeconds;
    }

    // call when an owner comes online — pushes lastBaseRaid forward by the offline duration and clears the skip marker
    public void resumeOnline(long nowEpochSeconds) {
        if (lastSkippedTime != 0) {
            lastBaseRaid += (nowEpochSeconds - lastSkippedTime);
            lastSkippedTime = 0;
        }
    }

    // returns true if enough online time has passed since the last raid
    public boolean isRaidEligible(long nowEpochSeconds, long cooldownSeconds) {
        long effectiveLastRaid = lastBaseRaid + (lastSkippedTime != 0 ? (nowEpochSeconds - lastSkippedTime) : 0);
        return (nowEpochSeconds - effectiveLastRaid) >= cooldownSeconds;
    }

    // resets the raid clock — call when a base raid fires
    public void onRaidStarted(long nowEpochSeconds) {
        lastBaseRaid = nowEpochSeconds;
        lastSkippedTime = 0;
        nextRaid = null;
    }

    @Override
    public String toString() {
        return "TerritoryData{center=(" + centerX + "," + centerY + "," + centerZ + ")"
                + ", owner=" + ownerUuid
                + ", bounds=(" + getMinX() + "-" + getMaxX()
                + ", " + getMinY() + "-" + getMaxY()
                + ", " + getMinZ() + "-" + getMaxZ() + ")}";
    }
}