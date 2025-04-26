package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.Expression;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluator;


public class ExpressionPredicateImpl extends ExpressionPredicate {

    private final RowExpressionEvaluator evaluator;
    private final Expression expression;

    public ExpressionPredicateImpl(RowExpressionEvaluator evaluator, Expression expression) {
        this.evaluator = evaluator;
        this.expression = expression;
    }

    @Override
    public RowExpressionEvaluator getEvaluator() {
        return this.evaluator;
    }

    @Override
    public Expression getExpression() {
        return this.expression;
    }
}