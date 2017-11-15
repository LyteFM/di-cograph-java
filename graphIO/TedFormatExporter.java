package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.ext.GraphExporter;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Fynn Leitow on 15.11.17.
 */
public class TedFormatExporter<V,E> implements GraphExporter<V,E> {

    private TreeMap<String, V> nameToVertex;

    /**
     * Empty Constructor
     */
    public TedFormatExporter() {
        nameToVertex = new TreeMap<>();
    }

    /**
     * @param nameToVertex mapping the names (String) to the vertices
     */
    public TedFormatExporter(TreeMap<String, V> nameToVertex) {
        this.nameToVertex = nameToVertex;
    }


    public void exportGraph(Graph<V, E> g, Writer writer) {

        String name;
        if (nameToVertex.isEmpty()) {
            for (V vertex : g.vertexSet()) {
                // Map vertex names to vertex objects by their String representation
                name = vertex.toString();
                if (nameToVertex.containsKey(name)) {
                    throw new IllegalArgumentException("Error: vertex name " + name + " not unique!");
                } else {
                    nameToVertex.put(name, vertex);
                }
            }
        } else {
            if (nameToVertex.size() != g.vertexSet().size()) {
                throw new IllegalArgumentException("Error: Size of vertex set (" + g.vertexSet().size()
                        + ") and given map of names to vertices (" + nameToVertex.size() + ") differ.");
            }
        }

        PrintWriter out = new PrintWriter(writer);
        StringBuilder lineBuilder;

        for (Map.Entry<String, V> xEntry : nameToVertex.entrySet()) {
            lineBuilder = new StringBuilder();
            boolean first = true;

            // unnecessary, but simpler now that I have the matrix code
            for (Map.Entry<String, V> yEntry : nameToVertex.entrySet()) {

                if (g.containsEdge(xEntry.getValue(), yEntry.getValue())) {
                    if(first){
                        lineBuilder.append(xEntry.getValue()).append("->").append(yEntry.getValue());
                        first = false;
                    }
                    else {
                        lineBuilder.append(',').append(yEntry.getValue());
                    }
                }
            }
            String line = lineBuilder.toString();
            if(line.isEmpty()){
                out.println(xEntry.getValue() + "->");
            } else {
                out.println(line);
            }
        }
        out.flush();
    }
}
