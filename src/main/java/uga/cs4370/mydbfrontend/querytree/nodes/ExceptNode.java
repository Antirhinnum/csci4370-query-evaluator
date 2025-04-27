package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.RelationProducingQueryTreeNode;

import java.util.List;

public class ExceptNode implements RelationProducingQueryTreeNode {

    private final RelationProducingQueryTreeNode leftChild;
    private final RelationProducingQueryTreeNode rightChild;

    public ExceptNode(RelationProducingQueryTreeNode left, RelationProducingQueryTreeNode right) {
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
