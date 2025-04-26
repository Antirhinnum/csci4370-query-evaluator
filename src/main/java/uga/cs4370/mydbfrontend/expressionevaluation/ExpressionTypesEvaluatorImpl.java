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
import java.util.List;

public class ExpressionTypesEvaluatorImpl extends ExpressionVisitorAdapter<Type> implements ExpressionTypesEvaluator {
    protected final Relation schema;

    public ExpressionTypesEvaluatorImpl(Relation schema) {
        this.schema = schema;
    }

    public static BigDecimal parseCellToNumeric(Cell c) {
        if (c == null) throw new NullPointerException("Cell was null");
        if (c.getType() == Type.STRING) return null;

        if (c.getType() == Type.DOUBLE) return BigDecimal.valueOf(c.getAsDouble());
        else if (c.getType() == Type.INTEGER) return BigDecimal.valueOf(c.getAsInt());
        else throw new UnsupportedOperationException("Cannot convert Cell to a numeric type");
    }

    @Override
    public <S> Type visit(DoubleValue doubleValue, S context) {
        return Type.DOUBLE;
    }

    @Override
    public <S> Type visit(LongValue longValue, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(StringValue stringValue, S context) {
        return Type.STRING;
    }

    @Override
    public <S> Type visit(BooleanValue booleanValue, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(Addition addition, S context) {
        Type leftType = addition.getLeftExpression().accept(this, context);
        Type rightType = addition.getRightExpression().accept(this, context);

        if (leftType == Type.STRING || rightType == Type.STRING)
            throw new IllegalArgumentException("Cannot add non-numeric values");

        if (leftType == Type.INTEGER && rightType == Type.INTEGER) {
            return Type.INTEGER;
        } else {
            return Type.DOUBLE;
        }
    }

    @Override
    public <S> Type visit(Division division, S context) {
        Type leftType = division.getLeftExpression().accept(this, context);
        Type rightType = division.getRightExpression().accept(this, context);

        if (leftType == Type.STRING || rightType == Type.STRING)
            throw new IllegalArgumentException("Cannot divide non-numeric values");

        return Type.DOUBLE;
    }

    @Override
    public <S> Type visit(IntegerDivision integerDivision, S context) {
        Type leftType = integerDivision.getLeftExpression().accept(this, context);
        Type rightType = integerDivision.getRightExpression().accept(this, context);

        if (leftType == Type.STRING || rightType == Type.STRING)
            throw new IllegalArgumentException("Cannot add non-numeric values");

        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(Multiplication multiplication, S context) {
        Type leftType = multiplication.getLeftExpression().accept(this, context);
        Type rightType = multiplication.getRightExpression().accept(this, context);

        if (leftType == Type.STRING || rightType == Type.STRING)
            throw new IllegalArgumentException("Cannot multiply non-numeric values");

        if (leftType == Type.INTEGER && rightType == Type.INTEGER) {
            return Type.INTEGER;
        } else {
            return Type.DOUBLE;
        }
    }

    @Override
    public <S> Type visit(Subtraction subtraction, S context) {
        Type leftType = subtraction.getLeftExpression().accept(this, context);
        Type rightType = subtraction.getRightExpression().accept(this, context);

        if (leftType == Type.STRING || rightType == Type.STRING)
            throw new IllegalArgumentException("Cannot subtract non-numeric values");

        if (leftType == Type.INTEGER && rightType == Type.INTEGER) {
            return Type.INTEGER;
        } else {
            return Type.DOUBLE;
        }
    }

    @Override
    public <S> Type visit(AndExpression andExpression, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(OrExpression orExpression, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(XorExpression xorExpression, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(Between between, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(EqualsTo equalsTo, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(GreaterThan greaterThan, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(GreaterThanEquals greaterThanEquals, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(MinorThan minorThan, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(MinorThanEquals minorThanEquals, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(NotEqualsTo notEqualsTo, S context) {
        return Type.INTEGER;
    }

    @Override
    public <S> Type visit(Column column, S context) {

        String columnName;
        if (column.getTable() != null) {
            columnName = column.getTableName() + column.getTableDelimiter() + column.getColumnName();
        } else {
            columnName = column.getColumnName();
        }

        if (this.schema.hasAttr(columnName)) {
            int index = this.schema.getAttrIndex(columnName);
            return this.schema.getTypes().get(index);
        }

        if (column.getTable() == null) {
            String columnNameWithDelimiter = "." + columnName;
            List<String> attrs = this.schema.getAttrs();
            for (int i = 0; i < attrs.size(); i++) {
                String attr = attrs.get(i);
                if (attr.endsWith(columnNameWithDelimiter)) {
                    return this.schema.getTypes().get(i);
                }
            }
        }

        throw new RuntimeException("Failed to evaluate column '" + columnName + "'");
    }

    @Override
    public Type evaluate(Expression expression) {
        return expression.accept(this, null);
    }
}
