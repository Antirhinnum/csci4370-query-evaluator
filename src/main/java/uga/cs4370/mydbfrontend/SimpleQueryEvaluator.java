package uga.cs4370.mydbfrontend;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.QueryTree;
import uga.cs4370.mydbfrontend.querytree.QueryTreeStatementVisitor;

import java.util.List;

/**
 * A class that can parse and evaluate simple SQL queries.
 */
public final class SimpleQueryEvaluator {

    private final ExtendedRA ra;
    private final List<Aliasable<Relation>> relations;
    private final StatementVisitor<QueryTree> visitor;

    public SimpleQueryEvaluator(ExtendedRA ra, List<Aliasable<Relation>> relations) {
        this.ra = ra;
        this.relations = relations;
        this.visitor = new QueryTreeStatementVisitor();
    }

    /**
     * Evaluates a given SQL query.
     *
     * @param query The query to evaluate.
     * @return The results of the evaluated query, or null either if an error occurred or if the query contained an
     * unsupported operation.
     */
    public Relation evaluate(final String query) {
        try {
            Statement parsedQuery = CCJSqlParserUtil.parse(query);
            if (parsedQuery == null) {
                return null;
            }

            QueryTree queryTree = parsedQuery.accept(this.visitor, this);
            if (queryTree == null) {
                return null;
            }

            return queryTree.evaluate(this.ra, this.relations);
        } catch (JSQLParserException e) {
            return null;
        }
    }
}
