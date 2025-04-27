package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitor;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluator;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluatorImpl;
import uga.cs4370.mydbfrontend.extendedra.OrderByColumn;
import uga.cs4370.mydbfrontend.extendedra.RowOrdering;

import java.util.Collection;

public class RowOrderingExpressionVisitor extends ExpressionVisitorAdapter<RowOrdering> implements OrderByVisitor<OrderByColumn> {
    @Override
    protected <S> RowOrdering visitExpression(Expression expression, S context) {
        return (schema, row) -> {
            RowExpressionEvaluator rowEvaluator = new RowExpressionEvaluatorImpl(schema);
            return rowEvaluator.evaluate(expression, row);
        };
    }

    @Override
    protected <S> RowOrdering visitBinaryExpression(BinaryExpression binaryExpression, S context) {
        return this.visitExpression(binaryExpression, context);
    }

    @Override
    protected <S> RowOrdering visitExpressions(Expression expression, S context, Collection<Expression> subExpressions) {
        return this.visitExpression(expression, context);
    }

    @Override
    public <S> OrderByColumn visit(OrderByElement orderBy, S context) {
        RowOrdering ordering = orderBy.getExpression().accept(this, context);
        return new OrderByColumn(ordering, orderBy.isAsc());
    }
}
