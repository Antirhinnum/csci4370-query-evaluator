package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Aliasable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

import java.util.List;

/**
 * A tree that contains all workable information about an SQL query.
 */
public class QueryTree implements QueryTreeNode {
    private final QueryTreeNode root;

    public QueryTree(QueryTreeNode root) {
        this.root = root;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Aliasable<Relation>> relations) {
        return this.root.evaluate(ra, relations);
    }
}

