package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import uga.cs4370.mydbfrontend.querytree.nodes.RelationNode;

public class QueryTreeNodeFromItemVisitor extends FromItemVisitorAdapter<QueryTreeNode> {
    @Override
    public <S> QueryTreeNode visit(Table table, S context) {
        String alias = table.getAlias() != null ? table.getAlias().getName() : null;
        return new RelationNode(table.getName(), alias);
    }

    @Override
    public <S> QueryTreeNode visit(ParenthesedSelect select, S context) {
        SelectVisitor<QueryTreeNode> selectVisitor = new QueryTreeNodeSelectVisitor();
        return select.accept(selectVisitor, context);
    }

    @Override
    public <S> QueryTreeNode visit(PlainSelect plainSelect, S context) {
        SelectVisitor<QueryTreeNode> selectVisitor = new QueryTreeNodeSelectVisitor();
        return plainSelect.accept(selectVisitor, context);
    }

    @Override
    public <S> QueryTreeNode visit(SetOperationList setOperationList, S context) {
        SelectVisitor<QueryTreeNode> selectVisitor = new QueryTreeNodeSelectVisitor();
        return setOperationList.accept(selectVisitor, context);
    }
}

