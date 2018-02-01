package dicograph.Editing;

import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import dicograph.graphIO.GraphGenerator;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.modDecomp.MDTreeNode;
import dicograph.utils.Edge;
import dicograph.utils.Parameters;
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

class MDEditor {

    private final SimpleDirectedGraph<Integer,DefaultEdge> inputGraph;
    private final int nVertices;
    private final Logger log;
    private final MDTree inputTree;
    private final List<Edge> oldInputEdits;
    private final EditType type;
    private final Parameters p;

    private final boolean firstRun;

    private final SimpleDirectedGraph<Integer, DefaultEdge> workGraph; // in theory, I can have several! One best enough, though.
    private final Map<ForbiddenSubgraph,Integer> subgraphStats;


    // 1st run: just one. Easy.
    // 2nd run:
    //      firstRun prime: Simply add all the edits that worked on this prime, with count as key.
    //
    //      next primes: combine with ALL previous solutions, updating the count. Assert: no same!
    //
    //      last prime: again, combine with ALL before.
    //                  - in the end, combine every of them with _prevEdits_
    //                  - check the Tree / forbiddenSubs. If successful: try removing loops.


    public MDEditor(SimpleDirectedGraph<Integer,DefaultEdge> input, MDTree tree, Logger logger,
                    List<Edge> oldEdits, EditType ed, Parameters params, boolean first){
        inputGraph = input;
        p = params;
        nVertices = inputGraph.vertexSet().size();
        workGraph = GraphGenerator.deepClone(inputGraph);
        log = logger;
        type = ed;
        inputTree = tree;
        oldInputEdits = oldEdits;
        firstRun = first;
        subgraphStats = new LinkedHashMap<>();
    }

    public MDEditor(SimpleDirectedGraph<Integer,DefaultEdge> input, MDTree tree, Logger logger,EditType ed, Parameters params){
        this(input, tree, logger, null, ed, params,true);
    }



    // want:
    // key < -1 means unsuccessful. Need empty entry if already below prime trh
    public TreeMap<Integer, List<Solution>> editIntoCograph(double relTime)
            throws ImportException, IOException, InterruptedException, IloException{

        TreeMap<Integer, List<Solution>> finalSolutions = new TreeMap<>();
        TreeMap<Integer, List<List<Edge>>> currentEditResults;
        List< List<Edge>> currentSolutions = new LinkedList<>(); // All Edits -> local. But need all NEW edits (i.e. from prev/lower primes)

        if(oldInputEdits != null){
            currentSolutions.add(new ArrayList<>(oldInputEdits));
            editGraph(workGraph,oldInputEdits); // start working on the primes from here.
        }


        log.fine(()->"Original Tree: " + MDTree.beautify(inputTree.toString()));
        TreeMap<Integer,LinkedList<MDTreeNode>> depthToPrimes = inputTree.getPrimeModulesBottomUp();

        log.info(type + " started: " +new SimpleDateFormat("HH:mm:ss:SSS").format(Calendar.getInstance().getTime()));

        for(Map.Entry<Integer,LinkedList<MDTreeNode>> entry : depthToPrimes.descendingMap().entrySet()){
            log.info(()->"Editing primes on lvl " + entry.getKey());
            for(MDTreeNode primeNode : entry.getValue()){

                // Skip them during first edit.
                if(firstRun && type.secondPrimeRun() &&  primeNode.getNumChildren() <= p.getBruteForceThreshold()){
                    log.info("Skipping small prime during first run: " + primeNode);
                    continue;
                }

                log.info(()->"Editing prime: " + MDTree.beautify(primeNode.toString()));

                currentEditResults = computeRealEditsForNode(primeNode, relTime);
                if(currentEditResults.isEmpty()){
                    log.warning(() -> "Aborting, no edit found for this prime: " + primeNode);
                    return finalSolutions;
                }

                List<List<Edge>> allNewSolutions = new LinkedList<>();
                // Combine every new edit with every previous edit
                for(Map.Entry<Integer, List<List<Edge>>> editsForCost : currentEditResults.entrySet() ){
                    if(currentSolutions.isEmpty()){
                        log.fine("Adding to empty 'currentSolutions': " + editsForCost.getValue());
                        allNewSolutions.addAll(editsForCost.getValue()); // addAll only OK if just one.

                    } else {
                        for (List<Edge> newEdit : editsForCost.getValue()) {
                            for (List<Edge> previousEdit : currentSolutions) {

                                List<Edge> combined = new LinkedList<>(previousEdit);
                                combined.addAll(newEdit);

                                //List<Edge> newEditCopy = new LinkedList<>(newEdit);
                                //assert editIsValid(oldInputEdits, previousEdit, newEdit) : "Illegal situation - edit of one prime included in edit of other!!!";
                                //newEditCopy.addAll(previousEdit);

                                allNewSolutions.add(combined);
                            }
                        }
                    }
                }
                log.info("Current solutions. Count: " + allNewSolutions.size() + ", Solutions: " + allNewSolutions);
                currentSolutions = allNewSolutions;
            }
        }

        // happens if all primes small enough in step 1.
        if(currentSolutions.isEmpty()){
            log.info(()->"No changes after first step.");
            LinkedList<Solution> trivial = new LinkedList<>();
            trivial.add( new Solution(workGraph, inputTree, new LinkedList<>(), type));
            finalSolutions.put(0,trivial);
            return finalSolutions;
        }

        log.info("Initially all edits of this run: " + currentSolutions);
        log.info("Verifying, eleminating loops and comparing.");

        if(oldInputEdits != null){
            editGraph(workGraph,oldInputEdits); //need to edit back
        }
        int lowestCost = nVertices * nVertices;
        boolean solved = false;

        // - Check all edits if valid
        // - Want to choose from ALL possible solutions if gap >= 0
        for ( List<Edge> possibleEdit : currentSolutions) {

            // verify, remove doubles, check loops
            Pair <Integer,MDTree> verificationRes = verifyAndClean(possibleEdit);
            int cost = verificationRes.getFirst();
            MDTree solTree = verificationRes.getSecond();

            // only consider better edits or edits within gap
            if ( p.getSolutionGap() < 0 && cost < lowestCost ||
                    p.getSolutionGap() >= 0 && cost <= lowestCost + p.getSolutionGap()){

                if(cost > 0 || firstRun && type != EditType.ILP) {
                    if(cost > 0 && cost < lowestCost) {
                        solved = true;
                        lowestCost = cost;
                        log.info(()->"Found valid edit with cost " + cost + ": " + possibleEdit);
                    }
                    // final solutions: add with real cost (or negative, if 1st run)
                    finalSolutions.putIfAbsent(cost, new LinkedList<>());
                    editGraph(workGraph, possibleEdit);
                    Solution sol = new Solution(GraphGenerator.deepClone(workGraph), solTree, possibleEdit, type);
                    finalSolutions.get(cost).add(sol);
                    editGraph(workGraph,possibleEdit);
                }
            }
        }

        if(solved)
            log.info(()->"Final Solution(s): "+ finalSolutions); // .get(lowestCost)
        else
            log.info(()->"Not yet a solution.");
        log.info(()->type + " ended: " +new SimpleDateFormat("HH:mm:ss:SSS").format(Calendar.getInstance().getTime()));


        return finalSolutions; // empty if nothing found.
    }

    // Positive, empty or negative.
    private TreeMap<Integer,List<List<Edge>>> computeRealEditsForNode(MDTreeNode primeNode, double relTime)throws ImportException, IOException, InterruptedException,
            IloException{

        TreeMap<Integer,List<List<Edge>>> allRealEdits = new TreeMap<>();
        PrimeSubgraph subGraph = new PrimeSubgraph(workGraph,primeNode,p, log, type);

        log.fine(() ->"Subgraph: " + subGraph.toString());
        log.info(() ->"Base-Vertex to Sub-Vertex " + subGraph.getBaseNoTosubNo());
        log.info(() -> "Computing possible edit Sets.");
        // counts in order: D_3, A, B, C_3, \bar{D_3}, P_4, N, \bar{N}.
        Map<ForbiddenSubgraph,Integer> subgraphCounts = new LinkedHashMap<>();
        for(ForbiddenSubgraph sg : ForbiddenSubgraph.len_3)
            subgraphCounts.put(sg,0);
        for(ForbiddenSubgraph sg : ForbiddenSubgraph.len_4)
            subgraphCounts.put(sg,0);
        // negative cost - not yet successful (aborted in step 1 due to threshold/ processed all)
        // empty: nothing found in step 2
        TreeMap<Integer, List<List<WeightedEdge>>> allPossibleEdits = subGraph.computeEdits(firstRun, subgraphCounts, relTime, oldInputEdits == null);

        // - compute ALL feasable edits within bruteForceGap
        // - choose the BEST edit when taking the original graph into account
        //      - Problem: difficult if there are several new prime modules. Would need to combine them all...
        if(!allPossibleEdits.isEmpty()){
            log.info(()->"Computed Edits: " + allPossibleEdits);
            int bestCost = nVertices * nVertices;

            // retrieve the original vertex-Nos and corresponding edits from the main graph
            for (Map.Entry<Integer, List<List<WeightedEdge>>> listOfEdits : allPossibleEdits.entrySet()) {
                for (List<WeightedEdge> oneEdit : listOfEdits.getValue()) {

                    ArrayList<Edge> currentList = new ArrayList<>(oneEdit.size());

                    // convert subgraph-edges to real edges. Add them first!!!
                    currentList.addAll( subGraph.getRealEdges(oneEdit) );

                    // edits between modules (as real edges). Also, if unsuccessful!
                    currentList.addAll(subGraph.addModuleEdges(oneEdit, log));


                    // remove doubles, check loops -> later. Here, just add all to map.
                    int cost = listOfEdits.getKey(); // negative means not successful!

                    if(cost <= bestCost + p.getSolutionGap()) {
                        if(cost < bestCost)
                            bestCost = cost;
                        log.info(() -> "Adding: cost " + cost + ", Real Edges: " + currentList);
                        allRealEdits.putIfAbsent(cost, new LinkedList<>());
                        allRealEdits.get(cost).add(currentList);
                    } else {
                        log.info(() -> "Discarding: cost " + cost + ", Real Edges: " + currentList);
                    }
                }
            }

        }
        // update stats
        for(Map.Entry<ForbiddenSubgraph,Integer> fsubEntry : subgraphCounts.entrySet()){
            int newCount = subgraphStats.getOrDefault(fsubEntry.getKey(),0) + fsubEntry.getValue();
            subgraphStats.put(fsubEntry.getKey(), newCount);
        }

        // else: no solution for this prime.
        return allRealEdits;
    }

    private Pair<Integer,MDTree> verifyAndClean(List<Edge> currentList) throws ImportException, InterruptedException, IOException{

        // check for loops and doubles:
        List<Edge> loops = new LinkedList<>();
        HashMap<Edge, Integer> doubles = new HashMap<>();
        for(int i = 0; i< currentList.size(); i++){
            for (int j = i+1; j < currentList.size(); j++) {
                Edge _1st = currentList.get(i);
                Edge _2nd = currentList.get(j);
                if((int) _1st.getFirst() == _2nd.getSecond() && (int) _1st.getSecond() == _2nd.getFirst()){
                    // loop: might be necessary
                    loops.add(_1st);
                    loops.add(_2nd);
                } else if( _1st.equals(_2nd)){
                    // doubles: can be deleted.
                    int cnt = doubles.getOrDefault(_1st,0);
                    doubles.put(_1st, ++cnt);
                }

            }
        }
//        // Do I have a problem if they occur more often?
//        for(Map.Entry<Edge,Integer> e: doubles.entrySet()){
//            if(e.getValue() > 1){
//                throw new IllegalStateException("Error: Multiple duplicates in edit: " + currentList);
//            }
//        }
        if(currentList.removeAll(doubles.keySet()))
            log.info("Removed doubles: " + doubles.keySet());

        // 1st: is this edit a solution?
        editGraph(workGraph,currentList); // no loops removed yet
        DirectedMD verifyMD = new DirectedMD(workGraph, log, false);
        MDTree verifyTree = verifyMD.computeModularDecomposition();
        int ret;

        if(verifyTree.getPrimeModulesBottomUp().isEmpty()) {

            // 2nd: if yes - can I remove loops?
            if (!loops.isEmpty()) {
                editGraph(workGraph, loops);
                DirectedMD looplessMD = new DirectedMD(workGraph, log, false);
                MDTree looplessRes = looplessMD.computeModularDecomposition();
                if (looplessRes.getPrimeModulesBottomUp().isEmpty()) {
                    log.info(()->"Removing Loops: " + loops);
                    currentList.removeAll(loops);
                    verifyTree = looplessRes;
                }
                editGraph(workGraph, loops); // edit back.
            }
            ret = currentList.size();

        } else {
            ret = - currentList.size();
        }
        editGraph(workGraph, currentList);

        return new Pair<>(ret,verifyTree);
    }

    // testing purposes only
    static boolean editIsValid(List<Edge> oldInputEdits, List<Edge> _prev, List<Edge> _new){
        if(oldInputEdits == null)
            return true;

        HashSet<Edge> validator = new HashSet<>(_prev);
        validator.removeAll(oldInputEdits);
        return !_prev.removeAll(_new);
    }

    // remove edge if present, add if not.
    private static void editGraph(Graph<Integer, DefaultEdge> g, List<Edge> edgeList){
        for(Edge e : edgeList){
            if(g.containsEdge(e.getFirst(),e.getSecond())){
                g.removeEdge(e.getFirst(),e.getSecond());
            } else {
                g.addEdge(e.getFirst(), e.getSecond());
            }
        }
    }

    public Map<ForbiddenSubgraph, Integer> getSubgraphStats() {
        return subgraphStats;
    }
}
