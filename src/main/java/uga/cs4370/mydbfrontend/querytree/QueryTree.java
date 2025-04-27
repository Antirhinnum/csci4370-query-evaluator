package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

import java.util.List;

/**
 * A tree that contains all workable information about an SQL query.
 */
public class QueryTree implements RelationProducingQueryTreeNode {
    private final RelationProducingQueryTreeNode root;

    public QueryTree(RelationProducingQueryTreeNode root) {
        if (root == null) {
            throw new NullPointerException("root is null");
        }
        this.root = root;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        return this.root.evaluate(ra, knownRelations);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return this.root.getRelationSchema(knownRelations);
    }
}

