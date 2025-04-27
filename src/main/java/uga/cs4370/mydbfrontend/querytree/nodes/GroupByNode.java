package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.GroupedRelation;
import uga.cs4370.mydbfrontend.extendedra.RowValueProducer;
import uga.cs4370.mydbfrontend.querytree.GroupsProducingQueryTreeNode;
import uga.cs4370.mydbfrontend.querytree.RelationProducingQueryTreeNode;

import java.util.List;

public class GroupByNode implements GroupsProducingQueryTreeNode {
    private final RelationProducingQueryTreeNode child;
    private final List<RowValueProducer> groupingValueProducers;

    public GroupByNode(RelationProducingQueryTreeNode child, List<RowValueProducer> groupingValueProducers) {
        this.child = child;
        this.groupingValueProducers = groupingValueProducers;
    }

    @Override
    public List<GroupedRelation> evaluateGroups(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation input = child.evaluate(ra, knownRelations);
        return ra.groupBy(input, groupingValueProducers);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return child.getRelationSchema(knownRelations);
    }
}
