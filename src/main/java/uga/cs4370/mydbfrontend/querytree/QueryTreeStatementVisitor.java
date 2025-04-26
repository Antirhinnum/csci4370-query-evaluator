package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectVisitor;

public class QueryTreeStatementVisitor extends StatementVisitorAdapter<QueryTree> {
    private final SelectVisitor<QueryTreeNode> selectVisitor = new QueryTreeNodeSelectVisitor();

    @Override
    public <S> QueryTree visit(Select select, S context) {
        return new QueryTree(select.accept(selectVisitor, context));
    }
}

