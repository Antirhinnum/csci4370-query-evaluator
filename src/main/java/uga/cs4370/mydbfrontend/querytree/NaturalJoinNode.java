package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Aliasable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

import java.util.List;

public class NaturalJoinNode implements QueryTreeNode {

    private final QueryTreeNode leftChild;
    private final QueryTreeNode rightChild;

    public NaturalJoinNode(QueryTreeNode leftChild, QueryTreeNode rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Aliasable<Relation>> relations) {
        Relation leftResult = this.leftChild.evaluate(ra, relations);
        Relation rightResult = this.rightChild.evaluate(ra, relations);
        return ra.join(leftResult, rightResult);
    }
}
