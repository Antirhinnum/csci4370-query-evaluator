package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Predicate;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.RelationProducingQueryTreeNode;

import java.util.List;

public class SelectNode implements RelationProducingQueryTreeNode {
    private final RelationProducingQueryTreeNode child;
    private final Predicate predicate;

    public SelectNode(RelationProducingQueryTreeNode child, Predicate predicate) {
        this.child = child;
        this.predicate = predicate;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation result = this.child.evaluate(ra, knownRelations);
        return ra.select(result, this.predicate);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return this.child.getRelationSchema(knownRelations);
    }
}

