package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

import java.util.List;

/**
 * One node in a {@link QueryTree}.
 */
public interface QueryTreeNode {
    /**
     * Evaluate this node, producing a valid {@link Relation} instance.
     *
     * @param ra             An {@link ExtendedRA} that can be used to perform relational algebra operations.
     * @param knownRelations All permanent {@link Relation Relations} that the query processor knows about.
     * @return The results of evaluating the operation this node handles.
     */
    Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations);

    /**
     * @return The schema of the {@link Relation} this node will produce.
     */
    Relation getRelationSchema(List<Nameable<Relation>> knownRelations);
}
