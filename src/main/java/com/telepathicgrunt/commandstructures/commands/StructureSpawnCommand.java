package com.telepathicgrunt.commandstructures.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.telepathicgrunt.commandstructures.CommandStructuresMain;
import com.telepathicgrunt.commandstructures.Utilities;
import com.telepathicgrunt.commandstructures.mixin.SinglePoolElementAccessor;
import com.telepathicgrunt.commandstructures.util.SimplePiecesHolder;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesHolder;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.structure.processor.StructureProcessorLists;
import net.minecraft.text.LiteralText;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.SimpleRandom;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;
/*import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;*/

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class StructureSpawnCommand {
    public static void dataGenCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        String commandString = "spawnstructure";
        String locationArg = "location";
        String poolArg = "startpoolresourcelocation";
        String depthArg = "depth";
        String heightmapArg = "heightmapsnap";
        String legacyBoundsArg = "legacyboundingboxrule";
        String disableProcessors = "disableprocessors";
        String sendChunkLightingPacket = "sendchunklightingpacket";
        String randomSeed = "randomseed";

        LiteralCommandNode<ServerCommandSource> source = dispatcher.register(CommandManager.literal(commandString)
            .requires((permission) -> permission.hasPermissionLevel(2))
            .then(CommandManager.argument(locationArg, Vec3ArgumentType.vec3())
            .then(CommandManager.argument(poolArg, IdentifierArgumentType.identifier())
            .suggests((ctx, sb) -> CommandSource.suggestIdentifiers(startPoolSuggestions(ctx), sb))
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(poolArg, Identifier.class), 10, false, false, false, false, null, cs);
                return 1;
            })
            .then(CommandManager.argument(depthArg, IntegerArgumentType.integer())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(poolArg, Identifier.class), cs.getArgument(depthArg, Integer.class), false, false, false, false, null, cs);
                return 1;
            })
            .then(CommandManager.argument(heightmapArg, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(poolArg, Identifier.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), false, false, false, null, cs);
                return 1;
            })
            .then(CommandManager.argument(legacyBoundsArg, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(poolArg, Identifier.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), false, false, null, cs);
                return 1;
            })
            .then(CommandManager.argument(disableProcessors, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(poolArg, Identifier.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), cs.getArgument(disableProcessors, Boolean.class), false, null, cs);
                return 1;
            })
            .then(CommandManager.argument(sendChunkLightingPacket, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(poolArg, Identifier.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), cs.getArgument(disableProcessors, Boolean.class), cs.getArgument(sendChunkLightingPacket, Boolean.class), null, cs);
                return 1;
            })
            .then(CommandManager.argument(randomSeed, LongArgumentType.longArg())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(poolArg, Identifier.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), cs.getArgument(disableProcessors, Boolean.class), cs.getArgument(sendChunkLightingPacket, Boolean.class), cs.getArgument(randomSeed, Long.class), cs);
                return 1;
            })
        )))))))));

        dispatcher.register(CommandManager.literal(commandString).redirect(source));
    }

    private static Set<Identifier> startPoolSuggestions(CommandContext<ServerCommandSource> cs) {
        return cs.getSource().getWorld().getRegistryManager().get(Registry.STRUCTURE_POOL_KEY).getIds();
    }

    private static void generateStructure(PosArgument coordinates, Identifier structureStartPoolRL, int depth, boolean heightmapSnap, boolean legacyBoundingBoxRule, boolean disableProcessors, boolean sendChunkLightingPacket, Long randomSeed, CommandContext<ServerCommandSource> cs) {
        ServerWorld level = cs.getSource().getWorld();
        BlockPos centerPos = coordinates.toAbsoluteBlockPos(cs.getSource());
        if(heightmapSnap) centerPos = centerPos.down(centerPos.getY()); //not a typo. Needed so heightmap is not offset by player height.

        StructurePool templatePool = level.getRegistryManager().getMutable(Registry.STRUCTURE_POOL_KEY).get(structureStartPoolRL);

        if(templatePool == null || templatePool.getElementCount() == 0) {
            String errorMsg = structureStartPoolRL + " template pool does not exist or is empty";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandException(new LiteralText(errorMsg));
        }

        StructurePoolFeatureConfig newConfig = new StructurePoolFeatureConfig(
                () -> templatePool,
                depth
        );

        ChunkRandom worldgenrandom;
        if(randomSeed == null) {
            worldgenrandom = new ChunkRandom();
            long i = worldgenrandom.setPopulationSeed(level.getSeed(), centerPos.getX(), centerPos.getZ());
            worldgenrandom.setDecoratorSeed(i, 0, 0);
        }
        else {
            worldgenrandom = new ChunkRandom(randomSeed);
        }

        StructurePoolElement startingElement = newConfig.getStartPool().get().getRandomElement(worldgenrandom);

        PoolStructurePiece poolStructurePiece = new PoolStructurePiece(level.getStructureManager(), startingElement, centerPos, startingElement.getGroundLevelDelta(), BlockRotation.NONE, new BlockBox(centerPos));
        ArrayList<PoolStructurePiece> pieces = new ArrayList<>();
        SimplePiecesHolder piecesHolder = new SimplePiecesHolder();
        //StructurePoolBasedGenerator.method_27230(level.getRegistryManager(), poolStructurePiece, depth, PoolStructurePiece::new, level.getChunkManager().getChunkGenerator(), level.getStructureManager(), pieces, worldgenrandom, level);
        StructurePoolBasedGenerator.generate(
                level.getRegistryManager(),
                new StructurePoolFeatureConfig(newConfig.getStartPool(), depth),
                PoolStructurePiece::new,
                level.getChunkManager().getChunkGenerator(),
                level.getStructureManager(),
                centerPos,
                piecesHolder,
                worldgenrandom,
                legacyBoundingBoxRule,
                heightmapSnap,
                level
                );
        for (StructurePiece piece : piecesHolder.getPieces()) {
            pieces.add((PoolStructurePiece) piece);
        }

        // Create a new context with the new config that has our json pool. We will pass this into JigsawPlacement.addPieces

        /*PieceGeneratorSupplier.Context<StructurePoolFeatureConfig> newContext = new PieceGeneratorSupplier.Context<>(
                level.getChunkManager().getChunkGenerator(),
                level.getChunkManager().getChunkGenerator().getBiomeSource(),
                level.getSeed(),
                randomSeed == null ? new ChunkPos(centerPos) : new ChunkPos(0, 0),
                newConfig,
                level,
                (b) -> true,
                level.getStructureManager(),
                level.getRegistryManager()
        );

        Optional<PieceGenerator<StructurePoolFeatureConfig>> pieceGenerator = StructurePoolBasedGenerator.generate(
                newContext,
                PoolStructurePiece::new,
                centerPos,
                legacyBoundingBoxRule,
                heightmapSnap);*/


        BlockPos finalCenterPos = centerPos;
        List<PoolStructurePiece> structurePieceList = (List<PoolStructurePiece>) pieces.clone();

        ChunkPos chunkPos = randomSeed == null ? new ChunkPos(centerPos) : new ChunkPos(0, 0);

        structurePieceList.forEach(piece -> {
            if(disableProcessors) {
                if(piece.getPoolElement() instanceof SinglePoolElement singlePoolElement) {
                    Supplier<StructureProcessorList> oldProcessorList = ((SinglePoolElementAccessor)singlePoolElement).getProcessors();
                    ((SinglePoolElementAccessor)singlePoolElement).setProcessors(() -> StructureProcessorLists.EMPTY);
                    generatePiece(level, chunkPos, worldgenrandom, finalCenterPos, piece);
                    ((SinglePoolElementAccessor)singlePoolElement).setProcessors(oldProcessorList); // Set the processors back or else our change is permanent.
                }
            }
            else {
                generatePiece(level, chunkPos, worldgenrandom, finalCenterPos, piece);
            }
        });

        if(!structurePieceList.isEmpty()) {
            if(sendChunkLightingPacket) {
                Utilities.refreshChunksOnClients(level);
            }
        }
        else {
            String errorMsg = structureStartPoolRL + " Template Pool spawned no pieces.";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandException(new LiteralText(errorMsg));
        }
    }

    private static void generatePiece(ServerWorld level, ChunkPos chunkPos, ChunkRandom worldgenrandom, BlockPos finalCenterPos, StructurePiece piece) {
        /*piece.generate(
                level,
                level.getStructureAccessor(),
                newContext.chunkGenerator(),
                worldgenrandom,
                BlockBox.infinite(),
                newContext.chunkPos(),
                finalCenterPos
        );*/
        piece.generate(
                level,
                level.getStructureAccessor(),
                level.getChunkManager().getChunkGenerator(),
                worldgenrandom,
                BlockBox.infinite(),
                chunkPos,
                finalCenterPos
        );
    }
}
