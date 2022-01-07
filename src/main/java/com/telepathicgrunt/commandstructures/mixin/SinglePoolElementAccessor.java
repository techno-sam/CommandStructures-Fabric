package com.telepathicgrunt.commandstructures.mixin;

import net.minecraft.world.level.levelgen.feature.structures.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;

@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
    @Accessor("processors")
    Supplier<StructureProcessorList> getProcessors();

    @Mutable
    @Accessor("processors")
    void setProcessors(Supplier<StructureProcessorList> processors);
}
