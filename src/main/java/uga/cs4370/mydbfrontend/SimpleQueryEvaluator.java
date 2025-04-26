package uga.cs4370.mydbfrontend;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.querytree.QueryTree;
import uga.cs4370.mydbfrontend.querytree.QueryTreeStatementVisitor;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that can parse and evaluate simple SQL queries.
 */
public final class SimpleQueryEvaluator {

    private final ExtendedRA ra;
    private final List<Nameable<Relation>> knownRelations;
    private final StatementVisitor<QueryTree> visitor;

    public SimpleQueryEvaluator(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        this.ra = ra;
        this.knownRelations = knownRelations;
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

            return queryTree.evaluate(this.ra, this.knownRelations);
        } catch (JSQLParserException e) {
            return null;
        }
    }

    /**
     * @return A {@link List} of all {@link Relation Relations} this evaluator knows about. {@link Relation Relations} not in this list cannot be used.
     */
    public List<Nameable<Relation>> getKnownRelations() {
        return new ArrayList<>(this.knownRelations);
    }
}