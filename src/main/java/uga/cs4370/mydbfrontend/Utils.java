package uga.cs4370.mydbfrontend;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;

import java.util.List;

/**
 * Utilities for this project.
 */
public class Utils {
    /**
     * Creates a new {@link Relation} with the schema of {@code relation} and no rows.
     *
     * @param relation The {@link Relation} with the schema to copy.
     * @return A copy of {@code relation}'s schema.
     */
    public static Relation copySchema(Relation relation) {
        if (relation == null) {
            throw new NullPointerException("relation is null");
        }

        List<String> attrNames = relation.getAttrs();
        List<Type> attrTypes = relation.getTypes();
        return new RelationBuilder().attributeNames(attrNames).attributeTypes(attrTypes).build();
    }
}