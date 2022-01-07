package com.telepathicgrunt.commandstructures;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.configurations.JigsawConfiguration;
import net.minecraft.world.level.levelgen.feature.structures.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGenerator;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import java.util.Optional;
import java.util.Set;

public class StructureSpawnCommand {
    public static void dataGenCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        String commandString = "spawnstructure";
        String poolArg = "startpoolresourcelocation";
        String depthArg = "depth";
        String heightmapArg = "heightmapsnap";
        String legacyBoundsArg = "legacyboundingboxrule";
        String randomSeed = "randomseed";

        LiteralCommandNode<CommandSourceStack> source = dispatcher.register(Commands.literal(commandString)
                .requires((permission) -> permission.hasPermission(2))
                .then(Commands.argument(poolArg, ResourceLocationArgument.id())
                        .suggests((ctx, sb) -> SharedSuggestionProvider.suggestResource(startPoolSuggestions(ctx), sb))
                        .executes(cs -> {
                            generateStructure(cs.getArgument(poolArg, ResourceLocation.class), 10, false, false, null, cs);
                            return 1;
                        })
                        .then(Commands.argument(depthArg, IntegerArgumentType.integer())
                                .executes(cs -> {
                                    generateStructure(cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), false, false, null, cs);
                                    return 1;
                                })
                                .then(Commands.argument(heightmapArg, BoolArgumentType.bool())
                                        .executes(cs -> {
                                            generateStructure(cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), false, null, cs);
                                            return 1;
                                        })
                                        .then(Commands.argument(legacyBoundsArg, BoolArgumentType.bool())
                                                .executes(cs -> {
                                                    generateStructure(cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), null, cs);
                                                    return 1;
                                                })
                                                .then(Commands.argument(randomSeed, LongArgumentType.longArg())
                                                        .executes(cs -> {
                                                            generateStructure(cs.getArgument(poolArg, ResourceLocation.class), cs.getArgument(depthArg, Integer.class), cs.getArgument(heightmapArg, Boolean.class), cs.getArgument(legacyBoundsArg, Boolean.class), cs.getArgument(randomSeed, Long.class), cs);
                                                            return 1;
                                                        })
                                                ))))));

        dispatcher.register(Commands.literal(commandString).redirect(source));
    }

    private static Set<ResourceLocation> startPoolSuggestions(CommandContext<CommandSourceStack> cs) {
        return cs.getSource().getLevel().registryAccess().registryOrThrow(Registry.TEMPLATE_POOL_REGISTRY).keySet();
    }

    private static void generateStructure(ResourceLocation structureStartPoolRL, int depth, boolean heightmapSnap, boolean legacyBoundingBoxRule, Long randomSeed, CommandContext<CommandSourceStack> cs) {
        ServerLevel level = cs.getSource().getLevel();
        Entity entity = cs.getSource().getEntity();
        BlockPos centerPos = level.getSharedSpawnPos();
        if(entity != null) centerPos = entity.blockPosition();
        if(heightmapSnap) centerPos = centerPos.below(centerPos.getY());

        JigsawConfiguration newConfig = new JigsawConfiguration(
                () -> level.registryAccess().ownedRegistryOrThrow(Registry.TEMPLATE_POOL_REGISTRY).get(structureStartPoolRL),
                depth
        );

        // Create a new context with the new config that has our json pool. We will pass this into JigsawPlacement.addPieces
        PieceGeneratorSupplier.Context<JigsawConfiguration> newContext = new PieceGeneratorSupplier.Context<>(
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGenerator().getBiomeSource(),
                level.getSeed(),
                randomSeed == null ? new ChunkPos(centerPos) : new ChunkPos(0, 0),
                newConfig,
                level,
                (b) -> true,
                level.getStructureManager(),
                level.registryAccess()
        );

        Optional<PieceGenerator<JigsawConfiguration>> pieceGenerator = JigsawPlacement.addPieces(
                newContext,
                PoolElementStructurePiece::new,
                centerPos,
                legacyBoundingBoxRule,
                heightmapSnap);

        if(pieceGenerator.isPresent()) {
            StructurePiecesBuilder structurepiecesbuilder = new StructurePiecesBuilder();
            pieceGenerator.get().generatePieces(
                    structurepiecesbuilder,
                    new PieceGenerator.Context<>(
                            newContext.config(),
                            newContext.chunkGenerator(),
                            newContext.structureManager(),
                            newContext.chunkPos(),
                            newContext.heightAccessor(),
                            new WorldgenRandom(new LegacyRandomSource(0L)),
                            newContext.seed()));

            WorldgenRandom worldgenrandom;
            if(randomSeed == null) {
                worldgenrandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
                long i = worldgenrandom.setDecorationSeed(newContext.seed(), centerPos.getX(), centerPos.getZ());
                worldgenrandom.setFeatureSeed(i, 0, 0);
            }
            else {
                worldgenrandom = new WorldgenRandom(new LegacyRandomSource(randomSeed));
            }

            BlockPos finalCenterPos = centerPos;
            structurepiecesbuilder.build().pieces().forEach(piece -> piece.postProcess(
                    level,
                    level.structureFeatureManager(),
                    newContext.chunkGenerator(),
                    worldgenrandom,
                    BoundingBox.infinite(),
                    newContext.chunkPos(),
                    finalCenterPos
            ));
        }
    }
}