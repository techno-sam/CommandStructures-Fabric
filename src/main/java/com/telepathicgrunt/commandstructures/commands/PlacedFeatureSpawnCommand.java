package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.telepathicgrunt.commandstructures.CommandStructuresMain;
import com.telepathicgrunt.commandstructures.Utilities;
import com.telepathicgrunt.commandstructures.mixin.PlacedFeatureAccessor;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.Set;
import java.util.stream.Collectors;

public class PlacedFeatureSpawnCommand {
    private static final Identifier BIOME_PLACEMENT_RL = new Identifier("minecraft", "biome");

    public static void dataGenCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        String commandString = "spawnplacedfeature";
        String locationArg = "location";
        String rlArg = "placedfeatureresourcelocation";
        String sendChunkLightingPacket = "sendchunklightingpacket";

        LiteralCommandNode<ServerCommandSource> source = dispatcher.register(CommandManager.literal(commandString)
            .requires((permission) -> permission.hasPermissionLevel(2))
            .then(CommandManager.argument(locationArg, Vec3ArgumentType.vec3())
            .then(CommandManager.argument(rlArg, IdentifierArgumentType.identifier())
            .suggests((ctx, sb) -> CommandSource.suggestIdentifiers(placedFeatureSuggestions(ctx), sb))
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(rlArg, Identifier.class), true, cs);
                return 1;
            })
            .then(CommandManager.argument(sendChunkLightingPacket, BoolArgumentType.bool())
            .executes(cs -> {
                generateStructure(Vec3ArgumentType.getPosArgument(cs, locationArg), cs.getArgument(rlArg, Identifier.class), cs.getArgument(sendChunkLightingPacket, Boolean.class), cs);
                return 1;
            })
        ))));

        dispatcher.register(CommandManager.literal(commandString).redirect(source));
    }

    private static Set<Identifier> placedFeatureSuggestions(CommandContext<ServerCommandSource> cs) {
        return cs.getSource().getWorld().getRegistryManager().get(Registry.PLACED_FEATURE_REGISTRY).getIds();
    }

    private static void generateStructure(PosArgument coordinates, Identifier placedFeatureRL, boolean sendChunkLightingPacket, CommandContext<ServerCommandSource> cs) {
        ServerWorld level = cs.getSource().getWorld();
        BlockPos centerPos = coordinates.toAbsoluteBlockPos(cs.getSource());
        PlacedFeature placedFeature = cs.getSource().getRegistryManager().get(Registry.PLACED_FEATURE_REGISTRY).get(placedFeatureRL);
        PlacementModifierType<?> biomePlacement = level.getRegistryManager().get(Registry.PLACEMENT_MODIFIER_REGISTRY).get(BIOME_PLACEMENT_RL);

        if(placedFeature == null) {
            String errorMsg = placedFeatureRL + " placedfeature does not exist in registry";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandException(new LiteralText(errorMsg));
        }

        BlockPos worldBottomPos = new BlockPos(centerPos.getX(), level.getDimension().getMinimumY(), centerPos.getZ());

        PlacedFeature noBiomeCheckPlacedFeature = new PlacedFeature(
                ((PlacedFeatureAccessor)placedFeature).getFeature(),
                placedFeature.getPlacement().stream()
                        .filter(placementModifier -> placementModifier.type() != biomePlacement)
                        .collect(Collectors.toList()));

        boolean success = noBiomeCheckPlacedFeature.place(level, level.getChunkManager().getChunkGenerator(), level.getRandom(), worldBottomPos);

        if(!success) {
            String errorMsg = placedFeatureRL + " placedfeature failed to be spawned. (It may have internal checks for valid spots or is chance based)";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandException(new LiteralText(errorMsg));
        }

        if(sendChunkLightingPacket) {
            Utilities.refreshChunksOnClients(level);
        }
    }
}
