package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.SortAndCompare;

/**
 * Created by Fynn Leitow on 21.12.17.
 */
public class InducedWeightedSubgraph extends SimpleDirectedGraph<Integer,DefaultWeightedEdge> {

    private final SimpleDirectedGraph<Integer, DefaultWeightedEdge> base;

    private InducedWeightedSubgraph(SimpleDirectedGraph<Integer,DefaultWeightedEdge> baseGraph){
        super(new ClassBasedEdgeFactory<>(DefaultWeightedEdge.class),true);
        base = baseGraph;
    }

    public InducedWeightedSubgraph(SimpleDirectedGraph<Integer,DefaultWeightedEdge> baseGraph, MDTreeNode node){
        this(baseGraph);
        node.initWeightedSubgraph(this,base);
    }

    public HashMap<Integer, List<List<DefaultWeightedEdge>>> allEditsByWeight(){
        // an entry in the map means: if present -> remove, if not -> add.
        //
        HashMap<Integer, List<List<DefaultWeightedEdge>>> costToEdges = new HashMap();

        // I need this to compute all possible permutations for the n^2 edges...
        HashMap<Integer, DefaultWeightedEdge> intToEdge = new HashMap<>();
        int i = 0;
        for( DefaultWeightedEdge e : edgeSet()){
            intToEdge.put(i, e);
            i++;
        }

        for(List<Integer> edgeSubset : SortAndCompare.computeAllSubsets( new ArrayList<>(intToEdge.keySet()) )){
            int cost = 0;
            List<DefaultWeightedEdge> edges = new ArrayList<>(edgeSubset.size());
            for(int edgeNo : edgeSubset){
                DefaultWeightedEdge e = intToEdge.get(edgeNo);
                edges.add(e);
                cost += Math.round(getEdgeWeight(e)); // better use rounding, just in case...
            }
            costToEdges.putIfAbsent(cost,new LinkedList<>());
            costToEdges.get(cost).add(edges);
        }
        // damn, these are only deletions...



            return costToEdges;
    }
}
