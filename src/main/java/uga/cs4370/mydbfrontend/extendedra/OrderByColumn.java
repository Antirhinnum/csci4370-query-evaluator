package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.Relation;

/**
 * A single column in a {@link Relation} to order that {@link Relation} by.
 */
public record OrderByColumn(RowValueProducer ordering, boolean ascending) {
}