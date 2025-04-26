package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.Type;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ProjectedColumns {

    private final Function<Relation, List<Type>> columnTypeGenerator;
    private final Function<Relation, List<String>> columnNameGenerator;
    private final BiFunction<Relation, List<Cell>, List<Cell>> rowValueGenerator;

    public ProjectedColumns(Function<Relation, List<Type>> columnTypeGenerator, Function<Relation, List<String>> columnNameGenerator, BiFunction<Relation, List<Cell>, List<Cell>> rowValueGenerator) {
        this.columnTypeGenerator = columnTypeGenerator;
        this.columnNameGenerator = columnNameGenerator;
        this.rowValueGenerator = rowValueGenerator;
    }

    public List<String> getColumnNames(Relation relation) {
        return columnNameGenerator.apply(relation);
    }

    public List<Type> getColumnTypes(Relation relation) {
        return columnTypeGenerator.apply(relation);
    }

    public List<Cell> getValuesForRow(Relation relation, List<Cell> cells) {
        return rowValueGenerator.apply(relation, cells);
    }
}