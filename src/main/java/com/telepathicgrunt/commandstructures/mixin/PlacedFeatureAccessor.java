package com.telepathicgrunt.commandstructures.mixin;

import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(PlacedFeature.class)
public interface PlacedFeatureAccessor {
    @Accessor("feature")
    Supplier<ConfiguredFeature<?, ?>> getFeature();
}
