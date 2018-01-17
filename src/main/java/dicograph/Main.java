package dicograph;


import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.io.Attribute;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.DOTImporter;
import org.jgrapht.io.EdgeProvider;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.ImportException;
import org.jgrapht.io.VertexProvider;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import dicograph.Editing.CplexDiCographEditingSolver;
import dicograph.Editing.MetaEditor;
import dicograph.Editing.Solution;
import dicograph.graphIO.GraphGenerator;
import dicograph.graphIO.IntegerVertexFactory;
import dicograph.graphIO.JGraphAdjecencyImporter;
import dicograph.graphIO.SimpleMatrixExporter;
import dicograph.graphIO.SimpleMatrixImporter;
import dicograph.graphIO.TedFormatExporter;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.GraphHandle;
import dicograph.modDecomp.MDTree;
import dicograph.utils.Parameters;
import dicograph.utils.SortAndCompare;
import dicograph.utils.VerySimpleFormatter;
import ilog.concert.IloException;



/**
 * Created by Fynn Leitow on 02.10.17.
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
            consoleHandler.setLevel(command.getLogLevel());
            log.addHandler( consoleHandler );

            if(command.isTest()){
                String[] params = command.getTestParams();
                int mTrials = Integer.parseInt(params[0]);
                int nVertices = Integer.parseInt(params[1]);
                int kDisturb = Integer.parseInt(params[2]);
                StringBuilder allGraphs = new StringBuilder();

                for (int i = 1; i <= mTrials; i++) {
                    if (!testRun(log, consoleHandler, command, nVertices, kDisturb, allGraphs)) {
                        System.err.println("Error occured after " + i + " successful runs.");
                        break;
                    }
                    System.out.println("Yay, " + i +" test runs went successful!");
                }

                log.info("All generated Graphs:");
                log.info(allGraphs.toString().substring(0,allGraphs.length()-1));
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

                File expFile = new File(exportPath);
                if(exportFormat.equals(".txt")){
                    SimpleMatrixExporter<Integer,DefaultEdge> myExporter = new SimpleMatrixExporter<>();
                    myExporter.exportGraph(sol.getGraph(), expFile);

                }  else if(exportFormat.equals(".dot")){
                    DOTExporter<Integer,DefaultEdge> myExporter = new DOTExporter<>();
                    PrintWriter writer = new PrintWriter(expFile);
                    myExporter.exportGraph(sol.getGraph(), writer);
                }
            }
        }



    }

    private static int editingTest(Logger log, String graphPath, Parameters p) throws Exception{
        int cost = -1;

        SimpleDirectedGraph<Integer, DefaultEdge> importGraph = SimpleMatrixImporter.importIntGraph(new File(graphPath));
        log.info("For file: " + graphPath);

        MetaEditor testMeta = new MetaEditor(importGraph, p, log);
        List<Solution> solutions = testMeta.computeSolutionsForMethods();
        if(!solutions.isEmpty()){
            cost = solutions.get(0).getCost();
        }

        return cost;
    }

    private static boolean testRun( Logger log, Handler baseHandler, Parameters p, int nVertices, int nDisturb, StringBuilder allPaths)
            throws IOException, ExportException, InterruptedException, ImportException{

        boolean ok = true;
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
        gen.generateRandomDirectedCograph(g_d, nVertices, true);
        gen.disturbDicograph(g_d, nDisturb);

        // export the graph for debug purposes
        String matrixPath = graphFolder + fileName + "_original.txt";
        allPaths.append("\"").append(matrixPath).append("\", ");
        File expfile = new File(matrixPath);
        SimpleMatrixExporter<Integer, DefaultEdge> myExporter = new SimpleMatrixExporter<>();
        myExporter.exportGraph(g_d, expfile);
        System.out.println(String.format("Generated random Dicograph with %s vertices and %s random edge-edits.", nVertices, nDisturb));
        System.out.println("Exported Matrix to: " + matrixPath);

        if(!p.isMDOnly()){
            try {
                int cost = editingTest(log,fileName + "_original.txt", p);
                ok = cost >= 0;
            } catch (Exception e){
                ok = false;
                log.severe(e.toString());
                e.printStackTrace(System.err);
            }
            System.out.println("Finished Editing Test. Log written to:");

        } else {
            log.info("Started modular decomposition");
            try {
                DirectedMD testMD = new DirectedMD(g_d, log, false);
                testMD.computeModularDecomposition();
            } catch (IllegalStateException | AssertionError e) {
                ok = false;
                log.severe(e.toString());
                e.printStackTrace(System.err);
            }
            System.out.println("Finished modular decomposition. Log written to:");
        }
        System.out.println(fileName + ".log");

        // clear this handler, I want separate logfiles.
        fileHandler.close();
        log.removeHandler(fileHandler);

        return ok;
    }

    private static void MDtestFromFile(Logger log, String importFilePath, boolean matrix) throws Exception {

        File importFile = new File(importFilePath);
        SimpleDirectedGraph<Integer, DefaultEdge> importGraph;
        if(matrix) {
            importGraph=SimpleMatrixImporter.importIntGraph(importFile);
        } else {
            importGraph = JGraphAdjecencyImporter.importIntGraph(importFile);
        }
        DOTExporter<Integer, DefaultEdge> exporter = new DOTExporter<>();
        Writer writer = new StringWriter();
        exporter.exportGraph(importGraph, writer);

        log.info("Computing MD for graph:" + importFilePath);


        DirectedMD testMD = new DirectedMD(importGraph, log, false);
        MDTree res = testMD.computeModularDecomposition();
        log.info(MDTree.beautify(res.toString()));

    }


//        List<String> tests_100 = Arrays.asList("testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:09:488_original.txt",
//                "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:11:625_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:12:174_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:13:053_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:14:209_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:14:625_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:16:675_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:18:649_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:18:947_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:19:297_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:19:786_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:20:011_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:20:467_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:22:305_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:27:114_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:28:717_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:36:891_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:37:980_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:43:031_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:43:533_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:45:925_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:46:284_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:46:721_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:47:146_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:48:190_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:48:802_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:54:570_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:55:079_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:00:279_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:02:106_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:03:080_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:03:621_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:04:226_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:04:634_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:08:463_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:08:710_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:08:863_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:09:140_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:15:812_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:16:588_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:17:146_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:20:153_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:20:872_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:29:679_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:35:015_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:35:496_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:36:363_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:37:150_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:37:918_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:38:346_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:39:003_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:39:498_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:45:942_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:48:470_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:49:160_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:51:841_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:52:381_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:52:562_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:57:450_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:58:906_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:59:741_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:00:118_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:01:889_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:02:386_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:02:875_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:03:304_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:03:847_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:04:339_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:07:465_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:07:846_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:11:802_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:12:198_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:12:775_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:15:813_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:16:387_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:17:060_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:17:639_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:19:170_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:19:480_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:20:282_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:22:025_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:25:339_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:27:107_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:27:763_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:29:462_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:32:493_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:33:141_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:33:331_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:33:543_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:36:638_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:37:289_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:37:924_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:38:903_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:41:207_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:43:292_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:43:820_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:44:776_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:46:077_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:48:338_original.txt", "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:50:533_original.txt");
//
//
//        //
//        // SEVERE:
//        //
//        String reTestPath = "testGraphs/DMDvery/";
//        String again = "testGraphs/DMDvery/randDigraph_n_20_edits_5_01-06_22:06:53:840_original.txt";
//        String not_among = reTestPath + "randDigraph_n_20_edits_5_01-06_22:02:00:398_original.txt";
//        String warning_same = reTestPath + "randDigraph_n_20_edits_5_01-06_21:51:50:027_original.txt";
//        String midnight_err = reTestPath + "randDigraph_n_40_edits_8_01-07_00:07:40:018_original.txt";
//        String bad_fucker = "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:28:717_original.txt";
//        // last of tests_100: Gets optimum when doing both continues!
//        String rekt_19 = "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:43:533_original.txt"; // finds cost 7 easily without threshold.
//        // ILP gives: {7=[[(0,6), (1,2), (4,9), (9,3), (10,16), (11,2), (11,5)], [(0,15), (1,2), (4,9), (9,3), (10,16), (11,2), (11,5)], [(1,2), (4,9), (6,15), (9,3), (10,16), (11,2), (11,5)]]}
//
//        // For file: testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:19:28:717_original.txt <- assertion error
//        String cost12 = "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:21:11:802_original.txt";
//
//        String primesSmallEnough = "testGraphs/DMDvery/randDigraph_n_40_edits_8_01-06_23:20:08:710_original.txt";


    //editingTest(log, primesSmallEnough,command);




//        Map<Integer,Integer> costToCount = new TreeMap<>();
//        for (int i = 0; i < tests_100.size() ; i++) {
//            String s = tests_100.get(i);
//            int cost = editingTest(log,s,false, command);
//            costToCount.putIfAbsent(cost,0);
//            costToCount.put(cost, costToCount.get(cost) + 1);
//            if(i == 20)
//                break;
//        }
//        System.out.println("Results: " + costToCount);



//        StringBuilder allPaths = new StringBuilder();
//
//        for (int i = 0; i < 100; i++) {
//            if(!testRun(true,log,consoleHandler,40,8,true, allPaths)) {
//                System.out.println("Error occured after " + i + " successful runs.");
//                break;
//            }
//            System.out.println("Proud you should be, " + i +" test runs went successful!");
//        }
//
//        log.info("All Paths:");
//        log.info(allPaths.toString().substring(0,allPaths.length()-1));


    /*

    static void md10test() throws Exception{
        String folder = "testGraphs/";

        String mderror_10 = folder + "randDigraph_n_10_edits_5_11-17_13:54:28:564_original.txt";
        //MDtestFromFile(log, mderror_10,true);

        SimpleDirectedGraph<Integer, DefaultEdge> mderr10G = SimpleMatrixImporter.importIntGraph( new File(mderror_10));
        System.out.println(getG_s(mderr10G).toString());


        for( int i : Arrays.asList(6,5)){
            mderr10G.removeVertex(i);
        }
        List<Integer> vertices = new ArrayList<>(mderr10G.vertexSet());
        List<List<Integer>> all = allPermutations(vertices);
        System.out.println("Total: " + all.size());

        int permNum = 0;
        for( List<Integer> perm : all){
            permNum++;
            if(permNum % 100 != 0)
                continue;
            System.out.println("Permutation number: " + permNum);
            Integer[] permutation = new Integer[perm.size()];
            int count = 0;
            for( int i : perm){
                permutation[count] = i;
                count++;
            }
            TedFormatExporter<Integer,DefaultEdge> tedXp = new TedFormatExporter<>();
            tedXp.setPermutation(permutation);
            String expPath = "ted_md10Cut" + 0 + ".txt";
            System.out.println(expPath);
            File exporty = new File(expPath);

            tedXp.exportGraph(getG_s(mderr10G), exporty);

            mdTestOldNew(expPath);

        }
    }

    static Integer[] randPermutation(Integer[] start, SecureRandom random){


        SortAndCompare.shuffleList(start, random);
        StringBuilder str = new StringBuilder("Permutation: ");
        for (Integer aStart : start) {
            str.append(aStart).append(", ");
        }
        System.out.println(str);
        return start;
    }

    static List<List<Integer>> allPermutations(List<Integer> elements){
        int size = elements.size();
        int permCount = factorial(size);
        ArrayList<List<Integer>> ret = new ArrayList<>(permCount);


        perm1(new LinkedList<>(), elements,ret);

        return ret;



    }

    private static void perm1(List<Integer> pre, List<Integer> list,List<List<Integer>> allLists){
        int n = list.size();
        if(n==0)
            allLists.add(pre);
        else{
            for (int i = 0; i < n; i++) {
                ArrayList<Integer> prefix = new ArrayList<>(pre);
                prefix.add(list.get(i));
                ArrayList<Integer> newList = new ArrayList<>(list.subList(0,i));
                newList.addAll(list.subList(i+1,n));
                perm1(prefix, newList,allLists);
            }
        }
    }

    static int factorial(int n){
        if(n==1)
            return 1;
        else
            return n*factorial(n-1);
    }

    // do reproduce error in Tedder's Code
    static void reorder(Integer[] permutation, String filePath){

        File file = new File(filePath);
        try( InputStream inStream = Files.newInputStream(file.toPath()) ) {
            BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
            StringBuilder builder = new StringBuilder();
            in.lines().forEach( line -> builder.append(line).append("\n") );
            String oldContent = builder.toString();
            String newContent = "";

            for (int i = 0; i < permutation.length; i++) {
                String oldPos = String.valueOf(i);
                if(oldPos.length() == 1)
                    oldPos = "0" + oldPos;
                String newPos = String.valueOf(permutation[i]);
                if(newPos.length() == 1)
                    newPos = "0" + newPos;
                newContent = oldContent.replaceAll(oldPos, newPos);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath())) {
                writer.write(newContent, 0, newContent.length());
            } catch (IOException x) {
                System.err.format("IOException: %s%n", x);
            }

        } catch (IOException e){
            System.err.format("IOException: %s%n", e);
        }
    }



    static void compareMatrixAndRand(SimpleDirectedGraph<Integer, DefaultEdge> matrixGraph, SimpleDirectedGraph<Integer, DefaultEdge> randGraph) {
        // Error in Tedder's MD appeared here..
        SimpleGraph <Integer,DefaultEdge> matrixUndirected = new SimpleGraph<>(DefaultEdge.class);
        matrixGraph.vertexSet().forEach(  matrixUndirected::addVertex );
        for (DefaultEdge edge : matrixGraph.edgeSet()) {
            int source = matrixGraph.getEdgeSource(edge);
            int target = matrixGraph.getEdgeTarget(edge);
            if (!matrixUndirected.containsEdge(source, target)) {
                matrixUndirected.addEdge(source, target);
            }
        }

        SimpleGraph <Integer,DefaultEdge> randUndirected = new SimpleGraph<>(DefaultEdge.class);
        randGraph.vertexSet().forEach(  randUndirected::addVertex );
        for (DefaultEdge edge : randGraph.edgeSet()) {
            int source = randGraph.getEdgeSource(edge);
            int target = randGraph.getEdgeTarget(edge);
            if (!randUndirected.containsEdge(source, target)) {
                randUndirected.addEdge(source, target);
            }
        }


        System.out.println("Testing: from matrix");

        List<Integer> se_5_matrix = Arrays.asList(15,16,18,19);
        TreeSet<Integer> matrix_others = new TreeSet<>(matrixUndirected.vertexSet());
        matrix_others.removeAll(se_5_matrix);
        debugTesting(matrixUndirected,matrix_others,se_5_matrix);

        System.out.println("\nTesting: randomly created");

        List<Integer> se_4_rand = Arrays.asList(4,5,22,23);
        TreeSet<Integer> rand_others = new TreeSet<>(randUndirected.vertexSet());
        rand_others.removeAll(se_4_rand);
        debugTesting(randUndirected, rand_others, se_4_rand);

        System.out.println("\n*** test with directed ***\nFrom Matrix");
        debugTesting2(matrixGraph, matrix_others, se_5_matrix);
        System.out.println("\nFrom rand:");
        debugTesting2(randGraph, rand_others, se_4_rand);


        VF2GraphIsomorphismInspector isomorphismInspector = new VF2GraphIsomorphismInspector<>(matrixGraph,randGraph);
        System.out.println("Iso exists for Directed: " + isomorphismInspector.isomorphismExists());

        SimpleGraph<Integer,DefaultEdge> g_s_matrix = getG_s(matrixGraph);
        SimpleGraph<Integer,DefaultEdge> g_s_rand = getG_s(randGraph);

        System.out.println("\nTest from matrix:\n");
        debugTesting(g_s_matrix, matrix_others, se_5_matrix);
        System.out.println("\nTest from rand:\n");
        debugTesting(g_s_rand, rand_others, se_4_rand);

        VF2GraphIsomorphismInspector iso2 = new VF2GraphIsomorphismInspector<>(g_s_matrix,g_s_rand);
        System.out.println("Iso exists for G_s: " + iso2.isomorphismExists());
    }

    static SimpleGraph<Integer,DefaultEdge> getG_s (SimpleDirectedGraph<Integer,DefaultEdge> inputGraph){
        SimpleGraph<Integer,DefaultEdge> G_s = new SimpleGraph<>(DefaultEdge.class);
        inputGraph.vertexSet().forEach( G_s::addVertex );
        for (DefaultEdge edge : inputGraph.edgeSet()) {
            int source = inputGraph.getEdgeSource(edge);
            int target = inputGraph.getEdgeTarget(edge);
            if (!G_s.containsEdge(source, target)) {
                G_s.addEdge(source, target);
            }
        }
        return G_s;
    }

    static void debugTesting(SimpleGraph<Integer,DefaultEdge> graph, Set<Integer> otherVertices, List<Integer> moduleVertices){

        for (int i : otherVertices) {
            System.out.println("Checking: " + i + " with edges: " + graph.edgesOf(i));

            boolean first = true;
            boolean hasEdge = false;
            for(int j : moduleVertices){
                if(first){
                    hasEdge = graph.containsEdge(i, j);
                    first = false;
                } else {
                    if (hasEdge != graph.containsEdge(i, j)){
                        System.out.println("Error for edge {" + i + "," + j + "}, expected hasEdge == " + hasEdge);
                    }
                }
            }
        }
    }

    static void debugTesting2(SimpleDirectedGraph<Integer,DefaultEdge> graph, Set<Integer> otherVertices, List<Integer> moduleVertices){

        for (int i : otherVertices) {
            System.out.println("Checking: " + i + " with edges: " + graph.edgesOf(i));

            boolean first = true;
            boolean hasEdge = false;
            for(int j : moduleVertices){
                if(first){
                    hasEdge = graph.containsEdge(i, j) || graph.containsEdge(j,i);
                    first = false;
                } else {
                    if (hasEdge != (graph.containsEdge(i, j) || graph.containsEdge(j,i))) {
                        System.out.println("Error for edge {" + i + "," + j + "}, expected hasEdge == " + hasEdge);
                    }
                }
            }
        }
    }

    void cographTesting(Logger log) throws Exception{
        // Cograph Testing
        SimpleGraph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        GraphGenerator gen = new GraphGenerator(log);
        gen.generateRandomCograph(g,35);

        GraphHandle gHand = new GraphHandle(g);
        String g_res = gHand.getMDTree().toString();
        System.out.println("\nNew Code:\n" + MDTree.beautify(g_res));
        // Dicograph Testing:



        for( int i = 10; i< 50; i++) {
            SimpleDirectedGraph<Integer, DefaultEdge> g_d = new SimpleDirectedGraph<>(DefaultEdge.class);
            gen.generateRandomDirectedCograph(g_d, i, true);

            CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(g_d, new Parameters(new String[0]), log);
            List<SimpleDirectedGraph<Integer, DefaultEdge>> solutions = mySolver.solve();
            System.out.print(solutions.get(0));
            Double sol = mySolver.getEditingDistances().get(0);
            if(sol.intValue() > 0){
                throw new RuntimeException("Created Dicograph " + i + "not recognized as one!!!");
            }
        }
        System.out.println("\nAll graphs recognized as cographs!");

    }


    private static void testRNG(String prng) throws NoSuchAlgorithmException, NoSuchProviderException {
        SecureRandom sr1 = SecureRandom.getInstance(prng, "SUN");
        SecureRandom sr2 = SecureRandom.getInstance(prng, "SUN");
        sr1.setSeed(1);
        sr2.setSeed(1);
        boolean same = false;
        for (int i = 0; i < 10; i++) {
            double d1 = sr1.nextDouble();
            double d2 = sr2.nextDouble();
            System.out.println("1.: " + d1 + ", 2.: " + d2);
            if (sr1.nextDouble() == sr2.nextDouble()) {
                same = true;
            }
        }
        if(same) {
            System.out.println(prng + " appears to produce the same values with the same seed");
        } else {
            System.out.println(prng + " does not produce the same values with the same seed");
        }
    }


    static void mdTestCompareTwo(String filePath, String filePath2){
        //String filePath = args[0];

        System.out.println("*** Matrix case: file " + filePath + " ***\n");
        mdTestOldNew(filePath);

        System.out.println("\n*** Rand case: file " + filePath2 + " ***\n");

        mdTestOldNew(filePath2);
    }

    static void mdTestOld(String filePath){
        System.out.println("Loading:\n"+ filePath);
        GraphHandle g = new GraphHandle(filePath);

        String g_res = g.getMDTreeOld().toString();
        System.out.println("Result:\n" + MDTree.beautify(g_res) + "\n");
    }

    static void mdTestOldNew(String filePath){

        GraphHandle g = new GraphHandle(filePath);

        String g_res = g.getMDTreeOld().toString();
        System.out.println("Old Code:\n" + MDTree.beautify(g_res));

        GraphHandle g2 = new GraphHandle(filePath);
        String g2_res = g2.getMDTree().toString();
        System.out.println(g2.getGraph());
        System.out.println("\nNew Code:\n" + MDTree.beautify(g2_res));
    }

    void cplexTest(Logger log) throws ExportException, IloException, IOException {
        String filePath = "importFiles/sz_15_pr_30";
        File importFile = new File(filePath+ ".txt");
        SimpleDirectedGraph<Integer, DefaultEdge> importGraph = SimpleMatrixImporter.importIntGraph(importFile);

        System.out.print(importGraph + "\n");

        CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(importGraph, new Parameters(new String[0]), log);
        List<SimpleDirectedGraph<Integer,DefaultEdge>> solutions = mySolver.solve();

        int count = 1;
        for(SimpleDirectedGraph<Integer,DefaultEdge> cograph : solutions){

            SimpleMatrixExporter<Integer, DefaultEdge> myExporter = new SimpleMatrixExporter<>();
            File expfile = new File(filePath + "_solution_"+ count + ".txt");
            myExporter.exportGraph(cograph, expfile);
            count++;
        }
    }

    void randImportExportTest(Logger log) throws IOException, ExportException{
        GraphGenerator graphGenerator = new GraphGenerator(log);
        SimpleDirectedGraph<Integer, DefaultEdge> testGraph = graphGenerator.generateRandomGnp(11,0.3);
        System.out.println(testGraph.toString());

        // test output
        String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        File logFile = new File(timeLog + ".txt");

        // This will output the full path where the file will be written to...
        System.out.println(logFile.getCanonicalPath());

        //

        SimpleMatrixExporter<Integer, DefaultEdge> myExporter = new SimpleMatrixExporter<>();
        myExporter.exportGraph(testGraph, logFile);

        // try importing and exporting again:
        SimpleDirectedGraph<Integer,DefaultEdge> testGraph2 = SimpleMatrixImporter.importIntGraph(logFile);
        String exp2 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        File expfile = new File(exp2 + "_reimported" + ".txt");
        myExporter.exportGraph(testGraph2, expfile);
    }

    */
    // This was to reproduce Tedder's MD-Err
//        Integer[] start = new Integer[sz];
//        for (int i = 0; i < sz; i++) {
//            start[i] = i;
//        }
//
//        for (int i = 0; i < 5; i++) {
//            Integer[] permutation = randPermutation(start,random);
//
//            TedFormatExporter<Integer,DefaultEdge> tedXp = new TedFormatExporter<>();
//            tedXp.setPermutation(permutation);
//
////            String filePath = "ted_matrix_case_" + i + ".txt";
////            String filePath2 = "ted_rand_case_" +i + ".txt";
////
////            File expfile = new File(filePath);
////            tedXp.exportGraph(getG_s(matrixGraph),expfile );
////            File expfile2 = new File(filePath2);
////            tedXp.exportGraph(getG_s(randGraph),expfile2);
//
//
//            String expPath = "ted_md10_error-" + i + ".txt";
//            System.out.println(expPath);
//            File exporty = new File(expPath);
//
//            tedXp.exportGraph(getG_s(mderr10G), exporty);
//
//            mdTestCompareTwo(filePath, filePath2);
//            mdTestOldNew(expPath);
//
//            start = permutation;
//        }

}
