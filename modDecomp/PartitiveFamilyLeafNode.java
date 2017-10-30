package dicograph.modDecomp;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyLeafNode extends PartitiveFamilyTreeNode {

    int vertex;
    // Debug: Corresponding String in JGraphT
    private String label;

    protected PartitiveFamilyLeafNode(int vertexNo, String vertexLabel) {
        super();
        label = vertexLabel;
        vertex = vertexNo;
    }

}
