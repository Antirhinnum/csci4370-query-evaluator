package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.GroupedRelation;
import uga.cs4370.mydbfrontend.extendedra.ProjectedAttributes;
import uga.cs4370.mydbfrontend.querytree.GroupsProducingQueryTreeNode;
import uga.cs4370.mydbfrontend.querytree.RelationProducingQueryTreeNode;

import java.util.ArrayList;
import java.util.List;

public class ExtendedProjectNode implements RelationProducingQueryTreeNode {
    private final GroupsProducingQueryTreeNode child;
    private final List<ProjectedAttributes> projectedColumns;

    public ExtendedProjectNode(GroupsProducingQueryTreeNode child, List<ProjectedAttributes> projectedColumns) {
        this.child = child;
        this.projectedColumns = projectedColumns;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        List<GroupedRelation> input = new ArrayList<>();
        if (this.child != null) {
            input.addAll(this.child.evaluateGroups(ra, knownRelations));
        }
        Relation result = getRelationSchema(knownRelations);
        for (GroupedRelation group : input) {
            Relation projection = ra.extendedProject(group, this.projectedColumns);
            for (int i = 0; i < projection.getSize(); i++) {
                result.insert(projection.getRow(i));
            }
        }
        return result;
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {

        Relation childSchema = null;
        if (this.child != null) {
            childSchema = this.child.getRelationSchema(knownRelations);
        }

        List<String> attrNames = new ArrayList<>();
        List<Type> attrTypes = new ArrayList<>();
        for (ProjectedAttributes p : this.projectedColumns) {
            attrNames.addAll(p.getAttrNames(childSchema));
            attrTypes.addAll(p.getAttrTypes(childSchema));
        }

        return new RelationBuilder().attributeNames(attrNames).attributeTypes(attrTypes).build();
    }
}

