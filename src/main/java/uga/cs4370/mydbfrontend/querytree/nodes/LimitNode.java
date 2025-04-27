package uga.cs4370.mydbfrontend.querytree.nodes;

import net.sf.jsqlparser.expression.Expression;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.expressionevaluation.ExpressionEvaluator;
import uga.cs4370.mydbfrontend.expressionevaluation.ExpressionEvaluatorImpl;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.RelationProducingQueryTreeNode;

import java.util.List;

public class LimitNode implements RelationProducingQueryTreeNode {
    private final RelationProducingQueryTreeNode child;
    private final Expression limitExpression;
    private final Expression offsetExpression;

    public LimitNode(RelationProducingQueryTreeNode child, Expression limitExpression, Expression offsetExpression) {
        this.child = child;
        this.limitExpression = limitExpression;
        this.offsetExpression = offsetExpression;
    }

    private static int coerceCellToInt(Cell cell) {
        if (cell == null) {
            throw new IllegalArgumentException("No value provided");
        }
        if (cell.getType() == Type.STRING) {
            throw new IllegalArgumentException("Cannot coerce a string to an integer");
        }
        if (cell.getType() == Type.DOUBLE) {
            throw new IllegalArgumentException("Cannot coerce a double to an integer");
        }

        if (cell.getType() == Type.INTEGER) {
            return cell.getAsInt();
        }

        throw new UnsupportedOperationException("Don't know how to coerce a cell of type " + cell.getType() + " into an integer");
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation rel = this.child.evaluate(ra, knownRelations);
        int limit, offset = 0;

        ExpressionEvaluator evaluator = new ExpressionEvaluatorImpl(rel);
        Cell limitValue = evaluator.evaluate(this.limitExpression);
        limit = coerceCellToInt(limitValue);

        if (this.offsetExpression != null) {
            Cell offsetValue = evaluator.evaluate(this.offsetExpression);
            offset = coerceCellToInt(offsetValue);
        }

        return ra.limit(rel, limit, offset);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return this.child.getRelationSchema(knownRelations);
    }
}
