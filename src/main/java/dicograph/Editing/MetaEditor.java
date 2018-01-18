package dicograph.Editing;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.ImportException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;

import dicograph.graphIO.IntegerComponentNameProvider;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.utils.Edge;
import dicograph.utils.Parameters;
import dicograph.utils.WeightedPair;
import ilog.concert.IloException;

/**
 * Created by Fynn Leitow on 09.01.18.
 */
public class MetaEditor {

    private final Parameters p;
    private final SimpleDirectedGraph<Integer,DefaultEdge> inputGraph;
    private final Logger log;
    private final MDTree origTree;
    private final int nVertices;


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
    }

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
        if(p.isLazy()){
            allMethodsSolutions.add( computeEditFor(EditType.Lazy) );
        }
        if(p.isBruteForce()){
            allMethodsSolutions.add( computeEditFor(EditType.BruteForce) );
        }
        if(p.isIlpMD()){
            allMethodsSolutions.add( computeEditFor(EditType.ILP) );
        }
        if(p.isGreedyPlusILP()){
            allMethodsSolutions.add( computeEditFor(EditType.GreedyILP));
        }
        if(p.isIlpOnly()){
            allMethodsSolutions.add(  computeGlobalILP() );
        }

        // best solution(s) - with gap
        int best = nVertices * nVertices;
        for(TreeMap<Integer, List<Solution>> solutionMap : allMethodsSolutions){
            int cost = solutionMap.firstKey();
            if( cost <= best){
                if( cost < best && best - cost > p.getSolutionGap()){
                    bestSolutions.clear();
                }
                best = cost;
                bestSolutions.addAll(solutionMap.firstEntry().getValue());
            }
        }
        // output one best solution as .dot:
        boolean done = false;
        for(Solution solution : bestSolutions){
            int cost = solution.getCost();
            log.info(() -> solution.getType() + ": Found solution with " + cost + " edits: " + solution.getEdits());
            log.fine(() ->"Edit-Graph: " + solution.getGraph());
            if(!done && solution.getCost() == best){
                System.out.println("//Edits: " + solution.getEdits());
                DOTExporter<Integer,DefaultEdge> exporter = new DOTExporter<>(new IntegerComponentNameProvider<>(), null, null);
                exporter.exportGraph(solution.getGraph(), System.out);
                done = true;
            }
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
            for(WeightedPair<Integer,Integer> e : glSolver.getSolutionEdgeEdits().get(i)){
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
            log.warning(()-> "ILP found no solution.");
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
}
