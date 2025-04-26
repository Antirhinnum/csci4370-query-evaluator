package uga.cs4370.mydbfrontend.expressionevaluation;

import net.sf.jsqlparser.expression.Expression;
import uga.cs4370.mydb.Type;

/**
 * An object that determines the types used in an {@link Expression}.
 */
public interface ExpressionTypesEvaluator {
    Type evaluate(Expression expression);
}
