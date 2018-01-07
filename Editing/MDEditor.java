package dicograph.Editing;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import dicograph.graphIO.GraphGenerator;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.WeightedPair;
import ilog.concert.IloException;

/**
 * Created by Fynn Leitow on 20.12.17.
 */
public class MDEditor {

    final SimpleDirectedGraph<Integer,DefaultWeightedEdge> inputGraph;
    SimpleDirectedGraph<Integer, DefaultWeightedEdge> editGraph;
    final  int nVertices;
    final Logger log;
    final int method;
    final MDTree inputTree;
    final List<Pair<Integer,Integer>> allEdits;
    List<Pair<Integer,Integer>> prevEdits;
    boolean first;

    public MDEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> input, MDTree tree, Logger logger, List<Pair<Integer,Integer>> oldEdits, int methodP, boolean _1st){
        inputGraph = input;
        nVertices = inputGraph.vertexSet().size();
        editGraph = GraphGenerator.deepClone(inputGraph);
        log = logger;
        method = methodP;
        inputTree = tree;
        allEdits = new LinkedList<>();
        prevEdits = oldEdits;
        first = _1st;
    }

    public MDEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> input, MDTree tree, Logger logger,int methodP){
        this(input, tree, logger, null, methodP, true);
    }



        public SimpleDirectedGraph<Integer,DefaultWeightedEdge> editIntoCograph() throws ImportException, IOException, InterruptedException, IloException{


        log.info(()->"Original Tree: " + MDTree.beautify(inputTree.toString()));
        TreeMap<Integer,LinkedList<MDTreeNode>> depthToPrimes = inputTree.getPrimeModulesBottomUp();
        if(!depthToPrimes.isEmpty()){
            for(Map.Entry<Integer,LinkedList<MDTreeNode>> entry : depthToPrimes.descendingMap().entrySet()){
                log.info(()->"Editing primes on lvl " + entry.getKey());
                for(MDTreeNode primeNode : entry.getValue()){
                    log.info(()->"Editing prime: " + primeNode);
                    List<Pair<Integer,Integer>> editEdges = editSubgraph(primeNode,  method);
                    if(editEdges != null) {
                        log.info(() -> "Found edit with " + editEdges.size() + " Edges for this prime: " + editEdges);
                        allEdits.addAll(editEdges);
                        editGraph.editGraph(editEdges);
                    } else {
                        log.warning(() -> "No edit found for this prime. Aborting.");
                        break;
                    }
                }
            }
            // if we're in a successive run and already did some editing:
            if(prevEdits != null){
                log.info("All edits of this run: " + allEdits);
                log.info("adding previous and removing double edits.");
                List<Pair<Integer,Integer>> tmp = new ArrayList<>(allEdits);
                allEdits.removeAll(prevEdits);
                prevEdits.removeAll(tmp);
                allEdits.addAll(prevEdits);

                // check for loops:
                List<Pair<Integer,Integer>> loops = new LinkedList<>();
                for(int i = 0; i< allEdits.size(); i++){
                    for (int j = i+1; j < allEdits.size(); j++) {
                        Pair<Integer,Integer> _1st = allEdits.get(i);
                        Pair<Integer,Integer> _2nd = allEdits.get(j);
                        if(_1st != _2nd){
                            if((int) _1st.getFirst() == _2nd.getSecond() && (int) _1st.getSecond() == _2nd.getFirst()){
                                loops.add(_1st);
                                loops.add(_2nd);
                            }
                        }
                    }
                }
                if(!loops.isEmpty()){
                    editGraph.editGraph(loops);
                    DirectedMD looplessMD = new DirectedMD(editGraph, log, false);
                    MDTree looplessRes = looplessMD.computeModularDecomposition();
                    if(looplessRes.getPrimeModulesBottomUp().isEmpty()){
                        allEdits.removeAll(loops);
                    } else {
                        editGraph.editGraph(loops);
                    }
                }
            }
            log.info("Total Cost: " + allEdits.size() + ", edges: " + allEdits);
        } else {
            log.warning("Input was already a di-cograph!");
        }

        return editGraph;
    }

    public List<Pair<Integer, Integer>> getAllEdits() {
        return allEdits;
    }

    private List<Pair<Integer,Integer>> editSubgraph(MDTreeNode primeNode, int method)throws ImportException, IOException, InterruptedException, IloException{

        // 1. create weighted subgraph
        PrimeSubgraph subGraph = new PrimeSubgraph(editGraph,primeNode);
        log.info("Subgraph: " + subGraph.toString());
        log.info("Base-Vertex to Sub-Vertex " + subGraph.getBaseNoTosubNo());



        // brute-Force approach: try out all possible edge-edits, costs from low to high, until the subgraph is non-prime.
        log.info(() -> "Computing all possible edit Sets for node " + primeNode);
        Map<Integer, List<List<WeightedPair<Integer, Integer>>>> allPossibleEdits = subGraph.computeBestEdgeEdit(log,method, first);



        TreeSet<Integer> allSizesSorted = new TreeSet<>(allPossibleEdits.keySet());
        int fst = allSizesSorted.first();
        log.info( "Valid Edits computed from cost: " + fst + " to cost: " + allSizesSorted.last());



        if(!allSizesSorted.isEmpty()){
            // retrieve the original vertex-Nos and corresponding edges from the main graph
            log.info("Computed Edits: " + allPossibleEdits);

            List<List<WeightedPair<Integer,Integer>>> edits = allPossibleEdits.get(fst);

            // let's try continue on error:
            if(fst == -1)
                fst = edits.size();

            List<Pair<Integer,Integer>> ret = new ArrayList<>(fst);

            // add edges between modules
            if (fst != edits.get(0).size()) {
                ret.addAll(subGraph.addModuleEdges(edits.get(0), log));
            }

            // convert subgraph-edge to real edge
            for( WeightedPair<Integer,Integer> subEdge : edits.get(0)){
                int src = subGraph.getSubNoToBaseNo()[subEdge.getFirst()];
                int dst = subGraph.getSubNoToBaseNo()[subEdge.getSecond()];
                ret.add(new Pair<>(src,dst));
            }
            return ret;
        } else
            return null;
    }


}
