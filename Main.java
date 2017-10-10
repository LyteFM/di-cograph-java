package dicograph;


import org.jgrapht.ext.ExportException;
import org.jgrapht.ext.MatrixExporter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleGraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

import dicograph.ILPSolver.CplexDiCographEditingSolver;
import dicograph.graphIO.GraphGenerator;
import dicograph.graphIO.SimpleMatrixExporter;
import dicograph.graphIO.SimpleMatrixImporter;
import dicograph.modDecomp.GraphHandle;
import dicograph.modDecomp.MDTree;
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
        System.out.println("The default PRNG on this system is " + new SecureRandom().getAlgorithm());

        Logger log = Logger.getLogger( "TestLogger" );
        Handler handler = new ConsoleHandler();
        handler.setLevel( Level.FINEST );
        log.addHandler( handler );
        log.setLevel( Level.FINEST );
        log.fine( "Alles ist fein!" );

        // so den RNG einrichten!

        SimpleGraph<String, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
        GraphGenerator gen = new GraphGenerator(log);
        String res = gen.generateRandomCograph(g,14);

        GraphHandle gHand = new GraphHandle(g);
        String g_res = gHand.getMDTree().toString();
        System.out.println("\nNew Code:\n" + MDTree.beautify(g_res));

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

    void mdTest(){
        //String filePath = args[0];
        String filePath = "importFiles/tedder_test0.txt";

        // old läuft korrekt.
        GraphHandle g = new GraphHandle(filePath);
        String g_res = g.getMDTreeOld().toString();
        System.out.println("Old Code:\n" + MDTree.beautify(g_res));

        GraphHandle g2 = new GraphHandle(filePath);
        String g2_res = g2.getMDTree().toString();
        System.out.println("\nNew Code:\n" + MDTree.beautify(g2_res));
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
