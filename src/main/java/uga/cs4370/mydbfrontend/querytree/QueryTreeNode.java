package uga.cs4370.mydbfrontend.querytree;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydbfrontend.Aliasable;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;

import java.util.List;

public interface QueryTreeNode {
    Relation evaluate(ExtendedRA ra, List<Aliasable<Relation>> relations);
}
