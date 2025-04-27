package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.Relation;

import java.util.List;

/**
 * An extension of {@link Relation} that features groups.
 */
public interface GroupedRelation extends Relation {

    /**
     * @return The indexes of all attributes in this {@link Relation} that are grouped. Grouped attributes can be safely
     * selected in {@link ExtendedRA#extendedProject(GroupedRelation, List)} when aggregates are used.
     */
    List<Integer> getGroupedAttributeIndexes();
}
