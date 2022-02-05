package com.telepathicgrunt.commandstructures.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.function.Supplier;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.processor.StructureProcessorList;

@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
    @Accessor("processors")
    Supplier<StructureProcessorList> getProcessors();

    @Mutable
    @Accessor("processors")
    void setProcessors(Supplier<StructureProcessorList> processors);
}
