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
import dicograph.utils.WeightedEdge;
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
    private final Logger log;
    private final EditType method;

    private int lazyReach;


    public PrimeSubgraph(SimpleDirectedGraph<Integer,DefaultEdge> baseGraph, MDTreeNode node, Parameters params, Logger logger, EditType type){
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
        log = logger;
        method = type;
        if(p.getLazyreach() > 0 ){
            lazyReach = p.getLazyreach();
        } else {
            lazyReach = (int) Math.sqrt(nVertices) + 1;
        }
    }


    // an entry in the map means: if present -> remove, if not -> add.
    // Key > 0 => success! Key < 0 => not yet done, but ok after round 1; Key = 0 ->  empty => fail after step 2.
    public TreeMap<Integer, List<List<WeightedEdge>>> computeEdits(boolean first)
    throws InterruptedException, IOException, ImportException, IloException{
        TreeMap<Integer, List<List<WeightedEdge>>> costToEdges = new TreeMap<>(); // results

        // Maximum edge-subset size
        int limit = p.getBruteForceThreshold() * (p.getBruteForceThreshold() -1 );

        // Recognize all forbidden subgraphs and compute subgraph-scores for edits.
        HashMap<Edge,Integer> edgeToCount = new HashMap<>();
        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> badSubs = ForbiddenSubgraph.verticesToForbidden(this, edgeToCount,false);
        log.fine(()->"Length 3:\n" + badSubs.getFirst());
        log.fine(()->"Length 4:\n" + badSubs.getSecond());
        // sort descending
        List<Map.Entry<Edge,Integer>> edgesToScore = new ArrayList<>(edgeToCount.entrySet());
        edgesToScore.sort( Comparator.comparingInt(e ->  -e.getValue()));
        log.info("Edges by subgraph-score: " + edgesToScore.size() + "\n" + edgesToScore);



        int cnt = 0;
        Map<Integer, WeightedEdge> intToEdge = new HashMap<>();

        // Computes all weights for an edit between subVertices i,j.
        double[][] weightMatrix = new double[nVertices][nVertices];
        for (int i : vertexSet()) {
            for (int j : vertexSet()) {
                if(j!=i){
                    double weight = subVertexToWeight[i] * subVertexToWeight[j];
                    weightMatrix[i][j] = weight;
                    // this only for Brute Force without globals.
                    if( !first  && method == EditType.BruteForce && !p.isUseGlobal()) {
                        WeightedEdge e = new WeightedEdge(i, j, weight);
                        intToEdge.put(cnt, e);
                        cnt++;
                    }
                }
            }
        }

        // todo: reqgl for lazy!!!
        // Lazy method. Not for normal ILP! -> but yes on 1st run for greedy-ILP
        if(first && method.doLazyOnFirst() || !first && method.doLazyOnSecond(p.isUseGlobal())){

            SimpleDirectedGraph<Integer,DefaultEdge> graphOfEditEdges = new SimpleDirectedGraph<>(DefaultEdge.class);
            ConnectivityInspector<Integer,DefaultEdge> pathFinder = new ConnectivityInspector<>(graphOfEditEdges);
            List<WeightedEdge> currEdgeList = new LinkedList<>(); // just one edge
            List<WeightedEdge> allEdgesList = new LinkedList<>();
            List<WeightedEdge> edgesToRemove = new LinkedList<>(); // remove edits below threshold, if no successful edit found.
            int index, u,v, global_u_v,global_v_u, both_global;
            int currScore = badSubs.getFirst().size() + badSubs.getSecond().size();
            int subCount = currScore;
            log.info(()->"Lazy reach: " + lazyReach);
            log.info(()->"Total no of forbidden subs: " + subCount);
            cnt = -1; // to init intToEdge for other edit-sets (by global edit-score)
            double weight;
            for (index = 0; index < edgesToScore.size(); ) {

                Map.Entry<Edge,Integer> edge = edgesToScore.get(index);

                if(method.stopAtHard(first) && edge.getValue() < p.getHardThreshold()){
                    log.warning("Reached hard threshold " + p.getHardThreshold() + " for edge " + edge);
                    break;
                }

                u = edge.getKey().getFirst();
                v = edge.getKey().getSecond();

                weight = subVertexToWeight[u] * subVertexToWeight[v];
                log.fine("Subgraph-Score: " + edge.getValue() + ", weight: " + weight);
                // determine the edit direction (Problem: what if both?)
                currEdgeList.clear();

                // if global: do global thing
                // else: use the "next k" - strategy
                if(p.isUseGlobal() && !first){

                    currEdgeList.clear();
                    global_u_v = computeEditScoreForEgde(u,v,weight,currEdgeList).getFirst();
                    log.fine("("+ u + ","  +v + ") global edit-score: " + global_u_v);
                    currEdgeList.clear();
                    global_v_u = computeEditScoreForEgde(v,u,weight,currEdgeList).getFirst();
                    log.fine("("+ v + ","  +u + ") global edit-score: "+ global_v_u);
                    //currEdgeList.add(new WeightedPair<>(u,v,weight)); v,u already there
                    both_global = (int) Math.round(computeEditScoreForEgde(u,v,weight,currEdgeList).getFirst() + weight * p.getWeightMultiplier());
                    log.fine("both global: "+ both_global);
                    currEdgeList.clear();

                    boolean globallyFeasable = global_u_v < subCount ||
                            global_v_u < subCount || both_global < subCount;

                    // todo: every edge here only once.
                    if(globallyFeasable) {
                        log.info("Adding edge(s) for (" + u + "," + v + ")");
                        if (global_u_v < global_v_u && global_u_v <= both_global) {
                            intToEdge.put(++cnt, new WeightedEdge(u, v, weight));

                        } else if (global_v_u < global_u_v && global_v_u <= both_global) {
                            intToEdge.put(++cnt, new WeightedEdge(v, u, weight));

                        } else {
                            intToEdge.put(++cnt, new WeightedEdge(u, v, weight));
                            intToEdge.put(++cnt, new WeightedEdge(v, u, weight));
                        }
                        if (intToEdge.size() >= limit) {
                            log.info("Computed globally feasable edits");
                            break;
                        }
                    } else {
                        log.info("Discarded: edge (" + u + "," + v + ") which doesn't improve the global score");
                    }
                    index++;

                } else {
                    // normal lazy run with local edit scores.
                    TreeMap<Integer,List <Pair<Map.Entry<Edge,Integer>, Pair<WeightedEdge, WeightedEdge>> >> editsByLocalScore = new TreeMap<>();
                    Pair<Integer,Boolean> v_u_res, u_v_res, both_local;
                    currEdgeList.clear(); // just one or two edge(s)
                    int firstIndex = -1;
                    boolean solutionFound = false;


                    for (; index < edgesToScore.size(); index++) {

                        edge = edgesToScore.get(index);
                        u = edge.getKey().getFirst();
                        v = edge.getKey().getSecond();
                        weight = subVertexToWeight[u] * subVertexToWeight[v];

                        // compute the graph of edited edits to exclude existing paths (any direction)
                        if( graphOfEditEdges.containsVertex(u) && graphOfEditEdges.containsVertex(v) ){
                            log.info("Both vertices for path " + u + ", " + v + " already exist!");
                            if(p.isSkipExistingVertices()){
                                continue;
                            } else if(p.isSkipPaths() && pathFinder.pathExists(u,v)) {
                                log.warning("Skipped: path already exists from " + u + " to " + v);
                                continue;
                            }
                        }

                        // compute local edit scores
                        currEdgeList.clear();
                        currEdgeList.addAll(allEdgesList);
                        u_v_res = computeEditScoreForEgde(u, v, weight, currEdgeList);
                        log.fine("(" + u + "," + v + ") local edit-score: " + u_v_res.getFirst());

                        currEdgeList.clear();
                        currEdgeList.addAll(allEdgesList);
                        v_u_res = computeEditScoreForEgde(v, u, weight, currEdgeList);
                        log.fine("(" + v + "," + u + ") local edit-score: " + v_u_res.getFirst());

                        both_local = computeEditScoreForEgde(u, v, weight, currEdgeList);
                        int both_score = (int) Math.round(both_local.getFirst() + weight * p.getWeightMultiplier());

                        log.fine("Both local: " + both_local);
                        if(!solutionFound) {
                            solutionFound = u_v_res.getSecond() || v_u_res.getSecond() || both_local.getSecond();
                        }

                        if ((u_v_res.getFirst() < currScore || v_u_res.getFirst() < currScore || both_score < currScore || solutionFound)) {
                            if(firstIndex < 0) {
                                firstIndex = index;
                            }
                            testEditEdge(u,v,u_v_res.getFirst(),v_u_res.getFirst(),both_score, weight, edge,editsByLocalScore); // added here with score.
                        } else {
                            log.fine("Discarded: edge (" + u + "," + v + ") which doesn't improve the module");
                        }

                        // There could be several best solutions... Or  solutions better than editing both...
                        if (solutionFound) {
                            log.info("Subgraph edited into Cograph.");
                            if( (u_v_res.getFirst() == 0 || v_u_res.getFirst() == 0) && weight < 1.5) {
                                break;
                            }
                        }

                        // todo: what if I reached the end??? -> set hth to 0 if still want to do this for 1s...
                        // how do I make sure it does the same as before when hard is reached?
                        // -> break after first found!
                        if(editsByLocalScore.size() >= lazyReach || edge.getValue() <= p.getHardThreshold() && editsByLocalScore.size() > 0 )
                            break;

                    }

                    // Analysis - choose the best!
                    if(editsByLocalScore.size() > 0){

                        LinkedList<WeightedEdge> addEdges = new LinkedList<>();
                        double bestWeight = 100000;
                        Map.Entry<Edge,Integer> bestEntry = null;
                        int bestEditScore = editsByLocalScore.firstKey();
                        int correspondingSubgraphScore = Integer.MAX_VALUE;

                        for( Pair<Map.Entry<Edge,Integer>, Pair<WeightedEdge, WeightedEdge>> best : editsByLocalScore.firstEntry().getValue()){
                            // take best cost, if several possible.
                            WeightedEdge one = best.getSecond().getFirst();
                            WeightedEdge two = best.getSecond().getSecond();
                            double w1 = 0;
                            double w2 = 0;
                            if(one != null){
                                w1 = one.getWeight();
                            }
                            if(two != null){
                                w2 = two.getWeight();
                            }
                            if(w1 + w2 < bestWeight){
                                bestEntry = best.getFirst();
                                correspondingSubgraphScore = bestEntry.getValue();
                                bestWeight = w1 + w2;
                                addEdges.clear();
                                if(one != null)
                                    addEdges.add(one);
                                if(two != null)
                                    addEdges.add(two);
                            }
                        }

                        // if improvement or solution:
                        if(bestEditScore < currScore || solutionFound) {
                            log.info("Added best edit(s): " + addEdges + " edit-score: " + bestEditScore + ", weight: " + addEdges.getFirst().getWeight());
                            allEdgesList.addAll(addEdges);
                            currScore = bestEditScore;
                            // if unsuccessful:
                            if(correspondingSubgraphScore <= p.getSoftThreshold()){
                                edgesToRemove.addAll(addEdges);
                            }

                            edit(allEdgesList);
                            DirectedMD checkSizeMD = new DirectedMD(this, log, false);
                            MDTree checkSizeTree = checkSizeMD.computeModularDecomposition();
                            int primeSize = checkSizeTree.getMaxPrimeSize();
                            log.info("Size of prime modules:" + primeSize);
                            edit(allEdgesList); // edit back

                            if(primeSize == 0){
                                int editCost = 0;
                                for(WeightedEdge w : allEdgesList){
                                    editCost += Math.round(w.getWeight());
                                }
                                List<List<WeightedEdge>> res = new LinkedList<>();
                                res.add(allEdgesList);
                                costToEdges.put(editCost, res);
                                return costToEdges;

                            } else if (primeSize <= p.getBruteForceThreshold() && first && !p.isStopOnlyAtHardThreshold() && method.checkPrimesSize()) {
                                // exit point for brute force/ greedy ILP
                                log.info("Size of prime modules now below " + p.getBruteForceThreshold() + ". Ready for second run.");
                                List<List<WeightedEdge>> res = new LinkedList<>();
                                res.add(allEdgesList);
                                int editCost = 0;
                                for (WeightedEdge w : allEdgesList) {
                                    editCost += Math.round(w.getWeight());
                                }
                                costToEdges.put(-editCost, res); // not yet a solution, but ok.
                                return costToEdges;
                            }

                            // update graph
                            for(WeightedEdge added : addEdges){
                                graphOfEditEdges.addVertex(added.getFirst());
                                graphOfEditEdges.addVertex(added.getSecond());
                                graphOfEditEdges.addEdge(added.getFirst(), added.getSecond());
                            }

                            // remove chosen edit.
                            editsByLocalScore.remove(editsByLocalScore.firstKey());
                            edgesToScore.remove(bestEntry);
                            index = firstIndex; // next round from here.

                        } else {
                            // no improvement? failure!
                            log.warning("lazy: no better value (shouldn't happen!)");
                            break;
                        }
                    } else {
                        // method failed
                        log.warning("Lazy: empty edit-map.");
                        break;
                    }
                }
            }

            // either continue with global brute force or no solution found
            if( !(!first && p.isUseGlobal()) ){

                log.info(() -> "Processed all edits or reached hard threshold - no cograph-edit found!!!"); // todo: hth, sth... wie verwenden?
                log.info(() -> "To remove (below soft threshold): " + edgesToRemove);

                // ok if we're in step 1:
                if (first) {
                    List<List<WeightedEdge>> res = new LinkedList<>();
                    // remove those below soft threshold:
                    allEdgesList.removeAll(edgesToRemove);
                    res.add(allEdgesList);
                    int editCost = 0;
                    for (WeightedEdge w : allEdgesList) {
                        editCost += Math.round(w.getWeight());
                    }
                    costToEdges.put(-editCost, res);
                    return costToEdges;
                } else {
                    log.warning(() -> "All Chosen Subgraph edges so far: " + allEdgesList);
                    log.warning(() -> "As real edges: " + getRealEdges(allEdgesList));
                    return costToEdges; // empty and failed.
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
            int solCount = 0;

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
                    List<WeightedEdge> currEdgeList = new ArrayList<>(edgeSubset.size());
                    for (int edgeNo : edgeSubset) {
                        WeightedEdge e = intToEdge.get(edgeNo);
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
                            if(++solCount >= p.getMaxBFResults()){
                                log.warning("Stopping Brute force after " + p.getMaxBFResults() + " found edits. Use -bflimit to include more.");
                                return costToEdges;
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

    // costToEdges...
    private Pair<Integer,Integer> computeBestLocalEdit( List<Map.Entry<Edge,Integer>> edgesToScore, int index, int prevScore,
         TreeMap<Integer,List <Pair<Map.Entry<Edge,Integer>, Pair<WeightedEdge, WeightedEdge>> >> editsByLocalScore,
         SimpleDirectedGraph<Integer,DefaultEdge> graphOfEditEdges,
         ConnectivityInspector<Integer,DefaultEdge> pathFinder,
         List<WeightedEdge> allEdgesList){

        Pair<Integer,Boolean> v_u_res, u_v_res, both_local;
        double weight;
        List<WeightedEdge> currEdgeList = new LinkedList<>(); // just one or two edge(s)
        int bestIndex = -1;
        int bestScore = Integer.MAX_VALUE;
        int firstIndex = -1;


        for (; index < edgesToScore.size(); index++) {

            Map.Entry<Edge,Integer> edge = edgesToScore.get(index);

            int u = edge.getKey().getFirst();
            int v = edge.getKey().getSecond();
            weight = subVertexToWeight[u] * subVertexToWeight[v];
            int score = Integer.MAX_VALUE;

            // compute the graph of edited edits to exclude existing paths (any direction)
            if( graphOfEditEdges.containsVertex(u) && graphOfEditEdges.containsVertex(v) ){
                log.info("Both vertices for path " + u + ", " + v + " already exist!");
                if(p.isSkipExistingVertices()){
                    continue;
                } else if(p.isSkipPaths() && pathFinder.pathExists(u,v)) {
                    log.warning("Skipped: path already exists from " + u + " to " + v);
                    continue;
                }
            }

            // todo: weight vs cograph edit...
            // compute local edit scores
            currEdgeList.clear();
            currEdgeList.addAll(allEdgesList);
            u_v_res = computeEditScoreForEgde(u, v, weight, currEdgeList);
            log.fine("(" + u + "," + v + ") local edit-score: " + u_v_res.getFirst());

            currEdgeList.clear();
            currEdgeList.addAll(allEdgesList);
            v_u_res = computeEditScoreForEgde(v, u, weight, currEdgeList);
            log.fine("(" + v + "," + u + ") local edit-score: " + v_u_res.getFirst());

            both_local = computeEditScoreForEgde(u, v, weight, currEdgeList);
            int both_score = (int) Math.round(both_local.getFirst() + weight * p.getWeightMultiplier());

            log.fine("Both local: " + both_local);
            boolean solutionFound = u_v_res.getSecond() || v_u_res.getSecond() || both_local.getSecond();

            if ((u_v_res.getFirst() < prevScore || v_u_res.getFirst() < prevScore || both_score < prevScore || solutionFound)) {
                if(firstIndex < 0) {
                    firstIndex = index;
                }
                score = testEditEdge(u,v,u_v_res.getFirst(),v_u_res.getFirst(),both_score, weight, edge,editsByLocalScore); // added here with score.

                if(score < bestScore) {
                    bestIndex = index;
                    bestScore = score;
                }

            } else {
                log.info("Discarded: edge (" + u + "," + v + ") which doesn't improve the module");
            }

            // There could be several best solutions... Or at least solutions better than editing both...
            if (score == 0) {
                log.info("Subgraph edited into Cograph.");
                if( (u_v_res.getFirst() == 0 || v_u_res.getFirst() == 0) && weight < 1.5) {
                    break; // else there might be sth better
                }
            }

            // todo: what if I reached the end??? -> set hth to 0 if still want to do this for 1s...
            // how do I make sure it does the same as before when hard is reached?
            // -> break after first found!
            if(editsByLocalScore.size() >= lazyReach || edge.getValue() <= p.getHardThreshold() && editsByLocalScore.size() > 0 )
                break;

        }
        return new Pair<>(firstIndex,bestIndex);
    }


    // adds the chosen edge to the map during a lazy run. takes the best of the three.
    private int testEditEdge(int u, int v, int u_v_score, int v_u_score, int both_local, double weight,
                             Map.Entry<Edge,Integer> entry,
                             TreeMap<Integer,List <Pair<Map.Entry<Edge,Integer>, Pair<WeightedEdge, WeightedEdge>> >> editsByLocalScore){

        if (u_v_score < v_u_score && u_v_score <= both_local) {
            editsByLocalScore.putIfAbsent(u_v_score,new LinkedList<>());
            editsByLocalScore.get(u_v_score).add( new Pair<>(entry, new Pair<>(new WeightedEdge(u,v,weight),null)));
            log.info("Possible edge-edit (" + u + "," + v + ") with score: " + u_v_score);
            return u_v_score;

        } else if (v_u_score < u_v_score && v_u_score <= both_local) {
            editsByLocalScore.putIfAbsent(v_u_score,new LinkedList<>());
            editsByLocalScore.get(v_u_score).add( new Pair<>(entry, new Pair<>(null,new WeightedEdge(v,u,weight))));
            log.info("Possible edge-edit (" + v + "," + u + ") with score: " + v_u_score);
            return v_u_score;
        } else if (both_local < u_v_score && both_local < v_u_score) {
            log.info("Possible edge-edits for both directions with score " + both_local);
            editsByLocalScore.putIfAbsent(both_local,new LinkedList<>());
            editsByLocalScore.get(both_local).add( new Pair<>(entry, new Pair<>(new WeightedEdge(u,v,weight),new WeightedEdge(v,u,weight))));
            return both_local;
        } else {
            log.warning("Same score for u " + u + ", v " + v);
            editsByLocalScore.putIfAbsent(u_v_score,new LinkedList<>());
            editsByLocalScore.get(u_v_score).add( new Pair<>(entry, new Pair<>(new WeightedEdge(u,v,weight),null)));
            return u_v_score;
        }
    }

    // high score means bad edit.
    private Pair<Integer,Boolean> computeEditScoreForEgde(int u, int v, double weight, List<WeightedEdge> currEdgeList){
        HashMap<Edge,Integer> edgeToCount = new HashMap<>();
        currEdgeList.add(new WeightedEdge(u,v,weight));
        edit(currEdgeList);
        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> badSubs = ForbiddenSubgraph.verticesToForbidden(this, edgeToCount,false);
        edit(currEdgeList);
        int badScore = badSubs.getSecond().size() + badSubs.getFirst().size();
        boolean solved = badScore == 0;
        if(solved)
            log.info("Prime edited to cograph with: " + currEdgeList);
        // todo: really???
        if( weight > 1.5){
            badScore =  (int) Math.round( badScore + (p.getWeightMultiplier() * weight));
        }
        return new Pair<>(badScore,solved);
    }

    // ed(this, edges)
    private void edit(List<WeightedEdge> edgeList){

        for(WeightedEdge e : edgeList){
            if(containsEdge(e.getFirst(),e.getSecond())){
                removeEdge(e.getFirst(),e.getSecond());
            } else {
                addEdge(e.getFirst(), e.getSecond());
            }
        }

    }

    // convert subgraph-edge to real edge
    List<Edge> getRealEdges(List<WeightedEdge> oneEdit){
        List<Edge> ret = new ArrayList<>(oneEdit.size());
        for (WeightedEdge subEdge : oneEdit) {
            int src = subNoToBaseNo[subEdge.getFirst()];
            int dst = subNoToBaseNo[subEdge.getSecond()];
            ret.add(new Edge(src, dst));
        }
        return ret;
    }

    // returns the additional edits (With real vertexNos)
    List<Edge> addModuleEdges(List<WeightedEdge> edgeList, Logger log){
        List<Edge> addEdges = new LinkedList<>();
        for (WeightedEdge singleEdge : edgeList) {
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

    // this doesn't give the desired results.
    @Deprecated
    private int computePrimeScoreForEdge(int u, int v, double weight, List<WeightedEdge> currEdgeList, Logger log)
            throws InterruptedException,IOException,ImportException {
        currEdgeList.add(new WeightedEdge(u,v,weight));
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
