package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.RelationProducingQueryTreeNode;

import java.util.List;

public class IntersectNode implements RelationProducingQueryTreeNode {
    private final RelationProducingQueryTreeNode leftChild;
    private final RelationProducingQueryTreeNode rightChild;

    public IntersectNode(RelationProducingQueryTreeNode leftChild, RelationProducingQueryTreeNode rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation leftRelation = this.leftChild.evaluate(ra, knownRelations);
        Relation rightRelation = this.rightChild.evaluate(ra, knownRelations);
        return ra.intersect(leftRelation, rightRelation);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return this.leftChild.getRelationSchema(knownRelations);
    }
}
