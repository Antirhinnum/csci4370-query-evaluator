package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.RA;
import uga.cs4370.mydb.Relation;

import java.util.List;

/**
 * An extension of {@link RA} used to perform extra operations found in SQL.
 */
public interface ExtendedRA extends RA {

    /**
     * An extended version of {@link #project(Relation, List)}. Instead of just selecting attributes from an input relation,
     * this method allows selecting arbitrary data based on the input relation. This includes attributes, the all columns
     * expression ({@code *}), algebraic expressions, constant values, etc.
     *
     * @param rel                     The {@link Relation} to project from, or null. If null, a relation with one row will be produced.
     * @param projectedAttributesList The set of {@link ProjectedAttributes} to project.
     * @return A new {@link Relation} with all projected attributes.
     */
    Relation extendedProject(Relation rel, List<ProjectedAttributes> projectedAttributesList);
}