package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.SelectItem;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.extendedra.ProjectedColumns;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ProjectedColumnExpressionVisitor extends ExpressionVisitorAdapter<ProjectedColumns> {

    private static <S> ProjectedColumns visitConstantWithAlias(Type type, BiFunction<Relation, List<Cell>, List<Cell>> generator, S context) {
        if (!(context instanceof SelectItem<?> selectItem)) {
            return null;
        }

        final String name = selectItem.getAlias() != null ? selectItem.getUnquotedAliasName() : selectItem.toString();
        return new ProjectedColumns(r -> List.of(type), r -> List.of(name), generator);
    }

    private static BigDecimal coerceCellToBigDecimal(Cell cell) {
        return switch (cell.getType()) {
            case INTEGER -> BigDecimal.valueOf(cell.getAsInt());
            case DOUBLE -> BigDecimal.valueOf(cell.getAsDouble());
            case STRING -> BigDecimal.ZERO;
        };
    }

    @Override
    public <S> ProjectedColumns visit(DoubleValue doubleValue, S context) {
        return visitConstantWithAlias(Type.DOUBLE, (r, row) -> List.of(Cell.val(doubleValue.getValue())), context);
    }

    @Override
    public <S> ProjectedColumns visit(LongValue longValue, S context) {
        return visitConstantWithAlias(Type.INTEGER, (r, row) -> List.of(Cell.val(longValue.getValue())), context);
    }

    @Override
    public <S> ProjectedColumns visit(StringValue stringValue, S context) {
        return visitConstantWithAlias(Type.STRING, (r, row) -> List.of(Cell.val(stringValue.getValue())), context);
    }

    @Override
    public <S> ProjectedColumns visit(Column column, S context) {
        if (!(context instanceof SelectItem<?> selectItem)) {
            return null;
        }

        final String name = Optional.ofNullable(selectItem.getUnquotedAliasName()).orElse(column.getColumnName());
        final String columnNameToSearchFor = (column.getTableName() != null ? column.getTableName() : "") + "." + column.getColumnName();
        return new ProjectedColumns(r -> {
            Optional<String> probableColumn = r.getAttrs().stream().filter(s -> s.endsWith(columnNameToSearchFor)).findFirst();
            if (probableColumn.isPresent()) {
                int index = r.getAttrs().indexOf(probableColumn.get());
                return List.of(r.getTypes().get(index));
            } else {
                return null;
            }
        }, r -> List.of(name), (r, row) -> {
            Optional<String> probableColumn = r.getAttrs().stream().filter(s -> s.endsWith(columnNameToSearchFor)).findFirst();
            if (probableColumn.isPresent()) {
                int index = r.getAttrs().indexOf(probableColumn.get());
                return List.of(row.get(index));
            } else {
                return null;
            }
        });
    }

    @Override
    public <S> ProjectedColumns visit(AllColumns allColumns, S context) {
        return new ProjectedColumns(Relation::getTypes, Relation::getAttrs, (r, row) -> new ArrayList<>(row));
    }

    @Override
    public <S> ProjectedColumns visit(Addition addition, S context) {
        return visitBinaryArithmeticExpressionWithAlias(addition, BigDecimal::add, context);
    }

    @Override
    public <S> ProjectedColumns visit(Subtraction subtraction, S context) {
        return visitBinaryArithmeticExpressionWithAlias(subtraction, BigDecimal::subtract, context);
    }

    @Override
    public <S> ProjectedColumns visit(Multiplication multiplication, S context) {
        return visitBinaryArithmeticExpressionWithAlias(multiplication, (a, b) -> a.multiply(b, MathContext.DECIMAL64), context);
    }

    @Override
    public <S> ProjectedColumns visit(Division division, S context) {
        return visitBinaryArithmeticExpressionWithAlias(division, (a, b) -> a.divide(b, MathContext.DECIMAL64), context);
    }

    @Override
    public <S> ProjectedColumns visit(IntegerDivision integerDivision, S context) {
        return visitBinaryArithmeticExpressionWithAlias(integerDivision, BigDecimal::divideToIntegralValue, context);
    }

    private <S> ProjectedColumns visitBinaryArithmeticExpressionWithAlias(BinaryExpression binaryExpression, BiFunction<BigDecimal, BigDecimal, BigDecimal> operation, S context) {
        if (!(context instanceof SelectItem<?> selectItem)) {
            return null;
        }

        final ProjectedColumns leftColumns = binaryExpression.getLeftExpression().accept(this, context);
        final ProjectedColumns rightColumns = binaryExpression.getRightExpression().accept(this, context);
        final String name = selectItem.getAlias() != null ? selectItem.getUnquotedAliasName() : selectItem.toString();

        Function<Relation, List<Type>> typesGenerator = rel -> {
            Type leftType = leftColumns.getColumnTypes(rel).get(0);
            Type rightType = rightColumns.getColumnTypes(rel).get(0);

            if (leftType == Type.DOUBLE || rightType == Type.DOUBLE) {
                return List.of(Type.DOUBLE);
            }

            return List.of(Type.INTEGER);
        };
        Function<Relation, List<String>> namesGenerator = rel -> List.of(name);
        BiFunction<Relation, List<Cell>, List<Cell>> rowGenerator = (rel, row) -> {
            Cell leftResult = leftColumns.getValuesForRow(rel, row).get(0);
            Cell rightResult = rightColumns.getValuesForRow(rel, row).get(0);

            BigDecimal leftValue = coerceCellToBigDecimal(leftResult);
            BigDecimal rightValue = coerceCellToBigDecimal(rightResult);
            BigDecimal resultValue = operation.apply(leftValue, rightValue);
            Cell result;

            if (leftResult.getType() == Type.DOUBLE || rightResult.getType() == Type.DOUBLE) {
                result = Cell.val(resultValue.doubleValue());
            } else {
                result = Cell.val(resultValue.intValue());
            }

            return List.of(result);
        };

        return new ProjectedColumns(typesGenerator, namesGenerator, rowGenerator);
    }
}
