package dicograph;


import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.ext.ExportException;
import org.jgrapht.graph.AsUndirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import dicograph.ILPSolver.CplexDiCographEditingSolver;
import dicograph.graphIO.GraphGenerator;
import dicograph.graphIO.JGraphAdjecencyImporter;
import dicograph.graphIO.SimpleMatrixExporter;
import dicograph.graphIO.SimpleMatrixImporter;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.GraphHandle;
import dicograph.modDecomp.MDTree;
import dicograph.utils.VerySimpleFormatter;
import ilog.concert.IloException;

/**
 * Created by Fynn Leitow on 02.10.17.
 */
public class Main {

//    static void usage() {
//        System.out.println("usage:   LPex1 <option>");
//        System.out.println("options:       -r   build model row by row");
//        System.out.println("options:       -c   build model column by column");
//        System.out.println("options:       -n   build model nonzero by nonzero");
//    }

    public static void main(String[] args) throws Exception{

//        if ( args.length != 1 || args[0].charAt(0) != '-' ) {
//            usage();
//            return;
//        }
        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));
//
//        Double d = 0.99999999999999999 * 2; // todo: kann echt 2 werden x)
//        System.out.println("0.99 * 2 = " + d.intValue());

        System.out.println("The default PRNG on this system is " + new SecureRandom().getAlgorithm());

        // remove defaults
        Logger log = Logger.getLogger( "TestLogger" );
        for (Handler iHandler : log.getParent().getHandlers()) {
            log.getParent().removeHandler(iHandler);
        }
        // add and configure console output
        Handler consoleHandler = new ConsoleHandler();
        VerySimpleFormatter myFormatter = new VerySimpleFormatter();
        consoleHandler.setFormatter(myFormatter);
        consoleHandler.setLevel( Level.FINEST );

        log.addHandler( consoleHandler );
        log.setLevel( Level.FINEST );
        log.fine( "Alles ist fein!" );

        //mdTest();
        //DirectedMD.dahlhausProcessDelegator("OverlapComponentProg/test3_neu.txt");

//        for( int i = 20; i <= 30; i ++) {
//            directedMDTesting(log, consoleHandler, i, i/3);
//        }

        String folder = "testGraphs/";
        String oftenUsedFile = folder + "randDigraph_n_50_edits_10_1031_16:37:01.txt";
        String smallStackoverflowFile = folder + "randDigraph_n_10_edits_2_11-03_11:35:47:010_original.txt";
        String weirdError = folder + "randDigraph_n_24_edits_8_11-14_18:05:20:306_original.txt";
        String test = "testy.txt";
        //MDtestFromFile(log, weirdError,true);


        File importFile = new File("testy.txt");
        SimpleDirectedGraph<Integer, DefaultEdge> matrixGraph = SimpleMatrixImporter.importIntGraph( new File(weirdError));
        SimpleDirectedGraph<Integer, DefaultEdge> randGraph = JGraphAdjecencyImporter.importIntGraph(importFile);

        AsUndirectedGraph<Integer,DefaultEdge> matrixUndirected = new AsUndirectedGraph<>(randGraph);
        AsUndirectedGraph<Integer,DefaultEdge> randUndirected = new AsUndirectedGraph<>(matrixGraph);

        System.out.println("Testing: from matrix");

        List<Integer> se_5_matrix = Arrays.asList(15,16,18,19);
        TreeSet<Integer> matrix_others = new TreeSet<>(matrixUndirected.vertexSet());
        matrix_others.removeAll(se_5_matrix);
        debugTesting(matrixUndirected,matrix_others,se_5_matrix);

        System.out.println("Testing: randomly created");

        List<Integer> se_4_rand = Arrays.asList(4,5,22,23);
        TreeSet<Integer> rand_others = new TreeSet<>(randUndirected.vertexSet());
        rand_others.removeAll(se_4_rand);
        debugTesting(randUndirected, rand_others, se_4_rand);

//
//        VF2GraphIsomorphismInspector isomorphismInspector = new VF2GraphIsomorphismInspector<>(,new AsUndirectedGraph<>(randGraph));
//        System.out.println("Iso exists: " + isomorphismInspector.isomorphismExists());

    }

    static void MDtestFromFile(Logger log, String importFilePath, boolean matrix) throws Exception {

        File importFile = new File(importFilePath);
        SimpleDirectedGraph<Integer, DefaultEdge> importGraph;
        if(matrix) {
            importGraph=SimpleMatrixImporter.importIntGraph(importFile);
        } else {
            importGraph = JGraphAdjecencyImporter.importIntGraph(importFile);
        }

        DirectedMD testMD = new DirectedMD(importGraph, log, true);
        testMD.computeModularDecomposition();

    }

    static void debugTesting(AsUndirectedGraph<Integer,DefaultEdge> graph, Set<Integer> otherVertices, List<Integer> moduleVertices){

        for (int i : otherVertices) {
            System.out.println("Checking: " + i + " with edges: ");

            boolean first = true;
            boolean hasEdge = false;
            for(int j : moduleVertices){
                if(first){
                    hasEdge = graph.containsEdge(i, j);
                    first = false;
                } else {
                    if (hasEdge != graph.containsEdge(i, j)) {
                        System.out.println("Error for edge {" + i + "," + j + "}, expected hasEdge == " + hasEdge);
                    }
                }
            }
        }
    }

    static void debugTesting2(AsUndirectedGraph<Integer,DefaultEdge> graph, Set<Integer> otherVertices, List<Integer> moduleVertices){

        for (int i : otherVertices) {
            System.out.println("Checking: " + i + " with edges: " + graph.edgesOf(i));

            boolean first = true;
            boolean hasFirstEdge = false;
            for(int j : moduleVertices){
                boolean hasEdge = graph.containsEdge(i, moduleVertices.get(0));
                if(first){
                    hasFirstEdge = hasEdge;
                    first = false;
                } else {
                    if (hasEdge != hasFirstEdge) {
                        System.out.println("Error for edge {" + i + "," + j + "}, expected hasEdge == " + hasFirstEdge);
                    }
                }
            }
        }
    }

    static SimpleDirectedGraph<Integer, DefaultEdge> directedMDTesting(Logger log, Handler baseHandler, int nVertices, int nDisturb) throws Exception{

        String timeStamp = new SimpleDateFormat("MM-dd_HH:mm:ss:SSS").format(Calendar.getInstance().getTime());

        GraphGenerator gen = new GraphGenerator(log);
        SimpleDirectedGraph<Integer, DefaultEdge> g_d = new SimpleDirectedGraph<>(DefaultEdge.class);
        gen.generateRandomDirectedCograph(g_d, nVertices, true);
        gen.disturbDicograph(g_d, nDisturb);

        // export the graph for debug purposes
        String filePath = "testGraphs/randDigraph_n_" + nVertices + "_edits_" + nDisturb + "_" + timeStamp;
        File expfile = new File(filePath + "_original.txt");
        SimpleMatrixExporter<Integer, DefaultEdge> myExporter = new SimpleMatrixExporter<>();
        myExporter.exportGraph(g_d, expfile);
        System.out.println(String.format("Generated random Dicograph with %s vertices and %s random edge-edits.", nVertices, nDisturb));
        System.out.println("Exported Matrix to :" + filePath + "_original.txt");

        // writes the log
        File logFile = new File(filePath +".log");
        FileHandler fileHandler = new FileHandler(logFile.getPath());
        fileHandler.setFormatter(baseHandler.getFormatter());
        fileHandler.setLevel( baseHandler.getLevel() );
        log.addHandler(fileHandler);

        System.out.println("Started modular decomposition");
        DirectedMD testMD = new DirectedMD(g_d, log, true);
        testMD.computeModularDecomposition();
        System.out.println("Finished modular decomposition. Log written to:");
        System.out.println(filePath+ ".log");

        if(nVertices + nDisturb <= 50){
            // compute the solution via ILP
            System.out.println("*** Starting ILP-Solver ***");
            int [] parameters = {0,0}; // one solution
            CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(g_d, parameters);
            List<SimpleDirectedGraph<Integer, DefaultEdge>> solutions = mySolver.solve();
            System.out.println("Saving solution for n = " + nVertices + " to:");
            System.out.println(filePath + "_edited.txt");
            File solFile = new File(filePath + "_edited.txt");
            myExporter.exportGraph(solutions.get(0), solFile);


        }

        // clear this handler, I want separate logfiles.
        fileHandler.close();
        log.removeHandler(fileHandler);

        return g_d;

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

        int [] parameters = {0,0}; // one solution



        for( int i = 10; i< 50; i++) {
            SimpleDirectedGraph<Integer, DefaultEdge> g_d = new SimpleDirectedGraph<>(DefaultEdge.class);
            gen.generateRandomDirectedCograph(g_d, i, true);

            CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(g_d, parameters);
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


    static void mdTest(){
        //String filePath = args[0];
        String filePath = "importFiles/tedder_test0.txt";

        // old läuft korrekt.
        GraphHandle g = new GraphHandle(filePath);
        String g_res = g.getMDTreeOld().toString();
        System.out.println("Old Code:\n" + MDTree.beautify(g_res));

        GraphHandle g2 = new GraphHandle(filePath);
        String g2_res = g2.getMDTree().toString();
        System.out.println("\nNew Code:\n" + MDTree.beautify(g2_res));
        System.out.print(g2.getMDTree().getSetRepresentationAsStrings()+ "\n\n");
    }

    void cplexTest() throws ExportException, IloException, IOException{
        String filePath = "importFiles/sz_15_pr_30";
        File importFile = new File(filePath+ ".txt");
        SimpleDirectedGraph<Integer, DefaultEdge> importGraph = SimpleMatrixImporter.importIntGraph(importFile);

        System.out.print(importGraph + "\n");
        int [] parameters = {0,1};

        CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(importGraph, parameters);
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
        SimpleDirectedGraph<String, DefaultEdge> testGraph = graphGenerator.generateRandomGnp(11,0.3);
        System.out.println(testGraph.toString());

        // test output
        String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        File logFile = new File(timeLog + ".txt");

        // This will output the full path where the file will be written to...
        System.out.println(logFile.getCanonicalPath());

        //

        SimpleMatrixExporter<String, DefaultEdge> myExporter = new SimpleMatrixExporter<>();
        myExporter.exportGraph(testGraph, logFile);

        // try importing and exporting again:
        SimpleDirectedGraph<String,DefaultEdge> testGraph2 = SimpleMatrixImporter.importStringGraph(logFile);
        String exp2 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        File expfile = new File(exp2 + "_reimported" + ".txt");
        myExporter.exportGraph(testGraph2, expfile);
    }


}
