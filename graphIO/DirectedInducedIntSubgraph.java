package dicograph.graphIO;

import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.BitSet;


/**
 * Created by Fynn Leitow on 09.11.17. Only Integers as vertices allowed s.t. BitSet works.
 */
public class DirectedInducedIntSubgraph<E> extends SimpleDirectedGraph<Integer, E>
{

    final SimpleDirectedGraph<Integer, E> base;
//    //
//    public DirectedInducedIntSubgraph(EdgeFactory<Integer, E> ef)
//    {
//        super(ef);
//    }
//
//    public DirectedInducedIntSubgraph(Class<? extends E> edgeClass){
//        this(new ClassBasedEdgeFactory<>(edgeClass));
//    }

    public DirectedInducedIntSubgraph(SimpleDirectedGraph<Integer, E> baseGraph,  BitSet vertices){
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

}
