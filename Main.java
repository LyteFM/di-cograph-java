package dicograph;


import org.jgrapht.ext.ExportException;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import dicograph.ILPSolver.CplexDiCographEditingSolver;
import dicograph.graphIO.GraphGenerator;
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
        //testRNG("NativePRNG");
        //testRNG("SHA1PRNG");
        System.out.println("Working Directory = " +
                System.getProperty("user.dir"));

        Double d = 0.99999999999999999 * 2; // kann echt 2 werden x)
        System.out.println("0.99 * 2 = " + d.intValue());

        System.out.println("The default PRNG on this system is " + new SecureRandom().getAlgorithm());

        Logger log = Logger.getLogger( "TestLogger" );
        for (Handler iHandler : log.getParent().getHandlers()) {
            log.getParent().removeHandler(iHandler);
        }
        Handler handler = new ConsoleHandler();
        VerySimpleFormatter myFormatter = new VerySimpleFormatter();
        handler.setFormatter(myFormatter);
        handler.setLevel( Level.FINEST );
        log.addHandler( handler );
        log.setLevel( Level.FINEST );
        log.fine( "Alles ist fein!" );

        //mdTest();
        //DirectedMD.dahlhausProcessDelegator("OverlapComponentProg/test3_neu.txt");
        directedMDTesting(log);
        // primeTest();

    }

    static void directedMDTesting(Logger log) throws Exception{
        GraphGenerator gen = new GraphGenerator(log);
        SimpleDirectedGraph<String, DefaultEdge> g_d = new SimpleDirectedGraph<>(DefaultEdge.class);
        gen.generateRandomDirectedCograph(g_d, 50);
        gen.disturbDicograph(g_d, 9);

        DirectedMD testMD = new DirectedMD(g_d, log, true);
        testMD.computeModularDecomposition();

    }

    void cographTesting(Logger log) throws Exception{
        // Cograph Testing
        SimpleGraph<String, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        GraphGenerator gen = new GraphGenerator(log);
        String res = gen.generateRandomCograph(g,35);

        GraphHandle gHand = new GraphHandle(g);
        String g_res = gHand.getMDTree().toString();
        System.out.println("\nNew Code:\n" + MDTree.beautify(g_res));
        // Dicograph Testing:

        int [] parameters = {0,0}; // one solution


        for( int i = 10; i< 50; i++) {
            SimpleDirectedGraph<String, DefaultEdge> g_d = new SimpleDirectedGraph<>(DefaultEdge.class);
            gen.generateRandomDirectedCograph(g_d, i);

            CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(g_d, parameters);
            List<SimpleDirectedGraph<String, DefaultEdge>> solutions = mySolver.solve();
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

    static Long[] primeTest() throws Exception {
        String filePath = "primes.txt";
        Long[] primes = new Long[1000];
        Long product = 1L;
        int index = 0;


        try (BufferedReader inputStream = new BufferedReader(new FileReader(filePath))) {

            String line;

            while ((line = inputStream.readLine()) != null) {
                String[] numbers = line.split("\\s+"); // any whitespace
                for (String num : numbers) {
                    if (num.equals(""))
                        continue;
                    primes[index] = Long.valueOf(num);
                    product *= primes[index];
                    if (product < 0) {
                        // overflow
                        break;
                    }

                    index++;

                }
                System.out.println(line + " : " + product);


            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }

        System.out.println("Read " + index + 1 + " primes before overflow\n last primes: \n");
        System.out.println(primes[index - 2] + "   " + primes[index - 1] + "  " + primes[index] + "\n");
        System.out.println(Long.MAX_VALUE);

        return primes;
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
        SimpleDirectedGraph<String, DefaultEdge> importGraph = SimpleMatrixImporter.importGraph(importFile);

        System.out.print(importGraph + "\n");
        int [] parameters = {0,1};

        CplexDiCographEditingSolver mySolver = new CplexDiCographEditingSolver(importGraph, parameters);
        List<SimpleDirectedGraph<String,DefaultEdge>> solutions = mySolver.solve();

        int count = 1;
        for(SimpleDirectedGraph<String,DefaultEdge> cograph : solutions){

            SimpleMatrixExporter<String, DefaultEdge> myExporter = new SimpleMatrixExporter<>();
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
        SimpleDirectedGraph<String,DefaultEdge> testGraph2 = SimpleMatrixImporter.importGraph(logFile);
        String exp2 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        File expfile = new File(exp2 + "_reimported" + ".txt");
        myExporter.exportGraph(testGraph2, expfile);
    }


}
