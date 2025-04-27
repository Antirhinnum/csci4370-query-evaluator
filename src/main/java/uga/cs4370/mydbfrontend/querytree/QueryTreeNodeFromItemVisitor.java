package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import uga.cs4370.mydbfrontend.querytree.nodes.RelationNode;

public class QueryTreeNodeFromItemVisitor extends FromItemVisitorAdapter<RelationProducingQueryTreeNode> {
    @Override
    public <S> RelationProducingQueryTreeNode visit(Table table, S context) {
        String alias = table.getAlias() != null ? table.getAlias().getName() : null;
        return new RelationNode(table.getName(), alias);
    }

    @Override
    public <S> RelationProducingQueryTreeNode visit(ParenthesedSelect select, S context) {
        SelectVisitor<RelationProducingQueryTreeNode> selectVisitor = new QueryTreeNodeSelectVisitor();
        return select.accept(selectVisitor, context);
    }

    @Override
    public <S> RelationProducingQueryTreeNode visit(PlainSelect plainSelect, S context) {
        SelectVisitor<RelationProducingQueryTreeNode> selectVisitor = new QueryTreeNodeSelectVisitor();
        return plainSelect.accept(selectVisitor, context);
    }

    @Override
    public <S> RelationProducingQueryTreeNode visit(SetOperationList setOperationList, S context) {
        SelectVisitor<RelationProducingQueryTreeNode> selectVisitor = new QueryTreeNodeSelectVisitor();
        return setOperationList.accept(selectVisitor, context);
    }
}

