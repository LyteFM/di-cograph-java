package dicograph.Editing;

import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
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

    public MDEditor(SimpleDirectedGraph<Integer,DefaultWeightedEdge> input, Logger logger){
        inputGraph = input;
        nVertices = inputGraph.vertexSet().size();
        editGraph = GraphGenerator.deepClone(inputGraph);
        log = logger;
    }

    public SimpleDirectedGraph<Integer,DefaultWeightedEdge> editIntoCograph() throws ImportException, IOException, InterruptedException, IloException{

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

    List<Pair<Integer,Integer>> editSubgraph(MDTreeNode primeNode)throws ImportException, IOException, InterruptedException, IloException{

        // 1. create weighted subgraph
        PrimeSubgraph subGraph = new PrimeSubgraph(editGraph,primeNode);

        // have a look at the forbidden subgraphs and the graph:
        log.info("Subgraph: " + subGraph.toString());
        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> badSubs = ForbiddenSubgraph.verticesToForbidden(subGraph);
        log.info("Length 3:\n" + badSubs.getFirst());
        log.info("Length 4:\n" + badSubs.getSecond());
        ForbiddenSubgraph.computeScores(badSubs, log,subGraph, false);

        // brute-Force approach: try out all possible edge-edits, costs from low to high, until the subgraph is non-prime.
        log.info(() -> "Computing all possible edit Sets for node " + primeNode);
        Map<Integer, List<List<WeightedPair<Integer, Integer>>>> allPossibleEdits = subGraph.computeBestEdgeEdit(log,true);

        TreeSet<Integer> allSizesSorted = new TreeSet<>(allPossibleEdits.keySet());
        int fst = allSizesSorted.first();
        log.info(() ->   "Valid Edits computed from cost: " + fst + " to cost: " + allSizesSorted.last());



        if(!allSizesSorted.isEmpty()){
            // retrieve the original vertex-Nos and corresponding edges from the main graph
            log.info("Computed Edits: " + allPossibleEdits);

            // here: conversion to real edge
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


}
