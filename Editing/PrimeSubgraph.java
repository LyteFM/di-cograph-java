package dicograph.Editing;

import com.google.common.collect.Sets;

import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.ClassBasedEdgeFactory;
import org.jgrapht.graph.DefaultEdge;
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
import java.util.Set;
import java.util.logging.Logger;

import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.WeightedPair;
import ilog.concert.IloException;

/**
 * Created by Fynn Leitow on 21.12.17.
 */
public class PrimeSubgraph extends SimpleDirectedGraph<Integer,DefaultWeightedEdge> {

    private static final int maxCost = 20;
    private int softThreshold = 3; // If no succesful edit found, discard edits with forbiddenSub-score <= this value
    private int hardThreshold = 0; // Force stop if forbiddenSub-Score <= this value during first run and use brute-force/branching/ILP in second run to complete.

    private final SimpleDirectedGraph<Integer, DefaultWeightedEdge> base;
    private final int nVertices;

    private final HashMap<Integer,Integer> baseNoTosubNo;
    private final double[] subVertexToWeight;
    private final int[] subNoToBaseNo;
    private final MDTreeNode primeNode;


    public PrimeSubgraph(SimpleDirectedGraph<Integer,DefaultWeightedEdge> baseGraph, MDTreeNode node){
        super(new ClassBasedEdgeFactory<>(DefaultWeightedEdge.class),true);
        base = baseGraph;
        baseNoTosubNo = new HashMap<>();
        primeNode = node;
        subVertexToWeight = node.initWeightedSubgraph(this,base);
        nVertices = subVertexToWeight.length;
        subNoToBaseNo = new int[nVertices];
        for(Map.Entry<Integer, Integer> base2Sub : baseNoTosubNo.entrySet()){
            subNoToBaseNo[base2Sub.getValue()] =  base2Sub.getKey();
        }
    }

    // Possible optimization: sequential subsets! (a,b) -> (a,b,c) ...
    // 0 - Greedy Heuristic
    // 1 - ILP
    // else: Brute Force
    public Map<Integer, List<List<WeightedPair<Integer, Integer>>>> computeBestEdgeEdit(Logger log, int method, boolean first)
    throws InterruptedException, IOException, ImportException, IloException{
        // an entry in the map means: if present -> remove, if not -> add.
        //


        // Recognize all forbidden subgraphs
        HashMap<Pair<Integer,Integer>,Integer> edgeToCount = new HashMap<>();
        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> badSubs = ForbiddenSubgraph.verticesToForbidden(this, edgeToCount);
        log.info("Length 3:\n" + badSubs.getFirst());
        log.info("Length 4:\n" + badSubs.getSecond());
        // Compute vertex and edge-scores
        ForbiddenSubgraph.computeScores(badSubs, log,this, false);
        ArrayList<Map.Entry<Pair<Integer,Integer>,Integer>> edgesToScore = new ArrayList<>(edgeToCount.entrySet());
        edgesToScore.sort( Comparator.comparingInt(e ->  -Math.abs( e.getValue() ))); // descending
        log.info("Favourable edges: " + edgesToScore.size() + "\n" + edgesToScore);
        int firstScore = edgesToScore.get(0).getValue();

        HashMap<Integer, List<List<WeightedPair<Integer, Integer>>>> costToEdges = new HashMap<>();
        Map<Integer, WeightedPair<Integer,Integer>> intToEdge = new HashMap<>();
        boolean thresholdReached = false;

        // a) use the EdgeFactory and create all those edges with their respective weight. Add them later. <- doesn't work if I don't add them...
        // b) use Pairs.
        double[][] weightMatrix = new double[nVertices][nVertices];


        int cnt = 0;
        // Computes all weights for an edit between subVertices i,j.
        for (int i : vertexSet()) {
            for (int j : vertexSet()) {
                if(j!=i){
                    double weight = subVertexToWeight[i] * subVertexToWeight[j];
                    weightMatrix[i][j] = weight;
                    // this only for brute force:
                    WeightedPair<Integer,Integer> e = new WeightedPair<>(i,j,weight);
                    intToEdge.put( cnt, e );
                    cnt++;
                }
            }
        }

        if(method == 0) {
            SimpleDirectedGraph<Integer,DefaultEdge> graphOfEditEdges = new SimpleDirectedGraph<Integer, DefaultEdge>(DefaultEdge.class);
            ConnectivityInspector<Integer,DefaultEdge> pathFinder = new ConnectivityInspector<>(graphOfEditEdges);
            List<WeightedPair<Integer, Integer>> currEdgeList = new LinkedList<>(); // just one edge
            List<WeightedPair<Integer,Integer >> allEdgesList = new LinkedList<>();
            List<WeightedPair<Integer, Integer>> edgesToRemove = new LinkedList<>(); // remove edges below threshold, if no successful edit found.
            WeightedPair<Integer,Integer > lastEdge = null;
            int i, u,v, u_v_score;
            int v_u_score = 0;
            int prevScore = badSubs.getFirst().size() + badSubs.getSecond().size();
            int cost = 0;
            double weight;
            for (i = 0; i < edgesToScore.size(); i++) {
                Map.Entry<Pair<Integer,Integer>,Integer> edge = edgesToScore.get(i);
                // don't do that for very small modules...
                if(first && allEdgesList.size() > 3 && Math.abs(edge.getValue()) <= hardThreshold){
                    thresholdReached = true;
                    log.warning("Reached hard threshold " + hardThreshold + " for edge " + edge);
                    break;
                }
                u = edge.getKey().getFirst();
                v = edge.getKey().getSecond();
                // compute the graph of edited edges to exclude existing paths (any direction)
                boolean addedU = graphOfEditEdges.addVertex(u);
                boolean addedV = graphOfEditEdges.addVertex(v);
                if( !addedU && !addedV ){
                    if(pathFinder.pathExists(u,v)) {
                        log.warning("Skipped: path already exists from " + u + " to " + v);
                        continue;
                    } else {
                        log.warning("Both vertices for path " + u + ", " + v + " already exist!");
                        //continue; // todo!!!
                    }
                }
                weight = subVertexToWeight[u] * subVertexToWeight[v];
                // determine the edit direction (Problem: what if both?)
                currEdgeList.addAll(allEdgesList);
                u_v_score = computeSubgraphScoreForEgde(u,v,weight,currEdgeList,log);
                if(u_v_score != 0) {
                    currEdgeList.clear();
                    currEdgeList.addAll(allEdgesList);
                    v_u_score = computeSubgraphScoreForEgde(v, u, weight, currEdgeList, log);
                    if(v_u_score == 0)
                        lastEdge = new WeightedPair<>(v,u,weight);
                } else {
                    lastEdge = new WeightedPair<>(u,v,weight);
                }

                if(lastEdge != null){
                    // we're done.
                    log.info("Subgraph edited into Cograph with edge " + lastEdge);
                    allEdgesList.add(lastEdge);
                    List<List<WeightedPair<Integer, Integer>>> res = new LinkedList<>();
                    res.add(allEdgesList);
                    cost += Math.round(weight);
                    costToEdges.put(cost,res);
                    break;
                }
                // todo: gut möglich, dass BEIDE Kantenrichtungen editiert werden...

                if(u_v_score < prevScore || v_u_score < prevScore) {

                    cost += Math.round(weight);
                    if (u_v_score < v_u_score) {
                        prevScore = addEditEdge(u,v,weight,u_v_score, v_u_score, log,graphOfEditEdges, allEdgesList);
                    } else if (v_u_score < u_v_score) {
                        prevScore = addEditEdge(v,u,weight, v_u_score, u_v_score, log,graphOfEditEdges, allEdgesList);
                    } else {
                        log.warning("Same score for u "+ u + ", v " + v);
                        prevScore = addEditEdge(u,v,weight,u_v_score, v_u_score, log,graphOfEditEdges, allEdgesList);
                    }
                    if(edge.getValue() <= softThreshold)
                        edgesToRemove.add(allEdgesList.get(allEdgesList.size() - 1));
                } else {
                    log.info("Discarded: edge (" + u + "," + v + ") which doesn't improve the module" );
                    if(addedU)
                        graphOfEditEdges.removeVertex(u);
                    if(addedV)
                        graphOfEditEdges.removeVertex(v);
                }

                currEdgeList.clear();
            }
            if(i == edgesToScore.size() || thresholdReached){
                log.warning("Processed all edges or reached hard threshold - no cograph-edit found!!!");
                List<List<WeightedPair<Integer, Integer>>> res = new LinkedList<>();
                // remove those below soft threshold:
                log.info("Removing (below soft threshold): " + edgesToRemove);
                allEdgesList.removeAll(edgesToRemove);
                res.add(allEdgesList);
                costToEdges.put(-1,res);
            }
            // Add edges later

        } else if(method == 1){
            // todo: later, I only need one best solution!
            CplexDiCographEditingSolver primeSolver = new CplexDiCographEditingSolver(
                    this, new int[]{0,1}, weightMatrix, log);
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



        } else {
            int smallestCost = maxCost;
            for (int i = 1; i < maxCost; i++) {
                System.out.println("Computing all permutations of size " + i + " for n = " + cnt);
                Set<Set<Integer>> combinations = Sets.combinations(intToEdge.keySet(), i);
                System.out.println(combinations.size() + " permutations!");
                boolean costStillValid = false;

                int count = 0;

                for (Set<Integer> edgeSubset : combinations) {
                    int cost = 0;
                    List<WeightedPair<Integer, Integer>> currEdgeList = new ArrayList<>(edgeSubset.size());
                    for (int edgeNo : edgeSubset) {
                        WeightedPair<Integer, Integer> e = intToEdge.get(edgeNo);
                        currEdgeList.add(e);
                        cost += Math.round(e.getWeight()); // better use rounding, just in case...
                    }
                    if (cost < maxCost) {

                        costStillValid = true;

                        edit(currEdgeList);
                        // Brute force checking for forbidden subgraphs might be more efficient...
                        DirectedMD subMD = new DirectedMD(this, log, false);
                        MDTree subTree = subMD.computeModularDecomposition();
                        log.info("Edges: " + currEdgeList);
                        log.info("Tree: " + MDTree.beautify(subTree.toString()));
                        // edit back.
                        edit(currEdgeList);
                        if (subTree.getPrimeModulesBottomUp().isEmpty()) {
                            log.info(() -> " Successful edit found: " + currEdgeList);
                            costToEdges.putIfAbsent(cost, new LinkedList<>());
                            costToEdges.get(cost).add(currEdgeList);
                            // need this to verify that an early, but expensive edit is best.
                            if (cost < smallestCost)
                                smallestCost = cost;
                            //if (smallestCost <= i)
                            //    return costToEdges;
                            // todo: disabled for complete test run.
                        }

                    }


                    count++;
                    if (count % 1000000 == 0)
                        log.info("i: " + i + ", Count: " + count + ", Edges: " + currEdgeList);

                    //if(count == 50000000) {
                    //    log.info("Aborted after 500");
                    //    break;
                    //}
                }

                if (!costStillValid) {
                    break; // no need to continue
                }

            }
        }

        // Subgraph Edges!
        return costToEdges;
    }

    static int addEditEdge(int u, int v, double weight, int u_v_score, int v_u_score, Logger log,
                           SimpleDirectedGraph<Integer,DefaultEdge> graphOfEditEdges, List<WeightedPair<Integer,Integer>> allEdgesList){
        allEdgesList.add(new WeightedPair<>(u, v, weight));
        graphOfEditEdges.addEdge(u,v);
        log.info("Chose edge (" + u + "," + v + ") with score: " + u_v_score +" < " + v_u_score);
        return u_v_score;
    }

    // returns the additional edges (With real vertexNos)
    List<Pair<Integer,Integer>> addModuleEdges(List<WeightedPair<Integer,Integer>> edgeList, Logger log){
        List<Pair<Integer, Integer>> addEdges = new LinkedList<>();
        for (WeightedPair<Integer, Integer> singleEdge : edgeList) {
            if (Math.round(singleEdge.getWeight()) > 1) {
                log.info(() -> "Adding edges for " + singleEdge);
                int realFirstVertex = subNoToBaseNo[singleEdge.getFirst()];
                int realSecondVertex = subNoToBaseNo[singleEdge.getSecond()];
                List<Pair<Integer,Integer>> added = primeNode.addModuleEdges(realFirstVertex,realSecondVertex);
                log.info(() -> "Added: " + added);
                addEdges.addAll(added);
            }
        }
        return addEdges;
    }

    private int computeSubgraphScoreForEgde(int u,int v, double weight, List<WeightedPair<Integer,Integer>> currEdgeList, Logger log){
        HashMap<Pair<Integer,Integer>,Integer> edgeToCount = new HashMap<>();
        currEdgeList.add(new WeightedPair<>(u,v,weight));
        edit(currEdgeList); // todo: I need to keep this edge list or the edit!
        Pair<Map<BitSet,ForbiddenSubgraph>,Map<BitSet,ForbiddenSubgraph>> badSubs = ForbiddenSubgraph.verticesToForbidden(this, edgeToCount);
        edit(currEdgeList);
        int res = badSubs.getSecond().size() + badSubs.getFirst().size();
        log.info("Edge: (" + u + "," + v + "), #forbiddenSubs: " + res);
        return res;
    }

    // this doesn't give the desired results.
    private int computePrimeScoreForEdge(int u,int v, double weight, List<WeightedPair<Integer,Integer>> currEdgeList, Logger log)
    throws InterruptedException,IOException,ImportException {
        currEdgeList.add(new WeightedPair<>(u,v,weight));
        edit(currEdgeList);
        DirectedMD subMD = new DirectedMD(this, log, false);
        MDTree subTree = subMD.computeModularDecomposition();
        edit(currEdgeList);
        return subTree.getNumPrimeChildren();
    }

    public void edit(List<WeightedPair<Integer,Integer>> edgeList){

        for(WeightedPair<Integer,Integer> e : edgeList){
            if(containsEdge(e.getFirst(),e.getSecond())){
                removeEdge(e.getFirst(),e.getSecond());
            } else {
                // todo: due to weighted Pair, I might not need weightedEdge at all.
                Graphs.addEdge(this, e.getFirst(), e.getSecond(), e.getWeight());
            }
        }

    }


    public HashMap<Integer, Integer> getBaseNoTosubNo() {
        return baseNoTosubNo;
    }

    public SimpleDirectedGraph<Integer, DefaultWeightedEdge> getBase() {
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