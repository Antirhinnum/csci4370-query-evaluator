package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.QueryTreeNode;

import java.util.ArrayList;
import java.util.List;

public class CartesianProductNode implements QueryTreeNode {

    private final QueryTreeNode leftChild;
    private final QueryTreeNode rightChild;

    public CartesianProductNode(QueryTreeNode leftChild, QueryTreeNode rightChild) {
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation leftResult = this.leftChild.evaluate(ra, knownRelations);
        Relation rightResult = this.rightChild.evaluate(ra, knownRelations);
        return ra.cartesianProduct(leftResult, rightResult);
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