package uga.cs4370.mydbfrontend.querytree;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.*;
import uga.cs4370.mydb.Predicate;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.SimpleQueryEvaluator;
import uga.cs4370.mydbfrontend.expressionevaluation.RowExpressionEvaluatorImpl;
import uga.cs4370.mydbfrontend.extendedra.OrderByColumn;
import uga.cs4370.mydbfrontend.extendedra.ProjectedAttributes;
import uga.cs4370.mydbfrontend.extendedra.RowValueProducer;
import uga.cs4370.mydbfrontend.querytree.nodes.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Visits a {@link Select} and returns a {@link RelationProducingQueryTreeNode} that can be used to evaluate that {@link Select}.
 */
public class QueryTreeNodeSelectVisitor extends SelectVisitorAdapter<RelationProducingQueryTreeNode> {
    private static RelationProducingQueryTreeNode evaluateJoins(RelationProducingQueryTreeNode sourceNode, List<Join> joins, SimpleQueryEvaluator evaluator) {
        FromItemVisitor<RelationProducingQueryTreeNode> fromItemVisitor = new QueryTreeNodeFromItemVisitor();
        for (Join join : joins) {
            RelationProducingQueryTreeNode addedNode = join.getRightItem().accept(fromItemVisitor, evaluator);
            if (join.isSimple() || join.isCross()) {
                sourceNode = new CartesianProductNode(sourceNode, addedNode);
            } else if (join.isInner()) {
                Collection<Expression> onExpressions = join.getOnExpressions();
                if (onExpressions != null && !onExpressions.isEmpty()) {
                    for (Expression expression : onExpressions) {
                        Relation schema = sourceNode.getRelationSchema(evaluator.getKnownRelations());
                        RowExpressionEvaluatorImpl rowEvaluator = new RowExpressionEvaluatorImpl(schema);
                        Predicate predicate = new ExpressionPredicateImpl(rowEvaluator, expression);
                        sourceNode = new ThetaJoinNode(sourceNode, addedNode, predicate);
                    }
                }
            } else if (join.isNatural()) {
                sourceNode = new NaturalJoinNode(sourceNode, addedNode);
            }
            // TODO: Support other join types
        }
        return sourceNode;
    }

    @Override
    public <S> RelationProducingQueryTreeNode visit(PlainSelect plainSelect, S context) {

        if (!(context instanceof SimpleQueryEvaluator evaluator)) {
            throw new IllegalArgumentException("context must be a SimpleQueryEvaluator");
        }

        RelationProducingQueryTreeNode sourceNode = null;
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem != null) {
            FromItemVisitor<RelationProducingQueryTreeNode> fromItemVisitor = new QueryTreeNodeFromItemVisitor();
            sourceNode = fromItem.accept(fromItemVisitor, context);
            List<Join> joins = plainSelect.getJoins();
            if (joins != null) {
                sourceNode = evaluateJoins(sourceNode, joins, evaluator);
            }
        }

        Expression whereClause = plainSelect.getWhere();
        if (sourceNode != null && whereClause != null) {
            Relation schema = sourceNode.getRelationSchema(evaluator.getKnownRelations());
            RowExpressionEvaluatorImpl rowEvaluator = new RowExpressionEvaluatorImpl(schema);
            Predicate predicate = new ExpressionPredicateImpl(rowEvaluator, whereClause);
            sourceNode = new SelectNode(sourceNode, predicate);
        }

        GroupsProducingQueryTreeNode groupsNode;
        GroupByElement groupByElement = plainSelect.getGroupBy();
        if (groupByElement != null) {
            RowValueProducerExpressionVisitor expressionVisitor = new RowValueProducerExpressionVisitor();
            List<RowValueProducer> groupingValueProducers = groupByElement.accept(expressionVisitor, context);
            groupsNode = new GroupByNode(sourceNode, groupingValueProducers);

            // TODO: HAVING
        } else {
            groupsNode = new GroupsProducingQueryTreeNodeAdapter(sourceNode);
        }

        List<SelectItem<?>> selectItems = plainSelect.getSelectItems();
        if (selectItems != null) {
            ProjectedColumnExpressionVisitor expressionVisitor = new ProjectedColumnExpressionVisitor();
            List<ProjectedAttributes> projectedColumns = new ArrayList<>();
            for (SelectItem<?> selectItem : selectItems) {
                projectedColumns.add(selectItem.accept(expressionVisitor, selectItem));
            }
            sourceNode = new ExtendedProjectNode(groupsNode, projectedColumns);
        }

        Distinct distinct = plainSelect.getDistinct();
        if (distinct != null) {
            // TODO: DISTINCT ON (a, b, ...)
            sourceNode = new DistinctNode(sourceNode);
        }

        List<OrderByElement> orderBys = plainSelect.getOrderByElements();
        if (orderBys != null && !orderBys.isEmpty()) {
            RowValueProducerExpressionVisitor expressionVisitor = new RowValueProducerExpressionVisitor();
            List<OrderByColumn> orderByColumns = new ArrayList<>();
            for (OrderByElement orderByElement : orderBys) {
                orderByColumns.add(orderByElement.accept(expressionVisitor, null));
            }
            sourceNode = new OrderByNode(sourceNode, orderByColumns);
        }

        Limit limit = plainSelect.getLimit();
        if (limit != null) {
            Expression limitExpression = limit.getRowCount();
            Expression offsetExpression = limit.getOffset();
            if (offsetExpression == null && plainSelect.getOffset() != null) {
                Offset offsetFromSelect = plainSelect.getOffset();
                offsetExpression = offsetFromSelect.getOffset();
            }
            sourceNode = new LimitNode(sourceNode, limitExpression, offsetExpression);
        }

        return sourceNode;
    }

    @Override
    public <S> RelationProducingQueryTreeNode visit(SetOperationList setOpList, S context) {

        // Evaluate set operations left-to-right.
        List<Select> selects = setOpList.getSelects();
        if (selects.isEmpty()) {
            throw new RuntimeException("Don't know how to evaluate set operations list with no selects");
        }

        List<SetOperation> setOps = setOpList.getOperations();
        RelationProducingQueryTreeNode leftNode = selects.get(0).accept(this, context);
        for (int i = 1; i < selects.size(); i++) {
            RelationProducingQueryTreeNode rightNode = selects.get(i).accept(this, context);
            SetOperation operation = setOps.get(i - 1);

            if (operation instanceof UnionOp) {
                leftNode = new UnionNode(leftNode, rightNode);
            } else if (operation instanceof MinusOp || operation instanceof ExceptOp) {
                leftNode = new ExceptNode(leftNode, rightNode);
            } else if (operation instanceof IntersectOp) {
                leftNode = new IntersectNode(leftNode, rightNode);
            } else {
                // Unsupported operation
                throw new UnsupportedOperationException("Cannot handle operation " + operation);
            }
        }
        return leftNode;
    }
}

