package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Predicate;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Aliasable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

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
    public Relation evaluate(ExtendedRA ra, List<Aliasable<Relation>> relations) {
        Relation leftResult = this.leftChild.evaluate(ra, relations);
        Relation rightResult = this.rightChild.evaluate(ra, relations);
        return ra.join(leftResult, rightResult, this.predicate);
    }
}
