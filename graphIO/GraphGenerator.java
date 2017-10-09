package dicograph.graphIO;

import org.jgrapht.DirectedGraph;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import dicograph.ILPSolver.CplexDiCographEditingSolver;

/**
 * Created by Fynn Leitow on 08.10.17.
 */
public class GraphGenerator {


    /**
     * Randomly creates a simple directed graph with:
     * @param numberVertices the number of vertices
     * @param edgeProbability the probability of a directed edge (u,v) between any two vertices u,v
     * @return the random graph
     */
    public static SimpleDirectedGraph<String, DefaultEdge> generateRandomGnp(int numberVertices, double edgeProbability){

        SimpleDirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

        GnpRandomGraphGenerator generator = new GnpRandomGraphGenerator(numberVertices, edgeProbability);
        generator.generateGraph(graph, new StringVertexFactory(),null);

        return graph;
    }

    /**
     * Randomly creates a simple directed graph from all possible graphs with:
     * @param numberVertices the number of vertices
     * @param numberEdges the number of edges
     * @return the random graph
     */
    public static SimpleDirectedGraph generateRandomGnm(int numberVertices, int numberEdges){

        SimpleDirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

        GnmRandomGraphGenerator generator = new GnmRandomGraphGenerator(numberVertices, numberEdges);
        generator.generateGraph(graph, new StringVertexFactory(), null);

        return graph;
    }

}
