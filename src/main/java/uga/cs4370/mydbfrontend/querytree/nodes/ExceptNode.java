package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.QueryTreeNode;

import java.util.List;

public class ExceptNode implements QueryTreeNode {

    private final QueryTreeNode leftChild;
    private final QueryTreeNode rightChild;

    public ExceptNode(QueryTreeNode left, QueryTreeNode right) {
        this.leftChild = left;
        this.rightChild = right;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation leftResult = leftChild.evaluate(ra, knownRelations);
        Relation rightResult = rightChild.evaluate(ra, knownRelations);
        return ra.diff(leftResult, rightResult);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return this.leftChild.getRelationSchema(knownRelations);
    }
}
