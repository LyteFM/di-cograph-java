package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.BitSet;
import java.util.List;
import java.util.TreeSet;


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
private DirectedInducedIntSubgraph(Graph<Integer, E> baseGraph) {
        super(baseGraph.getEdgeFactory());
    base = (SimpleDirectedGraph<Integer, E>) baseGraph;
}

    public DirectedInducedIntSubgraph(Graph<Integer, E> baseGraph, BitSet vertices) {
        this(baseGraph);

        vertices.stream().forEach( this::addVertex );
        addEdges();
    }

    public DirectedInducedIntSubgraph(Graph<Integer, E> baseGraph, List<Integer> vertices) {
        this(baseGraph);

        vertices.forEach( this::addVertex );
        addEdges();
    }

    private void addEdges() {
        // adds edges to vertices that are contained in this subgraph
        for (int vertex : vertexSet()) {
            for (E outEdge : base.outgoingEdgesOf(vertex)) {
                int target = base.getEdgeTarget(outEdge);

                if (containsVertex(target)) {
                    addEdge(vertex, target);
                }
            }
        }
    }

    /**
     * Naive, nonlinear method to verify whether the graph is a tournament.
     * one equivalence condition for being a tournament is:
     * "The score sequence of T is {0,1,2,...,n-1}".
     * For debugging purposes only.
     * @return true, if this graph is a Tournament
     */
    public boolean isTournament(){
        TreeSet<Integer> outdegrees = new TreeSet<>();
        for(Integer vertex : vertexSet()){
            outdegrees.add(outDegreeOf(vertex));
        }
        int score = 0;
        for(int outDeg :outdegrees){
            if(outDeg != score)
                return false;
            score++;
        }
        return true;
    }

    public SimpleDirectedGraph<Integer, E> getBase() {
        return base;
    }
}
