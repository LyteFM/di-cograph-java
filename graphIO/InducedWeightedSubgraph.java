package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import dicograph.modDecomp.MDTreeNode;

/**
 * Created by Fynn Leitow on 21.12.17.
 */
public class InducedWeightedSubgraph extends SimpleDirectedGraph<Integer,DefaultWeightedEdge> {

    private final SimpleDirectedGraph<Integer, DefaultEdge> base;

    private InducedWeightedSubgraph(SimpleDirectedGraph<Integer,DefaultEdge> baseGraph){
        super(new ClassBasedEdgeFactory<>(DefaultWeightedEdge.class),true);
        base = baseGraph;
    }

    public InducedWeightedSubgraph(SimpleDirectedGraph<Integer,DefaultEdge> baseGraph, MDTreeNode node){
        this(baseGraph);
        node.initWeightedSubgraph(this,base);
    }

}
