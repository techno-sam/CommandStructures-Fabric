package com.telepathicgrunt.commandstructures;

import com.telepathicgrunt.commandstructures.mixin.ChunkMapAccessor;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

public final class Utilities {
    private Utilities() {}

    public static void refreshChunksOnClients(ServerWorld level) {
        int viewDistance = ((ChunkMapAccessor)level.getChunkManager().threadedAnvilChunkStorage).getViewDistance();
        level.getPlayers().forEach(player -> {
            for(int x = -viewDistance; x <= viewDistance; x++) {
                for(int z = -viewDistance; z <= viewDistance; z++) {
                    if(x + z < viewDistance) {
                        Chunk chunkAccess = level.getChunk(new ChunkPos(player.getChunkPos().x + x, player.getChunkPos().z + z).getStartPos());
                        if(chunkAccess instanceof WorldChunk levelChunk) {
                            ChunkDataS2CPacket dataPacket = new ChunkDataS2CPacket(levelChunk);
                            LightUpdateS2CPacket lightPacket = new LightUpdateS2CPacket(levelChunk.getPos(), level.getLightingProvider(), null, null, true);
                            player.sendUnloadChunkPacket(levelChunk.getPos());
                            player.sendInitialChunkPackets(levelChunk.getPos(), dataPacket, lightPacket);
                        }
                    }
                }
            }
        });
    }
}
