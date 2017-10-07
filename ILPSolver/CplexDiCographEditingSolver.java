package dicograph.ILPSolver;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

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
    static int [] _p4    = {1,0,0, 1,1,0, 0,1,1, 0,0,1};
    static int [] _n     = {0,0,0, 0,0,0, 0,1,0, 1,1,0};
    static int [] _n_bar = {1,1,0, 1,0,0, 1,1,1, 1,1,1};
    static int [][] forbidden_len_4 = {_p4, _n, _n_bar};

    // forbidden subgraphs of length 3:
    static int [] _p3 = {1,0, 0,1, 0,0};
    static int [] _a  = {1,0, 0,1, 0,1};
    static int [] _b  = {0,0, 1,1, 0,1};
    static int [] _c3 = {1,0, 0,1, 1,0};
    static int [] _d3 = {1,1, 0,1, 1,0};
    static int [][] forbidden_len_3 = {_p3, _a, _b, _c3, _d3};


    // input data
    DirectedGraph<String, DefaultEdge> inputGraph;
    // parameters
    int [] parameters;

    // number of species todo??
    int noOfSpecies;

    // CPLEX solver
    IloCplex solver;
    // objective function
    IloNumExpr objFn;

    // number of vertices
    private int vertexCount;

    // orthology variables -> Adjacency Matrix?
    private IloIntVar[][] E;

    // sets the time limit (default: 2h), if given by parameter #1
    private int timelimit;

    /**
     * Constructs a new Cplex-Solver for the Di-Cograph-Editing Problem.
     * @param inputGraph the directed graph to be converted to a Di-Cograph
     * @param parameters the parameter array:
     * @throws IloException
     */

    public CplexDiCographEditingSolver(DirectedGraph<String, DefaultEdge> inputGraph, int [] parameters ) throws IloException {
        this.inputGraph = inputGraph;
        this.parameters = parameters;
        this.solver = new IloCplex();

        this.solver.setName("DiCographEditingSolver");
    }

    public String solve() throws IloException{
        this.solver.setName("CplexCographSolver");

        // initialize boolean variables as "E_x,y"
        this.E = new IloIntVar[vertexCount][vertexCount];
        for (int x = 0; x< vertexCount; x++){
            for (int y = x+1; y< vertexCount; y++){
                this.E[x][y]= solver.boolVar("E_"+x+","+y);
            }
        }

        // want the vertex set of the graph in sorted order for easier display and to uniquely define the matrix
        TreeSet<String> sortedVertexSet = new TreeSet<>(inputGraph.vertexSet());
        DefaultEdge currentEdge;
        int x = 0, y = 0, i = 0;

        // No diagonal entries as self-loops are excluded:
        int noOfAdjacencies = (this.vertexCount *(this.vertexCount -1));
        IloNumExpr symDiff1Expr[] = new IloNumExpr[noOfAdjacencies];
        IloNumExpr symDiff2Expr[] = new IloNumExpr[noOfAdjacencies];

        // Initializes the symmetric difference for the Cograph Computation
        for(String sourceVertex : sortedVertexSet){
            for(String targetVertext : sortedVertexSet) {
                if(!sourceVertex.equals(targetVertext)) {
                    DefaultEdge edge = inputGraph.getEdge(sourceVertex, targetVertext);
                    int hasEdge =  edge == null ? 0 :1;
                    symDiff1Expr[i] = solver.prod((1 - hasEdge), E[x][y]);
                    symDiff2Expr[i] = solver.prod(hasEdge, solver.diff(1, E[x][y] ));
                    i++;
                    y++;
                }
            }
            x++;
        }
        this.objFn = solver.sum(solver.sum(symDiff1Expr),solver.sum(symDiff2Expr));

        // Maximum number of edge edits todo: was ist das???
        int cographDistance = parameters[0];
        if(cographDistance > 0) {
            solver.addLe(this.objFn, noOfAdjacencies * cographDistance);
        }

        solver.addMinimize(this.objFn);

        // lazy constraints for each forbidden subgraph
        for (int w = 0; w<this.vertexCount; w++){
            for (x = 0; x<this.vertexCount; x++){
                if (w!=x){
                    for (y = 0; y<this.vertexCount; y++){
                        if (w!=y && x!=y){
                            // subgraphs of length 3:

                            // subgraphs of length 4:
                            for (int z = w+1; z<this.vertexCount; z++){
                                if (x!=z && y!=z){
                                    IloIntVar vars[] = {
                                                     E[w][x], E[w][y], E[w][z],
                                            E[x][w],          E[x][y], E[x][z],
                                            E[y][w], E[w][x],          E[y][z],
                                            E[z][w], E[z][x], E[z][y]
                                    };
                                    for( int[] vals : forbidden_len_4) {
                                        IloRange range = solver.le(solver.scalProd(vals, vars), 2, "P4_" + w + "," + x + "," + y + "," + z);
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

        if (this.parameters.isCographAllSolutions()){
            solver.setParam(IloCplex.DoubleParam.SolnPoolAGap, 0.0);
            solver.setParam(IloCplex.IntParam.SolnPoolIntensity, 4);
            solver.setParam(IloCplex.IntParam.PopulateLim, 100);
            solver.populate();
        }
        */

        boolean ret = solver.solve();
        String solution = "none";
        if (ret) {
            solution = getSolution();
        }
        // free memory
        solver.end();

        return solution;

    }


    /**
     * @return the current solution, i.e. the values of xVariables as string
     * @throws IloException
     */
    private String getSolution() throws  IloException {
        int noSolutions = solver.getSolnPoolNsolns();
        String solution = "";
        double bestObjectiveValue = solver.getBestObjValue();
        this.gf.setCographEditDistance((int) Math.round(bestObjectiveValue));
        for (int solutionId=0; solutionId<noSolutions; solutionId++){
            if (solver.getObjValue(solutionId)<=bestObjectiveValue){
                Boolean[][] cograph = new Boolean[this.vertexCount][this.vertexCount];
                solution += "Orthology:\n";
                for (int x = 0; x<this.vertexCount; x++){
                    for (int y = 0; y<this.vertexCount; y++){
                        double variable = 0;
                        if (x!=y){
                            variable = solver.getValue(getE(x,y), solutionId);
                        }
                        cograph[x][y]=(variable>0.5);
                        solution+=variable<=0.5?"0 ":"1 ";
                    }
                    solution+="\n";
                }
                gf.addCograph(cograph);
                solution+="\n\n";
                solution+=solver.getObjValue(solutionId)+"\n";
            }
        }
        return solution;
    }

}