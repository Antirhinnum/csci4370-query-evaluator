package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.OrderByColumn;
import uga.cs4370.mydbfrontend.querytree.QueryTreeNode;

import java.util.List;

public class OrderByNode implements QueryTreeNode {
    private final QueryTreeNode child;
    private final List<OrderByColumn> orderings;

    public OrderByNode(QueryTreeNode child, List<OrderByColumn> orderings) {
        this.child = child;
        this.orderings = orderings;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation relation = this.child.evaluate(ra, knownRelations);
        return ra.orderBy(relation, this.orderings);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return this.child.getRelationSchema(knownRelations);
    }
}
