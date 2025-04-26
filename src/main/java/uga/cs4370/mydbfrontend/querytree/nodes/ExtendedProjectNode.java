package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.ProjectedColumns;
import uga.cs4370.mydbfrontend.querytree.QueryTreeNode;

import java.util.ArrayList;
import java.util.List;

public class ExtendedProjectNode implements QueryTreeNode {
    private final QueryTreeNode child;
    private final List<ProjectedColumns> projectedColumns;

    public ExtendedProjectNode(QueryTreeNode child, List<ProjectedColumns> projectedColumns) {
        this.child = child;
        this.projectedColumns = projectedColumns;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Relation result = null;
        if (this.child != null) {
            result = this.child.evaluate(ra, knownRelations);
        }
        return ra.extendedProject(result, this.projectedColumns);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {

        Relation childSchema = null;
        if (this.child != null) {
            childSchema = this.child.getRelationSchema(knownRelations);
        }

        List<String> attrNames = new ArrayList<>();
        List<Type> attrTypes = new ArrayList<>();
        for (ProjectedColumns p : this.projectedColumns) {
            attrNames.addAll(p.getColumnNames(childSchema));
            attrTypes.addAll(p.getColumnTypes(childSchema));
        }

        return new RelationBuilder().attributeNames(attrNames).attributeTypes(attrTypes).build();
    }
}

