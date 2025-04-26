package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Aliasable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.ProjectedColumns;

import java.util.List;

public class ExtendedProjectNode implements QueryTreeNode {
    private final QueryTreeNode child;
    private final List<ProjectedColumns> projectedColumns;

    public ExtendedProjectNode(QueryTreeNode child, List<ProjectedColumns> projectedColumns) {
        this.child = child;
        this.projectedColumns = projectedColumns;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Aliasable<Relation>> relations) {
        Relation result = null;
        if (this.child != null) {
            result = this.child.evaluate(ra, relations);
        }
        return ra.extendedProject(result, this.projectedColumns);
    }
}

