package dicograph.graphIO;

import com.google.common.collect.Sets;

import org.jgrapht.Graphs;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.WeightedPair;

/**
 * Created by Fynn Leitow on 21.12.17.
 */
public class InducedWeightedSubgraph extends SimpleDirectedGraph<Integer,DefaultWeightedEdge> {

    private static final int maxCost = 6;
    private final SimpleDirectedGraph<Integer, DefaultWeightedEdge> base;
    private final int nVertices;

    private final HashMap<Integer,Integer> baseNoTosubNo;
    private final double[] subVertexToWeight;
    private final int[] subNoToBaseNo;


    public InducedWeightedSubgraph(SimpleDirectedGraph<Integer,DefaultWeightedEdge> baseGraph, MDTreeNode node){
        super(new ClassBasedEdgeFactory<>(DefaultWeightedEdge.class),true);
        base = baseGraph;
        baseNoTosubNo = new HashMap<>();
        subVertexToWeight = node.initWeightedSubgraph(this,base);
        nVertices = subVertexToWeight.length;
        subNoToBaseNo = new int[nVertices];
        for(Map.Entry<Integer, Integer> base2Sub : baseNoTosubNo.entrySet()){
            subNoToBaseNo[base2Sub.getValue()] =  base2Sub.getKey();
        }
    }

    // Possible optimization: sequential subsets! (a,b) -> (a,b,c) ...
    public Map<Integer, List<List<WeightedPair<Integer, Integer>>>> computeBestEdgeEdit(Logger log)
    throws InterruptedException, IOException, ImportException{
        // an entry in the map means: if present -> remove, if not -> add.
        //



        HashMap<Integer, List<List<WeightedPair<Integer, Integer>>>> costToEdges = new HashMap<>();
        Map<Integer, WeightedPair<Integer,Integer>> intToEdge = new HashMap<>();


        // I need this to compute all possible permutations for the n^2 edges...

        // a) use the EdgeFactory and create all those edges with their respective weight. Add them later. <- doesn't work if I don't add them...
        // b) use Pairs.

        int cnt = 0;
        // i,j are subVertices.
        for (int i : vertexSet()) {
            for (int j : vertexSet()) {
                if(j!=i){
                    double weight = subVertexToWeight[i] * subVertexToWeight[j];
                    WeightedPair<Integer,Integer> e = new WeightedPair<>(i,j,weight);
                    intToEdge.put( cnt, e );
                    cnt++;
                }
            }
        }

        int smallestCost = maxCost;
        for (int i = 1; i < intToEdge.keySet().size(); i++) {
            System.out.println("Computing all permutations of size " + i + " for n = " + cnt);
            Set<Set<Integer>> combinations =  Sets.combinations(intToEdge.keySet(), i);
            System.out.println(combinations.size() + " permutations!");
            boolean costStillValid = false;

            //int count = 0;

            for(Set<Integer> edgeSubset : combinations){
                int cost = 0;
                List<WeightedPair<Integer,Integer>> currEdgeList = new ArrayList<>(edgeSubset.size());
                for(int edgeNo : edgeSubset){
                    WeightedPair<Integer,Integer> e = intToEdge.get(edgeNo);
                    currEdgeList.add(e);
                    cost += Math.round(e.getWeight()); // better use rounding, just in case...
                }
                if(cost < maxCost) {

                    costStillValid = true;

                    edit(currEdgeList);
                    // Brute force checking for forbidden subgraphs might be more efficient...
                    log.info("i: " + i + ", Edges: " + currEdgeList);
                    DirectedMD subMD = new DirectedMD(this, log, false);
                    MDTree subTree = subMD.computeModularDecomposition();
                    // edit back.
                    edit(currEdgeList);
                    if(subTree.getPrimeModulesBottomUp().isEmpty()){
                        log.info(() -> " Successful edit found: " + currEdgeList);
                        costToEdges.putIfAbsent(cost, new LinkedList<>());
                        costToEdges.get(cost).add(currEdgeList);
                        // need this to verify that an early, but expensive edit is best.
                        if(cost < smallestCost)
                            smallestCost = cost;
                        if(smallestCost <= i)
                            return costToEdges;
                    }

                }


//                count++;
//                if(count == 50000000) {
//                    log.info("Aborted after 500");
//                    break;
//                }
            }

            if(!costStillValid){
                break; // no need to continue
            }

        }


        return costToEdges;
    }


    public void edit(List<WeightedPair<Integer,Integer>> edgeList){

        for(WeightedPair<Integer,Integer> e : edgeList){
            if(containsEdge(e.getFirst(),e.getSecond())){
                removeEdge(e.getFirst(),e.getSecond());
            } else {
                // todo: due to weighted Pair, I might not need weightedEdge at all.
                Graphs.addEdge(this, e.getFirst(), e.getSecond(), e.getWeight());
            }
        }

    }


    public HashMap<Integer, Integer> getBaseNoTosubNo() {
        return baseNoTosubNo;
    }

    public SimpleDirectedGraph<Integer, DefaultWeightedEdge> getBase() {
        return base;
    }

    public int getnVertices() {
        return nVertices;
    }

    public double[] getSubVertexToWeight() {
        return subVertexToWeight;
    }

    public int[] getSubNoToBaseNo() {
        return subNoToBaseNo;
    }
}
