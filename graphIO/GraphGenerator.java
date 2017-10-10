package dicograph.graphIO;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.EmptyGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import dicograph.ILPSolver.CplexDiCographEditingSolver;
import dicograph.modDecomp.MDNodeType;

import static dicograph.modDecomp.MDNodeType.*;

/**
 * Created by Fynn Leitow on 08.10.17.
 */
public class GraphGenerator {

    private SecureRandom random;
    private Logger logger;

    public GraphGenerator(Logger log) {
        random = new SecureRandom();
        random.setSeed(new byte[20]);
        logger = log;
    }




    /**
     * Randomly creates a simple directed graph with:
     * @param numberVertices the number of vertices
     * @param edgeProbability the probability of a directed edge (u,v) between any two vertices u,v
     * @return the random graph
     */
    public SimpleDirectedGraph<String, DefaultEdge> generateRandomGnp(int numberVertices, double edgeProbability){

        SimpleDirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

        GnpRandomGraphGenerator<String, DefaultEdge> generator =
                new GnpRandomGraphGenerator<>(numberVertices, edgeProbability, random, false);
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

        GnmRandomGraphGenerator<String, DefaultEdge> generator =
                new GnmRandomGraphGenerator<>(numberVertices, numberEdges, random, false, false);
        generator.generateGraph(graph, new StringVertexFactory(), null);

        return graph;
    }

    public String generateRandomCograph(SimpleGraph<String,DefaultEdge> graph, int nVertices){

        return generateCograph(graph, nVertices, false);

        // use same method as for Di-Cograph. Only difference:
        // - value for choosing the type must be 2
        // - everything else is ok!
    }

    public String generateRandomDirectedCograph(SimpleDirectedGraph<String, DefaultEdge> graph, int nVertices){
        return generateCograph(graph, nVertices,true);
    }


    private String generateCograph(Graph<String,DefaultEdge> graph, int nVertices, boolean isDirected){

        // todo: a) rekursiv bauen b) MDTree- und LeafNodes verwenden
        StringBuilder mdTree = new StringBuilder();

        // adds n vertices
        EmptyGraphGenerator<String, DefaultEdge> gen = new EmptyGraphGenerator<>(nVertices);
        gen.generateGraph(graph, new StringVertexFactory(), null);

        // Idea: Save the Graph in the same MDTree-String-Format as in Tedder's Code for easy comparison

        // init the List of modules
        ArrayList<HashSet<String>> modules = new ArrayList<>();
        for(String vertex : graph.vertexSet()){
            HashSet<String> moduleVertices = new HashSet<>();
            moduleVertices.add(vertex);
            modules.add(moduleVertices);
        }



        // Directed Graphs have one additional MDType.
        int typeMultiplier;
        if(isDirected)
            typeMultiplier = 3;
        else
            typeMultiplier =2;

        // Pick any number k modules G_i to perform parallel, series or order composition

        // Picking all modules should be much less likely than picking two.
        //      idea: add option for nextGaussian.
        //          Either: Take abs, closer to 0 means choose one, further away means any
        //          or: distribute around nVertices/2

        int moduleCount = nVertices;
        while (moduleCount > 1) {

            // generates k: number of modules to join
            Double nToCombine = random.nextDouble() * (moduleCount - 1);
            int k = nToCombine.intValue() + 2;
            // range of k must be from 2 to moduleCount!

            // generates MDNodeType
            Double mdType = random.nextDouble() * typeMultiplier;
            MDNodeType mdNodeType;
            if(mdType<1){
                mdNodeType = PARALLEL;
            } else if (mdType < 2){
                mdNodeType = SERIES;
            } else {
                mdNodeType = ORDER;
            }

            // todo: Das muss ich überprüfen mit den Indizes!!!
            ArrayList<Integer> possibleIndices = new ArrayList<>();
            for( int i = 0; i<k; i++){
                possibleIndices.add(i);
            }
            // generates k indices to choose the G_i
            ArrayList<Integer> chosenIndices = new ArrayList<>();
            for( int i = 0; i<k; i++){
                // 1.: get a random temp index for the list of possible indices
                int size = possibleIndices.size();
                Double nextIndex = random.nextDouble() * size;
                int tempIndex = nextIndex.intValue(); // can't be too large!

                // 2.: get the real index and update the lists
                int realIndex = possibleIndices.get(tempIndex);
                possibleIndices.remove(tempIndex);
                chosenIndices.add(realIndex);
            }

            ArrayList<HashSet<String>> chosenModules = new ArrayList<>();
            for( int i : chosenIndices){
                chosenModules.add(modules.get(i)); // ok jetzt wirft er schon hier.
                modules.remove(i);
            }

            // PARALLEL ("0"): disjoint union - the modules G_i together are now the graph
            // SERIES ("1"):   union of the modules plus all possible arcs between members of different G_i
            // ORDER ("1->"):  union of k modules plus all possible arcs from G_i to G_j with 1 <= i < j <= k
            String msg = "merging "+ mdNodeType + ": ";
            for(HashSet module : chosenModules){
                msg += module;
            }
            logger.fine(msg);
            HashSet<String> mergedModule = union(graph, chosenModules, mdNodeType);

            // remove the old modules and add the new one
            //for( int i : chosenIndices){
            //     // todo: Fehler hier!!! Index >= size!
            //}
            modules.add(mergedModule);

            moduleCount = modules.size();
        }

        return mdTree.toString();
    }

    private HashSet<String> union(Graph<String,DefaultEdge> g, ArrayList<HashSet<String>> selectedModules, MDNodeType type){

        // merge all vertices into the first module
        HashSet<String> ret = new HashSet<>(selectedModules.get(0));

        for( int i=1; i< selectedModules.size(); i++){
            ret.addAll(selectedModules.get(i));
        }

        // PARALLEL: no modifications on g, only on modules
        if (type == SERIES || type == ORDER){
            for(int i = 0; i< selectedModules.size(); i++){
                for (int j = 0; j< selectedModules.size(); j++){

                    if(i != j) {
                        // ORDER: add directed edges in module order only
                        // SERIES: add edges in both directions

                        if(type == SERIES || i <j) {
                            HashSet<String> firstModule = selectedModules.get(i);
                            HashSet<String> secondModule = selectedModules.get(j);

                            for(String outVertex : firstModule){
                                for(String inVertex : secondModule){
                                    logger.fine("Adding: " + outVertex + "->" + inVertex);
                                    assert !inVertex.equals(outVertex);
                                    g.addEdge(outVertex, inVertex);
                                }
                            }
                        }
                    }
                }
            }
        }

        return ret;
    }


    /**
     * Randomly chooses two distinct vertices of g. If the edge exists in g, it is deleted. If it doesn't, it is created.
     * This is repeated until exactly n edges have been modified. No directed edge will be modified more than once.
     * @param g the directed cograph
     * @param nEdgeEdits the number of edge edits
     * @return the disturbed cograph
     */
    public SimpleDirectedGraph<String, DefaultEdge> disturbDicograph(SimpleDirectedGraph<String, DefaultEdge> g, int nEdgeEdits){

        HashSet<String> usedEdges = new HashSet<>(nEdgeEdits*4/3);
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
