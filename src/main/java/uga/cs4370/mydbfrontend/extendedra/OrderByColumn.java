package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.Relation;

/**
 * A single column in a {@link Relation} to order that {@link Relation} by.
 */
public class OrderByColumn {
    private final RowOrdering ordering;
    private final boolean ascending;

    public OrderByColumn(RowOrdering ordering, boolean ascending) {
        this.ordering = ordering;
        this.ascending = ascending;
    }

    public RowOrdering getOrdering() {
        return ordering;
    }

    public boolean isAscending() {
        return ascending;
    }
}