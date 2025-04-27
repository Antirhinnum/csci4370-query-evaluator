package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.Type;

import java.util.List;

/**
 * For use with {@link ExtendedRA#extendedProject(Relation, List)}. Allows creating columns in a {@link Relation} based on the {@link Relation Relation's} schema.
 */
public interface ProjectedAttributes {

    /**
     * @param schema The schema of the {@link Relation} to project from.
     * @return A {@link List} of attribute names that this {@link ProjectedAttributes} will project.
     */
    List<String> getAttrNames(Relation schema);

    /**
     * @param schema The schema of the {@link Relation} to project from.
     * @return A {@link List} of attribute types that this {@link ProjectedAttributes} will project.
     */
    List<Type> getAttrTypes(Relation schema);

    /**
     * @param relation The schema of the {@link Relation} to project from.
     * @param row      The row in a {@link Relation} to project values from.
     * @return A {@link List} of values that this {@link ProjectedAttributes} projects from {@code row}.
     */
    List<Cell> projectFromRow(Relation relation, List<Cell> row);
}