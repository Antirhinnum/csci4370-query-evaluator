package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.statement.select.*;
import uga.cs4370.mydbfrontend.extendedra.ProjectedColumns;

import java.util.ArrayList;
import java.util.List;

public class QueryTreeNodeSelectVisitor extends SelectVisitorAdapter<QueryTreeNode> {
    @Override
    public <S> QueryTreeNode visit(PlainSelect plainSelect, S context) {

        FromItemVisitor<QueryTreeNode> fromItemVisitor = new QueryTreeNodeFromItemVisitor();
        QueryTreeNode sourceNode = null;
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem != null) {
            sourceNode = fromItem.accept(fromItemVisitor, context);
            List<Join> joins = plainSelect.getJoins();
            if (joins != null) {
                for (Join join : joins) {
                    QueryTreeNode addedNode = join.getRightItem().accept(fromItemVisitor, context);
                    if (join.isSimple() || join.isCross()) {
                        sourceNode = new CartesianProductNode(sourceNode, addedNode);
                    } else if (join.isInner()) {
                        // TODO: Evaluate predicate
                        sourceNode = new ThetaJoinNode(sourceNode, addedNode, null);
                    } else if (join.isNatural()) {
                        sourceNode = new NaturalJoinNode(sourceNode, addedNode);
                    } else {
                        // Unsupported join type
                        return null;
                    }
                }
            }

        } else {
            // TODO: Handle no source relations.
        }

        // TODO: Handle WHERE clause if one exists.
        // Each AND'ed predicate should be its own SelectNode

        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        if (selectItems != null) {

            ExpressionVisitor<ProjectedColumns> expressionVisitor = new ProjectedColumnExpressionVisitor();
            SelectItemVisitor<ProjectedColumns> selectItemVisitor = new SelectItemVisitor<>() {
                @Override
                public <S2> ProjectedColumns visit(SelectItem<? extends Expression> selectItem, S2 context) {
                    return selectItem.getExpression().accept(expressionVisitor, selectItem);
                }
            };

            List<ProjectedColumns> projectedColumns = new ArrayList<>();
            for (SelectItem<?> selectItem : selectItems) {
                projectedColumns.add(selectItem.accept(selectItemVisitor, context));
            }
            sourceNode = new ExtendedProjectNode(sourceNode, projectedColumns);
        }

        return sourceNode;
    }

    @Override
    public <S> QueryTreeNode visit(SetOperationList setOpList, S context) {

        // Evaluate set operations left-to-right.
        List<Select> selects = setOpList.getSelects();
        if (selects.isEmpty()) {
            return null;
        }

        List<SetOperation> setOps = setOpList.getOperations();
        QueryTreeNode leftNode = selects.get(0).accept(this, context);
        for (int i = 1; i < selects.size(); i++) {
            QueryTreeNode rightNode = selects.get(i).accept(this, context);
            SetOperation operation = setOps.get(i - 1);

            if (operation instanceof UnionOp) {
                leftNode = new UnionNode(leftNode, rightNode);
            } else if (operation instanceof MinusOp) {
                leftNode = new ExceptNode(leftNode, rightNode);
            } else {
                // Unsupported operation
                return null;
            }
        }
        return null;
    }
}

