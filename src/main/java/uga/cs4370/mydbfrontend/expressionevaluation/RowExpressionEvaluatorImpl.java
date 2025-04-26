package uga.cs4370.mydbfrontend.expressionevaluation;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.Type;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * Visits an {@link Expression} and returns a {@link Cell} containing the evaluated value. Must be provided the row of
 * the relation the {@link Expression} is being evaluated for.
 */
public class RowExpressionEvaluatorImpl extends ExpressionVisitorAdapter<Cell> implements RowExpressionEvaluator {

    private final Relation schema;

    public RowExpressionEvaluatorImpl(Relation schema) {
        this.schema = schema;
    }

    public static Cell parseBooleanToCell(boolean b) {
        return Cell.val(b ? 1 : 0);
    }

    public static boolean parseCellToBoolean(Cell c) {
        if (c == null) return false;
        if (c.getType() != Type.INTEGER) return false;
        return c.getAsInt() != 0;
    }

    public static BigDecimal parseCellToNumeric(Cell c) {
        if (c == null) throw new NullPointerException("Cell was null");
        if (c.getType() == Type.STRING) return null;

        if (c.getType() == Type.DOUBLE) return BigDecimal.valueOf(c.getAsDouble());
        else if (c.getType() == Type.INTEGER) return BigDecimal.valueOf(c.getAsInt());
        else throw new UnsupportedOperationException("Cannot convert Cell to a numeric type");
    }

    @Override
    public <S> Cell visit(DoubleValue doubleValue, S context) {
        return Cell.val(doubleValue.getValue());
    }

    @Override
    public <S> Cell visit(LongValue longValue, S context) {
        return Cell.val(longValue.getValue());
    }

    @Override
    public <S> Cell visit(StringValue stringValue, S context) {
        return Cell.val(stringValue.getValue());
    }

    @Override
    public <S> Cell visit(BooleanValue booleanValue, S context) {
        return parseBooleanToCell(booleanValue.getValue());
    }

    @Override
    public <S> Cell visit(Addition addition, S context) {
        Cell leftCell = addition.getLeftExpression().accept(this, context);
        Cell rightCell = addition.getRightExpression().accept(this, context);

        BigDecimal left = parseCellToNumeric(leftCell);
        BigDecimal right = parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot add non-numeric values");
        BigDecimal sum = left.add(right, MathContext.DECIMAL64);

        if (leftCell.getType() == Type.INTEGER && rightCell.getType() == Type.INTEGER) {
            return Cell.val(sum.intValue());
        } else {
            return Cell.val(sum.doubleValue());
        }
    }

    @Override
    public <S> Cell visit(Division division, S context) {
        Cell leftCell = division.getLeftExpression().accept(this, context);
        Cell rightCell = division.getRightExpression().accept(this, context);

        BigDecimal left = parseCellToNumeric(leftCell);
        BigDecimal right = parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot divide non-numeric values");
        BigDecimal quotient = left.divide(right, MathContext.DECIMAL64);
        return Cell.val(quotient.doubleValue());
    }

    @Override
    public <S> Cell visit(IntegerDivision integerDivision, S context) {
        Cell leftCell = integerDivision.getLeftExpression().accept(this, context);
        Cell rightCell = integerDivision.getRightExpression().accept(this, context);

        BigDecimal left = parseCellToNumeric(leftCell);
        BigDecimal right = parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot divide non-numeric values");
        BigDecimal quotient = left.divideToIntegralValue(right, MathContext.DECIMAL64);
        return Cell.val(quotient.intValue());
    }

    @Override
    public <S> Cell visit(Multiplication multiplication, S context) {
        Cell leftCell = multiplication.getLeftExpression().accept(this, context);
        Cell rightCell = multiplication.getRightExpression().accept(this, context);

        BigDecimal left = parseCellToNumeric(leftCell);
        BigDecimal right = parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot multiply non-numeric values");
        BigDecimal product = left.multiply(right, MathContext.DECIMAL64);

        if (leftCell.getType() == Type.INTEGER && rightCell.getType() == Type.INTEGER) {
            return Cell.val(product.intValue());
        } else {
            return Cell.val(product.doubleValue());
        }
    }

    @Override
    public <S> Cell visit(Subtraction subtraction, S context) {
        Cell leftCell = subtraction.getLeftExpression().accept(this, context);
        Cell rightCell = subtraction.getRightExpression().accept(this, context);

        BigDecimal left = parseCellToNumeric(leftCell);
        BigDecimal right = parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot subtract non-numeric values");
        BigDecimal difference = left.subtract(right, MathContext.DECIMAL64);

        if (leftCell.getType() == Type.INTEGER && rightCell.getType() == Type.INTEGER) {
            return Cell.val(difference.intValue());
        } else {
            return Cell.val(difference.doubleValue());
        }
    }

    @Override
    public <S> Cell visit(AndExpression andExpression, S context) {
        Cell leftCell = andExpression.getLeftExpression().accept(this, context);
        Cell rightCell = andExpression.getRightExpression().accept(this, context);
        return parseBooleanToCell(parseCellToBoolean(leftCell) && parseCellToBoolean(rightCell));
    }

    @Override
    public <S> Cell visit(OrExpression orExpression, S context) {
        Cell leftCell = orExpression.getLeftExpression().accept(this, context);
        Cell rightCell = orExpression.getRightExpression().accept(this, context);
        return parseBooleanToCell(parseCellToBoolean(leftCell) || parseCellToBoolean(rightCell));
    }

    @Override
    public <S> Cell visit(XorExpression xorExpression, S context) {
        Cell leftCell = xorExpression.getLeftExpression().accept(this, context);
        Cell rightCell = xorExpression.getRightExpression().accept(this, context);
        return parseBooleanToCell(parseCellToBoolean(leftCell) ^ parseCellToBoolean(rightCell));
    }

    @Override
    public <S> Cell visit(Between between, S context) {
        Cell checkCell = between.getLeftExpression().accept(this, context);
        Cell lowerBoundCell = between.getBetweenExpressionStart().accept(this, context);
        Cell upperBoundCell = between.getBetweenExpressionEnd().accept(this, context);

        BigDecimal check = parseCellToNumeric(checkCell);
        BigDecimal lower = parseCellToNumeric(lowerBoundCell);
        BigDecimal upper = parseCellToNumeric(upperBoundCell);
        if (check == null || lower == null || upper == null)
            throw new IllegalArgumentException("Cannot check between non-numeric values");

        boolean result = check.compareTo(lower) >= 0 && check.compareTo(upper) <= 0;
        if (between.isNot()) {
            result = !result;
        }
        return parseBooleanToCell(result);
    }

    @Override
    public <S> Cell visit(EqualsTo equalsTo, S context) {
        Cell leftCell = equalsTo.getLeftExpression().accept(this, context);
        Cell rightCell = equalsTo.getRightExpression().accept(this, context);

        boolean result;
        if (leftCell.getType() == Type.STRING && rightCell.getType() == Type.STRING) {
            result = leftCell.equals(rightCell);
        } else {
            // Using Cell::equals() here would often fail since JSQLParser prefers parsing numbers as doubles instead of integers,
            // meaning the expression "year = 2005" would fail since year is an INTEGER Cell while 2005 is a DOUBLE Cell.
            BigDecimal left = parseCellToNumeric(leftCell);
            BigDecimal right = parseCellToNumeric(rightCell);
            result = left.compareTo(right) == 0;
        }
        return parseBooleanToCell(result);
    }

    @Override
    public <S> Cell visit(GreaterThan greaterThan, S context) {
        Cell leftCell = greaterThan.getLeftExpression().accept(this, context);
        Cell rightCell = greaterThan.getRightExpression().accept(this, context);

        BigDecimal left = parseCellToNumeric(leftCell);
        BigDecimal right = parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot compare non-numeric values using greater than");
        return parseBooleanToCell(left.compareTo(right) > 0);
    }

    @Override
    public <S> Cell visit(GreaterThanEquals greaterThanEquals, S context) {
        Cell leftCell = greaterThanEquals.getLeftExpression().accept(this, context);
        Cell rightCell = greaterThanEquals.getRightExpression().accept(this, context);

        BigDecimal left = parseCellToNumeric(leftCell);
        BigDecimal right = parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot compare non-numeric values using greater than or equal to");
        return parseBooleanToCell(left.compareTo(right) >= 0);
    }

    @Override
    public <S> Cell visit(MinorThan minorThan, S context) {
        Cell leftCell = minorThan.getLeftExpression().accept(this, context);
        Cell rightCell = minorThan.getRightExpression().accept(this, context);

        BigDecimal left = parseCellToNumeric(leftCell);
        BigDecimal right = parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot compare non-numeric values using less than");
        return parseBooleanToCell(left.compareTo(right) < 0);
    }

    @Override
    public <S> Cell visit(MinorThanEquals minorThanEquals, S context) {
        Cell leftCell = minorThanEquals.getLeftExpression().accept(this, context);
        Cell rightCell = minorThanEquals.getRightExpression().accept(this, context);

        BigDecimal left = parseCellToNumeric(leftCell);
        BigDecimal right = parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot compare non-numeric values using less than or equal to");
        return parseBooleanToCell(left.compareTo(right) <= 0);
    }

    @Override
    public <S> Cell visit(NotEqualsTo notEqualsTo, S context) {
        Cell leftCell = notEqualsTo.getLeftExpression().accept(this, context);
        Cell rightCell = notEqualsTo.getRightExpression().accept(this, context);

        boolean result;
        if (leftCell.getType() == Type.STRING && rightCell.getType() == Type.STRING) {
            result = !leftCell.equals(rightCell);
        } else {
            // See `<S> Cell visit(EqualsTo,S)`
            BigDecimal left = parseCellToNumeric(leftCell);
            BigDecimal right = parseCellToNumeric(rightCell);
            result = left.compareTo(right) != 0;
        }
        return parseBooleanToCell(result);
    }

    @Override
    public <S> Cell visit(Column column, S context) {

        // How I wish `context instanceof List<Cell>` worked.
        if (!(context instanceof List<?> row) || row.isEmpty() || !(row.get(0) instanceof Cell)) {
            throw new IllegalArgumentException("Cannot evaluate column without row instance");
        }

        String columnName;
        if (column.getTable() != null) {
            columnName = column.getTableName() + column.getTableDelimiter() + column.getColumnName();
        } else {
            columnName = column.getColumnName();
        }

        if (this.schema.hasAttr(columnName)) {
            int index = this.schema.getAttrIndex(columnName);
            return (Cell) row.get(index);
        }

        if (column.getTable() == null) {
            String columnNameWithDelimiter = "." + columnName;
            List<String> attrs = this.schema.getAttrs();
            for (int i = 0; i < attrs.size(); i++) {
                String attr = attrs.get(i);
                if (attr.endsWith(columnNameWithDelimiter)) {
                    return (Cell) row.get(i);
                }
            }
        }

        throw new RuntimeException("Failed to evaluate column '" + columnName + "'");
    }

    @Override
    public Cell evaluate(Expression expression, List<Cell> row) {
        return expression.accept(this, row);
    }
}
