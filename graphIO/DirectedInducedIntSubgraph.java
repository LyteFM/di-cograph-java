package dicograph.graphIO;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.BitSet;
import java.util.List;


/**
 * Created by Fynn Leitow on 09.11.17. Only Integers as vertices allowed s.t. BitSet works.
 */
public class DirectedInducedIntSubgraph<E> extends SimpleDirectedGraph<Integer, E>
{

    final DirectedGraph<Integer, E> base;
//    //
//    public DirectedInducedIntSubgraph(EdgeFactory<Integer, E> ef)
//    {
//        super(ef);
//    }
//
//    public DirectedInducedIntSubgraph(Class<? extends E> edgeClass){
//        this(new ClassBasedEdgeFactory<>(edgeClass));
//    }

    public DirectedInducedIntSubgraph(DirectedGraph<Integer, E> baseGraph,  BitSet vertices){
        super(baseGraph.getEdgeFactory());
        base = baseGraph;

        // adds the vertices
        vertices.stream().forEach( this::addVertex );

        // adds edges to vertices that are contained in this subgraph
        for( int vertex : vertexSet()){
            for(E outEdge : baseGraph.outgoingEdgesOf(vertex)) {
                int target = baseGraph.getEdgeTarget(outEdge);

                if(containsVertex(target)) {
                    addEdge(vertex, target);
                }
            }
        }
    }

    public DirectedInducedIntSubgraph(DirectedGraph<Integer, E> baseGraph, List<Integer> vertices){
        super(baseGraph.getEdgeFactory());
        base = baseGraph;

        // adds the vertices
        vertices.forEach( this::addVertex );

        // adds edges to vertices that are contained in this subgraph
        for( int vertex : vertexSet()){
            for(E outEdge : baseGraph.outgoingEdgesOf(vertex)) {
                int target = baseGraph.getEdgeTarget(outEdge);

                if(containsVertex(target)) {
                    addEdge(vertex, target);
                }
            }
        }
    }

}
