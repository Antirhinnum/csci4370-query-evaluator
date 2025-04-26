package uga.cs4370.mydbfrontend.querytree.nodes;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Nameable;
import uga.cs4370.mydbfrontend.Utils;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRAImpl;
import uga.cs4370.mydbfrontend.querytree.QueryTreeNode;
import uga.cs4370.mydbimpl.RAImpl;

import java.util.List;

public class RelationNode implements QueryTreeNode {

    private final String name;
    private final String alias;

    public RelationNode(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Nameable<Relation>> knownRelations) {
        Nameable<Relation> relation = getRelationFromKnownRelations(knownRelations);
        if (relation == null) {
            throw new RuntimeException("Unknown table \"" + name + "\"");
        }

        // Prepend column names with table name
        String aliasToUse = (this.alias != null) ? this.alias : relation.getName();
        Relation rel = relation.getValue();
        List<String> attrs = rel.getAttrs();
        List<String> renames = attrs.stream().map(a -> aliasToUse + "." + a).toList();
        return ra.rename(rel, attrs, renames);
    }

    @Override
    public Relation getRelationSchema(List<Nameable<Relation>> knownRelations) {
        return Utils.copySchema(this.evaluate(new ExtendedRAImpl(new RAImpl()), knownRelations));
    }

    private Nameable<Relation> getRelationFromKnownRelations(List<Nameable<Relation>> knownRelations) {
        for (Nameable<Relation> relation : knownRelations) {
            if (relation.getName().equals(this.name)) {
                return relation;
            }
        }
        return null;
    }
}
