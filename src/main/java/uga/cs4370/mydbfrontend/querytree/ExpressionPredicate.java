package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.Expression;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Predicate;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluator;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluatorImpl;

import java.util.List;

/**
 * A {@link Predicate} that is evaluated using an {@link Expression} and a {@link RowExpressionEvaluator}.
 */
public abstract class ExpressionPredicate implements Predicate {

    public abstract RowExpressionEvaluator getEvaluator();

    public abstract Expression getExpression();

    @Override
    public final boolean check(List<Cell> row) {
        Cell result = this.getEvaluator().evaluate(this.getExpression(), row);
        return RowExpressionEvaluatorImpl.parseCellToBoolean(result);
    }
}
