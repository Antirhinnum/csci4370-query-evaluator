package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.Type;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ProjectedAttributesImpl implements ProjectedAttributes {
    private final Function<Relation, List<Type>> columnTypeGenerator;
    private final Function<Relation, List<String>> columnNameGenerator;
    private final BiFunction<Relation, List<Cell>, List<Cell>> rowValueGenerator;

    public ProjectedAttributesImpl(Function<Relation, List<Type>> columnTypeGenerator, Function<Relation, List<String>> columnNameGenerator, BiFunction<Relation, List<Cell>, List<Cell>> rowValueGenerator) {
        this.columnTypeGenerator = columnTypeGenerator;
        this.columnNameGenerator = columnNameGenerator;
        this.rowValueGenerator = rowValueGenerator;
    }

    public List<String> getAttrNames(Relation schema) {
        return columnNameGenerator.apply(schema);
    }

    public List<Type> getAttrTypes(Relation schema) {
        return columnTypeGenerator.apply(schema);
    }

    public List<Cell> projectFromRow(Relation schema, List<Cell> row) {
        return rowValueGenerator.apply(schema, row);
    }
}
