package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Predicate;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.QueryTreeNode;

import java.util.ArrayList;
import java.util.List;

public class ThetaJoinNode implements QueryTreeNode {

    private final QueryTreeNode leftChild;
    private final QueryTreeNode rightChild;
    private final Predicate predicate;

    public ThetaJoinNode(QueryTreeNode leftChild, QueryTreeNode rightChild, Predicate predicate) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
        this.predicate = predicate;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation leftResult = this.leftChild.evaluate(ra, knownRelations);
        Relation rightResult = this.rightChild.evaluate(ra, knownRelations);
        return ra.join(leftResult, rightResult, this.predicate);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        Relation leftSchema = this.leftChild.getRelationSchema(knownRelations);
        Relation rightSchema = this.rightChild.getRelationSchema(knownRelations);

        List<String> attrNames = new ArrayList<>();
        attrNames.addAll(leftSchema.getAttrs());
        attrNames.addAll(rightSchema.getAttrs());

        List<Type> attrTypes = new ArrayList<>();
        attrTypes.addAll(leftSchema.getTypes());
        attrTypes.addAll(rightSchema.getTypes());

        return new RelationBuilder().attributeNames(attrNames).attributeTypes(attrTypes).build();
    }
}
