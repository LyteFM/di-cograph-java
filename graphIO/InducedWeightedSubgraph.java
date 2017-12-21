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
import java.util.Map;

import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.SortAndCompare;

/**
 * Created by Fynn Leitow on 21.12.17.
 */
public class InducedWeightedSubgraph extends SimpleDirectedGraph<Integer,DefaultWeightedEdge> {

    private final SimpleDirectedGraph<Integer, DefaultWeightedEdge> base;
    private final int nVertices;
    private final Map<Integer,Double> vertexToWeight;


    public InducedWeightedSubgraph(SimpleDirectedGraph<Integer,DefaultWeightedEdge> baseGraph, MDTreeNode node){
        super(new ClassBasedEdgeFactory<>(DefaultWeightedEdge.class),true);
        base = baseGraph;
        vertexToWeight = node.initWeightedSubgraph(this,base);
        nVertices = vertexToWeight.size();
    }

    // Possible optimization: sequential subsets! (a,b) -> (a,b,c) ...
    public HashMap<Integer, List<List<DefaultWeightedEdge>>> allEditsByWeight(){
        // an entry in the map means: if present -> remove, if not -> add.
        //
        if(nVertices > 25)
            throw new RuntimeException("n too large!");

        HashMap<Integer, List<List<DefaultWeightedEdge>>> costToEdges = new HashMap();
        ClassBasedEdgeFactory<Integer,DefaultWeightedEdge> edgeFactory = (ClassBasedEdgeFactory<Integer, DefaultWeightedEdge>) getEdgeFactory();
        HashMap<Integer, DefaultWeightedEdge> intToEdge = new HashMap<>();


        // I need this to compute all possible permutations for the n^2 edges...

        // a) use the EdgeFactory and create all those edges with their respective weight. Add them later.
        // b) use Pairs.

        int cnt = 0;
        for (int i : vertexSet()) {
            for (int j : vertexSet()) {
                if(j!=i){
                    DefaultWeightedEdge e = getEdge(i,j);
                    if(e == null){
                        double weight = vertexToWeight.get(i) * vertexToWeight.get(j);
                        e = getEdgeFactory().createEdge(i,j);
                        setEdgeWeight(e, weight); // todo: is this okay? Should know the edge but not add.
                    }
                    intToEdge.put( cnt, e );
                    cnt++;
                }
            }
        }
        System.out.println("All Edges created with weights");

        // this will kill me with more than 5 vertices in the prime module... need a better strategy.
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


        return costToEdges;
    }


    public void edit(List<DefaultWeightedEdge> edgeList){

        for(DefaultWeightedEdge e : edgeList){
            if(containsEdge(e)){
                removeEdge(e);
            } else {
                addEdge(getEdgeSource(e), getEdgeTarget(e), e);
            }
        }

    }
}
