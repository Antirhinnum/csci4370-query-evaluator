package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.Expression;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Predicate;
import uga.cs4370.mydbfrontend.Utils;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluator;

import java.util.List;

/**
 * A {@link Predicate} that is evaluated using an {@link Expression} and a {@link RowExpressionEvaluator}.
 */
public abstract class ExpressionPredicate implements Predicate {

    /**
     * @return The {@link RowExpressionEvaluator} used to evaluate the {@link #getExpression() Expression} this holds.
     */
    public abstract RowExpressionEvaluator getEvaluator();

    /**
     * @return The {@link Expression} that this evaluates.
     */
    public abstract Expression getExpression();

    @Override
    public final boolean check(List<Cell> row) {
        Cell result = this.getEvaluator().evaluate(this.getExpression(), row);
        return Utils.parseCellToBoolean(result);
    }
}
