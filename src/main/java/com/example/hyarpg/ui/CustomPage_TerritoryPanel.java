package com.example.hyarpg.ui;

// Hytale Imports
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerRespawnPointData;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerWorldData;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Mod Imports
import com.example.hyarpg.utils.rooms.TerritoryData;
import com.example.hyarpg.utils.rooms.WorldRoomRegistry;

// Java Imports
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.joml.Vector3d;
import org.joml.Vector3i;

public class CustomPage_TerritoryPanel extends InteractiveCustomUIPage<CustomPage_TerritoryPanel.PageData> {

    // max co-owners and requests supported by the UI
    private static final int MAX_CO_OWNERS = 10;
    private static final int MAX_REQUESTS  = 10;

    // the territory this panel is showing
    private final TerritoryData territory;

    public CustomPage_TerritoryPanel(@Nonnull PlayerRef playerRef, @Nonnull TerritoryData territory) {
        super(playerRef, CustomPageLifetime.CanDismiss, PageData.CODEC);
        this.territory = territory;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        // load the UI file
        cmd.append("CustomPage_TerritoryPanel.ui");

        // get the viewing player's uuid to determine their role
        PlayerRef viewerRef = store.getComponent(ref, PlayerRef.getComponentType());
        UUID viewerUuid = viewerRef != null ? viewerRef.getUuid() : null;

        boolean isOwner   = viewerUuid != null && viewerUuid.equals(territory.getOwnerUuid());
        boolean isCoOwner = viewerUuid != null && territory.isCoOwner(viewerUuid);

        // bind co-owner remove buttons — owner only
        if (isOwner) {
            for (int i = 1; i <= MAX_CO_OWNERS; i++) {
                final int idx = i;
                events.addEventBinding(CustomUIEventBindingType.Activating, "#TPRemoveCoOwner" + i, EventData.of("Action", "remove:" + i));
            }

            // bind request approve/deny buttons
            for (int i = 1; i <= MAX_REQUESTS; i++) {
                events.addEventBinding(CustomUIEventBindingType.Activating, "#TPApproveRequest" + i, EventData.of("Action", "approve:" + i));
                events.addEventBinding(CustomUIEventBindingType.Activating, "#TPDenyRequest" + i,    EventData.of("Action", "deny:" + i));
            }
        }

        // bind action buttons for non-owners
        if (!isOwner) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TPRequestOwnership",  EventData.of("Action", "request"));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TPWithdrawRequest",   EventData.of("Action", "withdraw_request"));
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TPWithdrawOwnership", EventData.of("Action", "withdraw_ownership"));
        }

        // bind set spawn button — owner and co-owners
        if (isOwner || isCoOwner) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#TPSetSpawn", EventData.of("Action", "set_spawn"));
        }

        // apply initial state
        applyState(cmd, ref, store, viewerUuid, isOwner, isCoOwner);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageData data) {
        if (data.action == null) { sendUpdate((UICommandBuilder) null, false); return; }

        PlayerRef viewerRef = store.getComponent(ref, PlayerRef.getComponentType());
        UUID viewerUuid = viewerRef != null ? viewerRef.getUuid() : null;
        boolean isOwner   = viewerUuid != null && viewerUuid.equals(territory.getOwnerUuid());
        boolean isCoOwner = viewerUuid != null && territory.isCoOwner(viewerUuid);

        if (data.action.startsWith("remove:") && isOwner) {
            // remove co-owner at index
            int idx = Integer.parseInt(data.action.substring("remove:".length())) - 1;
            List<TerritoryData.CoOwnerEntry> coOwners = territory.getCoOwners();
            if (idx >= 0 && idx < coOwners.size()) {
                territory.removeCoOwner(coOwners.get(idx).uuid());
                saveTerritory(store);
            }
        } else if (data.action.startsWith("approve:") && isOwner) {
            // approve request at index
            int idx = Integer.parseInt(data.action.substring("approve:".length())) - 1;
            List<UUID> requests = territory.getCoOwnerRequests();
            if (idx >= 0 && idx < requests.size()) {
                UUID approvedUuid = requests.get(idx);
                PlayerRef approvedRef = Universe.get().getPlayer(approvedUuid);
                String approvedUsername = approvedRef != null ? approvedRef.getUsername() : approvedUuid.toString().substring(0, 8) + "...";
                territory.approveCoOwner(approvedUuid, viewerUuid, approvedUsername);
                saveTerritory(store);
            }
        } else if (data.action.startsWith("deny:") && isOwner) {
            // deny request at index
            int idx = Integer.parseInt(data.action.substring("deny:".length())) - 1;
            List<UUID> requests = territory.getCoOwnerRequests();
            if (idx >= 0 && idx < requests.size()) {
                territory.denyCoOwner(requests.get(idx), viewerUuid);
                saveTerritory(store);
            }
        } else if (data.action.equals("request") && !isOwner && !isCoOwner) {
            // request co-ownership
            if (viewerUuid != null) {
                territory.requestCoOwnership(viewerUuid);
                saveTerritory(store);
            }
        } else if (data.action.equals("withdraw_request") && !isOwner) {
            // withdraw pending request
            if (viewerUuid != null) {
                territory.denyCoOwner(viewerUuid, territory.getOwnerUuid());
                saveTerritory(store);
            }
        } else if (data.action.equals("withdraw_ownership") && isCoOwner) {
            // withdraw co-ownership
            if (viewerUuid != null) {
                territory.removeCoOwner(viewerUuid);
                saveTerritory(store);
            }
        } else if (data.action.equals("set_spawn") && (isOwner || isCoOwner)) {
            // set the territory's lightwell as the viewer's spawn point
            try {
                Player player = store.getComponent(ref, Player.getComponentType());
                if (player != null) {
                    Vector3i pos = territory.getCenter();
                    PlayerWorldData worldData = player.getPlayerConfigData().getPerWorldData(store.getExternalData().getWorld().getName());
                    PlayerRespawnPointData[] existing = worldData.getRespawnPoints();
                    PlayerRespawnPointData newPoint = new PlayerRespawnPointData(pos, new Vector3d(pos.x + 0.5, pos.y + 1.0, pos.z + 0.5), "Light Well");
                    PlayerRespawnPointData[] updated = existing == null ? new PlayerRespawnPointData[]{ newPoint } : Arrays.copyOf(existing, existing.length + 1);
                    if (existing != null) updated[existing.length] = newPoint;
                    worldData.setRespawnPoints(updated);
                }
            } catch (IllegalStateException ignored) {}
        }

        // refresh state after any action
        UICommandBuilder cmd = new UICommandBuilder();
        applyState(cmd, ref, store, viewerUuid, isOwner, territory.isCoOwner(viewerUuid));
        sendUpdate(cmd, false);
    }

    // apply all UI state based on current territory data and viewer role
    private void applyState(@Nonnull UICommandBuilder cmd, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nullable UUID viewerUuid, boolean isOwner, boolean isCoOwner) {
        // set owner name
        cmd.set("#TPOwnerName.Text", resolveName(territory.getOwnerUuid(), store));

        // populate co-owner rows
        List<TerritoryData.CoOwnerEntry> coOwners = territory.getCoOwners();
        for (int i = 1; i <= MAX_CO_OWNERS; i++) {
            int idx = i - 1;
            boolean hasCoOwner = idx < coOwners.size();
            cmd.set("#TPCoOwnerRow" + i + ".Visible", hasCoOwner);
            if (hasCoOwner) {
                cmd.set("#TPCoOwnerName" + i + ".Text", coOwners.get(idx).username());
                cmd.set("#TPRemoveCoOwner" + i + ".Visible", isOwner);
            }
        }

        // no co-owners placeholder
        cmd.set("#TPNoCoOwners.Visible", coOwners.isEmpty());

        // requests section — owner only
        List<UUID> requests = territory.getCoOwnerRequests();
        boolean showRequests = isOwner;
        cmd.set("#TPRequestsSeparator.Visible", showRequests);
        cmd.set("#TPRequestsLabel.Visible", showRequests);

        if (showRequests) {
            for (int i = 1; i <= MAX_REQUESTS; i++) {
                int idx = i - 1;
                boolean hasRequest = idx < requests.size();
                cmd.set("#TPRequestRow" + i + ".Visible", hasRequest);
                if (hasRequest) {
                    cmd.set("#TPRequestName" + i + ".Text", resolveName(requests.get(idx), store));
                }
            }
            cmd.set("#TPNoRequests.Visible", requests.isEmpty());
        } else {
            // hide all request rows for non-owners
            for (int i = 1; i <= MAX_REQUESTS; i++) cmd.set("#TPRequestRow" + i + ".Visible", false);
            cmd.set("#TPNoRequests.Visible", false);
        }

        // action buttons — non-owner only
        boolean hasPendingRequest = viewerUuid != null && territory.hasPendingRequest(viewerUuid);
        boolean atCapacity = coOwners.size() >= MAX_CO_OWNERS;

        cmd.set("#TPRequestOwnership.Visible",  !isOwner && !isCoOwner && !hasPendingRequest && !atCapacity);
        cmd.set("#TPWithdrawRequest.Visible",   !isOwner && !isCoOwner && hasPendingRequest);
        cmd.set("#TPWithdrawOwnership.Visible", isCoOwner);

        // set spawn button — owner and co-owners only
        cmd.set("#TPSetSpawn.Visible", isOwner || isCoOwner);
    }

    // resolve a UUID to a display name — tries online players first, falls back to UUID string
    private String resolveName(@Nullable UUID uuid, @Nonnull Store<EntityStore> store) {
        if (uuid == null) return "Unknown";
        try {
            PlayerRef found = Universe.get().getPlayer(uuid);
            if (found != null) return found.getUsername();
        } catch (Exception ignored) {}
        return uuid.toString().substring(0, 8) + "...";
    }

    // persist territory changes
    private void saveTerritory(@Nonnull Store<EntityStore> store) {
        try {
            WorldRoomRegistry registry = WorldRoomRegistry.get(store.getExternalData().getWorld());
            if (registry != null) registry.saveAsync(store.getExternalData().getWorld());
        } catch (Exception ignored) {}
    }

    public static class PageData {
        public static final BuilderCodec<PageData> CODEC = BuilderCodec
                .<PageData>builder(PageData.class, PageData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action).add()
                .build();

        public String action;
    }
}