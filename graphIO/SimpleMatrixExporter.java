package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.ext.GraphExporter;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Fynn Leitow on 08.10.17.
 */
public class SimpleMatrixExporter<V,E> implements GraphExporter<V,E>{

    public final String columnDelimiter = "";
    public final String lineDelimiter = "";
    private TreeMap<String, V> nameToVertex;

    public SimpleMatrixExporter(){
        nameToVertex = new TreeMap<>();
    }



    public void exportGraph(Graph<V,E> g, Writer writer){

        String name;
        for (V vertex : g.vertexSet()) {
            // Map vertex names to vertex objects
            name = vertex.toString();
            if(nameToVertex.containsKey(name)){
                throw new IllegalArgumentException("Error: vertex name " + name + " not unique!");
            } else {
                nameToVertex.put(name, vertex);
            }
        }

        PrintWriter out = new PrintWriter(writer);
        StringBuilder lineBuilder;

        for (Map.Entry<String, V> xEntry : nameToVertex.entrySet()) {
            lineBuilder = new StringBuilder();
            for(Map.Entry<String, V> yEntry : nameToVertex.entrySet()){

                if(g.containsEdge(xEntry.getValue(), yEntry.getValue())){
                    lineBuilder.append("1").append(columnDelimiter);
                } else {
                    lineBuilder.append("0").append(columnDelimiter);
                }
            }
            out.println(lineBuilder.append(lineDelimiter).toString());
        }

        out.flush();
    }

}
