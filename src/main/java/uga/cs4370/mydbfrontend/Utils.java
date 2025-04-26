package uga.cs4370.mydbfrontend;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;

import java.util.List;

public class Utils {
    public static Relation copySchema(Relation r) {
        List<String> attrNames = r.getAttrs();
        List<Type> attrTypes = r.getTypes();
        return new RelationBuilder().attributeNames(attrNames).attributeTypes(attrTypes).build();
    }
}
