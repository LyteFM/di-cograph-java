package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.EmptyGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
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
    public SimpleDirectedGraph<String, DefaultWeightedEdge> generateRandomGnp(int numberVertices, double edgeProbability){

        SimpleDirectedGraph<String, DefaultWeightedEdge> graph = new SimpleDirectedGraph<>(DefaultWeightedEdge.class);

        GnpRandomGraphGenerator<String, DefaultWeightedEdge> generator =
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

        SimpleDirectedGraph<String, DefaultWeightedEdge> graph = new SimpleDirectedGraph<>(DefaultWeightedEdge.class);

        GnmRandomGraphGenerator<String, DefaultWeightedEdge> generator =
                new GnmRandomGraphGenerator<>(numberVertices, numberEdges, random, false, false);
        generator.generateGraph(graph, new StringVertexFactory(), null);

        return graph;
    }

    public void generateRandomCograph(SimpleGraph<Integer,DefaultWeightedEdge> graph, int nVertices){

        generateCograph(graph, nVertices, false, false);

        // use same method as for Di-Cograph. Only difference:
        // - value for choosing the type must be 2
        // - everything else is ok!
    }

    public Set<BitSet> generateRandomDirectedCograph(SimpleDirectedGraph<Integer, DefaultWeightedEdge> graph, int nVertices, boolean getBitSets){
        return generateCograph(graph, nVertices,true, true);
    }


    private Set<BitSet> generateCograph(Graph<Integer,DefaultWeightedEdge> graph, int nVertices, boolean isDirected, boolean getBitSets){

        // adds n vertices
        EmptyGraphGenerator<Integer, DefaultWeightedEdge> gen = new EmptyGraphGenerator<>(nVertices);
        gen.generateGraph(graph, new IntegerVertexFactory(), null);

        // Save the Graph's modules in BitSets for easy comparison
        HashSet<BitSet> allmodules = new HashSet<>();

        // init the List of modules
        ArrayList<HashSet<Integer>> modules = new ArrayList<>();
        for(int vertex : graph.vertexSet()){
            HashSet<Integer> moduleVertices = new HashSet<>();
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

        int moduleCount = nVertices; // 10
        while (moduleCount > 1) {

            // generates k: number of modules to join
            // nicer distribution: small modules must be more likely. Larger modules still appear through
            // mergind same type of modules.
            // former: random.nextDouble()
            double rand = Math.abs(random.nextGaussian())/6;
            Double nToCombine = rand * (moduleCount - 1); // <9 + rounding -> intValue: <= 8
            int k = nToCombine.intValue() + 2;
            // fix rounding error
            if(k > moduleCount)
                k = moduleCount;
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

            // k <= 10 -> {0,1,...,k-1}.
            ArrayList<Integer> possibleIndices = new ArrayList<>();
            for( int i = 0; i<k; i++){
                possibleIndices.add(i);
            }
            // generates k indices to choose the G_i
            int [] chosenIndeces = new int[k];
            for( int i = 0; i<k; i++){
                // 1.: get a random temp index for the list of possible indices
                int size = possibleIndices.size(); // = k at first loop
                Double nextIndex = random.nextDouble() * size; // from 0 to k-1
                int tempIndex = nextIndex.intValue();
                // fix rounding error
                if(tempIndex>= possibleIndices.size()){
                    tempIndex = possibleIndices.size()-1;
                }

                // 2.: get the real index and update the lists
                int realIndex = possibleIndices.get(tempIndex);
                possibleIndices.remove(tempIndex);
                //chosenIndices.add(realIndex);
                chosenIndeces[i] = realIndex;
            }

            ArrayList<HashSet<Integer>> chosenModules = new ArrayList<>();
            for( int i : chosenIndeces){
                chosenModules.add(modules.get(i));
            }

            // PARALLEL ("0"): disjoint union - the modules G_i together are now the graph
            // SERIES ("1"):   union of the modules plus all possible arcs between members of different G_i
            // ORDER ("1->"):  union of k modules plus all possible arcs from G_i to G_j with 1 <= i < j <= k
            StringBuilder msg = new StringBuilder("merging "+ mdNodeType + ": ");
            for(HashSet module : chosenModules){
                msg.append("\"").append(module).append("\", ");
            }
            logger.fine(msg.toString());
            HashSet<Integer> mergedModule = union(graph, chosenModules, mdNodeType);

            // remove the old modules and add the new one
            Arrays.sort(chosenIndeces);
            for( int i = chosenIndeces.length-1; i >= 0; i--){
                modules.remove(i);
            }
            modules.add(mergedModule);
            if(getBitSets){
                BitSet moduleBits = new BitSet();
                for(int i : mergedModule){
                    moduleBits.set(i);
                }
                allmodules.add(moduleBits);
            }

            moduleCount = modules.size();
        }

        logger.info("Generated graph: " + graph.toString());

        if(getBitSets){
            return allmodules;
        } else {
            return null;
        }
    }

    private HashSet<Integer> union(Graph<Integer,DefaultWeightedEdge> g, ArrayList<HashSet<Integer>> selectedModules, MDNodeType type){

        // merge all vertices into the first module
        HashSet<Integer> ret = new HashSet<>(selectedModules.get(0));

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
                            HashSet<Integer> firstModule = selectedModules.get(i);
                            HashSet<Integer> secondModule = selectedModules.get(j);

                            for(int outVertex : firstModule){
                                for(int inVertex : secondModule){
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
    public SimpleDirectedGraph<Integer, DefaultWeightedEdge> disturbDicograph(SimpleDirectedGraph<Integer, DefaultWeightedEdge> g, int nEdgeEdits){

        HashSet<String> usedEdges = new HashSet<>(nEdgeEdits*4/3);
        ArrayList<Integer> vertices = new ArrayList<>(g.vertexSet());
        int nVertices = vertices.size();

        int count = 0;
        while (count < nEdgeEdits){

            int uIndex = getRandomVertex(nVertices);
            int u = vertices.get(uIndex);

            int vIndex = getRandomVertex(nVertices);
            int v = vertices.get(vIndex);
            String edgeString = u + "->" + v;

            // May not be the same and may not have been altered already
            if( ! (u==v) && !usedEdges.contains(edgeString) ){

                DefaultWeightedEdge edge = g.getEdge(u,v);
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

    /**
     * Returns a deep clone (new vertex and edge objects) of the graph
     * @param input input graph
     * @return cloned graph
     */
    public static SimpleDirectedGraph<Integer,DefaultWeightedEdge> deepClone(final SimpleDirectedGraph<Integer, DefaultWeightedEdge> input){
        SimpleDirectedGraph<Integer,DefaultWeightedEdge> ret = new SimpleDirectedGraph<>(DefaultWeightedEdge.class);
        input.vertexSet().forEach( ret::addVertex );
        input.edgeSet().forEach( e -> ret.addEdge( input.getEdgeSource(e), input.getEdgeTarget(e) ));
        return ret;
    }

}
