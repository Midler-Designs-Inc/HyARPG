package com.example.hyarpg.utils.rooms;

// Hytale Imports
import com.hypixel.hytale.math.vector.Vector3i;

// Java Imports
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Tracks all known rooms within a single territory.
// One TerritoryRoomRegistry exists per placed territory terminal.
public class TerritoryRoomRegistry {

    private final Vector3i terminalPosition;
    private final double territoryRadius;

    // CopyOnWriteArrayList for safe iteration while modifications happen
    private final List<RoomData> rooms = new CopyOnWriteArrayList<>();

    public TerritoryRoomRegistry(Vector3i terminalPosition, double territoryRadius) {
        this.terminalPosition = terminalPosition;
        this.territoryRadius = territoryRadius;
    }

    public Vector3i getTerminalPosition() { return terminalPosition; }
    public double getTerritoryRadius() { return territoryRadius; }

    public List<RoomData> getRooms() { return rooms; }

    // Returns the room whose interior contains this position, or null
    @Nullable
    public RoomData getRoomAt(int x, int y, int z) {
        for (RoomData room : rooms) {
            if (room.containsInterior(x, y, z)) {
                return room;
            }
        }
        return null;
    }

    // Returns the room whose shell (walls included) contains this position
    // Used to detect when a structural block affects a known room boundary
    @Nullable
    public RoomData getRoomNear(int x, int y, int z) {
        for (RoomData room : rooms) {
            if (room.containsWithWalls(x, y, z)) {
                return room;
            }
        }
        return null;
    }

    public void addRoom(RoomData room) {
        rooms.add(room);
    }

    public void removeRoom(RoomData room) {
        rooms.remove(room);
    }

    // Invalidate any room whose shell contains this position
    // Called on structural block place/break
    public List<RoomData> invalidateRoomsAt(int x, int y, int z) {
        List<RoomData> invalidated = new ArrayList<>();
        for (RoomData room : rooms) {
            if (room.containsWithWalls(x, y, z)) {
                invalidated.add(room);
            }
        }
        rooms.removeAll(invalidated);
        return invalidated;
    }

    // Clear all rooms — called when the territory terminal is removed
    public void clearAll() {
        rooms.clear();
    }

    // Returns true if the given world position is within this territory's radius
    public boolean isInTerritory(double x, double y, double z) {
        double dx = x - terminalPosition.x;
        double dy = y - terminalPosition.y;
        double dz = z - terminalPosition.z;
        return (dx * dx + dy * dy + dz * dz) <= territoryRadius * territoryRadius;
    }
}