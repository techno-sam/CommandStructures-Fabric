package com.telepathicgrunt.commandstructures;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandStructuresMain implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "command_structures";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> StructureSpawnCommand.dataGenCommand(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> SpawnPiecesCommand.dataGenCommand(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> ConfiguredFeatureSpawnCommand.dataGenCommand(dispatcher));
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> PlacedFeatureSpawnCommand.dataGenCommand(dispatcher));
    }
}
