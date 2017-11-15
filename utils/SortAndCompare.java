package dicograph.utils;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dicograph.graphIO.IntegerVertexFactory;

/**
 * Created by Fynn Leitow on 17.10.17.
 */
public class SortAndCompare {

    public static void shuffleList(Integer[] input, SecureRandom random){

        int size = input.length;

        for (int i = size-1; i >= 0; i--) {
            int newIndex = random.nextInt(i+1);
            int tmp = input[i];
            input[i] = input[newIndex];
            input[newIndex] = tmp;
        }
    }

    public static ArrayList<Set<Integer>> bucketSortBySize(ArrayList<Set<Integer>> input) {

        int[] sizes = new int[input.size()];
        int max = 0;
        // Bsp input: 6, 5, 3, 5 als sz

        // initialize size
        for (int i = 0; i < input.size(); i++) {
            int size = input.get(i).size();
            sizes[i] = size;
            if (size > max)
                max = size;
        }

        // initialize bucket
        int[] bucket = new int[max + 1];
        ArrayList<ArrayList<Set<Integer>>> bucketSets = new ArrayList<>(max + 1);
        for (int j = 0; j < bucket.length; j++) {
            bucket[j] = 0;
            bucketSets.add(j, new ArrayList<>());
        }
        // add entries for the existing sizes
        for (int i = 0; i < sizes.length; i++) {
            bucket[sizes[i]]++;
            bucketSets.get(sizes[i]).add(input.get(i));
            // bucket[6] = 1; bucket [5] = 2; bucket [3] = 1
            // bucketSets[6] hat {6}, [5] hat {5a, 5b}, [3] hat {3}
        }

        int returnPos = 0;
        ArrayList<Set<Integer>> ret = new ArrayList<>(input.size());

        for (int i = 0; i < bucket.length; i++) {
            for (int j = 0; j < bucket[i]; j++) {
                // Hier rein, wenn's Eintrag gibt
                Set<Integer> retSet = bucketSets.get(i).get(j);
                ret.add(returnPos++, retSet);
            }
        }

        return ret;
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
    public static BitSet[] edgesSortedByPerm(List<Integer> permutation, int[] positionInPermutation,  DirectedGraph<Integer, DefaultEdge> g, boolean outgoing){

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
                // todo: use the real vertexNo or the position in permutation?
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
}
