package dicograph;


import org.jgrapht.ext.MatrixExporter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import dicograph.graphIO.GraphGenerator;
import dicograph.graphIO.SimpleMatrixExporter;

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

        SimpleDirectedGraph<String, DefaultEdge> testGraph = GraphGenerator.generateRandomGnp(11,0.3);
        System.out.println(testGraph.toString());

        // test output
        String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        File logFile = new File(timeLog + ".txt");

        // This will output the full path where the file will be written to...
        System.out.println(logFile.getCanonicalPath());

        BufferedWriter writer = new BufferedWriter(new FileWriter(logFile));
        //

        SimpleMatrixExporter<String, DefaultEdge> myExporter = new SimpleMatrixExporter<>();
        myExporter.exportGraph(testGraph, logFile);

    }


}
