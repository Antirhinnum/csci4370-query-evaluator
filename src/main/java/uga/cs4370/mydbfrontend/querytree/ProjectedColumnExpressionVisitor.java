package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.SelectItem;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.Utils;
import uga.cs4370.mydbfrontend.expressionevaluation.ExpressionTypesEvaluator;
import uga.cs4370.mydbfrontend.expressionevaluation.ExpressionTypesEvaluatorImpl;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluator;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluatorImpl;
import uga.cs4370.mydbfrontend.extendedra.ProjectedAttributes;
import uga.cs4370.mydbfrontend.extendedra.RowValueProducer;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Visits an {@link Expression} and returns {@link ProjectedAttributes}. Intended for use with {@link SelectItem}.
 */
public class ProjectedColumnExpressionVisitor extends ExpressionVisitorAdapter<ProjectedAttributes> {

    private static <S> ProjectedAttributes visitAggregatingFunctionCall(Function function, S context, BiFunction<Relation, RowValueProducer, Cell> aggregatingFunction) {
        if (!(context instanceof SelectItem<?> selectItem)) {
            throw new IllegalArgumentException("context is not a SelectItem");
        }

        ExpressionList<?> parameters = function.getParameters();
        if (parameters.size() != 1) {
            throw new IllegalArgumentException("Aggregate functions should have exactly one parameter");
        }

        return new ProjectedAttributes() {

            @Override
            public boolean isAggregating() {
                return true;
            }

            @Override
            public List<String> getAttrNames(Relation schema) {
                if (selectItem.getAlias() != null) {
                    return List.of(selectItem.getAlias().getName());
                }
                return List.of(selectItem.getExpression().toString());
            }

            @Override
            public List<Type> getAttrTypes(Relation schema) {
                ExpressionTypesEvaluator evaluator = new ExpressionTypesEvaluatorImpl(schema);
                Type evaluated = evaluator.evaluate(selectItem.getExpression());
                return List.of(evaluated);
            }

            @Override
            public List<Cell> projectFromRow(Relation relation, List<Cell> row) {
                RowValueProducerExpressionVisitor producerVisitor = new RowValueProducerExpressionVisitor();
                RowValueProducer producer = function.getParameters().get(0).accept(producerVisitor, null);
                return List.of(aggregatingFunction.apply(relation, producer));
            }
        };
    }

    @Override
    protected <S> ProjectedAttributes visitExpression(Expression expression, S context) {
        if (!(context instanceof SelectItem<?> selectItem)) {
            throw new IllegalArgumentException("context is not a SelectItem");
        }

        return new ProjectedAttributes() {
            @Override
            public List<String> getAttrNames(Relation schema) {
                if (selectItem.getAlias() != null) {
                    return List.of(selectItem.getAlias().getName());
                }
                return List.of(selectItem.getExpression().toString());
            }

            @Override
            public List<Type> getAttrTypes(Relation schema) {
                ExpressionTypesEvaluator evaluator = new ExpressionTypesEvaluatorImpl(schema);
                Type evaluated = evaluator.evaluate(selectItem.getExpression());
                return List.of(evaluated);
            }

            @Override
            public List<Cell> projectFromRow(Relation relation, List<Cell> row) {
                RowExpressionEvaluator evaluator = new RowExpressionEvaluatorImpl(relation);
                Cell evaluated = evaluator.evaluate(selectItem.getExpression(), row);
                return List.of(evaluated);
            }
        };
    }

    @Override
    protected <S> ProjectedAttributes visitBinaryExpression(BinaryExpression binaryExpression, S context) {
        return this.visitExpression(binaryExpression, context);
    }

    @Override
    protected <S> ProjectedAttributes visitExpressions(Expression expression, S context, Collection<Expression> subExpressions) {
        return this.visitExpression(expression, context);
    }

    @Override
    public <S> ProjectedAttributes visit(AllColumns allColumns, S context) {
        return new ProjectedAttributes() {

            @Override
            public List<String> getAttrNames(Relation schema) {
                return schema.getAttrs();
            }

            @Override
            public List<Type> getAttrTypes(Relation schema) {
                return schema.getTypes();
            }

            @Override
            public List<Cell> projectFromRow(Relation relation, List<Cell> row) {
                return row;
            }
        };
    }

    @Override
    public <S> ProjectedAttributes visit(Function function, S context) {
        return switch (function.getName().trim().toLowerCase()) {
            case "avg" -> visitAggregatingFunctionCall(function, context, (relation, producer) -> {
                BigDecimal result = BigDecimal.ZERO;
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    result = result.add(Utils.parseCellToNumeric(value));
                }
                BigDecimal average = result.divide(new BigDecimal(relation.getSize()), MathContext.DECIMAL64);
                return Cell.val(average.doubleValue());
            });
            case "sum" -> visitAggregatingFunctionCall(function, context, (relation, producer) -> {
                BigDecimal result = BigDecimal.ZERO;
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    result = result.add(Utils.parseCellToNumeric(value));
                }
                return Cell.val(result.doubleValue());
            });
            case "min" -> visitAggregatingFunctionCall(function, context, (relation, producer) -> {
                BigDecimal result = BigDecimal.valueOf(Double.MAX_VALUE);
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    result = result.min(Utils.parseCellToNumeric(value));
                }
                return Cell.val(result.doubleValue());
            });
            case "max" -> visitAggregatingFunctionCall(function, context, (relation, producer) -> {
                BigDecimal result = BigDecimal.valueOf(-Double.MAX_VALUE);
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    result = result.max(Utils.parseCellToNumeric(value));
                }
                return Cell.val(result.doubleValue());
            });
            case "count" -> visitAggregatingFunctionCall(function, context, (relation, producer) -> {
                if (!function.isDistinct()) {
                    return Cell.val(relation.getSize());
                }

                int uniqueValues = 0;
                Set<Cell> knownValues = new HashSet<>();
                for (int i = 0; i < relation.getSize(); i++) {
                    List<Cell> row = relation.getRow(i);
                    Cell value = producer.getValueFromRow(relation, row);
                    if (!knownValues.add(value)) {
                        uniqueValues++;
                    }
                }
                return Cell.val(uniqueValues);
            });
            default -> super.visit(function, context);
        };
    }
}
