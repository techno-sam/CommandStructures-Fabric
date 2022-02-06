package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.telepathicgrunt.commandstructures.CommandStructuresMain;
import com.telepathicgrunt.commandstructures.Utilities;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;

import java.util.Set;
import java.util.stream.Collectors;

/*import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;*/

public class RawStructureSpawnCommand {
    public static void dataGenCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        String commandString = "spawnrawstructure";
        String locationArg = "location";
        String cfRL = "configuredstructure";
        String saveStructureBounds = "savestructurebounds";
        String sendChunkLightingPacket = "sendchunklightingpacket";
        String randomSeed = "randomseed";

        LiteralCommandNode<ServerCommandSource> source = dispatcher.register(CommandManager.literal(commandString)
            .requires((permission) -> permission.hasPermissionLevel(2))
            .then(CommandManager.argument(locationArg, Vec3ArgumentType.vec3())
            .then(CommandManager.argument(cfRL, IdentifierArgumentType.identifier())
            .suggests((ctx, sb) -> CommandSource.suggestIdentifiers(startPoolSuggestions(ctx), sb))
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(cfRL, Identifier.class), true, true, null, cs);
                return 1;
            })
            .then(CommandManager.argument(saveStructureBounds, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(cfRL, Identifier.class), cs.getArgument(saveStructureBounds, Boolean.class), true, null, cs);
                return 1;
            })
            .then(CommandManager.argument(sendChunkLightingPacket, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(cfRL, Identifier.class), cs.getArgument(saveStructureBounds, Boolean.class), cs.getArgument(sendChunkLightingPacket, Boolean.class), null, cs);
                return 1;
            })
            .then(CommandManager.argument(randomSeed, LongArgumentType.longArg())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(cfRL, Identifier.class), cs.getArgument(saveStructureBounds, Boolean.class), cs.getArgument(sendChunkLightingPacket, Boolean.class), cs.getArgument(randomSeed, Long.class), cs);
                return 1;
            })
        ))))));

        dispatcher.register(CommandManager.literal(commandString).redirect(source));
    }

    private static Set<Identifier> startPoolSuggestions(CommandContext<ServerCommandSource> cs) {
        return cs.getSource().getWorld().getRegistryManager().get(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY).getIds();
    }

    private static void generateStructure(PosArgument coordinates, Identifier structureRL, boolean saveStructureBounds, boolean sendChunkLightingPacket, Long randomSeed, CommandContext<ServerCommandSource> cs) {
        ServerWorld level = cs.getSource().getWorld();
        BlockPos centerPos = coordinates.toAbsoluteBlockPos(cs.getSource());
        ChunkPos chunkPos = new ChunkPos(centerPos);
        Chunk chunkAccess = level.getChunk(chunkPos.x, chunkPos.z);
        ChunkSectionPos sectionpos = ChunkSectionPos.from(chunkAccess);

        ChunkRandom worldgenrandom;
        if (randomSeed == null) {
            worldgenrandom = new ChunkRandom();
            long i = worldgenrandom.setPopulationSeed(level.getSeed(), centerPos.getX(), centerPos.getZ());
            worldgenrandom.setDecoratorSeed(i, 0, 0);
        } else {
            worldgenrandom = new ChunkRandom(randomSeed);
        }

        ConfiguredStructureFeature<?, ?> configuredStructureFeature = level.getRegistryManager().getMutable(Registry.CONFIGURED_STRUCTURE_FEATURE_KEY).get(structureRL);

        if (configuredStructureFeature == null) {
            String errorMsg = structureRL + " ConfiguredStructureFeature does not exist in registry";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandException(new LiteralText(errorMsg));
        }

        StructureStart<?> structureStart;

        /*if (configuredStructureFeature.feature == StructureFeature.MINESHAFT) {
            structureStart = new MineshaftFeature.Start(StructureFeature.MINESHAFT, chunkPos, 0, level.getSeed());
        }
        else if (configuredStructureFeature.feature == StructureFeature.MONUMENT) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            OceanMonumentFeatureAccessor.callGeneratePieces(
                    structurePiecesBuilder,
                    new PieceGenerator.Context<>(
                            (DefaultFeatureConfig) configuredStructureFeature.config,
                            level.getChunkManager().getChunkGenerator(),
                            level.getStructureManager(),
                            chunkPos,
                            level,
                            worldgenrandom,
                            0)
            );
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if (configuredStructureFeature.feature == StructureFeature.PILLAGER_OUTPOST) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            PieceGeneratorSupplier.Context<StructurePoolFeatureConfig> newContext = new PieceGeneratorSupplier.Context<>(
                    level.getChunkManager().getChunkGenerator(),
                    level.getChunkManager().getChunkGenerator().getBiomeSource(),
                    level.getSeed(),
                    randomSeed == null ? new ChunkPos(centerPos) : new ChunkPos(0, 0),
                    (StructurePoolFeatureConfig) configuredStructureFeature.config,
                    level,
                    (b) -> true,
                    level.getStructureManager(),
                    level.getRegistryManager()
            );
            Optional<PieceGenerator<StructurePoolFeatureConfig>> pieceGenerator = StructurePoolBasedGenerator.generate(
                    newContext,
                    PoolStructurePiece::new,
                    centerPos.down(centerPos.getY()),
                    true,
                    true);
            pieceGenerator.ifPresent(jigsawConfigurationPieceGenerator -> jigsawConfigurationPieceGenerator.generatePieces(
                    structurePiecesBuilder,
                    new PieceGenerator.Context<>(
                            (StructurePoolFeatureConfig) configuredStructureFeature.config,
                            level.getChunkManager().getChunkGenerator(),
                            level.getStructureManager(),
                            chunkPos,
                            level,
                            worldgenrandom,
                            0)
            ));
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if(configuredStructureFeature.feature == StructureFeature.FORTRESS) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            NetherFortressFeatureAccessor.callGeneratePieces(
                    structurePiecesBuilder,
                    new PieceGenerator.Context<>(
                            (DefaultFeatureConfig) configuredStructureFeature.config,
                            level.getChunkManager().getChunkGenerator(),
                            level.getStructureManager(),
                            chunkPos,
                            level,
                            worldgenrandom,
                            0)
            );
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if(configuredStructureFeature.feature == StructureFeature.NETHER_FOSSIL) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            NetherFossilGenerator.addPieces(level.getStructureManager(), structurePiecesBuilder, worldgenrandom, centerPos);
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if(configuredStructureFeature.feature == StructureFeature.BURIED_TREASURE) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            structurePiecesBuilder.addPiece(new BuriedTreasureGenerator.Piece(centerPos));
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }
        else if(configuredStructureFeature.feature == StructureFeature.STRONGHOLD) {
            StructurePiecesBuilder structurePiecesBuilder = new StructurePiecesBuilder();
            StrongholdFeatureAccessor.callGeneratePieces(
                    structurePiecesBuilder,
                    new PieceGenerator.Context<>(
                            (DefaultFeatureConfig) configuredStructureFeature.config,
                            level.getChunkManager().getChunkGenerator(),
                            level.getStructureManager(),
                            chunkPos,
                            level,
                            worldgenrandom,
                            0)
            );
            structureStart = new StructureStart<>(configuredStructureFeature.feature, chunkPos, 0, structurePiecesBuilder.build());
        }*/
        if (false) {}
        else {
            structureStart = configuredStructureFeature.tryPlaceStart(
                    level.getRegistryManager(),
                    level.getChunkManager().getChunkGenerator(),
                    level.getChunkManager().getChunkGenerator().getBiomeSource(),
                    level.getStructureManager(),
                    randomSeed == null ? level.getSeed() : randomSeed, //seed
                    chunkPos, //chunkpos
                    level.getChunkManager().getChunkGenerator().getBiomeSource().getBiomeForNoiseGen(chunkPos), //biome
                    0, //reference count
                    new StructureConfig(1, 0, 0), //structure config
                    level //height limit view
            );
        }

        structureStart.getChildren().forEach(piece -> generatePiece(level, worldgenrandom, centerPos, piece));
        level.getStructureAccessor().setStructureStart(sectionpos, configuredStructureFeature.feature, structureStart, chunkAccess);

        if(saveStructureBounds) {
            Set<ChunkPos> chunkPosSet = structureStart.getChildren().stream().map(piece -> new ChunkPos(piece.getBoundingBox().getCenter())).collect(Collectors.toSet());
            for(ChunkPos chunkPos1 : chunkPosSet) {
                Chunk chunkAccess1 = level.getChunk(chunkPos1.x, chunkPos1.z);
                level.getChunkManager().getChunkGenerator().addStructureReferences(level, level.getStructureAccessor(), chunkAccess1);
            }
        }

        if(!structureStart.getChildren().isEmpty()) {
            if(sendChunkLightingPacket) {
                Utilities.refreshChunksOnClients(level);
            }
        }
        else {
            String errorMsg = structureRL + " ConfiguredStructure failed to be spawned. (It may have internal checks for valid spots)";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandException(new LiteralText(errorMsg));
        }
    }

    private static void generatePiece(ServerWorld level, ChunkRandom worldgenrandom, BlockPos finalCenterPos, StructurePiece piece) {
        piece.generate(
                level,
                level.getStructureAccessor(),
                level.getChunkManager().getChunkGenerator(),
                worldgenrandom,
                BlockBox.infinite(),
                new ChunkPos(finalCenterPos),
                finalCenterPos
        );
    }
}
