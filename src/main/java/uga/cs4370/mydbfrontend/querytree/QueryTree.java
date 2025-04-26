package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

import java.util.List;

/**
 * A tree that contains all workable information about an SQL query.
 */
public class QueryTree implements QueryTreeNode {
    private final QueryTreeNode root;

    public QueryTree(QueryTreeNode root) {
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

