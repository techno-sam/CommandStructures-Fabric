package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.telepathicgrunt.commandstructures.CommandStructuresMain;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StructureBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.CoordinateArgument;
import net.minecraft.command.argument.DefaultPosArgument;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructureManager;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SpawnPiecesCommand {
    private static MinecraftServer currentMinecraftServer = null;
    private static Set<Identifier> cachedSuggestion = new HashSet<>();

    public static void dataGenCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        String commandString = "spawnpieces";
        String rlArg = "resourcelocationpath";
        String locationArg = "location";
        String savepieceArg = "savepieces";
        String floorblockArg = "floorblock";
        String rowlengthArg = "rowlength";
        String fillerblockArg = "fillerblock";

        LiteralCommandNode<ServerCommandSource> source = dispatcher.register(CommandManager.literal(commandString)
                .requires((permission) -> permission.hasPermissionLevel(2))
                .then(CommandManager.argument(rlArg, IdentifierArgumentType.identifier())
                .suggests((ctx, sb) -> CommandSource.suggestIdentifiers(templatePathsSuggestions(ctx), sb))
                .executes(cs -> {
                    DefaultPosArgument worldCoordinates = new DefaultPosArgument(
                            new CoordinateArgument(false, cs.getSource().getPosition().getX()),
                            new CoordinateArgument(false, cs.getSource().getPosition().getY()),
                            new CoordinateArgument(false, cs.getSource().getPosition().getZ())
                    );
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), worldCoordinates, false, Blocks.BARRIER.getDefaultState(), Blocks.AIR.getDefaultState(), 13, cs);
                    return 1;
                })
                .then(CommandManager.argument(locationArg, Vec3ArgumentType.vec3())
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3ArgumentType.getPosArgument(cs, locationArg), false, Blocks.BARRIER.getDefaultState(), Blocks.AIR.getDefaultState(), 13, cs);
                    return 1;
                })
                .then(CommandManager.argument(savepieceArg, BoolArgumentType.bool())
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(savepieceArg, boolean.class), Blocks.BARRIER.getDefaultState(), Blocks.AIR.getDefaultState(), 13, cs);
                    return 1;
                })
                .then(CommandManager.argument(floorblockArg, BlockStateArgumentType.blockState())
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(savepieceArg, boolean.class), BlockStateArgumentType.getBlockState(cs, floorblockArg).getBlockState(), Blocks.AIR.getDefaultState(), 13, cs);
                    return 1;
                })
                .then(CommandManager.argument(fillerblockArg, BlockStateArgumentType.blockState())
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(savepieceArg, boolean.class), BlockStateArgumentType.getBlockState(cs, floorblockArg).getBlockState(), BlockStateArgumentType.getBlockState(cs, fillerblockArg).getBlockState(), 13, cs);
                    return 1;
                })
                .then(CommandManager.argument(rowlengthArg, IntegerArgumentType.integer())
                .executes(cs -> {
                    spawnPieces(cs.getArgument(rlArg, Identifier.class), Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(savepieceArg, boolean.class), BlockStateArgumentType.getBlockState(cs, floorblockArg).getBlockState(), BlockStateArgumentType.getBlockState(cs, fillerblockArg).getBlockState(), cs.getArgument(rowlengthArg, Integer.class), cs);
                    return 1;
                })
        )))))));

        dispatcher.register(CommandManager.literal(commandString).redirect(source));
    }

    private static Set<Identifier> templatePathsSuggestions(CommandContext<ServerCommandSource> cs) {
        if(currentMinecraftServer == cs.getSource().getServer()) {
            return cachedSuggestion;
        }

        ResourceManager resourceManager = cs.getSource().getWorld().getServer().getResourceManager();
        Set<String> modidStrings = new HashSet<>();
        Set<Identifier> rlSet = resourceManager.findResources("structures", (filename) -> filename.endsWith(".nbt"))
                .stream()
                .map(resourceLocation -> {
                    String namespace = resourceLocation.getNamespace();
                    modidStrings.add(namespace);

                    String path = resourceLocation.getPath()
                            .replaceAll("structures/", "")
                            .replaceAll(".nbt", "");

                    // We want to suggest folders instead of individual nbts
                    int i = path.lastIndexOf('/');
                    if(i > 0) {
                        path = path.substring(0, i) + "/";
                    }

                    return new Identifier(namespace, path);
                })
                .collect(Collectors.toSet());

        // add suggestion for entire mods/vanilla too
        rlSet.addAll(modidStrings.stream()
                .map(modid -> new Identifier(modid, ""))
                .collect(Collectors.toSet()));

        currentMinecraftServer = cs.getSource().getServer();
        cachedSuggestion = rlSet;
        return rlSet;
    }

    public static void spawnPieces(Identifier path, PosArgument coordinates, boolean savePieces, BlockState floorBlockState, BlockState fillBlockState, int rowlength, CommandContext<ServerCommandSource> cs) {
        ServerWorld level = cs.getSource().getWorld();
        PlayerEntity player = cs.getSource().getEntity() instanceof PlayerEntity player1 ? player1 : null;
        BlockPos pos = coordinates.toAbsoluteBlockPos(cs.getSource());

        List<Identifier> nbtRLs = getResourceLocations(player, level, path.getNamespace(), path.getPath());

        if(nbtRLs.isEmpty()) {
            String errorMsg = path + " path has no nbt pieces in it. No pieces will be placed.";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandException(new LiteralText(errorMsg));
        }

        // Size of area we will need
        int columnCount = rowlength;
        int rowCount = (int) Math.max(Math.ceil((float)nbtRLs.size() / columnCount), 1);
        if(rowCount == 1) {
            columnCount = nbtRLs.size();
        }

        int spacing = 48;
        BlockPos bounds = new BlockPos((spacing * rowCount) + 16, spacing, spacing * columnCount);

        // Fill/clear area with structure void
        clearAreaNew(level, pos, player, bounds, fillBlockState, floorBlockState);
        generateStructurePieces(level, pos, player, nbtRLs, columnCount, spacing, savePieces);
    }

    private static void clearAreaNew(ServerWorld world, BlockPos pos, PlayerEntity player, BlockPos bounds, BlockState fillBlock, BlockState floorBlock) {
        BlockPos.Mutable mutableChunk = new BlockPos.Mutable().set(pos.getX() >> 4, pos.getY(), pos.getZ() >> 4);
        mutableChunk.move(1,0,0);
        int endChunkX = (pos.getX() + bounds.getX()) >> 4;
        int endChunkZ = (pos.getZ() + bounds.getZ()) >> 4;

        int maxChunks = (endChunkX - mutableChunk.getX()) * (endChunkZ - mutableChunk.getZ());
        int currentSection = 0;
        for(; mutableChunk.getX() < endChunkX; mutableChunk.move(1,0,0)) {
            for (; mutableChunk.getZ() < endChunkZ; mutableChunk.move(0, 0, 1)) {
                WorldChunk chunk = world.getChunk(mutableChunk.getX(), mutableChunk.getZ());
                BlockPos.Mutable mutable = new BlockPos(mutableChunk.getX() << 4, pos.getY(), mutableChunk.getZ() << 4).mutableCopy();
                mutable.move(-1, 0, 0);
                for(int x = 0; x < 16; x++) {
                    mutable.setZ(mutableChunk.getZ() << 4);
                    mutable.move(1, 0, -1);
                    for(int z = 0; z < 16; z++) {
                        mutable.move(0, 0, 1);
                        mutable.setY(pos.getY());
                        BlockState oldState = chunk.setBlockState(mutable, floorBlock, false);
                        if(oldState != null) {
                            world.getChunkManager().markForUpdate(mutable);
                            world.getChunkManager().getLightingProvider().checkBlock(mutable);
                        }
                        for(int y = pos.getY() + 1; y < pos.getY() + 64; y++) {
                            mutable.setY(y);
                            oldState = chunk.setBlockState(mutable, fillBlock, false);
                            if(oldState != null) {
                                world.getChunkManager().markForUpdate(mutable);
                                world.getChunkManager().getLightingProvider().checkBlock(mutable);
                            }
                        }
                    }
                }
                currentSection++;
                if(player != null) {
                    player.sendMessage(new TranslatableText("Working: %" +  Math.round(((float)currentSection / maxChunks) * 100f)), true);
                }
            }
            mutableChunk.set(mutableChunk.getX(), mutableChunk.getY(), pos.getZ() >> 4); // Set back to start of row
        }
    }



    private static List<Identifier> getResourceLocations(PlayerEntity player, ServerWorld world, String modId, String filter) {
        ResourceManager resourceManager = world.getServer().getResourceManager();
        return resourceManager.findResources("structures", (filename) -> filename.endsWith(".nbt"))
                .stream()
                .filter(resourceLocation -> resourceLocation.getNamespace().equals(modId))
                .filter(resourceLocation -> resourceLocation.getPath().startsWith("structures/" + filter))
                .map(resourceLocation -> new Identifier(resourceLocation.getNamespace(), resourceLocation.getPath().replaceAll("^structures/", "").replaceAll(".nbt$", "")))
                .toList();
    }


    private static void generateStructurePieces(ServerWorld world, BlockPos pos, PlayerEntity player, List<Identifier> nbtRLs, int columnCount, int spacing, boolean savePieces) {
        BlockPos.Mutable mutable = new BlockPos.Mutable().set(((pos.getX() >> 4) + 1) << 4, pos.getY(), (pos.getZ() >> 4) << 4);

        for(int pieceIndex = 1; pieceIndex <= nbtRLs.size(); pieceIndex++) {
            if(player != null) {
                player.sendMessage(new TranslatableText(" Working making structure: " + nbtRLs.get(pieceIndex - 1)), true);
            }

            world.setBlockState(mutable, Blocks.STRUCTURE_BLOCK.getDefaultState().with(StructureBlock.MODE, StructureBlockMode.LOAD), 3);
            BlockEntity be = world.getBlockEntity(mutable);
            if(be instanceof StructureBlockBlockEntity structureBlockTileEntity) {
                structureBlockTileEntity.setStructureName(nbtRLs.get(pieceIndex-1)); // set identifier

                structureBlockTileEntity.setMode(StructureBlockMode.LOAD);
                structureBlockTileEntity.setIgnoreEntities(false);

                fillStructureVoidSpace(world, nbtRLs.get(pieceIndex-1), mutable);
                structureBlockTileEntity.loadStructure(world,false); // load structure

                structureBlockTileEntity.setMode(StructureBlockMode.SAVE);
                if(savePieces) {
                    structureBlockTileEntity.saveStructure(true);
                }
                //structureBlockTileEntity.setShowAir(true);
                structureBlockTileEntity.setIgnoreEntities(false);
            }

            mutable.move(0,0, spacing);

            // Move back to start of row
            if(pieceIndex % columnCount == 0) {
                mutable.move(spacing,0, (-spacing * columnCount));
            }
        }
    }

    // Needed so that structure void is preserved in structure pieces.
    private static void fillStructureVoidSpace(ServerWorld world, Identifier resourceLocation, BlockPos startSpot) {
        StructureManager structuremanager = world.getStructureManager();
        Optional<Structure> optional = structuremanager.getStructure(resourceLocation);
        optional.ifPresent(template -> {
            BlockPos.Mutable mutable = startSpot.mutableCopy();
            Chunk chunk = world.getChunk(mutable);
            for(int x = 0; x < template.getSize().getX(); x++) {
                for (int z = 0; z < template.getSize().getZ(); z++) {
                    for(int y = 0; y < template.getSize().getY(); y++) {
                        mutable.set(startSpot).move(x, y + 1, z);
                        if(chunk.getPos().x != mutable.getX() >> 4 || chunk.getPos().z != mutable.getZ() >> 4) {
                            chunk = world.getChunk(mutable);
                        }

                        BlockState oldState = chunk.setBlockState(mutable, Blocks.STRUCTURE_VOID.getDefaultState(), false);
                        if(oldState != null) {
                            world.getChunkManager().markForUpdate(mutable);
                            world.getChunkManager().getLightingProvider().checkBlock(mutable);
                        }
                    }
                }
            }
        });
    }
}
