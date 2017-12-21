package dicograph.ILPSolver;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedGraph;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

/**
 * Taken from package paraphylo by Mark Hellmuth. Adapted for Directed Graphs.
 * Todo: Link
 */

public class CplexDiCographEditingSolver {

    // forbidden subgraphs of length 4:
    static int [] _p4    = {    1,-1,-1,
                             1,    1,-1,
                            -1, 1,    1,
                            -1,-1, 1    };

    static int [] _n     = {   -1,-1,-1,
                            -1,   -1,-1,
                            -1, 1,   -1,
                             1, 1,-1    };

    static int [] _n_bar = {    1, 1,-1,
                             1,   -1,-1,
                             1, 1,    1,
                             1, 1, 1    };

    static int [][] forbidden_len_4 = {_p4, _n, _n_bar};
    static String [] forbidden_len_4_names = {"p4", "n", "n_bar"};
    // threshold = number of ones - 1
    static int [] threshold_len_4 = {5, 2, 8};

    // forbidden subgraphs of length 3:
    static int [] _p3 = {    1,-1,
                         -1,    1,
                         -1,-1    };

    static int [] _a  = {    1,-1,
                         -1,    1,
                         -1, 1    };

    static int [] _b  = {   -1,-1,
                          1,    1,
                         -1, 1    };

    static int [] _c3 = {    1,-1,
                         -1,    1,
                          1,-1    };

    static int [] _d3 = {    1, 1,
                            -1, 1,
                          1,-1    };

    static int [][] forbidden_len_3 = {_p3, _a, _b, _c3, _d3};
    static String [] forbidden_len_3_names = {"p3", "a", "b", "c3", "d3"};
    static int [] threshold_len_3 = {1, 2, 2, 2, 3};


    // input data. Also works, if has self-loops (will be ignored)
    SimpleDirectedGraph<Integer, DefaultWeightedEdge> inputGraph;
    TreeSet<Integer> sortedVertexSet;
    // parameters
    int [] parameters;

    List<SimpleDirectedGraph<Integer, DefaultWeightedEdge>> solutionGraphs;
    List<Double> editingDistances;

    // CPLEX solver
    IloCplex solver;
    // objective function
    IloNumExpr objFn;

    // number of vertices
    private int vertexCount;

    // orthology variables -> Adjacency Matrix?
    private IloIntVar[][] E;
    private Logger log;

    // sets the time limit (default: 2h), if given by parameter #1
    private int timelimit;

    /**
     * Constructs a new Cplex-Solver for the Di-Cograph-Editing Problem.
     * @param inputGraph the directed graph to be converted to a Di-Cograph
     * @param parameters the parameter array:
     * @throws IloException
     */

    public CplexDiCographEditingSolver(SimpleDirectedGraph<Integer, DefaultWeightedEdge> inputGraph, int[] parameters, Logger log) throws IloException {
        this.inputGraph = inputGraph;
        // want the vertex set of the graph in sorted order for easier display and to uniquely define the matrix
        sortedVertexSet = new TreeSet<>(inputGraph.vertexSet()); // todo: not necessary!
        vertexCount = sortedVertexSet.size();

        solver = new IloCplex();
        solver.setName("DiCographEditingSolver");
        solutionGraphs = new ArrayList<>();
        editingDistances = new ArrayList<>();

        this.parameters = parameters;
        this.log = log;

    }

    public List<SimpleDirectedGraph<Integer,DefaultWeightedEdge>> solve() throws IloException{
        this.solver.setName("CplexCographSolver");

        // initialize boolean variables as "E_x,y"
        int x,y;
        this.E = new IloIntVar[vertexCount][vertexCount];
        for (x = 0; x< vertexCount; x++){
            for (y = 0; y< vertexCount; y++){
                this.E[x][y]= solver.boolVar("E_"+x+","+y);
            }
        }


        DefaultWeightedEdge edge;
        int i = 0;

        // No diagonal entries as self-loops are excluded:
        int noOfAdjacencies = (this.vertexCount *(this.vertexCount -1));
        IloNumExpr symDiff1Expr[] = new IloNumExpr[noOfAdjacencies];
        IloNumExpr symDiff2Expr[] = new IloNumExpr[noOfAdjacencies];

        // Initializes the symmetric difference for the Cograph Computation
        for(int sourceVertex = 0; sourceVertex < vertexCount; sourceVertex++){
            for(int targetVertext = 0; targetVertext < vertexCount; targetVertext++) {
                if(sourceVertex != targetVertext) {
                    edge = inputGraph.getEdge(sourceVertex, targetVertext);
                    int hasEdge =  edge == null ? 0 :1;
                    //assert x != y;
                    symDiff1Expr[i] = solver.prod((1 - hasEdge), E[sourceVertex][targetVertext]); // arrayOutOfBound!!
                    symDiff2Expr[i] = solver.prod(hasEdge, solver.diff(1, E[sourceVertex][targetVertext] ));
                    i++;
                }
            }
        }
        this.objFn = solver.sum(solver.sum(symDiff1Expr),solver.sum(symDiff2Expr));

        // Maximum number of edge edits todo: was ist das???

        //int cographDistance = parameters[0];
        //if(cographDistance > 0) {
        //    solver.addLe(this.objFn, noOfAdjacencies * cographDistance);
        //}

        solver.addMinimize(objFn);

        int w, z, j, k;
        // lazy constraints for each forbidden subgraph
        for (w = 0; w<vertexCount; w++){
            for (x = 0; x<vertexCount; x++){
                if (w!=x){
                    for (y = 0; y<vertexCount; y++){
                        if (w!=y && x!=y){
                            // todo: oder innere Schleife?
                            IloIntVar vars_3[] = {
                                    E[w][x], E[w][y],
                                    E[x][w],          E[x][y],
                                    E[y][w], E[y][x]
                            };
                            // subgraphs of length 3:
                            for(j = 0; j< forbidden_len_3.length; j++){

                                IloRange range = solver.le(solver.scalProd(
                                        forbidden_len_3[j], vars_3), threshold_len_3[j],
                                        forbidden_len_3_names[j] + "_" + w + "," + x + "," + y);
                                solver.addLazyConstraint(range);
                            }

                            // subgraphs of length 4:
                            for (z = 0; z<vertexCount; z++){
                                if (w != z && x!=z && y!=z){
                                    IloIntVar vars_4[] = {
                                                     E[w][x], E[w][y], E[w][z],
                                            E[x][w],          E[x][y], E[x][z],
                                            E[y][w], E[y][x],          E[y][z],
                                            E[z][w], E[z][x], E[z][y]
                                    };
                                    for( k = 0; k< forbidden_len_4.length; k++) {

                                        IloRange range = solver.le(solver.scalProd(
                                                forbidden_len_4[k], vars_4), threshold_len_4[k],
                                                forbidden_len_4_names[k] + "_" + w + "," + x + "," + y + "," + z);
                                        solver.addLazyConstraint(range);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // todo: diese optionen verstehen und auch einbauen!
        /*
        if (this.timelimit>0){
            solver.setParam(IloCplex.DoubleParam.TiLim, parameters.getTimelimitCGE());
        }

        // solve
        if (!parameters.isVerbose()){
            solver.setOut(new NullOutputStream());
        }
        */

        // all solutions
        if (parameters[1] != 0){
            solver.setParam(IloCplex.DoubleParam.SolnPoolAGap, 0.0);
            solver.setParam(IloCplex.IntParam.SolnPoolIntensity, 4);
            solver.setParam(IloCplex.IntParam.PopulateLim, 100);
            solver.populate();
        }


        //solver.exportModel("DiCographEdit_" + new Date());
        boolean ret = solver.solve();
        String solution = "none";
        if (ret) {
            solution = getSolution();
        }
        // free memory
        solver.end();
        log.info(solution);

        return solutionGraphs;
    }


    /**
     * @return the current solution, i.e. the values of xVariables as string
     * @throws IloException
     */
    private String getSolution() throws  IloException {
        int noSolutions = solver.getSolnPoolNsolns();
        StringBuilder solution = new StringBuilder();
        double bestObjectiveValue = solver.getBestObjValue();

        log.fine("Solution status = " + solver.getStatus());
        // todo: das hier...
        //this.gf.setCographEditDistance((int) Math.round(bestObjectiveValue));
        log.fine("CographEditDistance: " + bestObjectiveValue);

        for (int solutionId=0; solutionId<noSolutions; solutionId++){
            if (solver.getObjValue(solutionId)<=bestObjectiveValue){

                // initialize JGraph with same vertex-Set:
                SimpleDirectedGraph<Integer, DefaultWeightedEdge> solutionGraph = new SimpleDirectedGraph<>(DefaultWeightedEdge.class);
                for(int vertex = 0; vertex < vertexCount; vertex++){
                    solutionGraph.addVertex(vertex);
                }

                //Boolean[][] cograph = new Boolean[this.vertexCount][this.vertexCount];
                solution.append("Adjacency Matrix:\n");

                for (int vertex_x = 0; vertex_x < vertexCount; vertex_x++ ){
                    for (int vertex_y = 0; vertex_y < vertexCount; vertex_y++){
                        double variable = 0;
                        if (vertex_x!=vertex_y){
                            variable = solver.getValue(E[vertex_x][vertex_y], solutionId);
                        }
                        boolean hasEdge = variable>0.5;
                        //cograph[x][y]=(hasEdge);
                        solution.append( hasEdge ?"1 " : "0 ");
                        if(hasEdge){
                            solutionGraph.addEdge(vertex_x, vertex_y);
                        }
                    }
                    solution.append("\n");
                }
                // gf.addCograph(cograph);
                editingDistances.add(solver.getObjValue(solutionId));
                solutionGraphs.add(solutionGraph);
                solution.append("\n\n")
                        .append(solver.getObjValue(solutionId))
                        .append("\n");
            }
        }
        return solution.toString();
    }

    public List<Double> getEditingDistances() {
        return editingDistances;
    }
}