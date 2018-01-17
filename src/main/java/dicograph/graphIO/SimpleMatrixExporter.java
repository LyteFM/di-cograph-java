package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.io.GraphExporter;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Fynn Leitow on 08.10.17.
 */
public class SimpleMatrixExporter<V,E> implements GraphExporter<V,E>{

    public final static String columnDelimiter = "";
    public final static String lineDelimiter = "";
    private TreeMap<String, V> nameToVertex;

    /**
     * Empty Constructor
     */
    public SimpleMatrixExporter(){
        nameToVertex = new TreeMap<>();
    }

    /**
     *
     * @param nameToVertex mapping the names (String) to the vertices
     */
    public SimpleMatrixExporter(TreeMap<String, V> nameToVertex){
        this.nameToVertex = nameToVertex;
    }



    public void exportGraph(Graph<V,E> g, Writer writer){

        String name;
        if(nameToVertex.isEmpty()) {
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
            if(nameToVertex.size() != g.vertexSet().size()){
                throw new IllegalArgumentException("Error: Size of vertex set (" + g.vertexSet().size()
                        + ") and given map of names to vertices (" + nameToVertex.size() + ") differ.");
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
