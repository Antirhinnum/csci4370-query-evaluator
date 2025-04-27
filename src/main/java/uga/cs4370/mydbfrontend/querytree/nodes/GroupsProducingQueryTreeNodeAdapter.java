package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.GroupedRelation;
import uga.cs4370.mydbfrontend.extendedra.GroupedRelationImpl;
import uga.cs4370.mydbfrontend.querytree.GroupsProducingQueryTreeNode;
import uga.cs4370.mydbfrontend.querytree.RelationProducingQueryTreeNode;

import java.util.List;

public class GroupsProducingQueryTreeNodeAdapter implements GroupsProducingQueryTreeNode {
    private final RelationProducingQueryTreeNode wrapped;

    public GroupsProducingQueryTreeNodeAdapter(RelationProducingQueryTreeNode wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public List<GroupedRelation> evaluateGroups(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation result = wrapped.evaluate(ra, knownRelations);
        return List.of(new GroupedRelationImpl(result, List.of()));
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return wrapped.getRelationSchema(knownRelations);
    }
}
