package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Predicate;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Aliasable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

import java.util.List;

public class SelectNode implements QueryTreeNode {
    private final QueryTreeNode child;
    private final Predicate predicate;

    public SelectNode(QueryTreeNode child, Predicate predicate) {
        this.child = child;
        this.predicate = predicate;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Aliasable<Relation>> relations) {
        Relation result = this.child.evaluate(ra, relations);
        return ra.select(result, this.predicate);
    }
}

