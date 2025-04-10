package uga.cs4370.mydbfrontend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
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

    // TODO: Needs a way to access a Relation instance of a table by name
    private final RA ra;
    private final Map<String, Relation> tables;
    private final ExpressionVisitor<Void> expressionVisitor;
    private final FromItemVisitor<Void> fromItemVisitor;
    private final SelectVisitor<Void> selectVisitor;
    private final SelectItemVisitor<Void> selectItemVisitor;

    private final List<String> columnsToInclude;
    private final List<String> relationsToInclude;
    private Optional<Expression> whereExpression;
    private boolean allColumns;

    public RASQLVisitor(RA ra, Map<String, Relation> tables) {
        this.allColumns = false;
        this.columnsToInclude = new ArrayList<>();
        this.relationsToInclude = new ArrayList<>();
        this.whereExpression = null;

        this.ra = ra;
        this.tables = tables;
        this.expressionVisitor = new RASQLExpressionVisitor();
        this.fromItemVisitor = new RASQLFromItemVisitor();
        this.selectVisitor = new RASQLSelectVisitor();
        this.selectItemVisitor = new RASQLSelectItemVisitor();
    }

    @Override
    public <S> Relation visit(Select select, S context) {
        select.getPlainSelect().accept(this.selectVisitor, context);
        // All fields should be populated now.

        if (this.relationsToInclude.isEmpty()) {
            return new RelationBuilder()
                    .attributeNames(List.of())
                    .attributeTypes(List.of())
                    .build();
        }

        Relation result = this.tables.get(this.relationsToInclude.get(0));
        for (int i = 1; i < this.relationsToInclude.size(); i++) {
            result = ra.cartesianProduct(result, this.tables.get(this.relationsToInclude.get(i)));
        }
        // TODO: Joins
        // TODO: Aliases

        if (this.whereExpression.isPresent()) {
            // TODO: Apply where clause
        }

        if (!this.allColumns) {
            result = ra.project(result, this.columnsToInclude);
        }

        return result;
    }

    private final class RASQLExpressionVisitor extends ExpressionVisitorAdapter<Void> {

        @Override
        public <S> Void visit(AllColumns allColumns, S context) {
            RASQLVisitor.this.allColumns = true;
            return super.visit(allColumns, context);
        }

        @Override
        public <S> Void visit(Column column, S context) {
            columnsToInclude.add(column.getColumnName());
            return super.visit(column, context);
        }

        @Override
        public <S> Void visit(GreaterThan greaterThan, S context) {
            greaterThan.getLeftExpression().accept(this, context);
            greaterThan.getRightExpression().accept(this, context);
            return super.visit(greaterThan, context);
        }
    }

    private final class RASQLFromItemVisitor extends FromItemVisitorAdapter<Void> {

        @Override
        public <S> Void visit(Table table, S context) {
            RASQLVisitor.this.relationsToInclude.add(table.getName());
            return super.visit(table, context);
        }
    }

    private final class RASQLSelectVisitor extends SelectVisitorAdapter<Void> {

        @Override
        public <S> Void visit(PlainSelect plainSelect, S context) {
            plainSelect.getSelectItems().forEach(s -> s.accept(selectItemVisitor, null));
            plainSelect.getFromItem().accept(RASQLVisitor.this.fromItemVisitor);
            // plainSelect.getWhere().accept(RASQLVisitor.this.expressionVisitor); // TODO: getWhere() is just an Expression, maybe store it and evaluate it directly instead of breaking it up?
            
            return super.visit(plainSelect, context);
        }
    }

    private final class RASQLSelectItemVisitor extends SelectItemVisitorAdapter<Void> {

        @Override
        public <S> Void visit(SelectItem<? extends Expression> item, S context) {
            item.getExpression().accept(expressionVisitor);
            return super.visit(item, context);
        }
    }
}
