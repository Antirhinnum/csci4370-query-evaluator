package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.GroupedRelation;
import uga.cs4370.mydbfrontend.extendedra.GroupedRelationImpl;
import uga.cs4370.mydbfrontend.querytree.ExpressionPredicate;
import uga.cs4370.mydbfrontend.querytree.GroupsProducingQueryTreeNode;

import java.util.ArrayList;
import java.util.List;

public class HavingNode implements GroupsProducingQueryTreeNode {
    private final GroupsProducingQueryTreeNode child;
    private final ExpressionPredicate predicate;

    public HavingNode(GroupsProducingQueryTreeNode child, ExpressionPredicate predicate) {
        this.child = child;
        this.predicate = predicate;
    }

    @Override
    public List<GroupedRelation> evaluateGroups(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        List<GroupedRelation> inputs = this.child.evaluateGroups(ra, knownRelations);
        List<GroupedRelation> results = new ArrayList<>();
        for (GroupedRelation groupedRelation : inputs) {
            this.predicate.getEvaluator().setCurrentRelation(groupedRelation);
            Relation result = ra.select(groupedRelation, this.predicate);
            if (result.getSize() != 0) {
                results.add(new GroupedRelationImpl(result, groupedRelation.getGroupedAttributeIndexes()));
            }
        }
        return results;
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return this.child.getRelationSchema(knownRelations);
    }
}
