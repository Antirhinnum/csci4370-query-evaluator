package uga.cs4370.mydbfrontend;

import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;

import java.math.BigDecimal;
import java.util.List;

/**
 * Utilities for this project.
 */
public class Utils {
    private static final String REGEX_MATCH_ANY_SUBSTRING = ".*";
    private static final String REGEX_MATCH_SINGLE_CHARACTER = ".";
    private static final String REGEX_RESERVED_CHARACTERS = "\\[]{}()^&.$*+?!|<>:=";

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

    /**
     * Converts an SQL pattern (used in the {@code LIKE} keyword) into a regex pattern.
     *
     * @param pattern The pattern to convert.
     * @param escape  An optional string to escape pattern literals, or {@code null} if escaping isn't necessary.
     * @return A regex pattern that matches the same text as {@code pattern}.
     */
    public static String convertSqlPatternToRegex(String pattern, Character escape) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (escape != null && c == escape) {
                i++;
                if (i < pattern.length()) {
                    regex.append(pattern.charAt(i));
                }
                continue;
            }

            if (REGEX_RESERVED_CHARACTERS.indexOf(c) >= 0) {
                regex.append('\\');
            }

            switch (c) {
                case '%':
                    regex.append(REGEX_MATCH_ANY_SUBSTRING);
                    break;
                case '_':
                    regex.append(REGEX_MATCH_SINGLE_CHARACTER);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }

        return regex.toString();
    }

    /**
     * Creates a new {@link Relation} containing a description of the attributes found in {@code relation}.
     *
     * @param relation The {@link Relation} to describe.
     * @return A new {@link Relation} that describes {@code relation}.
     */
    public static Relation describeRelation(Relation relation) {
        if (relation == null) {
            throw new NullPointerException("relation is null");
        }

        Relation description = new RelationBuilder().attributeNames(List.of("attribute", "type")).attributeTypes(List.of(Type.STRING, Type.STRING)).build();

        for (int i = 0; i < relation.getAttrs().size(); i++) {
            List<Cell> row = List.of(Cell.val(relation.getAttrs().get(i)), Cell.val(relation.getTypes().get(i).toString()));
            description.insert(row);
        }

        return description;
    }

    public static Cell parseBooleanToCell(boolean b) {
        return Cell.val(b ? 1 : 0);
    }

    public static boolean parseCellToBoolean(Cell c) {
        if (c == null)
            throw new NullPointerException("Cannot parse null to boolean, this may be a result of an unimplemented operation");
        if (c.getType() != Type.INTEGER) return false;
        return c.getAsInt() != 0;
    }

    public static BigDecimal parseCellToNumeric(Cell c) {
        if (c == null) throw new NullPointerException("Cell was null");
        if (c.getType() == Type.STRING) return null;

        if (c.getType() == Type.DOUBLE) return BigDecimal.valueOf(c.getAsDouble());
        else if (c.getType() == Type.INTEGER) return BigDecimal.valueOf(c.getAsInt());
        else throw new UnsupportedOperationException("Cannot convert Cell to a numeric type");
    }
}