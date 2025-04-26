package uga.cs4370.mydbfrontend.expressionevaluation;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;

import java.util.List;

/**
 * Visits an {@link Expression} and returns a {@link Cell} containing the evaluated value. Must be provided the row of
 * the relation the {@link Expression} is being evaluated for.
 */
public class RowExpressionEvaluatorImpl extends ExpressionEvaluatorImpl implements RowExpressionEvaluator {

    public RowExpressionEvaluatorImpl(Relation schema) {
        super(schema);
    }

    @Override
    public <S> Cell visit(Column column, S context) {

        // How I wish `context instanceof List<Cell>` worked.
        if (!(context instanceof List<?> row) || row.isEmpty() || !(row.get(0) instanceof Cell)) {
            throw new IllegalArgumentException("Cannot evaluate column without row instance");
        }

        String columnName;
        if (column.getTable() != null) {
            columnName = column.getTableName() + column.getTableDelimiter() + column.getColumnName();
        } else {
            columnName = column.getColumnName();
        }

        if (this.schema.hasAttr(columnName)) {
            int index = this.schema.getAttrIndex(columnName);
            return (Cell) row.get(index);
        }

        if (column.getTable() == null) {
            String columnNameWithDelimiter = "." + columnName;
            List<String> attrs = this.schema.getAttrs();
            for (int i = 0; i < attrs.size(); i++) {
                String attr = attrs.get(i);
                if (attr.endsWith(columnNameWithDelimiter)) {
                    return (Cell) row.get(i);
                }
            }
        }

        throw new RuntimeException("Failed to evaluate column '" + columnName + "'");
    }

    @Override
    public Cell evaluate(Expression expression, List<Cell> row) {
        return expression.accept(this, row);
    }
}
