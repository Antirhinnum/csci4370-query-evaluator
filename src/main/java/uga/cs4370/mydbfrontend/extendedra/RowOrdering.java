package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;

import java.util.List;

/**
 * An object that produces the value a row in a {@link Relation} should be ordered by.
 */
@FunctionalInterface
public interface RowOrdering {
    Cell getValueToOrderBy(Relation schema, List<Cell> row);
}