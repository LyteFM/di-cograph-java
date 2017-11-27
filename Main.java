package dicograph;


import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
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
import dicograph.graphIO.TedFormatExporter;
import dicograph.modDecomp.DirectedMD;
import dicograph.modDecomp.GraphHandle;
import dicograph.modDecomp.MDTree;
import dicograph.utils.SortAndCompare;
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

//        boolean morePls = true;
//        while (morePls){
//            morePls = directedMDTesting(log,consoleHandler,10,5,false);
//        }


        String fromPaper = "fromFactPermPaper.txt";
        SimpleDirectedGraph<Integer, DefaultEdge> paperGraph = JGraphAdjecencyImporter.importIntGraph(new File(fromPaper), false);
        //DirectedMD paperMD = new DirectedMD(paperGraph, log, true);        paperMD.computeModularDecomposition();

//        System.out.println(paperGraph);
//        DOTExporter<Integer,DefaultEdge> exporter =new DOTExporter<>();
//        exporter.exportGraph(paperGraph, new File(fromPaper+ ".dot"));




//        for( int i = 20; i <= 30; i ++) {
//            boolean ok = directedMDTesting(log, consoleHandler, i, i/2,true);
//            if(!ok)
//                break;
//        }





//        System.out.println("From rand:\n\n");
//        MDtestFromFile(log, test, false);
//        System.out.println("From Matrix:\n\n");
//        MDtestFromFile(log, weirdError,true);

        String folderPath = "testGraphs/DMDtest/ERR/";
        String folder = "testGraphs/";


        // OK:
        String smallNotAtournament = folder + "randDigraph_n_10_edits_5_11-17_14:11:11:882_original.txt";
        String smallTourErrgraph = folder + "randDigraph_n_7_edits_3_11-17_14:04:24:233_original.txt";

        String n21err = folderPath +"randDigraph_n_21_edits_10_11-22_14:38:33:813_original.txt";
        String n24err = folderPath + "randDigraph_n_24_edits_12_11-22_14:38:47:196_original.txt"; // EXTREM fette prim :/ aber ok
        String n23err = folderPath + "randDigraph_n_23_edits_11_11-22_14:38:41:882_original.txt"; // several weak.


        // ERR:
        String viceVera = "testGraphs/randDigraph_n_10_edits_5_11-17_14:13:25:176_original.txt"; // more than 1 eqiv class :) - Höh, passt. aso...

        // Once, this had more than 1 equiv class. Hmm. Now, [3,4,5,23,24] is an undetected SERIES module.
        // happened, when there was a WEAK series module :) -> wut, where though???
        String n25err = folderPath + "randDigraph_n_25_edits_12_11-22_14:38:53:932_original.txt";

        // here: [12, 1, 0] is parallel module.
        String n23err2 = folderPath + "randDigraph_n_23_edits_11_11-22_14:32:56:841_original.txt";

        // here: [5,6] is parallel module.
        String n22err= folderPath + "randDigraph_n_22_edits_11_11-22_14:32:52:354_original.txt";

        //
        // here: [3,4] - ORDER and fully disconnected; [9,13] - SERIES and arcs to 6,...,14 ; [20,21,23] - PARALLEL and fully disconnected.
        String weirdError = folder + "randDigraph_n_24_edits_8_11-14_18:05:20:306_original.txt"; // corresp matrix
        // here: [11,12], [17,20], [6,7,9].
        String test = "testy.txt"; // not matrix
        //

        // not a tournament: after deletion of weak, parent still order???
        // parent for sure is a module in G. (by RC/LC)
        // wtf, where is my new "determine node type" method???


        MDtestFromFile(log,  smallTourErrgraph, true);

        // todo: Teste ich meine Prim-Module genug??? die sehen mir so groß aus :(

//        ArrayList<Integer> baseSet = new ArrayList<>(Arrays.asList(1,2,3,4,5,6));
//        List<List<Integer>> allSubsets = SortAndCompare.computeAllSubsets(baseSet);
//        System.out.println("size: " + allSubsets.size());










//
//        File importFile = new File("testy.txt");
//        SimpleDirectedGraph<Integer, DefaultEdge> matrixGraph = SimpleMatrixImporter.importIntGraph( new File(weirdError));
//        SimpleDirectedGraph<Integer, DefaultEdge> randGraph = JGraphAdjecencyImporter.importIntGraph(importFile);



        SecureRandom random = new SecureRandom();
        random.setSeed(new byte[17]);

        int sz = 10;

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


        //compareMatrixAndRand(matrixGraph,randGraph);


    }

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

    static void MDtestFromFile(Logger log, String importFilePath, boolean matrix) throws Exception {

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
        log.info(".dot for Graph:\n" + writer.toString());

        DirectedMD testMD = new DirectedMD(importGraph, log, true);
        testMD.computeModularDecomposition();

    }

    static void compareMatrixAndRand(SimpleDirectedGraph<Integer, DefaultEdge> matrixGraph, SimpleDirectedGraph<Integer, DefaultEdge> randGraph) {
        //AsUndirectedGraph<Integer,DefaultEdge> matrixUndirected = new AsUndirectedGraph<>(randGraph);
        //AsUndirectedGraph<Integer,DefaultEdge> randUndirected = new AsUndirectedGraph<>(matrixGraph);
        // same here as in MD
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

    static boolean directedMDTesting(Logger log, Handler baseHandler, int nVertices, int nDisturb, boolean solveILP) throws Exception{

        boolean ok = true;
        String timeStamp = new SimpleDateFormat("MM-dd_HH:mm:ss:SSS").format(Calendar.getInstance().getTime());

        GraphGenerator gen = new GraphGenerator(log);
        SimpleDirectedGraph<Integer, DefaultEdge> g_d = new SimpleDirectedGraph<>(DefaultEdge.class);
        gen.generateRandomDirectedCograph(g_d, nVertices, true);
        gen.disturbDicograph(g_d, nDisturb);

        // export the graph for debug purposes
        String filePath = "testGraphs/DMDtest/randDigraph_n_" + nVertices + "_edits_" + nDisturb + "_" + timeStamp;
        File expfile = new File(filePath + "_original.txt");
        SimpleMatrixExporter<Integer, DefaultEdge> myExporter = new SimpleMatrixExporter<>();
        myExporter.exportGraph(g_d, expfile);
        log.info(String.format("Generated random Dicograph with %s vertices and %s random edge-edits.", nVertices, nDisturb));
        log.info("Exported Matrix to :" + filePath + "_original.txt");

        // writes the log
        File logFile = new File(filePath +".log");
        FileHandler fileHandler = new FileHandler(logFile.getPath());
        fileHandler.setFormatter(baseHandler.getFormatter());
        fileHandler.setLevel( baseHandler.getLevel() );
        log.addHandler(fileHandler);

        log.info("Started modular decomposition");

        try {
            DirectedMD testMD = new DirectedMD(g_d, log, true);
            testMD.computeModularDecomposition();
        } catch (IllegalStateException | AssertionError e){
            ok = false;
            log.severe(e.toString());
        }


        log.info("Finished modular decomposition. Log written to:");
        log.info(filePath + ".log");

        if(solveILP){
            // compute the solution via ILP
            log.info("*** Starting ILP-Solver ***");
            int [] parameters = {0,0}; // one solution
            CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(g_d, parameters, log);
            List<SimpleDirectedGraph<Integer, DefaultEdge>> solutions = mySolver.solve();
            log.info("Saving solution for n = " + nVertices + " to:");
            log.info(filePath + "_edited.txt");
            File solFile = new File(filePath + "_edited.txt");
            myExporter.exportGraph(solutions.get(0), solFile);


        }

        // clear this handler, I want separate logfiles.
        fileHandler.close();
        log.removeHandler(fileHandler);

        return ok;

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

            CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(g_d, parameters, log);
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
        int [] parameters = {0,1};

        CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(importGraph, parameters, log);
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
