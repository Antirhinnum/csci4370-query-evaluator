package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
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
import uga.cs4370.mydbfrontend.extendedra.ProjectedAttributes;

import java.util.List;

/**
 * Visits an {@link Expression} and returns {@link ProjectedAttributes}. Intended for use with {@link SelectItem}.
 */
public class ProjectedColumnExpressionVisitor extends ExpressionVisitorAdapter<ProjectedAttributes> {

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
            public List<Cell> projectFromRow(Relation schema, List<Cell> row) {
                RowExpressionEvaluator evaluator = new RowExpressionEvaluatorImpl(schema);
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
    protected <S> ProjectedAttributes visitExpressions(Expression expression, S context, ExpressionList<? extends Expression> subExpressions) {
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
            public List<Cell> projectFromRow(Relation schema, List<Cell> row) {
                return row;
            }
        };
    }
}
