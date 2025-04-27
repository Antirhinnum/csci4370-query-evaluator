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

    /**
     * Limits the number of rows in a {@link Relation}.
     *
     * @param rel    The {@link Relation} to operate on.
     * @param limit  The maximum number of rows the resulting {@link Relation} can have.
     * @param offset The number of rows to skip when selecting rows. If {@code rel} has fewer than {@code offset} rows, the resulting {@link Relation} will be empty.
     * @return A new {@link Relation} with at most {@code limit} rows.
     */
    Relation limit(Relation rel, int limit, int offset);

    /**
     * Deduplicates the rows of a {@link Relation}.
     *
     * @param rel The {@link Relation} to deduplicate.
     * @return A new {@link Relation} where no two columns are identical.
     */
    Relation distinct(Relation rel);

    /**
     * Orders the rows in {@code rel} by the orderings present in {@code orderings}.
     *
     * @param rel       The {@link Relation} to order.
     * @param orderings The columns to order {@code rel} by. If multiple are present, rows are sorted by all orderings sequentially.
     * @return A new {@link Relation} where rows are ordered by the specified columns.
     */
    Relation orderBy(Relation rel, List<OrderByColumn> orderings);

    /**
     * Takes the intersection of two relations.
     *
     * @return The resulting relation after applying the intersection operation.
     * @throws IllegalArgumentException If rel1 and rel2 are not compatible.
     */
    Relation intersect(Relation rel1, Relation rel2);
}