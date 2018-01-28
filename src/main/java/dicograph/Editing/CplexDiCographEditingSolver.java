package dicograph.Editing;

import com.google.common.io.ByteStreams;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Logger;

import dicograph.utils.Parameters;
import dicograph.utils.WeightedEdge;
import ilog.concert.IloException;
import ilog.concert.IloIntVar;
import ilog.concert.IloNumExpr;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

/**
 *   Blueprint from package ParaPhylo by Mark Hellmuth. Adapted for Directed Graphs.
 *
 *   http://pacosy.informatik.uni-leipzig.de/209-0-Downloads.html

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

class CplexDiCographEditingSolver {

    // input data. Also works, if has self-loops (will be ignored)
    private final SimpleDirectedGraph<Integer, DefaultEdge> inputGraph;
    private final TreeSet<Integer> sortedVertexSet;
    // parameters
    private Parameters parameters;

    private List<SimpleDirectedGraph<Integer, DefaultEdge>> solutionGraphs;
    private List<Double> editingDistances;

    // CPLEX solver
    private IloCplex solver;
    // objective function
    private IloNumExpr objFn;

    private long bestObjectiveValue;
    // number of vertices
    private final int vertexCount;

    // Adjacency Matrix
    private IloIntVar[][] E;

    private final double[][] weightMatrix;
    private List<List<WeightedEdge>> solutionEdgeEdits;
    private Logger log;

    /**
     * Constructs a new Cplex-Solver for the Di-Cograph-Editing Problem.
     * @param inputGraph the directed graph to be converted to a Di-Cograph
     * @param parameters the parameter array:
     * @throws IloException
     */
    CplexDiCographEditingSolver(SimpleDirectedGraph<Integer, DefaultEdge> inputGraph, Parameters parameters, Logger log) throws IloException {
        this(inputGraph,parameters,null,log);
    }


    CplexDiCographEditingSolver(SimpleDirectedGraph<Integer, DefaultEdge> inputGraph, Parameters params, double[][] weights, Logger logger) throws IloException {
        this.inputGraph = inputGraph;
        // want the vertex set of the graph in sorted order for easier display and to uniquely define the matrix
        sortedVertexSet = new TreeSet<>(inputGraph.vertexSet()); // todo: not necessary!
        vertexCount = sortedVertexSet.size();

        solver = new IloCplex();
        solver.setName("DiCographEditingSolver");
        solutionGraphs = new ArrayList<>();
        editingDistances = new ArrayList<>();

        parameters = params;
        log = logger;
        weightMatrix = weights;
        solutionEdgeEdits = new LinkedList<>();

    }

    public List<SimpleDirectedGraph<Integer,DefaultEdge>> solve() throws IloException{

        // initialize boolean variables as "E_x,y"
        int x,y;
        this.E = new IloIntVar[vertexCount][vertexCount];
        for (x = 0; x< vertexCount; x++){
            for (y = 0; y< vertexCount; y++){
                this.E[x][y]= solver.boolVar("E_"+x+","+y);
            }
        }


        // No diagonal entries as self-loops are excluded:
        int noOfAdjacencies = (this.vertexCount *(this.vertexCount -1));
        IloNumExpr symDiff1Expr[] = new IloNumExpr[noOfAdjacencies];
        IloNumExpr symDiff2Expr[] = new IloNumExpr[noOfAdjacencies];
        initSymDiff(symDiff1Expr,symDiff2Expr);

        this.objFn = solver.sum(solver.sum(symDiff1Expr),solver.sum(symDiff2Expr));
        solver.addMinimize(objFn);

        int w, z, j, k;
        // lazy constraints for each forbidden subgraph
        for (w = 0; w<vertexCount; w++){
            for (x = 0; x<vertexCount; x++){
                if (w!=x){
                    for (y = 0; y<vertexCount; y++){
                        if (w!=y && x!=y){
                            IloIntVar vars_3[] = {
                                    E[w][x], E[w][y],
                                    E[x][w],          E[x][y],
                                    E[y][w], E[y][x]
                            };
                            // subgraphs of length 3:
                            for(j = 0; j< ForbiddenSubgraph.len_3.length; j++){

                                IloRange range = solver.le(solver.scalProd(
                                        ForbiddenSubgraph.len_3[j].get(), vars_3), ForbiddenSubgraph.len_3[j].getThreshold(),
                                        ForbiddenSubgraph.len_3[j] + "_" + w + "," + x + "," + y);
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
                                    for(k = 0; k< ForbiddenSubgraph.len_4.length; k++) {

                                        IloRange range = solver.le(solver.scalProd(
                                                ForbiddenSubgraph.len_4[k].get(), vars_4), ForbiddenSubgraph.len_4[k].getThreshold(),
                                                ForbiddenSubgraph.len_4[k] + "_" + w + "," + x + "," + y + "," + z);
                                        solver.addLazyConstraint(range);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (parameters.getTimeOut()>0){
            solver.setParam(IloCplex.DoubleParam.TiLim, parameters.getTimeOut());
        }

        // solve
        if (!parameters.isVerbose()){
            solver.setOut( ByteStreams.nullOutputStream() );
        }


        // want all solutions
        if (parameters.getSolutionGap() >= 0){
            solver.setParam(IloCplex.DoubleParam.SolnPoolAGap, parameters.getSolutionGap()); // should be 0 to only get the best solutions.
            solver.setParam(IloCplex.IntParam.SolnPoolIntensity, 4);
            solver.setParam(IloCplex.IntParam.PopulateLim, 100);
            solver.populate();
        }

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

    private void initSymDiff(IloNumExpr symDiff1Expr[], IloNumExpr symDiff2Expr[]) throws IloException{
        DefaultEdge edge;
        int i = 0;
        for(int sourceVertex = 0; sourceVertex < vertexCount; sourceVertex++){
            for(int targetVertext = 0; targetVertext < vertexCount; targetVertext++) {
                if(sourceVertex != targetVertext) {
                    edge = inputGraph.getEdge(sourceVertex, targetVertext);
                    int hasEdge =  edge == null ? 0 :1;
                    double weight = weightMatrix == null ? 1.0 : weightMatrix[sourceVertex][targetVertext];
                    // new: add weights!
                    symDiff1Expr[i] = solver.prod( weight*(1 - hasEdge), E[sourceVertex][targetVertext]);
                    symDiff2Expr[i] = solver.prod(weight*hasEdge, solver.diff(1, E[sourceVertex][targetVertext] ));
                    i++;
                }
            }
        }
    }

    /**
     * @return the current solution, i.e. the values of xVariables as string
     * @throws IloException
     */
    private String getSolution() throws  IloException {
        int noSolutions = solver.getSolnPoolNsolns();
        StringBuilder solution = new StringBuilder();
        bestObjectiveValue = Math.round(solver.getBestObjValue());

        log.fine("Solution status = " + solver.getStatus());
        log.fine("CographEditDistance: " + bestObjectiveValue);



        for (int solutionId=0; solutionId<noSolutions; solutionId++){
            if (Math.round(solver.getObjValue(solutionId)) <=bestObjectiveValue + parameters.getSolutionGap() + 1){

                // initialize JGraph with same vertex-Set:
                SimpleDirectedGraph<Integer, DefaultEdge> solutionGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
                for(int vertex = 0; vertex < vertexCount; vertex++){
                    solutionGraph.addVertex(vertex);
                }
                List<WeightedEdge> edgeEdits = new LinkedList<>();

                solution.append("Adjacency Matrix:\n");

                for (int vertex_x = 0; vertex_x < vertexCount; vertex_x++ ){
                    for (int vertex_y = 0; vertex_y < vertexCount; vertex_y++){
                        double variable = 0;
                        if (vertex_x!=vertex_y){
                            variable = solver.getValue(E[vertex_x][vertex_y], solutionId);
                        }
                        boolean hasEdge = variable>0.5;
                        solution.append( hasEdge ?"1 " : "0 ");
                        boolean edgeEdit;
                        if(hasEdge){
                            solutionGraph.addEdge(vertex_x, vertex_y);
                            edgeEdit = !inputGraph.containsEdge(vertex_x,vertex_y);
                        } else {
                            edgeEdit = inputGraph.containsEdge(vertex_x,vertex_y);
                        }
                        if(edgeEdit){
                            WeightedEdge editEdge = new WeightedEdge(vertex_x,vertex_y);
                            if(weightMatrix != null){
                                editEdge.setWeight(weightMatrix[vertex_x][vertex_y]);
                            }
                            edgeEdits.add(editEdge);
                        }
                    }
                    solution.append("\n");
                }
                solutionEdgeEdits.add(edgeEdits);
                editingDistances.add(solver.getObjValue(solutionId));
                solutionGraphs.add(solutionGraph);
                solution.append("\n\n")
                        .append(solver.getObjValue(solutionId))
                        .append("\n");
            }
        }
        return solution.toString();
    }

    // These two go together (same order)
    public List<Double> getEditingDistances() {
        return editingDistances;
    }

    public List<List<WeightedEdge>> getSolutionEdgeEdits() {
        return solutionEdgeEdits;
    }

    public List<SimpleDirectedGraph<Integer, DefaultEdge>> getSolutionGraphs() {
        return solutionGraphs;
    }

    public long getBestObjectiveValue() {
        return bestObjectiveValue;
    }
}