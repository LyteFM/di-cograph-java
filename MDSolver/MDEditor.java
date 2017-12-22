package dicograph.MDSolver;

import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
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
import dicograph.utils.SortAndCompare;
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

        DirectedMD modDecomp = new DirectedMD(inputGraph, log, true);
        MDTree currTree = modDecomp.computeModularDecomposition();
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
        List<WeightedPair<Integer,Integer>> successfulEdit = null;

        // brute-Force approach: try out all possible edge-edits, costs from low to high, until the subgraph is non-prime.
        log.info(() -> "Computing all possible edit Sets for node " + primeNode);
        HashMap<Integer, List<List<WeightedPair<Integer,Integer>>>> allPossibleEdits = subGraph.allEditsByWeight();
        log.info(() ->  allPossibleEdits.size() + " subsets computed.");

        TreeSet<Integer> allSizesSorted = new TreeSet<>(allPossibleEdits.keySet());

        for(int i : allSizesSorted){
            log.info(() -> "Testing: cost " + i);
            List<List<WeightedPair<Integer,Integer>>> edgeLists = allPossibleEdits.get(i);
            for(List<WeightedPair<Integer,Integer>> currEdgeList : edgeLists) {
                // should I edit and then edit back? Or create a copy?
                subGraph.edit(currEdgeList);
                // Brute force checking for forbidden subgraphs might be more efficient...
                DirectedMD subMD = new DirectedMD(subGraph, log, false);
                // todo: exit earlier.
                MDTree subTree = subMD.computeModularDecomposition();
                if(subTree.getPrimeModulesBottomUp().isEmpty()){
                    log.info(() -> "Successful edit found!");
                    successfulEdit = currEdgeList;
                    break;
                }
                // edit back.
                subGraph.edit(currEdgeList);
            }
            if(successfulEdit != null)
                break;
        }

        if(successfulEdit != null){
            // retrieve the original vertex-Nos and corresponding edges from the main graph
            ArrayList<Pair<Integer,Integer>> ret = new ArrayList<>(successfulEdit.size());
            for( WeightedPair<Integer,Integer> subEdge : successfulEdit){
                int src = subGraph.getSubNoToBaseNo()[subEdge.getFirst()];
                int dst = subGraph.getSubNoToBaseNo()[subEdge.getSecond()];
                ret.add(new Pair<>(src,dst));
            }
            return ret;
        } else
            return null;
    }
}
