package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.Collection;

/**
 * Created by Fynn Leitow on 16.11.17.
 */
public class UndirectedInducedIntSubgraph<E> extends SimpleGraph<Integer,E> {

    private final SimpleGraph<Integer, E> base;

    public UndirectedInducedIntSubgraph(Graph<Integer, E> baseGraph, Collection<Integer> vertices){
        super(baseGraph.getEdgeFactory());
        base = (SimpleGraph<Integer, E>) baseGraph;

        // adds the vertices
        vertices.forEach( this::addVertex );

        // adds edges to vertices that are contained in this subgraph
        for( int vertex : vertexSet()){
            for(E outEdge : baseGraph.edgesOf(vertex)) {
                int target = baseGraph.getEdgeTarget(outEdge);
                int source = baseGraph.getEdgeSource(outEdge);

                if(target == vertex){
                    if(containsVertex(source)) {
                        addEdge(source, vertex);
                    }
                } else if (source == vertex){
                    if(containsVertex(target)) {
                        addEdge(vertex, target);
                    }
                } else {
                    throw new IllegalStateException("Error: Edge not connected to vertex " + vertex);
                }


            }
        }
    }


}
