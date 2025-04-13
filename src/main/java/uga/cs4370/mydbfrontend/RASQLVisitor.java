package uga.cs4370.mydbfrontend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import uga.cs4370.mydb.RA;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;

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
    private final List<Table> tablesFromQuery;
    private Optional<Expression> whereExpression;
    private boolean allColumns;

    public RASQLVisitor(RA ra, Map<String, Relation> knownRelations) {
        this.allColumns = false;
        this.selectItemsFromQuery = new ArrayList<>();
        this.tablesFromQuery = new ArrayList<>();
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

        if (this.tablesFromQuery.isEmpty()) {
            return new RelationBuilder()
                    .attributeNames(List.of())
                    .attributeTypes(List.of())
                    .build();
        }

        // Process tables before processing columns since columns need to be associated with tables.
        List<Aliasable<Relation>> relations = processTables();
        List<Aliasable<String>> columns = processSelectColumns(relations);

        Relation result = relations.get(0).getValue();
        for (int i = 1; i < relations.size(); i++) {
            result = ra.cartesianProduct(result, relations.get(i).getValue());
        }

        if (this.whereExpression.isPresent()) {
            // TODO: Apply where clause
        }

        if (!this.allColumns) {
            List<String> columnNamesToProject = columns.stream()
                    .map(c -> c.getName())
                    .toList();
            result = ra.project(result, columnNamesToProject);
        }

        // Can't remove the table name from attributes because a relation cannot have attributes with the same name
        List<String> originalAttrs = result.getAttrs();
        List<String> renames = columns.stream().map(c -> {
            return c.getAlias().orElse(c.getName());
        }).toList();
        result = ra.rename(result, originalAttrs, renames);
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

    private List<Aliasable<String>> processSelectColumns(List<Aliasable<Relation>> relations) {
        List<Aliasable<String>> attributes = new ArrayList<>();
        // Every attribute present on the present relations.
        Stream<String> allKnownAttributes = relations.stream()
                .flatMap(r -> r.getValue().getAttrs().stream());
        ExpressionVisitor<String> visitor = new RASQLSelectItemExpressionVisitor();

        for (SelectItem<?> selectItem : this.selectItemsFromQuery) {
            String name = selectItem.getExpression().accept(visitor, null);
            if (name == null) {
                // TODO: Mention that this operation is unsupported
                continue;
            }

            if ("*".equals(name)) {
                this.allColumns = true;
                continue;
            }

            int periodIndex = name.indexOf(".");
            String tableName;
            if (periodIndex != -1) {
                // There is a table present here, try to match it to a known relation.
                tableName = name.substring(0, periodIndex);
                Optional<Aliasable<Relation>> referencedTable = relations.stream()
                        .filter(r -> r.getNameOrAlias().equals(tableName))
                        .findFirst();

                if (referencedTable.isEmpty()) {
                    throw new UnsupportedOperationException("Column " + name + " references an unknown table " + tableName);
                }

                Relation table = referencedTable.get().getValue();
                if (table.getAttrs().stream().noneMatch(name::equals)) {
                    throw new UnsupportedOperationException("Table " + tableName + " doesn't contain column named " + name.substring(periodIndex + 1));
                }

                // The attribute is now confirmed to exist, and is named in the proper format.
            } else {
                // No table present, try to find a known attribute this column could be referring to.
                String nameSuffix = "." + name; // All attributes are of form "table.attr", so check for any that end with this attribute.
                List<String> possibleAttributes = allKnownAttributes.filter(a -> a.endsWith(nameSuffix))
                        .toList();
                if (possibleAttributes.size() != 1) {
                    throw new UnsupportedOperationException("Attribute " + name + " either doesn't exist or is ambiguous");
                }

                // Get the attribute's name in the correct format.
                name = possibleAttributes.get(0);
            }

            // Value is unused here since attributes are just names
            Aliasable<String> attribute;
            Alias alias = selectItem.getAlias();
            if (alias != null) {
                attribute = new AliasableImpl<>(name, name, alias.getName());
            } else {
                attribute = new AliasableImpl<>(name, name);
            }

            attributes.add(attribute);
        }

        return attributes;
    }

    private final class RASQLSelectItemExpressionVisitor extends ExpressionVisitorAdapter<String> {

        @Override
        public <S> String visit(AllColumns allColumns, S context) {
            return "*";
        }

        @Override
        public <S> String visit(Column column, S context) {
            String tableName = column.getTableName();
            if (tableName != null) {
                return tableName + "." + column.getColumnName();
            } else {
                return column.getColumnName();
            }
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
            plainSelect.getFromItem().accept(RASQLVisitor.this.fromItemVisitor);
            List<Join> joins = plainSelect.getJoins();
            if (joins != null)
            {
                joins.forEach(j -> j.getFromItem().accept(RASQLVisitor.this.fromItemVisitor));
            }
            // plainSelect.getWhere().accept(RASQLVisitor.this.expressionVisitor);
            // TODO: getWhere() is just an Expression, maybe store it and evaluate it directly instead of breaking it up?

            return super.visit(plainSelect, context);
        }
    }
}
