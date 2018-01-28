package dicograph;


import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.DOTImporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.ImportException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import dicograph.Editing.ForbiddenSubgraph;
import dicograph.Editing.MetaEditor;
import dicograph.Editing.Solution;
import dicograph.graphIO.GraphGenerator;
import dicograph.graphIO.IntegerComponentNameProvider;
import dicograph.graphIO.JGraphAdjecencyImporter;
import dicograph.graphIO.SimpleMatrixExporter;
import dicograph.graphIO.SimpleMatrixImporter;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.MDTree;
import dicograph.utils.Parameters;
import dicograph.utils.VerySimpleFormatter;
import ilog.concert.IloException;


/**
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
public class Main {

    private final static String logFolder = "logs/";
    private final static String graphFolder = "graphs/";

    public static void main(String[] args) throws Exception {

        Parameters command = new Parameters(args);
        command.parse();
        String exportFormat = ".dot";

        if(!command.isValid())
            return;

        // remove defaults
        Logger log = Logger.getLogger( "DiCographLog" );
        for (Handler iHandler : log.getParent().getHandlers()) {
            log.getParent().removeHandler(iHandler);
        }
        VerySimpleFormatter myFormatter = new VerySimpleFormatter();

        if(command.isVerbose() || command.isTest()) {
            // log to console output
            Handler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(myFormatter);
            consoleHandler.setLevel( command.getLogLevel() );
            log.addHandler( consoleHandler );
            log.getParent().setLevel( command.getLogLevel() );

            if(command.isTest()){
                String[] params = command.getTestParams();
                int mTrials = Integer.parseInt(params[0]);
                int nVertices = Integer.parseInt(params[1]);
                int kDisturb = Integer.parseInt(params[2]);
                StringBuilder allGraphs = new StringBuilder();

                TreeMap<Integer, List<Integer>> ilpCostToLazyCorrects = new TreeMap<>();
                TreeMap<Integer, List<Integer>> bestCostToLazyCost = new TreeMap<>();
                List<Double> tripleDistances = new ArrayList<>(mTrials);

                Map<ForbiddenSubgraph,Integer> subgraphCounts =new LinkedHashMap<>();

                int lazyNotOptimal = 0;
                int lazyFailures = 0;
                for (int i = 1; i <= mTrials; i++) {
                    MetaEditor editor;
                    try {
                        editor = testRun(log, consoleHandler, command, nVertices, kDisturb, allGraphs);

                        // todo: what if lazy fails???
                        // get the results for statistics
                        if(!command.isMDOnly() && command.isLazy() && command.isIlpMD()) {

                            int best = editor.getBestCost();
                            int lazy = editor.getLazyCost();

                            // only if not optimal:
                            if(lazy > best) {
                                lazyNotOptimal++;
                                ilpCostToLazyCorrects.putIfAbsent(best, new LinkedList<>());
                                ilpCostToLazyCorrects.get(best).add(editor.getLazyCorrectRun());
                            }

                            if(editor.getLazySolution() != null) {
                                bestCostToLazyCost.putIfAbsent(editor.getBestCost(), new LinkedList<>());
                                bestCostToLazyCost.get(editor.getBestCost()).add(editor.getLazyCost());

                                // Trees:
                                tripleDistances.add(editor.getLazyTTDistance());
                            } else {
                                lazyFailures++;
                            }

                            // stats
                            for(Map.Entry<ForbiddenSubgraph,Integer> fsubEntry : editor.getSubgraphCounts().entrySet()){
                                int newCount = subgraphCounts.getOrDefault(fsubEntry.getKey(),0) + fsubEntry.getValue();
                                subgraphCounts.put(fsubEntry.getKey(), newCount);
                            }
                        }

                    } catch ( Exception e){
                        e.printStackTrace(System.err);
                        log.severe(e.getMessage());
                        System.err.println("Error occured after " + i + " successful runs.");
                        break;
                    }
                    System.out.println("Yay, " + i +" test runs went successful!");
                }

                log.info("All generated Graphs:");
                log.info(allGraphs.toString().substring(0,allGraphs.length()-1));
                log.info("ILP to correct lazy (when not optimal): " + ilpCostToLazyCorrects);
                log.info( "Best cost to lazy: " + bestCostToLazyCost);
                log.info("Tree distances: " + tripleDistances);
                log.info("Lazy failed " + lazyFailures + " times, worse than ILP " + lazyNotOptimal + " times.");
                log.info("Subgraph stats: " + subgraphCounts);
                return;
            }
        }

        String timeStamp = new SimpleDateFormat("MM-dd_HH:mm:ss:SSS").format(Calendar.getInstance().getTime());
        // writes the log
        File logFile = new File(logFolder + timeStamp +".log");
        FileHandler fileHandler = new FileHandler(logFile.getPath());
        fileHandler.setFormatter( myFormatter);
        fileHandler.setLevel( command.getLogLevel());
        log.addHandler(fileHandler);
        log.getParent().setLevel( command.getLogLevel() );


        // no test -> have an input file
        String inFilePath = command.getInFileAbsPath();
        if(inFilePath == null || inFilePath.isEmpty()){
            System.err.println("No valid input file: " + inFilePath);
            return;
        }

        File importFile = new File(inFilePath);
        SimpleDirectedGraph<Integer, DefaultEdge> importGraph;
        if(inFilePath.endsWith(".txt")) {
            importGraph=SimpleMatrixImporter.importIntGraph(importFile);
            exportFormat = ".txt";

        } else if(inFilePath.endsWith(".jtxt")){
            importGraph = JGraphAdjecencyImporter.importIntGraph(importFile);
            // read the log if you want the graph

        } else if(inFilePath.endsWith(".dot")){
            exportFormat = ".dot";
            try(InputStream inStream = Files.newInputStream(importFile.toPath())) {

                BufferedReader inReader = new BufferedReader(new InputStreamReader(inStream));
                importGraph = new SimpleDirectedGraph<>(DefaultEdge.class);
                DOTImporter<Integer, DefaultEdge> importer = new DOTImporter<>(
                        (label, attributes) -> Integer.parseInt(label),
                        (from, to, label, attributes) -> importGraph.addEdge(from, to));
                importer.importGraph(importGraph, inReader);

            } catch (IOException e){
                System.err.println("For file: " + importFile.toString() + "\n" + e.toString());
                return;
            }
        } else {
            System.err.println("No valid input file: " + inFilePath);
            return;
        }

        if(command.isMDOnly()){
            DirectedMD directedMD = new DirectedMD(importGraph, log, false);
            MDTree res = directedMD.computeModularDecomposition();
            log.info(()->MDTree.beautify(res.toString()));
            System.out.println(res.exportAsDot());

        } else {
            MetaEditor editor = new MetaEditor(importGraph, command, log);
            List<Solution> solutions = editor.computeSolutionsForMethods();
            if(solutions.isEmpty()){
                System.err.println("No solution found! Try different methods or adjust parameters.");

            } else {
                Solution sol = solutions.get(0);
                String exportPath = command.getOutFileAbsPath();
                if(exportPath.isEmpty()){
                    exportPath = graphFolder + timeStamp + "_edit-cost_" + sol.getCost() + exportFormat;
                } else {
                    int index = exportPath.lastIndexOf(".");
                    exportFormat = exportPath.substring(index);
                }

                exportSolution(sol, exportFormat, exportPath);
            }
        }
    }

    private static void exportSolution(Solution sol, String exportFormat, String exportPath)
    throws ExportException, FileNotFoundException{

        File expFile = new File(exportPath);
        if(exportFormat.equals(".txt")){
            SimpleMatrixExporter<Integer,DefaultEdge> myExporter = new SimpleMatrixExporter<>();
            myExporter.exportGraph(sol.getGraph(), expFile);

        }  else if(exportFormat.equals(".dot")){
            DOTExporter<Integer,DefaultEdge> myExporter = new DOTExporter<>(new IntegerComponentNameProvider<>(), null, null);
            PrintWriter writer = new PrintWriter(expFile);
            myExporter.exportGraph(sol.getGraph(), writer);
        }
    }

    private static MetaEditor editingTest(Logger log, SimpleDirectedGraph<Integer,DefaultEdge> importGraph, String expPath, MDTree cotree, Parameters p)
            throws IOException, ImportException, InterruptedException, IloException, ExportException{
        int cost;
        double dist;

        MetaEditor testMeta = new MetaEditor(importGraph, p, log);
        testMeta.setCotreeTriples(cotree.getTriples());
        List<Solution> solutions = testMeta.computeSolutionsForMethods();
        if(!solutions.isEmpty()){

            if(p.isLazy() && p.isIlpMD()){
                cost = testMeta.getLazyCost();
                dist = testMeta.getLazyCorrectRun();
            } else {
                cost = solutions.get(0).getCost();
                dist = solutions.get(0).getTreeDistance(); // might _not_ be the best!
            }

            String solName = expPath + "_edit-cost_" + cost + "_TT-dist_" + new DecimalFormat("0.000000").format(dist) + ".txt";
            exportSolution(solutions.get(0), ".txt", solName);
            System.out.println("Exported solution to: " + solName);
        }

        return testMeta;
    }

    private static MetaEditor testRun( Logger log, Handler baseHandler, Parameters p, int nVertices, int nDisturb, StringBuilder allPaths)
            throws ImportException, InterruptedException, IloException, ExportException, IOException{

        String timeStamp = new SimpleDateFormat("MM-dd_HH:mm:ss:SSS").format(Calendar.getInstance().getTime());
        String fileName = "randDigraph_n_" + nVertices + "_edits_" + nDisturb + "_" + timeStamp;

        // writes the log
        File logFile = new File(logFolder + fileName +".log");
        FileHandler fileHandler = new FileHandler(logFile.getPath());
        fileHandler.setFormatter(baseHandler.getFormatter());
        fileHandler.setLevel( baseHandler.getLevel() );
        log.addHandler(fileHandler);

        GraphGenerator gen = new GraphGenerator(log);
        SimpleDirectedGraph<Integer, DefaultEdge> g_d = new SimpleDirectedGraph<>(DefaultEdge.class);
        gen.generateRandomDirectedCograph(g_d, nVertices,p.getCographProbabilities());

        // for metric:
        MDTree cotree = null;
        if(!p.isMDOnly()) {
            DirectedMD cotreeMD = new DirectedMD(g_d, log, false);
            cotree = cotreeMD.computeModularDecomposition();
            log.info("Generated Cotree: " + MDTree.beautify(cotree.toString()));
        }

        gen.disturbDicograph(g_d, nDisturb);

        // export the graph for debug purposes
        String matrixPath = graphFolder + fileName + "_original.txt";
        allPaths.append("\"").append(matrixPath).append("\", ");
        File expfile = new File(matrixPath);
        SimpleMatrixExporter<Integer, DefaultEdge> myExporter = new SimpleMatrixExporter<>();
        myExporter.exportGraph(g_d, expfile);
        System.out.println(String.format("Generated random Dicograph with %s vertices and %s random edge-edits.", nVertices, nDisturb));
        System.out.println("Exported Matrix to: " + matrixPath);
        MetaEditor ret = null;

        if(!p.isMDOnly()){
            ret = editingTest(log,g_d,graphFolder + fileName, cotree, p);
            System.out.println("Finished Editing Test. Log written to:");

        } else {
            log.info("Started modular decomposition");
            DirectedMD testMD = new DirectedMD(g_d, log, false);
            testMD.computeModularDecomposition();
            System.out.println("Finished modular decomposition. Log written to:");
        }
        System.out.println(fileName + ".log");

        // clear this handler, I want separate logfiles.
        fileHandler.close();
        log.removeHandler(fileHandler);

        return ret;
    }

    // use gap to 100%, same height for all!
    // for Matlab, need format:
    // x = [7,8,9,10] or categorical( '7 = #7s, 8 = #8s,...' );
    // y = [ 7_perc = 7_sum / 7* 7_count , 1 - 7_perc ; 8_avg, 8_gap; ...]
    // bar(x,y,'stacked')


}
