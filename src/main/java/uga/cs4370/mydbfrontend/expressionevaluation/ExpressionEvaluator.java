package uga.cs4370.mydbfrontend.expressionevaluation;

import net.sf.jsqlparser.expression.Expression;
import uga.cs4370.mydb.Cell;

/**
 * An object that can evaluate the result of an {@link Expression}. Cannot evaluate {@link Expression Expressions} that contain non-constant values.
 */
public interface ExpressionEvaluator {
    Cell evaluate(Expression expression);
}
