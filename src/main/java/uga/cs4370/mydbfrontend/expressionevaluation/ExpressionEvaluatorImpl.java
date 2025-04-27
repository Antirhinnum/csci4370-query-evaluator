package uga.cs4370.mydbfrontend.expressionevaluation;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.Driver;
import uga.cs4370.mydbfrontend.Utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ExpressionEvaluatorImpl extends ExpressionVisitorAdapter<Cell> implements ExpressionEvaluator {
    protected final Relation schema;

    public ExpressionEvaluatorImpl(Relation schema) {
        this.schema = schema;
    }

    @Override
    public <S> Cell visit(DoubleValue doubleValue, S context) {
        return Cell.val(doubleValue.getValue());
    }

    @Override
    public <S> Cell visit(LongValue longValue, S context) {
        return Cell.val((int) longValue.getValue());
    }

    @Override
    public <S> Cell visit(HexValue hexValue, S context) {
        return hexValue.getLongValue().accept(this, context);
    }

    @Override
    public <S> Cell visit(StringValue stringValue, S context) {
        return Cell.val(stringValue.getValue());
    }

    @Override
    public <S> Cell visit(BooleanValue booleanValue, S context) {
        return Utils.parseBooleanToCell(booleanValue.getValue());
    }

    @Override
    public <S> Cell visit(Addition addition, S context) {
        Cell leftCell = addition.getLeftExpression().accept(this, context);
        Cell rightCell = addition.getRightExpression().accept(this, context);

        BigDecimal left = Utils.parseCellToNumeric(leftCell);
        BigDecimal right = Utils.parseCellToNumeric(rightCell);

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

        BigDecimal left = Utils.parseCellToNumeric(leftCell);
        BigDecimal right = Utils.parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot divide non-numeric values");
        BigDecimal quotient = left.divide(right, MathContext.DECIMAL64);
        return Cell.val(quotient.doubleValue());
    }

    @Override
    public <S> Cell visit(IntegerDivision integerDivision, S context) {
        Cell leftCell = integerDivision.getLeftExpression().accept(this, context);
        Cell rightCell = integerDivision.getRightExpression().accept(this, context);

        BigDecimal left = Utils.parseCellToNumeric(leftCell);
        BigDecimal right = Utils.parseCellToNumeric(rightCell);

        if (left == null || right == null) throw new IllegalArgumentException("Cannot divide non-numeric values");
        BigDecimal quotient = left.divideToIntegralValue(right, MathContext.DECIMAL64);
        return Cell.val(quotient.intValue());
    }

    @Override
    public <S> Cell visit(Multiplication multiplication, S context) {
        Cell leftCell = multiplication.getLeftExpression().accept(this, context);
        Cell rightCell = multiplication.getRightExpression().accept(this, context);

        BigDecimal left = Utils.parseCellToNumeric(leftCell);
        BigDecimal right = Utils.parseCellToNumeric(rightCell);

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

        BigDecimal left = Utils.parseCellToNumeric(leftCell);
        BigDecimal right = Utils.parseCellToNumeric(rightCell);

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
        if (Driver.DEBUG) {
            return Utils.parseBooleanToCell(Utils.parseCellToBoolean(leftCell) & Utils.parseCellToBoolean(rightCell));
        } else {
            return Utils.parseBooleanToCell(Utils.parseCellToBoolean(leftCell) && Utils.parseCellToBoolean(rightCell));
        }
    }

    @Override
    public <S> Cell visit(OrExpression orExpression, S context) {
        Cell leftCell = orExpression.getLeftExpression().accept(this, context);
        Cell rightCell = orExpression.getRightExpression().accept(this, context);
        if (Driver.DEBUG) {
            return Utils.parseBooleanToCell(Utils.parseCellToBoolean(leftCell) | Utils.parseCellToBoolean(rightCell));
        } else {
            return Utils.parseBooleanToCell(Utils.parseCellToBoolean(leftCell) || Utils.parseCellToBoolean(rightCell));
        }
    }

    @Override
    public <S> Cell visit(XorExpression xorExpression, S context) {
        Cell leftCell = xorExpression.getLeftExpression().accept(this, context);
        Cell rightCell = xorExpression.getRightExpression().accept(this, context);
        return Utils.parseBooleanToCell(Utils.parseCellToBoolean(leftCell) ^ Utils.parseCellToBoolean(rightCell));
    }

    @Override
    public <S> Cell visit(Between between, S context) {
        Cell checkCell = between.getLeftExpression().accept(this, context);
        Cell lowerBoundCell = between.getBetweenExpressionStart().accept(this, context);
        Cell upperBoundCell = between.getBetweenExpressionEnd().accept(this, context);

        BigDecimal check = Utils.parseCellToNumeric(checkCell);
        BigDecimal lower = Utils.parseCellToNumeric(lowerBoundCell);
        BigDecimal upper = Utils.parseCellToNumeric(upperBoundCell);
        if (check == null || lower == null || upper == null)
            throw new IllegalArgumentException("Cannot check between non-numeric values");

        boolean result = check.compareTo(lower) >= 0 && check.compareTo(upper) <= 0;
        return Utils.parseBooleanToCell(result ^ between.isNot());
    }

    @Override
    public <S> Cell visit(EqualsTo equalsTo, S context) {
        Cell leftCell = equalsTo.getLeftExpression().accept(this, context);
        Cell rightCell = equalsTo.getRightExpression().accept(this, context);
        return Utils.parseBooleanToCell(leftCell.equals(rightCell));
    }

    @Override
    public <S> Cell visit(GreaterThan greaterThan, S context) {
        Cell leftCell = greaterThan.getLeftExpression().accept(this, context);
        Cell rightCell = greaterThan.getRightExpression().accept(this, context);

        BigDecimal left = Utils.parseCellToNumeric(leftCell);
        BigDecimal right = Utils.parseCellToNumeric(rightCell);

        if (left == null || right == null)
            throw new IllegalArgumentException("Cannot compare non-numeric values using greater than");
        return Utils.parseBooleanToCell(left.compareTo(right) > 0);
    }

    @Override
    public <S> Cell visit(GreaterThanEquals greaterThanEquals, S context) {
        Cell leftCell = greaterThanEquals.getLeftExpression().accept(this, context);
        Cell rightCell = greaterThanEquals.getRightExpression().accept(this, context);

        BigDecimal left = Utils.parseCellToNumeric(leftCell);
        BigDecimal right = Utils.parseCellToNumeric(rightCell);

        if (left == null || right == null)
            throw new IllegalArgumentException("Cannot compare non-numeric values using greater than or equal to");
        return Utils.parseBooleanToCell(left.compareTo(right) >= 0);
    }

    @Override
    public <S> Cell visit(MinorThan minorThan, S context) {
        Cell leftCell = minorThan.getLeftExpression().accept(this, context);
        Cell rightCell = minorThan.getRightExpression().accept(this, context);

        BigDecimal left = Utils.parseCellToNumeric(leftCell);
        BigDecimal right = Utils.parseCellToNumeric(rightCell);

        if (left == null || right == null)
            throw new IllegalArgumentException("Cannot compare non-numeric values using less than");
        return Utils.parseBooleanToCell(left.compareTo(right) < 0);
    }

    @Override
    public <S> Cell visit(MinorThanEquals minorThanEquals, S context) {
        Cell leftCell = minorThanEquals.getLeftExpression().accept(this, context);
        Cell rightCell = minorThanEquals.getRightExpression().accept(this, context);

        BigDecimal left = Utils.parseCellToNumeric(leftCell);
        BigDecimal right = Utils.parseCellToNumeric(rightCell);

        if (left == null || right == null)
            throw new IllegalArgumentException("Cannot compare non-numeric values using less than or equal to");
        return Utils.parseBooleanToCell(left.compareTo(right) <= 0);
    }

    @Override
    public <S> Cell visit(NotEqualsTo notEqualsTo, S context) {
        Cell leftCell = notEqualsTo.getLeftExpression().accept(this, context);
        Cell rightCell = notEqualsTo.getRightExpression().accept(this, context);
        return Utils.parseBooleanToCell(!leftCell.equals(rightCell));
    }

    @Override
    public <S> Cell visit(LikeExpression likeExpression, S context) {
        Cell checkCell = likeExpression.getLeftExpression().accept(this, context);
        Cell patternCell = likeExpression.getRightExpression().accept(this, context);

        if (checkCell.getType() != Type.STRING || patternCell.getType() != Type.STRING) {
            throw new IllegalArgumentException("Cannot compare non-string values using LIKE");
        }

        Character escape = null;
        if (likeExpression.getEscape() != null) {
            Cell escapeCell = likeExpression.getEscape().accept(this, context);
            if (escapeCell.getType() != Type.STRING) {
                throw new IllegalArgumentException("ESCAPE must be a string");
            }
            escape = escapeCell.getAsString().charAt(0);
        }

        String regex = Utils.convertSqlPatternToRegex(patternCell.getAsString(), escape);
        Pattern pattern = Pattern.compile(regex);
        return Utils.parseBooleanToCell(pattern.matcher(checkCell.getAsString()).matches() ^ likeExpression.isNot());
    }

    @Override
    public <S> Cell visit(Function function, S context) {
        ExpressionList<?> parameters = function.getParameters();
        List<Cell> evaluatedParameters = parameters.stream().map(cell -> cell.accept(this, context)).toList();
        return switch (function.getName().trim().toLowerCase()) {
            case "concat" -> {
                List<String> strings = evaluatedParameters.stream().map(Cell::toString).toList();
                yield Cell.val(String.join("", strings));
            }
            case "substring" -> {
                if (evaluatedParameters.size() != 3) {
                    throw new IllegalArgumentException("Substring expects exactly three parameters");
                }
                if (evaluatedParameters.get(0).getType() != Type.STRING || evaluatedParameters.get(1).getType() != Type.INTEGER || evaluatedParameters.get(2).getType() != Type.INTEGER) {
                    throw new IllegalArgumentException("Substring expects the parameters (String, int, int), got (" + evaluatedParameters.stream().map(cell -> cell.getType().toString()).collect(Collectors.joining(", ")) + ")");
                }
                String toSubstring = evaluatedParameters.get(0).getAsString();
                int start = evaluatedParameters.get(1).getAsInt();
                int length = evaluatedParameters.get(2).getAsInt();
                if (start < 1) {
                    throw new IllegalArgumentException("Cannot substring before first character of string");
                }
                if (length < 0) {
                    throw new IllegalArgumentException("Length must be non-negative");
                }
                // SQL substring uses one-based indexing, Java uses zero-based indexing
                yield Cell.val(toSubstring.substring(start - 1, Math.min(start + length - 1, toSubstring.length())));
            }
            case "length" -> {
                if (evaluatedParameters.size() != 1) {
                    throw new IllegalArgumentException("Length expects exactly one parameter");
                }
                Cell argument = evaluatedParameters.get(0);
                if (argument.getType() != Type.STRING) {
                    throw new IllegalArgumentException("Length expects a string");
                }
                yield Cell.val(argument.getAsString().length());
            }
            case "trim" -> {
                if (evaluatedParameters.size() != 1) {
                    throw new IllegalArgumentException("Trim expects exactly one parameter");
                }
                Cell argument = evaluatedParameters.get(0);
                if (argument.getType() != Type.STRING) {
                    throw new IllegalArgumentException("Trim expects a string");
                }
                yield Cell.val(argument.getAsString().trim());
            }
            default -> throw new UnsupportedOperationException("Unknown function: " + function.getName());
        };
    }

    @Override
    public <S> Cell visit(NotExpression notExpr, S context) {
        Cell toNegate = notExpr.getExpression().accept(this, context);
        return Utils.parseBooleanToCell(!Utils.parseCellToBoolean(toNegate));
    }

    @Override
    public Cell evaluate(Expression expression) {
        return expression.accept(this, null);
    }
}
