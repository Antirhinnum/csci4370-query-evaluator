package uga.cs4370.mydbfrontend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Predicate;
import uga.cs4370.mydb.RA;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;

/**
 * Visits an {@link net.sf.jsqlparser.statement.select.Select SQL query} and
 * produces a {@link uga.cs4370.mydb.Relation relation} with the results.
 */
public class RASQLVisitor extends StatementVisitorAdapter<Relation> {

    private final RA ra;
    private final Map<String, Relation> knownRelations;
    private final FromItemVisitor<Void> fromItemVisitor;
    private final SelectVisitor<Void> selectVisitor;

    private final List<SelectItem<?>> selectItemsFromQuery;
    private final List<String> constantSelectItemsFromQuery;
    private final List<Table> tablesFromQuery;
    private List<Aliasable<Relation>> relationsFromQuery;
    private List<String> attributesFromRelations;
    private Optional<Expression> whereExpression;
    private boolean allColumns;

    public RASQLVisitor(RA ra, Map<String, Relation> knownRelations) {
        this.allColumns = false;
        this.selectItemsFromQuery = new ArrayList<>();
        this.constantSelectItemsFromQuery = new ArrayList<>();
        this.tablesFromQuery = new ArrayList<>();
        this.attributesFromRelations = new ArrayList<>();
        this.whereExpression = Optional.empty();

        this.ra = ra;
        this.knownRelations = knownRelations;
        this.fromItemVisitor = new RASQLFromItemVisitor();
        this.selectVisitor = new RASQLSelectVisitor();
    }

    @Override
    public <S> Relation visit(Select select, S context) {
        select.getPlainSelect().accept(this.selectVisitor, context);
        // All fields should be populated now.

        // Process tables before processing columns since columns need to be associated with tables.
        relationsFromQuery = processTables();
        attributesFromRelations = relationsFromQuery.stream().flatMap(r -> r.getValue().getAttrs().stream()).toList();
        List<Aliasable<String>> columns = processSelectColumns();

        if (!constantSelectItemsFromQuery.isEmpty()) {
            List<String> names = new ArrayList<>();
            List<Type> types = new ArrayList<>();
            List<Cell> row = new ArrayList<>();
            for (var constant : constantSelectItemsFromQuery) {
                names.add(constant);
                types.add(Type.STRING);
                row.add(Cell.val(constant));
            }

            Relation constants = new RelationBuilder()
                    .attributeNames(names)
                    .attributeTypes(types)
                    .build();
            constants.insert(row);
            relationsFromQuery.add(new AliasableImpl<>(constants, "dual"));
        }

        // TODO: Explicit (natural or inner) join keyword
        Relation result = relationsFromQuery.get(0).getValue();
        for (int i = 1; i < relationsFromQuery.size(); i++) {
            result = ra.cartesianProduct(result, relationsFromQuery.get(i).getValue());
        }

        if (this.whereExpression.isPresent()) {
            // Process the where clause now that we know the attributes of the final relation
            ExpressionVisitor<Predicate> visitor = new RASQLExpressionLogicVisitor();
            Predicate predicate = this.whereExpression.get().accept(visitor, result);
            result = ra.select(result, predicate);
        }

        if (!this.allColumns) {
            List<String> columnNamesToProject = columns.stream()
                    .map(c -> c.getName())
                    .toList();
            result = ra.project(result, columnNamesToProject);
        }

        // Remove the leading table name from every attribute unless doing so would create duplicate attributes
        List<String> originalAttrs = result.getAttrs();
        List<String> desiredRenames = columns.stream().map(c -> {
            if (c.getAlias().isPresent()) {
                return c.getAlias().get();
            }
            return c.getValue();
        }).toList();
        Set<String> duplicateDesiredRenames = desiredRenames.stream()
                .filter(rename -> Collections.frequency(desiredRenames, rename) > 1)
                .collect(java.util.stream.Collectors.toSet());
        if (!duplicateDesiredRenames.isEmpty()) {
            for (int i = 0; i < originalAttrs.size(); i++) {
                String rename = desiredRenames.get(i);
                if (duplicateDesiredRenames.contains(rename)) {
                    desiredRenames.set(i, originalAttrs.get(i));
                }
            }
        }

        result = ra.rename(result, originalAttrs, desiredRenames);
        return result;
    }

    private List<Aliasable<Relation>> processTables() {
        List<Aliasable<Relation>> relations = new ArrayList<>();
        Set<String> knownTableNames = new HashSet<>(this.tablesFromQuery.size());
        for (Table table : this.tablesFromQuery) {
            String name = table.getName();
            Alias alias = table.getAlias();

            if (!this.knownRelations.containsKey(name)) {
                throw new UnsupportedOperationException("Unknown table " + name);
            }

            Relation relation = this.knownRelations.get(name);
            Aliasable<Relation> aliasableRelation;
            if (alias != null) {
                aliasableRelation = new AliasableImpl<>(relation, name, alias.getName());
            } else {
                aliasableRelation = new AliasableImpl<>(relation, name);
            }

            if (!knownTableNames.add(aliasableRelation.getNameOrAlias())) {
                throw new UnsupportedOperationException("Cannot process query where tables share a name");
            }

            // Prepend all attributes with the relations's alias/name
            List<String> originalAttributes = relation.getAttrs();
            List<String> newAttributes = originalAttributes.stream()
                    .map(a -> aliasableRelation.getNameOrAlias() + "." + a).toList();
            Relation renamedRelation = ra.rename(relation, originalAttributes, newAttributes);
            aliasableRelation.setValue(renamedRelation);

            relations.add(aliasableRelation);
        }
        return relations;
    }

    private List<Aliasable<String>> processSelectColumns() {
        List<Aliasable<String>> attributes = new ArrayList<>();
        ExpressionVisitor<Nameable<String>> visitor = new RASQLSelectItemExpressionVisitor();

        for (SelectItem<?> selectItem : this.selectItemsFromQuery) {
            Nameable<String> name = selectItem.getExpression().accept(visitor, selectItem);
            if (name == null) {
                // TODO: Mention that this operation is unsupported
                continue;
            }

            Aliasable<String> attribute = new AliasableImpl<>(name.getValue(), name.getName());
            Alias alias = selectItem.getAlias();
            if (alias != null) {
                attribute.setAlias(alias.getName());
            }

            attributes.add(attribute);
        }

        return attributes;
    }

    private String getTableQualifiedNameFromColumn(Column column) {
        String columnName = column.getColumnName();
        String tableName = column.getTableName();
        if (tableName != null) {
            Optional<Aliasable<Relation>> referencedTable = relationsFromQuery.stream()
                    .filter(r -> r.getNameOrAlias().equals(tableName))
                    .findFirst();

            if (referencedTable.isEmpty()) {
                throw new UnsupportedOperationException("Column " + columnName + " references an unknown table " + tableName);
            }

            String tableQualifiedName = tableName + "." + columnName;
            Relation table = referencedTable.get().getValue();
            if (table.getAttrs().stream().noneMatch(tableQualifiedName::equals)) {
                throw new UnsupportedOperationException("Table " + tableName + " doesn't contain column named " + columnName);
            }

            return tableQualifiedName;
        }

        // No explicit table name, so try to match the column's name to a known relation's attribute
        String nameSuffix = "." + columnName; // All attributes are of form "table.attr", so check for any that end with this attribute.
        List<String> possibleAttributes = attributesFromRelations.stream()
                .filter(a -> a.endsWith(nameSuffix)).toList();
        if (possibleAttributes.size() != 1) {
            throw new UnsupportedOperationException("Attribute " + columnName + " either doesn't exist or is ambiguous");
        }

        // Get the attribute's name in the correct format.
        return possibleAttributes.get(0);
    }

    private final class RASQLSelectItemExpressionVisitor extends ExpressionVisitorAdapter<Nameable<String>> {

        @Override
        public <S> Nameable<String> visit(AllColumns allColumns, S context) {
            RASQLVisitor.this.allColumns = true;
            return null;
        }

        @Override
        public <S> Nameable<String> visit(Column column, S context) {
            String listedValue;
            if (column.getTable() != null) {
                listedValue = column.getTableName() + column.getTableDelimiter() + column.getColumnName();
            } else {
                listedValue = column.getColumnName();
            }
            return new NameableImpl<>(listedValue, getTableQualifiedNameFromColumn(column));
        }

        @Override
        public <S> Nameable<String> visit(StringValue stringValue, S context) {
            constantSelectItemsFromQuery.add(stringValue.getValue());
            return new NameableImpl<>(stringValue.getValue(), stringValue.getValue());
        }
    }

    private final class RASQLFromItemVisitor extends FromItemVisitorAdapter<Void> {

        @Override
        public <S> Void visit(Table table, S context) {
            RASQLVisitor.this.tablesFromQuery.add(table);
            return super.visit(table, context);
        }
    }

    private final class RASQLSelectVisitor extends SelectVisitorAdapter<Void> {

        @Override
        public <S> Void visit(PlainSelect plainSelect, S context) {
            plainSelect.getSelectItems().forEach(s -> selectItemsFromQuery.add(s));
            FromItem fromItem = plainSelect.getFromItem();
            if (fromItem != null) {
                fromItem.accept(RASQLVisitor.this.fromItemVisitor);
            }
            List<Join> joins = plainSelect.getJoins();
            if (joins != null) {
                // TODO: Explicit (natural or inner) join keyword
                joins.forEach(j -> j.getFromItem().accept(RASQLVisitor.this.fromItemVisitor));
            }
            RASQLVisitor.this.whereExpression = Optional.ofNullable(plainSelect.getWhere());
            return super.visit(plainSelect, context);
        }
    }

    private final class RASQLExpressionLogicVisitor extends ExpressionVisitorAdapter<Predicate> {

        @Override
        public <S> Predicate visit(AndExpression andExpression, S context) {
            Predicate left = andExpression.getLeftExpression().accept(this, context);
            Predicate right = andExpression.getRightExpression().accept(this, context);
            return row -> left.check(row) && right.check(row);
        }

        @Override
        public <S> Predicate visit(OrExpression orExpression, S context) {
            Predicate left = orExpression.getLeftExpression().accept(this, context);
            Predicate right = orExpression.getRightExpression().accept(this, context);
            return row -> left.check(row) || right.check(row);
        }

        @Override
        public <S> Predicate visit(NotExpression notExpr, S context) {
            Predicate expression = notExpr.getExpression().accept(this, context);
            return row -> !expression.check(row);
        }

        private <S> Predicate getBinaryMathLogicPredicate(BinaryExpression expression, S context, BiPredicate<Double, Double> comparisonFunction) {

            if (!(context instanceof Relation relation)) {
                return null;
            }

            Expression left = expression.getLeftExpression();
            Expression right = expression.getRightExpression();
            ExpressionVisitor<Double> numberVisitor = new RASQLExpressionNumericValueVisitor();
            ExpressionVisitor<Column> columnVisitor = new RASQLExpressionColumnNumericValueVisitor();

            Double leftNumber = left.accept(numberVisitor, context);
            Double rightNumber = right.accept(numberVisitor, context);
            Column leftColumn = left.accept(columnVisitor, context);
            Column rightColumn = right.accept(columnVisitor, context);
            Integer leftColumnIndex = null, rightColumnIndex = null;

            // If columns are found, make sure they're numerically comparable
            if (leftColumn != null) {
                String leftName = getTableQualifiedNameFromColumn(leftColumn);
                if (!relation.hasAttr(leftName)) {
                    return null;
                }

                leftColumnIndex = relation.getAttrIndex(leftName);
                Type leftType = relation.getTypes().get(leftColumnIndex);
                if (leftType != Type.DOUBLE && leftType != Type.INTEGER) {
                    return null;
                }
            }
            if (rightColumn != null) {
                String rightName = getTableQualifiedNameFromColumn(rightColumn);
                if (!relation.hasAttr(rightName)) {
                    return null;
                }

                rightColumnIndex = relation.getAttrIndex(rightName);
                Type rightType = relation.getTypes().get(rightColumnIndex);
                if (rightType != Type.DOUBLE && rightType != Type.INTEGER) {
                    return null;
                }
            }

            if (leftNumber != null && rightNumber != null) {
                return new BinaryNumericComparisonPredicate(comparisonFunction, leftNumber, rightNumber);
            } else if (leftNumber != null && rightColumnIndex != null) {
                return new BinaryNumericComparisonPredicate(comparisonFunction, leftNumber, rightColumnIndex);
            } else if (leftColumnIndex != null && rightNumber != null) {
                return new BinaryNumericComparisonPredicate(comparisonFunction, leftColumnIndex, rightNumber);
            } else if (leftColumnIndex != null && rightColumnIndex != null) {
                return new BinaryNumericComparisonPredicate(comparisonFunction, leftColumnIndex, rightColumnIndex);
            } else {
                return null;
            }
        }

        private <S> Predicate getBinaryStringCompPredicate(BinaryExpression expression, S context, BiPredicate<String, String> comparisonFunction) {
            if (!(context instanceof Relation relation)) {
                return null;
            }

            Expression left = expression.getLeftExpression();
            Expression right = expression.getRightExpression();
            ExpressionVisitor<String> numberVisitor = new RASQLExpressionStringValueVisitor();
            ExpressionVisitor<Column> columnVisitor = new RASQLExpressionColumnNumericValueVisitor();

            String leftConstant = left.accept(numberVisitor, context);
            String rightConstant = right.accept(numberVisitor, context);
            Column leftColumn = left.accept(columnVisitor, context);
            Column rightColumn = right.accept(columnVisitor, context);
            Integer leftColumnIndex = null, rightColumnIndex = null;

            // If columns are found, make sure they're numerically comparable
            if (leftColumn != null) {
                String leftName = getTableQualifiedNameFromColumn(leftColumn);
                if (!relation.hasAttr(leftName)) {
                    return null;
                }

                leftColumnIndex = relation.getAttrIndex(leftName);
                Type leftType = relation.getTypes().get(leftColumnIndex);
                if (leftType != Type.STRING) {
                    return null;
                }
            }
            if (rightColumn != null) {
                String rightName = getTableQualifiedNameFromColumn(rightColumn);
                if (!relation.hasAttr(rightName)) {
                    return null;
                }

                rightColumnIndex = relation.getAttrIndex(rightName);
                Type rightType = relation.getTypes().get(rightColumnIndex);
                if (rightType != Type.STRING) {
                    return null;
                }
            }

            if (leftConstant != null && rightConstant != null) {
                return new BinaryStringComparisonPredicate(comparisonFunction, leftConstant, rightConstant);
            } else if (leftConstant != null && rightColumnIndex != null) {
                return new BinaryStringComparisonPredicate(comparisonFunction, leftConstant, rightColumnIndex);
            } else if (leftColumnIndex != null && rightConstant != null) {
                return new BinaryStringComparisonPredicate(comparisonFunction, leftColumnIndex, rightConstant);
            } else if (leftColumnIndex != null && rightColumnIndex != null) {
                return new BinaryStringComparisonPredicate(comparisonFunction, leftColumnIndex, rightColumnIndex);
            } else {
                return null;
            }
        }

        @Override
        public <S> Predicate visit(GreaterThan greaterThan, S context) {
            return getBinaryMathLogicPredicate(greaterThan, context, (a, b) -> a.compareTo(b) > 0);
        }

        @Override
        public <S> Predicate visit(GreaterThanEquals greaterThanEquals, S context) {
            return getBinaryMathLogicPredicate(greaterThanEquals, context, (a, b) -> a.compareTo(b) >= 0);
        }

        @Override
        public <S> Predicate visit(EqualsTo equalsTo, S context) {
            Predicate result = getBinaryMathLogicPredicate(equalsTo, context, (a, b) -> a.compareTo(b) == 0);
            if (result == null) {
                result = getBinaryStringCompPredicate(equalsTo, context, (a, b) -> a.equals(b));
            }
            return result;
        }

        @Override
        public <S> Predicate visit(NotEqualsTo notEqualsTo, S context) {
            Predicate result = getBinaryMathLogicPredicate(notEqualsTo, context, (a, b) -> a.compareTo(b) == 0);
            if (result == null) {
                result = getBinaryStringCompPredicate(notEqualsTo, context, (a, b) -> !a.equals(b));
            }
            return result;
        }

        @Override
        public <S> Predicate visit(MinorThanEquals minorThanEquals, S context) {
            return getBinaryMathLogicPredicate(minorThanEquals, context, (a, b) -> a.compareTo(b) <= 0);
        }

        @Override
        public <S> Predicate visit(MinorThan minorThan, S context) {
            return getBinaryMathLogicPredicate(minorThan, context, (a, b) -> a.compareTo(b) < 0);
        }

        private final class BinaryNumericComparisonPredicate implements Predicate {

            private final Double leftValue, rightValue;
            private final Integer leftColumnIndex, rightColumnIndex;
            private final BiPredicate<Double, Double> comparisonFunction;

            public BinaryNumericComparisonPredicate(BiPredicate<Double, Double> comparisonFunction, Double leftValue, Double rightValue) {
                this.comparisonFunction = comparisonFunction;
                this.leftValue = leftValue;
                this.rightValue = rightValue;
                this.leftColumnIndex = null;
                this.rightColumnIndex = null;
            }

            public BinaryNumericComparisonPredicate(BiPredicate<Double, Double> comparisonFunction, Double leftValue, Integer rightColumnIndex) {
                this.comparisonFunction = comparisonFunction;
                this.leftValue = leftValue;
                this.rightValue = null;
                this.leftColumnIndex = null;
                this.rightColumnIndex = rightColumnIndex;
            }

            public BinaryNumericComparisonPredicate(BiPredicate<Double, Double> comparisonFunction, Integer leftColumnIndex, Double rightValue) {
                this.comparisonFunction = comparisonFunction;
                this.leftValue = null;
                this.rightValue = rightValue;
                this.leftColumnIndex = leftColumnIndex;
                this.rightColumnIndex = null;
            }

            public BinaryNumericComparisonPredicate(BiPredicate<Double, Double> comparisonFunction, Integer leftColumnIndex, Integer rightColumnIndex) {
                this.comparisonFunction = comparisonFunction;
                this.leftValue = null;
                this.rightValue = null;
                this.leftColumnIndex = leftColumnIndex;
                this.rightColumnIndex = rightColumnIndex;
            }

            @Override
            public boolean check(List<Cell> row) {
                if (leftValue != null && rightValue != null) {
                    return comparisonFunction.test(leftValue, rightValue);
                } else if (leftValue != null && rightColumnIndex != null) {
                    Cell rightCell = row.get(rightColumnIndex);
                    Double rightCellValue = rightCell.getType() == Type.DOUBLE ? rightCell.getAsDouble() : rightCell.getAsInt();
                    return comparisonFunction.test(leftValue, rightCellValue);
                } else if (leftColumnIndex != null && rightValue != null) {
                    Cell leftCell = row.get(leftColumnIndex);
                    Double leftCellValue = leftCell.getType() == Type.DOUBLE ? leftCell.getAsDouble() : leftCell.getAsInt();
                    return comparisonFunction.test(leftCellValue, rightValue);
                } else if (leftColumnIndex != null && rightColumnIndex != null) {
                    Cell leftCell = row.get(leftColumnIndex);
                    Cell rightCell = row.get(rightColumnIndex);
                    Double leftCellValue = leftCell.getType() == Type.DOUBLE ? leftCell.getAsDouble() : leftCell.getAsInt();
                    Double rightCellValue = rightCell.getType() == Type.DOUBLE ? rightCell.getAsDouble() : rightCell.getAsInt();
                    return comparisonFunction.test(leftCellValue, rightCellValue);
                } else {
                    return false;
                }
            }
        }

        private final class BinaryStringComparisonPredicate implements Predicate {

            private final String leftValue, rightValue;
            private final Integer leftColumnIndex, rightColumnIndex;
            private final BiPredicate<String, String> comparisonFunction;

            public BinaryStringComparisonPredicate(BiPredicate<String, String> comparisonFunction, String leftValue, String rightValue) {
                this.comparisonFunction = comparisonFunction;
                this.leftValue = leftValue;
                this.rightValue = rightValue;
                this.leftColumnIndex = null;
                this.rightColumnIndex = null;
            }

            public BinaryStringComparisonPredicate(BiPredicate<String, String> comparisonFunction, String leftValue, Integer rightColumnIndex) {
                this.comparisonFunction = comparisonFunction;
                this.leftValue = leftValue;
                this.rightValue = null;
                this.leftColumnIndex = null;
                this.rightColumnIndex = rightColumnIndex;
            }

            public BinaryStringComparisonPredicate(BiPredicate<String, String> comparisonFunction, Integer leftColumnIndex, String rightValue) {
                this.comparisonFunction = comparisonFunction;
                this.leftValue = null;
                this.rightValue = rightValue;
                this.leftColumnIndex = leftColumnIndex;
                this.rightColumnIndex = null;
            }

            public BinaryStringComparisonPredicate(BiPredicate<String, String> comparisonFunction, Integer leftColumnIndex, Integer rightColumnIndex) {
                this.comparisonFunction = comparisonFunction;
                this.leftValue = null;
                this.rightValue = null;
                this.leftColumnIndex = leftColumnIndex;
                this.rightColumnIndex = rightColumnIndex;
            }

            @Override
            public boolean check(List<Cell> row) {
                if (leftValue != null && rightValue != null) {
                    return comparisonFunction.test(leftValue, rightValue);
                } else if (leftValue != null && rightColumnIndex != null) {
                    Cell rightCell = row.get(rightColumnIndex);
                    String rightCellValue = rightCell.getAsString();
                    return comparisonFunction.test(leftValue, rightCellValue);
                } else if (leftColumnIndex != null && rightValue != null) {
                    Cell leftCell = row.get(leftColumnIndex);
                    String leftCellValue = leftCell.getAsString();
                    return comparisonFunction.test(leftCellValue, rightValue);
                } else if (leftColumnIndex != null && rightColumnIndex != null) {
                    Cell leftCell = row.get(leftColumnIndex);
                    Cell rightCell = row.get(rightColumnIndex);
                    String leftCellValue = leftCell.getAsString();
                    String rightCellValue = rightCell.getAsString();
                    return comparisonFunction.test(leftCellValue, rightCellValue);
                } else {
                    return false;
                }
            }
        }
    }

    private final class RASQLExpressionNumericValueVisitor extends ExpressionVisitorAdapter<Double> {

        @Override
        public <S> Double visit(LongValue longValue, S context) {
            return (double) longValue.getValue();
        }

        @Override
        public <S> Double visit(DoubleValue doubleValue, S context) {
            return doubleValue.getValue();
        }
    }

    private final class RASQLExpressionStringValueVisitor extends ExpressionVisitorAdapter<String> {

        @Override
        public <S> String visit(StringValue stringValue, S context) {
            return stringValue.getValue();
        }
    }

    private final class RASQLExpressionColumnNumericValueVisitor extends ExpressionVisitorAdapter<Column> {

        @Override
        public <S> Column visit(Column column, S context) {
            return column;
        }

    }
}
