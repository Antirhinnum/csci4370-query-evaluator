package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Aliasable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

import java.util.List;

public class UnionNode implements QueryTreeNode {

    private final QueryTreeNode leftChild;
    private final QueryTreeNode rightChild;

    public UnionNode(QueryTreeNode left, QueryTreeNode right) {
        this.leftChild = left;
        this.rightChild = right;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Aliasable<Relation>> relations) {
        Relation leftResult = leftChild.evaluate(ra, relations);
        Relation rightResult = rightChild.evaluate(ra, relations);
        return ra.union(leftResult, rightResult);
    }
}