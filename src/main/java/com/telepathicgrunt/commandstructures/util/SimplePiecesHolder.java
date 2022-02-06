package com.telepathicgrunt.commandstructures.util;

import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePiecesHolder;
import net.minecraft.util.math.BlockBox;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class SimplePiecesHolder implements StructurePiecesHolder {

    protected ArrayList<StructurePiece> pieces;

    public SimplePiecesHolder() {
        this.pieces = new ArrayList<>();
    }

    @Override
    public void addPiece(StructurePiece piece) {
        this.pieces.add(piece);
    }

    @Nullable
    @Override
    public StructurePiece getIntersecting(BlockBox box) {
        for (StructurePiece piece : this.pieces) {
            if (piece.getBoundingBox().intersects(box)) {
                return piece;
            }
        }
        return null;
    }

    public ArrayList<StructurePiece> getPieces() {
        return pieces;
    }
}
