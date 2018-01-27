package dicograph.Editing;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import dicograph.graphIO.IntegerComponentNameProvider;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.utils.Edge;
import dicograph.utils.Parameters;
import dicograph.utils.Triple;
import dicograph.utils.WeightedEdge;
import ilog.concert.IloException;

import static com.google.common.math.IntMath.binomial;
import static com.google.common.math.IntMath.factorial;

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

public class MetaEditor {

    private final DecimalFormat df = new DecimalFormat("0.000000");

    private final Parameters p;
    private final SimpleDirectedGraph<Integer,DefaultEdge> inputGraph;
    private final Logger log;
    private final MDTree origTree;
    private final int nVertices;
    private Set<Triple> cotreeTriples;
    private int ilpCost;
    private int lazyCost;
    private int lazyCorrectRun;
    private double lazyTTDistance;
    private double bestTTDistance;
    private int bestCost;
    private Solution lazySolution;


    // Original Graph and all the parameters
    // Calls MDEditor twice for each mode (firstRun & oldInputEdits as flag)
    public MetaEditor(SimpleDirectedGraph<Integer,DefaultEdge> g, Parameters params, Logger logger)
            throws IOException, ImportException, InterruptedException{
        inputGraph = g;
        nVertices = g.vertexSet().size();
        p = params;
        log = logger;
        DirectedMD modDecomp = new DirectedMD(inputGraph, log, false);
        origTree = modDecomp.computeModularDecomposition();
        lazyCorrectRun = 0;
        bestTTDistance = Double.MAX_VALUE;
        lazyTTDistance = Double.MAX_VALUE;
        bestCost = Integer.MAX_VALUE;
        lazyCost = Integer.MAX_VALUE;
        lazySolution = null;
    }

    // best tt distance will be first.
    public List<Solution> computeSolutionsForMethods() throws IOException, ImportException, IloException, InterruptedException, ExportException{

        List<Solution> bestSolutions = new LinkedList<>();

        // already cograph?
        log.fine("Input graph:\n" + inputGraph);
        log.info("MD of input graph:\n" + MDTree.beautify(origTree.toString()));
        if(origTree.getPrimeModulesBottomUp().isEmpty()){
            log.info("Input graph is already a dicograph. Aborting.");
            Solution trivial = new Solution(inputGraph, new LinkedList<>(), EditType.None);
            bestSolutions.add(trivial);
            return bestSolutions;
        }

        // List of Maps: Cost -> One best Edit-Graph with Edit-Edges. No zeros here!
        List<TreeMap<Integer,List<Solution>>> allMethodsSolutions = new ArrayList<>(6);
        List<Solution> bestILPSolns = null;
        if(p.isLazy()){
            TreeMap<Integer, List<Solution>> sols =  computeEditFor(EditType.Lazy);
            allMethodsSolutions.add( sols );
            if(!sols.isEmpty()) {
                lazySolution = sols.firstEntry().getValue().get(0);
            }
        }
        if(p.isBruteForce()){
            allMethodsSolutions.add( computeEditFor(EditType.BruteForce) );
        }
        if(p.isIlpMD()){
            TreeMap<Integer, List<Solution>> sols = computeEditFor(EditType.ILP);
            allMethodsSolutions.add( sols );
            bestILPSolns = sols.firstEntry().getValue();
        }
        if(p.isGreedyPlusILP()){
            allMethodsSolutions.add( computeEditFor(EditType.GreedyILP));
        }
        if(p.isIlpOnly()){
            allMethodsSolutions.add(  computeGlobalILP() );
        }

        // how good was lazy compared to ILP?
        if(lazySolution != null && bestILPSolns != null){
            ilpCost= bestILPSolns.get(0).getCost();
            for(Solution sol : bestILPSolns){
                int count = 0;
                for( Edge edit : lazySolution.getEdits()) {
                    if(sol.getEdits().contains(edit)){
                        sol.getEdits().remove(edit);
                        sol.getEdits().add(count,edit); // reorder for easier comparison.
                        count++;
                    } else {
                        break;
                    }
                }
                if(count > 0 && count > lazyCorrectRun){
                    lazyCorrectRun = count;
                }
            }
            lazyCost = lazySolution.getCost();
            log.info("Lazy got the first " + lazyCorrectRun + " of " + ilpCost + " edits right.");
            log.info("Lazy cost: " + lazyCost + "; ILP cost: " + ilpCost);
        }

        // best solution(s) - with gap
        Solution bestDistSolution = null;
        for(TreeMap<Integer, List<Solution>> solutionMap : allMethodsSolutions){
            int cost = solutionMap.firstKey();
            if( cost <= bestCost){
                if( cost < bestCost && bestCost - cost > p.getSolutionGap()){
                    bestSolutions.clear();
                }
                bestCost = cost;
                bestSolutions.addAll(solutionMap.firstEntry().getValue());
            }
            // triple metric - check all!
            if(cotreeTriples != null){
                int divisor =  2*binomial(nVertices,3); // 2* (n \choose 3) possible triples

                for(List<Solution> solutions : solutionMap.values()) {
                    for(Solution solution : solutions) {

                        DirectedMD solMD = new DirectedMD(solution.getGraph(), log, false);
                        MDTree solTree = solMD.computeModularDecomposition();
                        log.fine("Tree of solution: " + MDTree.beautify(solTree.toString()));
                        Set<Triple> solTriples = solTree.getTriples();
                        Set<Triple> coTriples = new HashSet<>(cotreeTriples);
                        coTriples.removeAll(solTriples);
                        solTriples.removeAll(cotreeTriples);
                        int tt_dist = coTriples.size() + solTriples.size();
                        double tt_distance_normed = (1.0 *tt_dist) / divisor;
                        solution.setTreeDistance(tt_distance_normed);
                        log.info("TT-distance: " + tt_dist + ", Normalized: " + df.format(tt_distance_normed) + " for solution: " + solution);
                        if(solution.getType() == EditType.Lazy)
                            lazyTTDistance = tt_distance_normed;

                        if (tt_distance_normed < bestTTDistance) {
                            bestTTDistance = tt_distance_normed;
                            bestDistSolution = solution;
                        }
                    }
                }
            }
        }
        // output one best solution as .dot:
        boolean done = false;
        for(Solution solution : bestSolutions){
            int cost = solution.getCost();
            log.info(() -> solution.getType() + ": Found solution with " + cost + " edits: " + solution.getEdits());
            log.fine(() ->"Edit-Graph: " + solution.getGraph());
//            todo: activate!!!
//            if(!done && solution.getCost() == bestCost){
//                System.out.println("//Edits: " + solution.getEdits());
//                DOTExporter<Integer,DefaultEdge> exporter = new DOTExporter<>(new IntegerComponentNameProvider<>(), null, null);
//                exporter.exportGraph(solution.getGraph(), System.out);
//                done = true;
//            }

        }
        if(bestDistSolution != null){
            log.info("Best TT-Distance: " + df.format(bestTTDistance) + " for solution: " + bestDistSolution);
            bestSolutions.remove(bestDistSolution);
            bestSolutions.add(0,bestDistSolution);
        }




        return bestSolutions;
    }

    private TreeMap<Integer, List<Solution>> computeGlobalILP() throws IloException{
        TreeMap<Integer, List<Solution>> ret = new TreeMap<>();
        CplexDiCographEditingSolver glSolver = new CplexDiCographEditingSolver(inputGraph, p, log);
        glSolver.solve();
        for (int i = 0; i < glSolver.getEditingDistances().size(); i++) {

            int val = (int) Math.round(glSolver.getEditingDistances().get(i));
            List<Edge> edges = new ArrayList<>(glSolver.getSolutionEdgeEdits().get(i).size());
            for(WeightedEdge e : glSolver.getSolutionEdgeEdits().get(i)){
                edges.add( new Edge(e.getFirst(), e.getSecond()));
            }
            ret.putIfAbsent(val,new LinkedList<>());
            ret.get(val).add(new Solution(glSolver.getSolutionGraphs().get(i),edges, EditType.ILPGlobal));
        }
        return ret;
    }

    private TreeMap<Integer, List<Solution>> computeEditFor(EditType method) throws
    IOException, ImportException, InterruptedException, IloException{

        log.info("Starting Editor for method: " + method);
        MDEditor firstEditor = new MDEditor(inputGraph, origTree, log, method, p);
        TreeMap<Integer, List<Solution>> firstSolns = firstEditor.editIntoCograph();

        // 1st run gives only one solution whenever we plan a 2nd run.
        Solution firstSol = firstSolns.firstEntry().getValue().get(0);
        DirectedMD firstMD = new DirectedMD(firstSol.getGraph(), log, false);
        MDTree firstTree = firstMD.computeModularDecomposition();

        if(firstTree.getPrimeModulesBottomUp().isEmpty()){
            log.info(()-> method + " method was successful after first run.");
            return firstSolns;
        } else if (method == EditType.ILP){
            log.severe(()-> "Error: ILP found no solution.");
        }

        // second call. Input graph original but firstTree edited.
        log.info(() ->"Starting second run.");
        MDEditor secondEdit = new MDEditor(inputGraph, firstTree, log, firstSol.getEdits(), method, p);
        TreeMap<Integer, List<Solution>> secondSolns = secondEdit.editIntoCograph();
        if(secondSolns.isEmpty()){
            log.warning(()-> method + " method was unsuccessful.");
            secondSolns.clear();
        }

        return secondSolns;
    }

    public void setCotreeTriples(Set<Triple> cotreeTriples) {
        this.cotreeTriples = cotreeTriples;
    }

    public int getIlpCost() {
        return ilpCost;
    }

    public int getLazyCost() {
        return lazyCost;
    }

    public int getLazyCorrectRun() {
        return lazyCorrectRun;
    }

    public double getBestTTDistance() {
        return bestTTDistance;
    }

    public int getBestCost() {
        return bestCost;
    }

    public double getLazyTTDistance() {
        return lazyTTDistance;
    }

    public Solution getLazySolution() {
        return lazySolution;
    }
}
