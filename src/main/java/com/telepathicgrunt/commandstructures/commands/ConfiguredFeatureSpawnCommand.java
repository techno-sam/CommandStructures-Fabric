package com.telepathicgrunt.commandstructures.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.telepathicgrunt.commandstructures.CommandStructuresMain;
import com.telepathicgrunt.commandstructures.Utilities;
import java.util.Set;
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
import net.minecraft.world.gen.feature.ConfiguredFeature;

public class ConfiguredFeatureSpawnCommand {
    public static void dataGenCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        String commandString = "spawnfeature";
        String locationArg = "location";
        String rlArg = "configuredfeatureresourcelocation";
        String sendChunkLightingPacket = "sendchunklightingpacket";

        LiteralCommandNode<ServerCommandSource> source = dispatcher.register(CommandManager.literal(commandString)
            .requires((permission) -> permission.hasPermissionLevel(2))
            .then(CommandManager.argument(locationArg, Vec3ArgumentType.vec3())
            .then(CommandManager.argument(rlArg, IdentifierArgumentType.identifier())
            .suggests((ctx, sb) -> CommandSource.suggestIdentifiers(configuredFeatureSuggestions(ctx), sb))
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

    private static Set<Identifier> configuredFeatureSuggestions(CommandContext<ServerCommandSource> cs) {
        return cs.getSource().getWorld().getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY).getIds();
    }

    private static void generateStructure(PosArgument coordinates, Identifier configuredFeatureRL, boolean sendChunkLightingPacket, CommandContext<ServerCommandSource> cs) {
        ServerWorld level = cs.getSource().getWorld();
        BlockPos centerPos = coordinates.toAbsoluteBlockPos(cs.getSource());
        ConfiguredFeature<?, ?> cf = cs.getSource().getRegistryManager().get(Registry.CONFIGURED_FEATURE_KEY).get(configuredFeatureRL);

        if(cf == null) {
            String errorMsg = configuredFeatureRL + " configuredfeature does not exist in registry";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandException(new LiteralText(errorMsg));
        }

        boolean success = cf.generate(level, level.getChunkManager().getChunkGenerator(), level.getRandom(), centerPos);

        if(!success) {
            String errorMsg = configuredFeatureRL + " configuredfeature failed to be spawned. (It may have internal checks for valid spots)";
            CommandStructuresMain.LOGGER.error(errorMsg);
            throw new CommandException(new LiteralText(errorMsg));
        }

        if(sendChunkLightingPacket) {
            Utilities.refreshChunksOnClients(level);
        }
    }
}
