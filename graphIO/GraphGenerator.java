package dicograph.graphIO;

import org.jgrapht.DirectedGraph;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import dicograph.ILPSolver.CplexDiCographEditingSolver;

/**
 * Created by Fynn Leitow on 08.10.17.
 */
public class GraphGenerator {

    SecureRandom random;

    public GraphGenerator() {
        random = new SecureRandom();
        random.setSeed(new byte[20]);
    }

    public GraphGenerator(SecureRandom secureRandom){
        random = secureRandom;
    }


    /**
     * Randomly creates a simple directed graph with:
     * @param numberVertices the number of vertices
     * @param edgeProbability the probability of a directed edge (u,v) between any two vertices u,v
     * @return the random graph
     */
    public SimpleDirectedGraph<String, DefaultEdge> generateRandomGnp(int numberVertices, double edgeProbability){

        SimpleDirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

        GnpRandomGraphGenerator generator = new GnpRandomGraphGenerator(numberVertices, edgeProbability, random, false);
        generator.generateGraph(graph, new StringVertexFactory(),null);

        return graph;
    }

    /**
     * Randomly creates a simple directed graph from all possible graphs with:
     * @param numberVertices the number of vertices
     * @param numberEdges the number of edges
     * @return the random graph
     */
    public SimpleDirectedGraph generateRandomGnm(int numberVertices, int numberEdges){

        SimpleDirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

        GnmRandomGraphGenerator generator = new GnmRandomGraphGenerator(numberVertices, numberEdges, random, false, false);
        generator.generateGraph(graph, new StringVertexFactory(), null);

        return graph;
    }

    public SimpleDirectedGraph generateRandomDicograph(int nVertices){
        SimpleDirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);



        return graph;
    }

    /**
     * Randomly chooses two distinct vertices of g. If the edge exists in g, it is deleted. If it doesn't, it is created.
     * This is repeated until exactly n edges have been modified. No directed edge will be modified more than once.
     * @param g the directed cograph
     * @param nEdgeEdits the number of edge edits
     * @return the disturbed cograph
     */
    public SimpleDirectedGraph<String, DefaultEdge> disturbDicograph(SimpleDirectedGraph<String, DefaultEdge> g, int nEdgeEdits){

        HashSet<String> usedEdges = new HashSet<String>(nEdgeEdits*4/3);
        ArrayList<String> vertices = new ArrayList<>(g.vertexSet());
        int nVertices = vertices.size();

        int count = 0;
        while (count < nEdgeEdits){

            int uIndex = getRandomVertex(nVertices);
            String u = vertices.get(uIndex);

            int vIndex = getRandomVertex(nVertices);
            String v = vertices.get(vIndex);
            String edgeString = u + "->" + v;

            // May not be the same and may not have been altered already
            if( ! u.equals(v) && !usedEdges.contains(edgeString) ){

                DefaultEdge edge = g.getEdge(u,v);
                if( edge == null){
                    g.addEdge(u,v);
                } else {
                    g.removeEdge(edge);
                }
                usedEdges.add(edgeString);
                count++;
            }
        }

        return g;
    }

    private int getRandomVertex(int nVertices){

        // want a random vertex: 0 <= rand < number of vertices
        Double value = random.nextDouble() * nVertices;
        int vertexIndex = value.intValue();
        // possible due to rounding errors:
        if( vertexIndex == nVertices) {
            vertexIndex = nVertices - 1;
        }
        return vertexIndex;
    }

}
