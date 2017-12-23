package dicograph.Editing;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import dicograph.graphIO.GraphGenerator;
import dicograph.graphIO.InducedWeightedSubgraph;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.WeightedPair;

/**
 * Created by Fynn Leitow on 20.12.17.
 */
public class MDEditor {

    final SimpleDirectedGraph<Integer,DefaultWeightedEdge> inputGraph;
    SimpleDirectedGraph<Integer, DefaultWeightedEdge> editGraph;
    final  int nVertices;
    final Logger log;

    public MDEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> input, Logger logger){
        inputGraph = input;
        nVertices = inputGraph.vertexSet().size();
        editGraph = GraphGenerator.deepClone(inputGraph);
        log = logger;
    }

    public SimpleDirectedGraph<Integer,DefaultWeightedEdge> editIntoCograph() throws ImportException, IOException, InterruptedException{

        DirectedMD modDecomp = new DirectedMD(inputGraph, log, false);
        MDTree currTree = modDecomp.computeModularDecomposition();
        log.info(()->"Original Tree: " + MDTree.beautify(currTree.toString()));
        TreeMap<Integer,LinkedList<MDTreeNode>> depthToPrimes = currTree.getPrimeModulesBottomUp();
        if(!depthToPrimes.isEmpty()){
            for(Map.Entry<Integer,LinkedList<MDTreeNode>> entry : depthToPrimes.descendingMap().entrySet()){
                log.info(()->"Editing primes on lvl " + entry.getKey());
                for(MDTreeNode primeNode : entry.getValue()){
                    log.info(()->"Editing prime: " + primeNode);
                    List<Pair<Integer,Integer>> editEdges = editSubgraph(primeNode);
                    if(editEdges != null) {
                        log.info(() -> "Found edit with cost " + editEdges.size() + " for this prime: " + editEdges);
                        editGraph.editGraph(editEdges);
                    } else {
                        log.warning(() -> "No edit found for this prime. Aborting.");
                        break;
                    }
                }
            }
        }

        return editGraph;
    }

//    void editGraph(List<Pair<Integer,Integer>> edgeList){
//        for(Pair<Integer,Integer> e : edgeList){
//            if(editGraph.containsEdge(e.getFirst(),e.getSecond())){
//                editGraph.removeEdge(e.getFirst(),e.getSecond());
//            } else {
//                editGraph.addEdge(e.getFirst(), e.getSecond());
//            }
//        }
//    }

    List<Pair<Integer,Integer>> editSubgraph(MDTreeNode primeNode)throws ImportException, IOException, InterruptedException{

        // 1. create weighted subgraph
        InducedWeightedSubgraph subGraph = new InducedWeightedSubgraph(editGraph,primeNode);

        // have a look at the forbidden subgraphs and the graph:
        log.info("Subgraph: " + subGraph.toString());
        Pair<Map<BitSet,ForbiddenSubgraphs>,Map<BitSet,ForbiddenSubgraphs>> badSubs = verticesToForbidden(subGraph);
        log.info("Length 3:\n" + badSubs.getFirst());
        log.info("Length 4:\n" + badSubs.getSecond());

        // brute-Force approach: try out all possible edge-edits, costs from low to high, until the subgraph is non-prime.
        log.info(() -> "Computing all possible edit Sets for node " + primeNode);
        Map<Integer, List<List<WeightedPair<Integer, Integer>>>> allPossibleEdits = subGraph.computeBestEdgeEdit(log,false);

        TreeSet<Integer> allSizesSorted = new TreeSet<>(allPossibleEdits.keySet());
        int fst = allSizesSorted.first();
        log.info(() ->   "Valid Edits computed from cost: " + fst + " to cost: " + allSizesSorted.last());



        if(!allSizesSorted.isEmpty()){
            // retrieve the original vertex-Nos and corresponding edges from the main graph

            ArrayList<Pair<Integer,Integer>> ret = new ArrayList<>(allPossibleEdits.get(fst).get(0).size());
            for( WeightedPair<Integer,Integer> subEdge : allPossibleEdits.get(fst).get(0)){
                int src = subGraph.getSubNoToBaseNo()[subEdge.getFirst()];
                int dst = subGraph.getSubNoToBaseNo()[subEdge.getSecond()];
                ret.add(new Pair<>(src,dst));
            }
            return ret;
        } else
            return null;
    }

    public static Pair<Map<BitSet,ForbiddenSubgraphs>,Map<BitSet,ForbiddenSubgraphs>> verticesToForbidden(SimpleDirectedGraph<Integer,DefaultWeightedEdge> g){
        
        HashMap<BitSet,ForbiddenSubgraphs> len3 = new HashMap<>();
        HashMap<BitSet,ForbiddenSubgraphs> len4 = new HashMap<>();
        int n = g.vertexSet().size();
        boolean[][] E = new boolean[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if(i != j){
                    E[i][j] = g.containsEdge(i,j);
                }
            }
        }

        // same procedure as in Cplex-Solver
        int w, x,y, z;
        boolean vars_3[], vars_4[];
        for (w = 0; w<n; w++){
            for (x = 0; x<n; x++){
                if (w!=x){
                    for (y = 0; y<n; y++){
                        if (w!=y && x!=y){
                            vars_3 = new boolean[]{
                                                 E[w][x], E[w][y],
                                        E[x][w],          E[x][y],
                                        E[y][w], E[y][x]
                            };

                            // subgraphs of length 3:
                            findForbiddenSubgraphs(ForbiddenSubgraphs.len_3,len3,vars_3,w,x,y);

                            // subgraphs of length 4:
                            for (z = 0; z<n; z++){
                                if (w != z && x!=z && y!=z){
                                    vars_4 = new boolean[]{
                                                E[w][x], E[w][y], E[w][z],
                                                E[x][w],          E[x][y], E[x][z],
                                                E[y][w], E[y][x],          E[y][z],
                                                E[z][w], E[z][x], E[z][y]
                                    };
                                    findForbiddenSubgraphs(ForbiddenSubgraphs.len_4,len4,vars_4,w,x,y,z);
                                }
                            }
                        }
                    }
                }
            }
        }

        return new Pair<>(len3,len4);
    }

    private static void findForbiddenSubgraphs(ForbiddenSubgraphs[] subs, Map<BitSet,ForbiddenSubgraphs> subsMap, boolean[] subMatrix, int ... vertices){
        int sum, index;
        // check every subgraph:
        for(int j = 0; j< subs.length; j++){

            sum = 0;
            // matrix must have same lenght as subgraphs.
            for(index=0; index < subMatrix.length; index++){
                if(subMatrix[index]){
                    sum += subs[j].get()[index];
                }
            }
            if(sum > subs[j].getThreshold()) {
                BitSet vertexSet = new BitSet();
                for(int v : vertices){
                    vertexSet.set(v);
                }
                subsMap.put(vertexSet, subs[j]);
            }
        }
    }
}
