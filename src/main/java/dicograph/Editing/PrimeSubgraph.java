package dicograph.Editing;

import com.google.common.collect.Sets;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultEdge;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.Edge;
import dicograph.utils.Parameters;
import dicograph.utils.TimerLog;
import dicograph.utils.WeightedPair;
import ilog.concert.IloException;

/*
 *   This source file is part of the program for editing directed graphs
 *   into cographs using modular decomposition.
 *   Copyright (C) 2018 Fynn Leitow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

public class PrimeSubgraph extends SimpleDirectedGraph<Integer,DefaultEdge> {

    private static final boolean useMD = false; // If used for large n (>200), MD will be more efficient. todo: time!

    private final Parameters p;
    private final SimpleDirectedGraph<Integer, DefaultEdge> base;
    private final int nVertices;

    private final HashMap<Integer,Integer> baseNoTosubNo;
    private final double[] subVertexToWeight;
    private final int[] subNoToBaseNo;
    private final MDTreeNode primeNode;


    public PrimeSubgraph(SimpleDirectedGraph<Integer,DefaultEdge> baseGraph, MDTreeNode node, Parameters params){
        super(new ClassBasedEdgeFactory<>(DefaultEdge.class),true);
        base = baseGraph;
        p = params;
        baseNoTosubNo = new HashMap<>();
        primeNode = node;
        subVertexToWeight = node.initWeigthsAndSubgraph(this,base);
        nVertices = subVertexToWeight.length;
        subNoToBaseNo = new int[nVertices];
        for(Map.Entry<Integer, Integer> base2Sub : baseNoTosubNo.entrySet()){
            subNoToBaseNo[base2Sub.getValue()] =  base2Sub.getKey();
        }
    }


    // an entry in the map means: if present -> remove, if not -> add.
    // Key > 0 => success! Key < 0 => not yet done, but ok after round 1; Key = 0 ->  empty => fail after step 2.
    public TreeMap<Integer, List<List<WeightedPair<Integer, Integer>>>> computeEdits(Logger log, EditType method, boolean first)
    throws InterruptedException, IOException, ImportException, IloException{
        TreeMap<Integer, List<List<WeightedPair<Integer, Integer>>>> costToEdges = new TreeMap<>(); // results

        // Maximum edge-subset size
        int limit = p.getBruteForceThreshold() * (p.getBruteForceThreshold() -1 );

        // Recognize all forbidden subgraphs and compute subgraph-scores for edits.
        HashMap<Edge,Integer> edgeToCount = new HashMap<>();
        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> badSubs = ForbiddenSubgraph.verticesToForbidden(this, edgeToCount,false);
        log.fine(()->"Length 3:\n" + badSubs.getFirst());
        log.fine(()->"Length 4:\n" + badSubs.getSecond());
        // sort descending
        ArrayList<Map.Entry<Edge,Integer>> edgesToScore = new ArrayList<>(edgeToCount.entrySet());
        edgesToScore.sort( Comparator.comparingInt(e ->  -e.getValue()));
        log.info(()->"Edges by subgraph-score: " + edgesToScore.size() + "\n" + edgesToScore);



        int cnt = 0;
        Map<Integer, WeightedPair<Integer,Integer>> intToEdge = new HashMap<>();

        // Computes all weights for an edit between subVertices i,j.
        double[][] weightMatrix = new double[nVertices][nVertices];
        for (int i : vertexSet()) {
            for (int j : vertexSet()) {
                if(j!=i){
                    double weight = subVertexToWeight[i] * subVertexToWeight[j];
                    weightMatrix[i][j] = weight;
                    // this only for Brute Force without globals.
                    if( !first  && method == EditType.BruteForce && !p.isUseGlobal()) {
                        WeightedPair<Integer, Integer> e = new WeightedPair<>(i, j, weight);
                        intToEdge.put(cnt, e);
                        cnt++;
                    }
                }
            }
        }

        // Lazy method. Not for normal ILP! -> but yes on 1st run for greedy-ILP
        if(first && method.doLazyOnFirst() || !first && method.doLazyOnSecond(p.isUseGlobal())){

            SimpleDirectedGraph<Integer,DefaultEdge> graphOfEditEdges = new SimpleDirectedGraph<>(DefaultEdge.class);
            ConnectivityInspector<Integer,DefaultEdge> pathFinder = new ConnectivityInspector<>(graphOfEditEdges);
            List<WeightedPair<Integer, Integer>> currEdgeList = new LinkedList<>(); // just one edge
            List<WeightedPair<Integer,Integer >> allEdgesList = new LinkedList<>();
            List<WeightedPair<Integer, Integer>> edgesToRemove = new LinkedList<>(); // remove edits below threshold, if no successful edit found.
            WeightedPair<Integer,Integer > lastEdge = null;
            int i, u,v, u_v_score, global_u_v,global_v_u, both_global;
            int v_u_score = 0, both_local = 0;
            int prevScore = badSubs.getFirst().size() + badSubs.getSecond().size();
            int subCount = prevScore;
            log.info(()->"Total no of forbidden subs: " + subCount);
            int cost = 0;
            cnt = -1; // to init intToEdge for other edit-sets (by global edit-score)
            double weight;
            for (i = 0; i < edgesToScore.size(); i++) {
                Map.Entry<Edge,Integer> edge = edgesToScore.get(i);

                if(method.stopAtHard(first) && edge.getValue() < p.getHardThreshold()){
                    log.warning("Reached hard threshold " + p.getHardThreshold() + " for edge " + edge);
                    break;
                }

                u = edge.getKey().getFirst();
                v = edge.getKey().getSecond();
                // compute the graph of edited edits to exclude existing paths (any direction)
                boolean addedU = graphOfEditEdges.addVertex(u);
                boolean addedV = graphOfEditEdges.addVertex(v);
                if( !addedU && !addedV ){
                    log.info("Both vertices for path " + u + ", " + v + " already exist!");
                    if(p.isSkipExistingVertices()){
                        continue;
                    } else if(p.isSkipPaths() && pathFinder.pathExists(u,v)) {
                        log.warning("Skipped: path already exists from " + u + " to " + v);
                        continue;
                    }
                }
                weight = subVertexToWeight[u] * subVertexToWeight[v];
                log.fine("Subgraph-Score: " + edge.getValue() + ", weight: " + weight);
                // determine the edit direction (Problem: what if both?)

                // global score might be useful, too.
                currEdgeList.clear();
                global_u_v = computeEditScoreForEgde(u,v,weight,currEdgeList);
                log.fine("("+ u + ","  +v + ") global edit-score: " + global_u_v);
                currEdgeList.clear();
                global_v_u = computeEditScoreForEgde(v,u,weight,currEdgeList);
                log.fine("("+ v + ","  +u + ") global edit-score: "+ global_v_u);
                //currEdgeList.add(new WeightedPair<>(u,v,weight)); v,u already there
                both_global = (int) Math.round(computeEditScoreForEgde(u,v,weight,currEdgeList) + weight * p.getWeightMultiplier());
                log.fine("both global: "+ both_global);
                currEdgeList.clear();

                boolean globallyFeasable = global_u_v < subCount ||
                        global_v_u < subCount || both_global < subCount;
                if(p.isUseGlobal() && !first){

                    if(globallyFeasable) {
                        log.info("Adding edge(s) for (" + u + "," + v + ")");
                        if (global_u_v < global_v_u && global_u_v <= both_global) {
                            intToEdge.put(++cnt, new WeightedPair<>(u, v, weight));

                        } else if (global_v_u < global_u_v && global_v_u <= both_global) {
                            intToEdge.put(++cnt, new WeightedPair<>(v, u, weight));

                        } else {
                            intToEdge.put(++cnt, new WeightedPair<>(u, v, weight));
                            intToEdge.put(++cnt, new WeightedPair<>(v, u, weight));
                        }
                        if (intToEdge.size() >= limit) {
                            log.info("Computed globally feasable edits");
                            break;
                        }
                    } else {
                        log.info("Discarded: edge (" + u + "," + v + ") which doesn't improve the global score");
                    }

                } else { // usual run
                    currEdgeList.addAll(allEdgesList);

                    u_v_score = computeEditScoreForEgde(u, v, weight, currEdgeList);
                    log.fine("(" + u + "," + v + ") local edit-score: " + u_v_score);

                    if (u_v_score != 0) {
                        currEdgeList.clear();
                        currEdgeList.addAll(allEdgesList);
                        v_u_score = computeEditScoreForEgde(v, u, weight, currEdgeList);
                        log.fine("(" + v + "," + u + ") local edit-score: " + v_u_score);

                        both_local = computeEditScoreForEgde(u, v, weight, currEdgeList);
                        if (v_u_score == 0 || both_local == 0) {
                            lastEdge = new WeightedPair<>(v, u, weight);
                        } else {
                            both_local = (int) Math.round(both_local + weight * p.getWeightMultiplier()); // if not a solution, consider weight.
                        }
                        log.fine("Both local: " + both_local);

                    } else {
                        lastEdge = new WeightedPair<>(u, v, weight);
                    }

                    if (lastEdge != null) {
                        // we're done.
                        log.info("Subgraph edited into Cograph with edge " + lastEdge);
                        allEdgesList.add(lastEdge);
                        if (v_u_score != 0 && both_local == 0) { // need both
                            allEdgesList.add(new WeightedPair<>(u, v, weight));
                        }
                        List<List<WeightedPair<Integer, Integer>>> res = new LinkedList<>();
                        res.add(allEdgesList);
                        cost += Math.round(weight);
                        costToEdges.put(cost, res);
                        return costToEdges;
                    }

                    if ((u_v_score < prevScore || v_u_score < prevScore || both_local < prevScore) && (!p.isRequireGlobal() || globallyFeasable)) {

                        if (!globallyFeasable) {
                            log.warning("Adding edge-edit with unexpected global score!");
                        }
                        cost += Math.round(weight);
                        if (u_v_score < v_u_score && u_v_score <= both_local) {
                            prevScore = addEditEdge(u, v, weight, u_v_score, log, graphOfEditEdges, allEdgesList);
                        } else if (v_u_score < u_v_score && v_u_score <= both_local) {
                            prevScore = addEditEdge(v, u, weight, v_u_score, log, graphOfEditEdges, allEdgesList);
                        } else if (both_local < u_v_score && both_local < v_u_score) {
                            log.info("Adding edge-edits for both directions!");
                            addEditEdge(u, v, weight, u_v_score, log, graphOfEditEdges, allEdgesList);
                            prevScore = addEditEdge(v, u, weight, v_u_score, log, graphOfEditEdges, allEdgesList);
                        } else {
                            log.warning("Same score for u " + u + ", v " + v);
                            prevScore = addEditEdge(u, v, weight, u_v_score, log, graphOfEditEdges, allEdgesList);
                        }

                        // exit point for brute force/ greedy ILP
                        if (first && !p.isStopOnlyAtHardThreshold() && method.checkPrimesSize()) {
                            edit(allEdgesList);
                            DirectedMD checkSizeMD = new DirectedMD(this, log, false);
                            MDTree checkSizeTree = checkSizeMD.computeModularDecomposition();
                            if (checkSizeTree.getMaxPrimeSize() <= p.getBruteForceThreshold()) {
                                log.info("Size of prime modules now below " + p.getBruteForceThreshold() + ". Ready for second run.");
                                List<List<WeightedPair<Integer, Integer>>> res = new LinkedList<>();
                                res.add(allEdgesList);
                                costToEdges.put(-allEdgesList.size(), res); // not yet a solution, but ok.
                                return costToEdges;
                            } else {
                                edit(allEdgesList); // edit back
                            }
                        }

                        if (edge.getValue() <= p.getSoftThreshold())
                            edgesToRemove.add(allEdgesList.get(allEdgesList.size() - 1));

                    } else {
                        log.info("Discarded: edge (" + u + "," + v + ") which doesn't improve the module");
                        if (addedU)
                            graphOfEditEdges.removeVertex(u);
                        if (addedV)
                            graphOfEditEdges.removeVertex(v);
                    }
                    currEdgeList.clear();
                }
            }

            // either continue with global brute force or no solution found
            if( !(!first && p.isUseGlobal()) ){

                log.info(() -> "Processed all edits or reached hard threshold - no cograph-edit found!!!");
                log.info(() -> "To remove (below soft threshold): " + edgesToRemove);

                // ok if we're in step 1:
                if (first) {
                    List<List<WeightedPair<Integer, Integer>>> res = new LinkedList<>();
                    // remove those below soft threshold:
                    allEdgesList.removeAll(edgesToRemove);
                    res.add(allEdgesList);
                    costToEdges.put(-allEdgesList.size(), res);
                    return costToEdges;
                } else {
                    log.warning(() -> "All Chosen Subgraph edges so far: " + allEdgesList);
                    log.warning(() -> "As real edges: " + getRealEdges(allEdgesList));
                    return costToEdges; // empty.
                }
            }
        }
        // ILP on the quotient-graph (with costs)
        if(first && method == EditType.ILP || !first && method == EditType.GreedyILP){
            CplexDiCographEditingSolver primeSolver = new CplexDiCographEditingSolver(
                    this, p, weightMatrix, log);
            primeSolver.solve();
            for (int i = 0; i < primeSolver.getEditingDistances().size(); i++) {
                int val = (int) Math.round(primeSolver.getEditingDistances().get(i));
                costToEdges.putIfAbsent(val,new LinkedList<>());
                costToEdges.get(val).add(primeSolver.getSolutionEdgeEdits().get(i));
            }

            log.info("MD Tree for CPlex subgraph solution:");
            edit(primeSolver.getSolutionEdgeEdits().get(0));
            DirectedMD subMD = new DirectedMD(this, log, false);
            MDTree subTree = subMD.computeModularDecomposition();
            log.info(MDTree.beautify(subTree.toString()));

            return costToEdges;
        }
        // Cost-aware Brute Force
        else if(method == EditType.BruteForce){
            TimerLog timer = new TimerLog(log, log.getParent().getLevel());
            boolean timeOut = false;
            int maxCost;
            if(p.getBruteForceLimit() > 0){
                maxCost = p.getBruteForceLimit();
            } else {
                maxCost = primeNode.getNumChildren();
            }

            int smallestCost = maxCost;

            for (int i = 1; i <= maxCost; i++) {
                // e.g. i=5, smallest cost was 2, wanted all solutions up to cost 4
                if (smallestCost + p.getBruteForceGap() < i) {
                    return costToEdges;
                }
                log.info("Computing all permutations of size " + i + " for n = " + cnt);
                Set<Set<Integer>> combinations = Sets.combinations(intToEdge.keySet(), i);
                log.info(combinations.size() + " permutations!");
                boolean costStillValid = false;

                int count = 0;
                boolean success = false;


                for (Set<Integer> edgeSubset : combinations) {
                    int cost = 0;
                    List<WeightedPair<Integer, Integer>> currEdgeList = new ArrayList<>(edgeSubset.size());
                    for (int edgeNo : edgeSubset) {
                        WeightedPair<Integer, Integer> e = intToEdge.get(edgeNo);
                        currEdgeList.add(e);
                        cost += Math.round(e.getWeight()); // better use rounding, just in case...
                    }
                    if (cost <= maxCost) {

                        costStillValid = true;
                        edit(currEdgeList);
                        // subgraph checking for forbidden subgraphs is way more efficient for not-too large n.
                        if(useMD) {
                            DirectedMD subMD = new DirectedMD(this, log, false);
                            MDTree subTree = subMD.computeModularDecomposition();
                            if(subTree.getPrimeModulesBottomUp().isEmpty()){
                                success = true;
                                log.info("Tree: " + MDTree.beautify(subTree.toString()));
                            }
                        } else {
                        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> check = ForbiddenSubgraph.verticesToForbidden(this, new HashMap<>(),true);
                        success = check.getFirst().isEmpty() && check.getSecond().isEmpty();
                        }
                        if(success){
                            log.info(() -> method + ": Successful subgraph edit found: " + currEdgeList);
                            costToEdges.putIfAbsent(cost, new LinkedList<>());
                            costToEdges.get(cost).add(currEdgeList);
                            // need this to verify if an early, but expensive edit is best.
                            if (cost < smallestCost) {
                                if (p.getSolutionGap() < 0) {
                                    log.info("returning first found solution.");
                                    edit(currEdgeList); // back
                                    return costToEdges; // take the firstRun one below maxCost
                                }
                                smallestCost = cost;
                            }
                        }

                        // edit back.
                        edit(currEdgeList);

                    }


                    count++;
                    timeOut = count % 5000 == 0 && timer.elapsedSeconds() > p.getTimeOut();
                    if(timeOut)
                        break;
                }
                if(i > 3)
                    timer.logTime("i = " + i);
                // all of size i are done now.


                if (!costStillValid) {
                    log.info("Exiting: i = " + i + ", every cost was > " + maxCost);
                    return costToEdges;
                } else if(timeOut){
                    log.warning("Aborting due to timeout.");
                    return costToEdges;
                }
            }
        }

        // Subgraph Edges!
        return costToEdges;
    }

    private static int addEditEdge(int u, int v, double weight, int u_v_score, Logger log,
                           SimpleDirectedGraph<Integer,DefaultEdge> graphOfEditEdges, List<WeightedPair<Integer,Integer>> allEdgesList){
        allEdgesList.add(new WeightedPair<>(u, v, weight));
        graphOfEditEdges.addEdge(u,v);
        log.info("Chose edge-edit (" + u + "," + v + ") with score: " + u_v_score);
        return u_v_score;
    }

    // returns the additional edits (With real vertexNos)
    List<Edge> addModuleEdges(List<WeightedPair<Integer,Integer>> edgeList, Logger log){
        List<Edge> addEdges = new LinkedList<>();
        for (WeightedPair<Integer, Integer> singleEdge : edgeList) {
            if (Math.round(singleEdge.getWeight()) > 1) {
                log.info(() -> "Adding edits for " + singleEdge);
                int realFirstVertex = subNoToBaseNo[singleEdge.getFirst()];
                int realSecondVertex = subNoToBaseNo[singleEdge.getSecond()];
                List<Edge> added = primeNode.addModuleEdges(realFirstVertex,realSecondVertex);
                log.info(() -> "Added: " + added);
                addEdges.addAll(added);
            }
        }
        return addEdges;
    }

    // high score means bad edit.
    private int computeEditScoreForEgde(int u, int v, double weight, List<WeightedPair<Integer,Integer>> currEdgeList){
        HashMap<Edge,Integer> edgeToCount = new HashMap<>();
        currEdgeList.add(new WeightedPair<>(u,v,weight));
        edit(currEdgeList);
        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> badSubs = ForbiddenSubgraph.verticesToForbidden(this, edgeToCount,false);
        edit(currEdgeList);
        int badScore = badSubs.getSecond().size() + badSubs.getFirst().size();
        // take any solution here, nevermind.
        if(badScore != 0 && weight > 1.5){
            // I'm officially an idiot, score must INCREASE of course...
            badScore =  (int) Math.round( badScore + (p.getWeightMultiplier() * weight));
        }
        return badScore;
    }

    private void edit(List<WeightedPair<Integer, Integer>> edgeList){

        for(WeightedPair<Integer,Integer> e : edgeList){
            if(containsEdge(e.getFirst(),e.getSecond())){
                removeEdge(e.getFirst(),e.getSecond());
            } else {
                addEdge(e.getFirst(), e.getSecond());
            }
        }

    }

    // convert subgraph-edge to real edge
    List<Edge> getRealEdges(List<WeightedPair<Integer,Integer>> oneEdit){
        List<Edge> ret = new ArrayList<>(oneEdit.size());
        for (WeightedPair<Integer, Integer> subEdge : oneEdit) {
            int src = subNoToBaseNo[subEdge.getFirst()];
            int dst = subNoToBaseNo[subEdge.getSecond()];
            ret.add(new Edge(src, dst));
        }
        return ret;
    }

    // this doesn't give the desired results.
    @Deprecated
    private int computePrimeScoreForEdge(int u,int v, double weight, List<WeightedPair<Integer,Integer>> currEdgeList, Logger log)
            throws InterruptedException,IOException,ImportException {
        currEdgeList.add(new WeightedPair<>(u,v,weight));
        edit(currEdgeList);
        DirectedMD subMD = new DirectedMD(this, log, false);
        MDTree subTree = subMD.computeModularDecomposition();
        edit(currEdgeList);
        return subTree.getNumPrimeChildren();
    }


    public HashMap<Integer, Integer> getBaseNoTosubNo() {
        return baseNoTosubNo;
    }

    public SimpleDirectedGraph<Integer, DefaultEdge> getBase() {
        return base;
    }

    public int getnVertices() {
        return nVertices;
    }

    public double[] getSubVertexToWeight() {
        return subVertexToWeight;
    }

    public int[] getSubNoToBaseNo() {
        return subNoToBaseNo;
    }
}
