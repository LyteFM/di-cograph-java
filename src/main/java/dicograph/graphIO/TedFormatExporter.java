package dicograph.graphIO;

import org.jgrapht.Graph;
import org.jgrapht.io.GraphExporter;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Fynn Leitow on 15.11.17. Export to the format used by M.Tedder's undirected MD for testing purposes.
 */
public class TedFormatExporter<V,E> implements GraphExporter<V,E> {

    private final TreeMap<String, V> nameToVertex;
    private Integer[] permutation;

    /**
     * Empty Constructor
     */
    public TedFormatExporter() {
        nameToVertex = new TreeMap<>();
        permutation = null;
    }

//    /**
//     * @param nameToVertex mapping the names (String) to the vertices
//     */
//    public TedFormatExporter(TreeMap<String, V> nameToVertex) {
//        this.nameToVertex = nameToVertex;
//    }


    public void exportGraph(Graph<V, E> g, Writer writer) {

        int sz = g.vertexSet().size();

        if(permutation == null ||  permutation.length != sz){
            // init
            permutation = new Integer[sz];
            for (int i = 0; i < sz; i++) {
                permutation[i] = i;
            }
        }

        String name;
        if (nameToVertex.isEmpty()) {

            HashMap<Integer,Integer> vNumToPos = new HashMap<>();
            for (int i = 0; i < permutation.length; i++) {
                vNumToPos.put(permutation[i],i);
            }

            for (V vertex : g.vertexSet()) {
                // Map vertex names to vertex objects by their String representation
                // And consider the permutation, if one was provided
                name = vertex.toString();
                int vertexNo = Integer.valueOf(name);
                int permutedIndex = vNumToPos.get(vertexNo);
                int permutedNo = permutation[permutedIndex];
                name = String.valueOf(permutedNo);

                if(name.length() == 1){
                    name = "0" + name;
                }
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
                        lineBuilder.append(xEntry.getKey()).append("->").append(yEntry.getKey());
                        first = false;
                    }
                    else {
                        lineBuilder.append(',').append(yEntry.getKey());
                    }
                }
            }
            String line = lineBuilder.toString();
            if(line.isEmpty()){
                out.println(xEntry.getKey() + "->");
            } else {
                out.println(line);
            }
        }
        out.flush();
    }

    public Integer[] getPermutation() {
        return permutation;
    }

    public void setPermutation(Integer[] permutation) {
        this.permutation = permutation;
    }
}
