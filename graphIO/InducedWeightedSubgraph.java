package dicograph.graphIO;

import org.jgrapht.Graphs;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.SortAndCompare;
import dicograph.utils.WeightedPair;

/**
 * Created by Fynn Leitow on 21.12.17.
 */
public class InducedWeightedSubgraph extends SimpleDirectedGraph<Integer,DefaultWeightedEdge> {

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
    public HashMap<Integer, List<List<WeightedPair<Integer,Integer>>>> allEditsByWeight(){
        // an entry in the map means: if present -> remove, if not -> add.
        //
        if(nVertices > 6)
            throw new RuntimeException("n too large!");

        HashMap<Integer, List<List<WeightedPair<Integer,Integer>>>> costToEdges = new HashMap<>();
        HashMap<Integer, WeightedPair<Integer,Integer>> intToEdge = new HashMap<>();


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
        System.out.println("All Edges created with weights");

        // this will kill me with more than 5 vertices in the prime module... need a better strategy.
        for(List<Integer> edgeSubset : SortAndCompare.computeAllSubsets( new ArrayList<>(intToEdge.keySet()) )){
            int cost = 0;
            List<WeightedPair<Integer,Integer>> edges = new ArrayList<>(edgeSubset.size());
            for(int edgeNo : edgeSubset){
                WeightedPair<Integer,Integer> e = intToEdge.get(edgeNo);
                edges.add(e);
                cost += Math.round(e.getWeight()); // better use rounding, just in case...
            }
            costToEdges.putIfAbsent(cost,new LinkedList<>());
            costToEdges.get(cost).add(edges);
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
