package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.Type;

import java.util.ArrayList;
import java.util.List;

public class GroupedRelationImpl implements GroupedRelation {

    private final Relation relation;
    private final List<Integer> groupedAttributeIndexes;

    public GroupedRelationImpl(Relation relation, List<Integer> groupedAttributeIndexes) {
        this.relation = relation;
        this.groupedAttributeIndexes = groupedAttributeIndexes;
    }

    @Override
    public List<Integer> getGroupedAttributeIndexes() {
        return new ArrayList<>(groupedAttributeIndexes);
    }

    @Override
    public int getSize() {
        return relation.getSize();
    }

    @Override
    public List<Cell> getRow(int i) {
        return relation.getRow(i);
    }

    @Override
    public List<Type> getTypes() {
        return relation.getTypes();
    }

    @Override
    public List<String> getAttrs() {
        return relation.getAttrs();
    }

    @Override
    public boolean hasAttr(String attr) {
        return relation.hasAttr(attr);
    }

    @Override
    public int getAttrIndex(String attr) {
        return relation.getAttrIndex(attr);
    }

    @Override
    public void insert(List<Cell> row) {
        relation.insert(row);
    }

    @Override
    public void loadData(String path) {
        relation.loadData(path);
    }

    @Override
    public void print() {
        relation.print();
    }
}
