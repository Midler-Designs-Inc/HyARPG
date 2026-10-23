package com.example.hyarpg.utils;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.HashMap;
import java.util.Map;

public final class WorldGenChunkAccess {

    private final WorldChunk chunk;
    private final Map<Integer, Section> sections = new HashMap<>();

    public WorldGenChunkAccess(Holder<ChunkStore> holder, WorldChunk chunk) {
        this.chunk = chunk;

        ChunkColumn column = holder.getComponent(ChunkColumn.getComponentType());
        if (column == null) {
            throw new IllegalStateException("Generated chunk holder has no ChunkColumn component");
        }

        for (Holder<ChunkStore> sectionHolder : column.getSectionHolders()) {
            if (sectionHolder == null) continue;

            ChunkSection chunkSection = sectionHolder.getComponent(ChunkSection.getComponentType());
            BlockSection blockSection = sectionHolder.getComponent(BlockSection.getComponentType());
            if (chunkSection != null && blockSection != null) {
                sections.put(chunkSection.getY(), new Section(chunkSection, blockSection));
            }
        }
    }

    public int getX() {
        return chunk.getX();
    }

    public int getZ() {
        return chunk.getZ();
    }

    public int getBlock(int x, int y, int z) {
        if (y < ChunkUtil.MIN_Y || y >= ChunkUtil.MIN_Y + ChunkUtil.HEIGHT) {
            return 0;
        }

        Section section = getSection(y);
        return section.blocks.get(ChunkUtil.indexBlock(x & 31, y & 31, z & 31));
    }

    public boolean setBlock(int x, int y, int z, int blockId, int rotation, int filler) {
        if (y < ChunkUtil.MIN_Y || y >= ChunkUtil.MIN_Y + ChunkUtil.HEIGHT) {
            return false;
        }

        Section section = getSection(y);
        return BlockOperations.setBlockPreLoad(
            section.chunk,
            section.blocks,
            x & 31,
            y & 31,
            z & 31,
            blockId,
            rotation,
            filler
        );
    }

    private Section getSection(int y) {
        Section section = sections.get(ChunkUtil.indexSection(y));
        if (section == null) {
            throw new IllegalStateException("Generated chunk is missing section " + ChunkUtil.indexSection(y));
        }
        return section;
    }

    private record Section(ChunkSection chunk, BlockSection blocks) {
    }
}
