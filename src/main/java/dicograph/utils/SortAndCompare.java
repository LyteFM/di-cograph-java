package dicograph.utils;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

public class SortAndCompare {


    public static List<List<Integer>> computeAllSubsets(List<Integer> inputSet){
        List<List<Integer>> retList = new LinkedList<>();
        if(inputSet.size() == 0) {
            retList.add(new ArrayList<>());
        } else {
            int singleElement = inputSet.get(0);
            List<Integer> subList = inputSet.subList(1,inputSet.size());
            List<List<Integer>> subSets = computeAllSubsets(subList);
            retList.addAll(subSets);

            for(List<Integer> subSet : subSets){
                List<Integer> addList = new ArrayList<>(subSet.size() + 1);
                addList.add(singleElement);
                addList.addAll(subSet);
                retList.add(addList);
            }
        }

        return retList;
    }

    /**
     * Sorts BitSets according to their cardinality in descending size.
     * @param input the list of input sets
     * @param eliminateSingletons if singleton elements should be excluded
     */
    public static void bucketSortBySize(ArrayList<BitSet> input, boolean eliminateSingletons) {

        int[] sizes = new int[input.size()];
        int max = 0;
        // Bsp input: 6, 5, 3, 5 als sz

        // initialize size
        for (int i = 0; i < input.size(); i++) {
            int size = input.get(i).cardinality();
            sizes[i] = size;
            if (size > max)
                max = size;
        }

        // initialize bucket
        int[] bucket = new int[max + 1];
        ArrayList<LinkedList<BitSet>> bucketSets = new ArrayList<>(max + 1);
        for (int j = 0; j < bucket.length; j++) {
            bucket[j] = 0;
            bucketSets.add(j, new LinkedList<>());
        }
        // add entries for the existing sizes
        for (int i = 0; i < sizes.length; i++) {
            bucket[sizes[i]]++;
            bucketSets.get(sizes[i]).add(input.get(i));
            // bucket[6] = 1; bucket [5] = 2; bucket [3] = 1
            // bucketSets[6] hat {6}, [5] hat {5a, 5b}, [3] hat {3}
        }

        int returnPos = 0;
        input.clear();

        for (int i = bucket.length-1; i >= 0 ; i--) {
            if(bucket[i] > 0){
                LinkedList<BitSet> bucketList = bucketSets.get(i);
                while (!bucketList.isEmpty()) {
                    BitSet retSet = bucketList.removeFirst();
                    if(!eliminateSingletons || retSet.cardinality() > 1) {
                        input.add(returnPos++, retSet);
                    }
                }
            }
        }
    }

    /**
     * Sorts the outgoing/incoming edges according to the given permutation (Proof of Lem. 24).
     * - indices of the returned array are according to σ(v)
     * - the set bits are according to σ(v)
     * @param permutation the permutation σ of the v ∈ V
     * @param positionInPermutation the position every v in σ
     * @param g the directed input graph of the MD
     * @param outgoing true for outgoing, false for incoming edges
     * @return the edges sorted according to the permutation
     */
    public static BitSet[] edgesSortedByPerm(List<Integer> permutation, int[] positionInPermutation,  SimpleDirectedGraph<Integer, DefaultEdge> g, boolean outgoing){

        BitSet[] retSets = new BitSet[g.vertexSet().size()];
        int n = permutation.size();


        // get edges, save in BitSet with their vertexNo in the permutation
        for(int vertex : permutation){
            Set<DefaultEdge> edgeSet;
            if(outgoing)
                edgeSet = g.outgoingEdgesOf(vertex);
            else
                edgeSet = g.incomingEdgesOf(vertex);

            if(!edgeSet.isEmpty()) {
                // iterate edges and get the ordering. Using BitSet for performance
                BitSet edgeTargets = new BitSet(n);
                for (DefaultEdge e : edgeSet) {
                    int secondVertex;
                    if(outgoing)
                        secondVertex = g.getEdgeTarget(e);
                    else
                        secondVertex = g.getEdgeSource(e);
                    int pos = positionInPermutation[secondVertex];
                    edgeTargets.set(pos);
                    //permVertexToEdge.put(pos, e);
                }
                retSets[ positionInPermutation[vertex] ] = edgeTargets;

            } else {
                // add an empty BitSet to avoid nulls
                retSets[ positionInPermutation[vertex] ] = new BitSet();
            }

        }

        return retSets;
    }

    /**
     * Returns the symmetrical difference of two BitSet as a new BitSet, not modifying the incoming sets.
     * @param A the BitSet A
     * @param B the BitSet B
     * @return the BitSet A ∪ B \ A ∩ B
     */
    public static BitSet symDiff(BitSet A, BitSet B){
        BitSet A_cup_B = (BitSet) A.clone();
        BitSet A_cap_B = (BitSet) A.clone();

        A_cup_B.or(B); // ∪
        A_cap_B.and(B); // ∩
        A_cup_B.andNot(A_cap_B); // \
        return A_cup_B;
    }


    // assuming internal type correctness, now check if truly a module.
    public static String checkModuleBruteForce( Graph<Integer, DefaultEdge> graph, Collection<Integer> moduleVertices, boolean expectModule) {

        // 0 - no edge
        //
        String msg;
        StringBuilder res = new StringBuilder();
        TreeSet<Integer> otherVertices = new TreeSet<>(graph.vertexSet());

        otherVertices.removeAll(moduleVertices);
        boolean isModule = true;
        boolean otherToModule = true;
        boolean moduleToOther = true;

        for (int otherV : otherVertices) {
            boolean first = true;
            //boolean isUniformToOtherV = true;

            for (int moduleV : moduleVertices) {
                if (first) {
                    otherToModule = graph.containsEdge(otherV, moduleV);
                    moduleToOther = graph.containsEdge(moduleV, otherV);
                    first = false;
                } else {
                    if (otherToModule != graph.containsEdge(otherV, moduleV)) {
                        isModule = false;
                        if(expectModule) {
                            msg = "Expected value: " + otherToModule + " for edge (" + otherV + "," + moduleV + ")\n";
                            res.append(msg);
                        }
                        //break;
                    }
                    if (moduleToOther != graph.containsEdge(moduleV, otherV)) {
                        isModule = false;
                        if(expectModule) {
                            msg = "Expected value: " + moduleToOther + " for edge (" + moduleV + "," + otherV + ")\n";
                            res.append(msg);
                        }
                        //break;
                    }
                }
            }

        }

        if(isModule && !expectModule){
            // err: subset-module inside supposed prime found.
            msg = "Found only uniform adjacencies.\n";
            res.append(msg);
        }
        return res.toString();
    }
}
