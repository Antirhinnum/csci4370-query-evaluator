package uga.cs4370.mydbfrontend.expressionevaluation;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Utils;
import uga.cs4370.mydbfrontend.extendedra.RowValueProducer;
import uga.cs4370.mydbfrontend.querytree.RowValueProducerExpressionVisitor;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

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
        if (!(context instanceof List<?> row) || row.isEmpty()) {
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
    public <S> Cell visit(Function function, S context) {
        return switch (function.getName().trim().toLowerCase()) {
            case "avg" -> visitAggregatingFunctionCall(function, (relation, producer) -> {
                BigDecimal result = BigDecimal.ZERO;
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    result = result.add(Utils.parseCellToNumeric(value));
                }
                BigDecimal average = result.divide(new BigDecimal(relation.getSize()), MathContext.DECIMAL64);
                return Cell.val(average.doubleValue());
            });
            case "sum" -> visitAggregatingFunctionCall(function, (relation, producer) -> {
                BigDecimal result = BigDecimal.ZERO;
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    result = result.add(Utils.parseCellToNumeric(value));
                }
                return Cell.val(result.doubleValue());
            });
            case "min" -> visitAggregatingFunctionCall(function, (relation, producer) -> {
                BigDecimal result = BigDecimal.valueOf(Double.MAX_VALUE);
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    result = result.min(Utils.parseCellToNumeric(value));
                }
                return Cell.val(result.doubleValue());
            });
            case "max" -> visitAggregatingFunctionCall(function, (relation, producer) -> {
                BigDecimal result = BigDecimal.valueOf(-Double.MAX_VALUE);
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    result = result.max(Utils.parseCellToNumeric(value));
                }
                return Cell.val(result.doubleValue());
            });
            case "count" -> visitAggregatingFunctionCall(function, (relation, producer) -> {
                if (!function.isDistinct()) {
                    return Cell.val(relation.getSize());
                }

                int uniqueValues = 0;
                Set<Cell> knownValues = new HashSet<>();
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    if (knownValues.add(value)) {
                        uniqueValues++;
                    }
                }
                return Cell.val(uniqueValues);
            });
            default -> super.visit(function, context);
        };
    }

    private Cell visitAggregatingFunctionCall(Function function, BiFunction<Relation, RowValueProducer, Cell> aggregatingFunction) {
        if (getCurrentRelation() == null) {
            throw new IllegalArgumentException("Need a Relation to evaluate aggregate functions");
        }

        ExpressionList<?> parameters = function.getParameters();
        if (parameters.size() != 1) {
            throw new IllegalArgumentException("Aggregate functions should have exactly one parameter");
        }

        RowValueProducerExpressionVisitor producerVisitor = new RowValueProducerExpressionVisitor();
        RowValueProducer producer = function.getParameters().get(0).accept(producerVisitor, null);
        return aggregatingFunction.apply(getCurrentRelation(), producer);
    }

    @Override
    public Cell evaluate(Expression expression, List<Cell> row) {
        return expression.accept(this, row);
    }
}
