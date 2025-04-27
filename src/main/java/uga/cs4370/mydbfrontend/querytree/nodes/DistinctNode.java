package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.QueryTreeNode;

import java.util.List;

public class DistinctNode implements QueryTreeNode {
    private final QueryTreeNode child;

    public DistinctNode(QueryTreeNode child) {
        this.child = child;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation relation = this.child.evaluate(ra, knownRelations);
        return ra.distinct(relation);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return this.child.getRelationSchema(knownRelations);
    }
}
