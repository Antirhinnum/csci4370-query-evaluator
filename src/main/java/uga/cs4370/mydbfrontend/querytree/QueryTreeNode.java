package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;

import java.util.List;

public interface QueryTreeNode {
    /**
     * @return The schema of the {@link Relation} this node will produce.
     */
    Relation getRelationSchema(List<Nameable<Relation>> knownRelations);
}
