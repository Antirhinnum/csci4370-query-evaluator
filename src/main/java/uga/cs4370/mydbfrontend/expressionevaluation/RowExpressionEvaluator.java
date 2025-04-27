package uga.cs4370.mydbfrontend.expressionevaluation;

import net.sf.jsqlparser.expression.Expression;
import uga.cs4370.mydb.Cell;

import java.util.List;

/**
 * An object that can evaluate the result of an {@link Expression} given a certain row in a relation.
 */
public interface RowExpressionEvaluator extends ExpressionEvaluator {
    Cell evaluate(Expression expression, List<Cell> row);

    @Override
    default Cell evaluate(Expression expression) {
        return evaluate(expression, null);
    }
}