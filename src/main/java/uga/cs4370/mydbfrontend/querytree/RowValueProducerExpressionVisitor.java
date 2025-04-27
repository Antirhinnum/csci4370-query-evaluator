package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.select.GroupByElement;
import net.sf.jsqlparser.statement.select.GroupByVisitor;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.OrderByVisitor;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluator;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluatorImpl;
import uga.cs4370.mydbfrontend.extendedra.OrderByColumn;
import uga.cs4370.mydbfrontend.extendedra.RowValueProducer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Creates a {@link RowValueProducer} from an {@link Expression}. Expects no context.
 */
public class RowValueProducerExpressionVisitor extends ExpressionVisitorAdapter<RowValueProducer> implements OrderByVisitor<OrderByColumn>, GroupByVisitor<List<RowValueProducer>> {
    @Override
    protected <S> RowValueProducer visitExpression(Expression expression, S context) {
        return (schema, row) -> {
            RowExpressionEvaluator rowEvaluator = new RowExpressionEvaluatorImpl(schema);
            return rowEvaluator.evaluate(expression, row);
        };
    }

    @Override
    protected <S> RowValueProducer visitBinaryExpression(BinaryExpression binaryExpression, S context) {
        return this.visitExpression(binaryExpression, context);
    }

    @Override
    protected <S> RowValueProducer visitExpressions(Expression expression, S context, Collection<Expression> subExpressions) {
        return this.visitExpression(expression, context);
    }

    @Override
    public <S> OrderByColumn visit(OrderByElement orderBy, S context) {
        RowValueProducer ordering = orderBy.getExpression().accept(this, context);
        return new OrderByColumn(ordering, orderBy.isAsc());
    }

    @Override
    public <S> List<RowValueProducer> visit(GroupByElement groupBy, S context) {
        List<RowValueProducer> result = new ArrayList<>();
        ExpressionList<?> groupByExpressionList = groupBy.getGroupByExpressionList();
        for (Expression expression : groupByExpressionList) {
            result.add(expression.accept(this, context));
        }
        return result;
    }
}
