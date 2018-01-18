package dicograph.modDecomp;

import org.jgrapht.alg.util.Pair;

import java.util.BitSet;
import java.util.logging.Logger;
/*
 *   This source file is part of the program for editing directed graphs
 *   into cographs using modular decomposition.
 *   Copyright (C) 2018 Fynn Leitow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyLeafNode extends PartitiveFamilyTreeNode {

    private final int vertex;

    PartitiveFamilyLeafNode(int vertexNo, PartitiveFamilyTree tree) {
        super(tree);
        vertex = vertexNo;
    }

    public int getVertex() {
        return vertex;
    }

    @Override
    public String toString() {
        return "(Leaf: " + vertex + ")";
    }

    /**
     * See proof of Lem 24: "If X is a leaf, lc(X) = rc(X) = σ(x)"
     * @return the position of the assiciated vertex in σ
     */
    @Override
    protected Pair<Integer,Integer> computeLeftRightCutter(BitSet[] outNeighbors, BitSet[] inNeighbors, int[] positionInPermutation, Logger log){
        lc_X = positionInPermutation[vertex];
        rc_X = positionInPermutation[vertex];
        return new Pair<>(lc_X,rc_X);
    }

    @Override
    int exportAsDot(StringBuilder output, int[] counter){
        counter[0]++;
        int myCounter = counter[0];
        output.append(myCounter).append("[label=").append(vertex).append("];\n");
        return myCounter;
    }
}
