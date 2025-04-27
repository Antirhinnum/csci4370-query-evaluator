package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.GroupedRelation;

import java.util.List;

public interface GroupsProducingQueryTreeNode extends QueryTreeNode {
    /**
     * Evaluate this node, producing a set of {@link GroupedRelation} instances.
     *
     * @param ra             An {@link ExtendedRA} that can be used to perform relational algebra operations.
     * @param knownRelations All permanent {@link Relation Relations} that the query processor knows about.
     * @return The results of evaluating the operation this node handles.
     */
    List<GroupedRelation> evaluateGroups(ExtendedRA ra, List<Nameable<Relation>> knownRelations);
}
