package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;

import java.util.List;

/**
 * An object that produces a value a row in a {@link Relation}.
 */
@FunctionalInterface
public interface RowValueProducer {
    Cell getValueFromRow(Relation schema, List<Cell> row);
}