package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitor;

/**
 * Visits a {@link Select} and returns a {@link QueryTree}. Will return {@code null} for any other kind of {@link net.sf.jsqlparser.statement.Statement}.
 */
public class QueryTreeStatementVisitor extends StatementVisitorAdapter<QueryTree> {
    private final SelectVisitor<QueryTreeNode> selectVisitor = new QueryTreeNodeSelectVisitor();

    @Override
    public <S> QueryTree visit(Select select, S context) {
        return new QueryTree(select.accept(selectVisitor, context));
    }
}