package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Aliasable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

import java.util.List;

public class RelationNode implements QueryTreeNode {

    private final String name;
    private final String alias;

    public RelationNode(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    @Override
    public Relation evaluate(ExtendedRA ra, List<Aliasable<Relation>> relations) {
        for (Aliasable<Relation> relation : relations) {
            if (relation.getName().equals(name)) {
                relation.setAlias(this.alias);

                // Prepend column names with table name
                String aliasToUse = relation.getNameOrAlias();
                Relation rel = relation.getValue();
                List<String> attrs = rel.getAttrs();
                List<String> renames = attrs.stream().map(a -> aliasToUse + "." + a).toList();
                return ra.rename(rel, attrs, renames);
            }
        }

        return null;
    }
}
