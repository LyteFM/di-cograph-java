package dicograph.graphIO;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.StringTokenizer;

/**
 * Created by Fynn Leitow on 14.11.17.
 */
public class JGraphAdjecencyImporter {

    public static SimpleDirectedGraph<Integer, DefaultEdge> importIntGraph(File file){
        return importIntGraph(file,false);
    }
    /**
     * Creates a directed Graph from the format of DirectedGraph.toString() in JGraphT. Note: You
     * need to separate edges and values by a newline after the "],".
     * @param file
     * @return
     */
    private static SimpleDirectedGraph<Integer, DefaultEdge> importIntGraph(File file, boolean startAtOne) {

        try(InputStream inStream = Files.newInputStream(file.toPath())) {
            BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
            String line;

            boolean verticesFound = false;
            int vertex = 0;

            String vertexLine = null;
            String edgeLine = null;


            SimpleDirectedGraph<Integer, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);

            while ( (line = in.readLine()) != null){
                line = line.trim(); // remove whitespace at start and end
                if(line.isEmpty())
                    continue;
                if(line.startsWith("[") && line.endsWith("],")){
                    vertexLine = line.substring(1,line.length() - 2);
                    verticesFound = true;
                } else if(line.startsWith("[") && line.endsWith(")]")){
                    if(verticesFound) {
                        edgeLine = line.substring(1, line.length() - 1);
                    } else {
                        // all in one line
                        int splitIndex = line.indexOf("], [");
                        if(splitIndex > 0){
                            vertexLine = line.substring(1,splitIndex);
                            edgeLine = line.substring(splitIndex + 4, line.length() -1);
                        }
                    }
                }


            }
            if(startAtOne)
                vertex++;

            if(vertexLine != null && edgeLine != null) {
                for (StringTokenizer toki = new StringTokenizer(vertexLine, ", "); toki.hasMoreTokens(); ) {
                    String s = toki.nextToken();
                    if (vertex == Integer.valueOf(s)) {
                        graph.addVertex(vertex);
                        vertex++;
                    } else {
                        System.err.println("Invalid vertexNo " + s + ", expected " + vertex);
                        return null;
                    }
                }
                for (String s : edgeLine.split(", ")) {
                    if (s.startsWith("(") && s.endsWith(")")) {
                        String[] vals = s.substring(1, s.length() - 1).split(",");
                        if (vals.length != 2) {
                            System.err.println("Invalid edge entry " + s + ", need format: (u,v), (x,y),...");
                            return null;
                        }
                        int from = Integer.valueOf(vals[0]);
                        int to = Integer.valueOf(vals[1]);
                        if (graph.vertexSet().contains(from) && graph.vertexSet().contains(to)) {
                            graph.addEdge(from, to);
                        } else {
                            System.err.println("Invalid edge entry " + s + " not contained in vertex set");
                            return null;
                        }
                    } else {
                        System.err.println("Invalid edge entry " + s + ", need format: (u,v), (x,y),...");
                        return null;
                    }

                }
            } else {
                System.err.println("No input according to org.jgrapht.DirectedGraph.toString found");
                return null;
            }

            return graph;

        } catch (IOException e){
            System.err.println("For file: " + file.toString() + "\n" + e.toString());
            return null;
        }

    }
}
