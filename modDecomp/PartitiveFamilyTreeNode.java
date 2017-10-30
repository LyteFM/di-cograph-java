package dicograph.modDecomp;

/**
 * Created by Fynn Leitow on 29.10.17.
 */
public class PartitiveFamilyTreeNode extends RootedTreeNode {

    boolean marked;
    int numMarkedChildren;


    protected PartitiveFamilyTreeNode() {
        super();
        marked = false;
        numMarkedChildren = 0;
    }

    protected void mark() {
        marked = true;
        // todo: what if root?
        PartitiveFamilyTreeNode parent = (PartitiveFamilyTreeNode) getParent();
        parent.numMarkedChildren++;
    }

    protected void unmark() {
        marked = false;
        PartitiveFamilyTreeNode parent = (PartitiveFamilyTreeNode) getParent();
        parent.numMarkedChildren--;
    }

    void unmarkAllChildren() {
        PartitiveFamilyTreeNode currentChild = (PartitiveFamilyTreeNode) this.getFirstChild();

        while (currentChild != null) {
            currentChild.unmark();
            currentChild = (PartitiveFamilyTreeNode) currentChild.getRightSibling();
        }

        // todo: assert numMarkedChildren == 0
        if (numMarkedChildren != 0)
            System.err.println("Illegal number of marked children after unmarking!!!");
    }

    public int getNumMarkedChildren() {
        return numMarkedChildren;
    }

    public boolean isMarked() {
        return marked;
    }
}
