package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.ext.GraphImporter;
import org.jgrapht.ext.ImportException;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by Fynn Leitow on 08.10.17.
 */
public class SimpleMatrixImporter {

    public static SimpleDirectedGraph importGraph(Path file) throws IOException {

        InputStream inStream = Files.newInputStream(file);
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));

        String line;
        boolean firstRun = true;
        int numberVertices = 0;
        int rowNumber = 0;

        SimpleDirectedGraph<String, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

        while ((line = in.readLine()) != null) {

            // handle delimiters
            if(line.isEmpty() || line.equals(SimpleMatrixExporter.lineDelimiter))
                continue;
            line = line.replaceAll(SimpleMatrixExporter.columnDelimiter,"");

            if(firstRun){
                numberVertices = line.length();

                // read the vertices as Strings
                for( int i = 0; i< line.length(); i++){
                    graph.addVertex( String.valueOf(i) );
                }
            } else {
                if(line.length() != numberVertices){
                    throw new IOException("Error: in row " + rowNumber + ", Invalid Matrix line:\n" + line);
                }
            }
            for( int inVertex = 0; inVertex< line.length(); inVertex++){

                // reads and asserts the 0/1-Matrix-Entry
                char c = line.charAt(inVertex);
                assert Character.isDigit(c);
                int isEdge = Character.getNumericValue(c);
                assert isEdge == 0 || isEdge == 1;

                String outVertex = Integer.toString(rowNumber);

                if(isEdge == 1){
                    graph.addEdge(outVertex, Integer.toString(inVertex));
                }

            }

            firstRun =false;
            rowNumber++;
        }

        assert rowNumber == numberVertices;

        return graph;
    }
}
