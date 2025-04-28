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
import uga.cs4370.mydbfrontend.expressionevaluation.ExpressionTypesEvaluator;
import uga.cs4370.mydbfrontend.expressionevaluation.ExpressionTypesEvaluatorImpl;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluator;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluatorImpl;
import uga.cs4370.mydbfrontend.extendedra.GroupedRelation;
import uga.cs4370.mydbfrontend.extendedra.ProjectedAttributes;

import java.util.Collection;
import java.util.List;

/**
 * Visits an {@link Expression} and returns {@link ProjectedAttributes}. Intended for use with {@link SelectItem}.
 */
public class ProjectedColumnExpressionVisitor extends ExpressionVisitorAdapter<ProjectedAttributes> {

    private static <S> ProjectedAttributes visitAggregatingFunctionCall(Function function, S context) {
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
            public List<Cell> projectFromRow(GroupedRelation relation, List<Cell> row) {
                RowExpressionEvaluator evaluator = new RowExpressionEvaluatorImpl(relation);
                evaluator.setCurrentRelation(relation);
                Cell evaluated = evaluator.evaluate(selectItem.getExpression(), row);
                return List.of(evaluated);
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
            public List<Cell> projectFromRow(GroupedRelation relation, List<Cell> row) {
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
            public List<Cell> projectFromRow(GroupedRelation relation, List<Cell> row) {
                return row;
            }
        };
    }

    @Override
    public <S> ProjectedAttributes visit(Function function, S context) {
        return switch (function.getName().trim().toLowerCase()) {
            case "avg", "sum", "min", "max", "count" -> visitAggregatingFunctionCall(function, context);
            default -> super.visit(function, context);
        };
    }
}
