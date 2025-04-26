package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.QueryTreeNode;

import java.util.List;

public class NaturalJoinNode implements QueryTreeNode {

    private final QueryTreeNode leftChild;
    private final QueryTreeNode rightChild;

    public NaturalJoinNode(QueryTreeNode leftChild, QueryTreeNode rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation leftResult = this.leftChild.evaluate(ra, knownRelations);
        Relation rightResult = this.rightChild.evaluate(ra, knownRelations);
        return ra.join(leftResult, rightResult);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {

        // TODO
        Relation leftSchema = this.leftChild.getRelationSchema(knownRelations);
        Relation rightSchema = this.rightChild.getRelationSchema(knownRelations);

        return null;
    }
}
