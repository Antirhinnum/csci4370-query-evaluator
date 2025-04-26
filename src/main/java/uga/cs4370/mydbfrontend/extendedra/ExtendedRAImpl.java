package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.*;

import java.util.ArrayList;
import java.util.List;

public final class ExtendedRAImpl implements ExtendedRA {
    private final RA ra;

    public ExtendedRAImpl(RA ra) {
        this.ra = ra;
    }

    @Override
    public Relation extendedProject(Relation rel, List<ProjectedColumns> projectedColumns) {

        List<String> attrNames = new ArrayList<>();
        List<Type> attrTypes = new ArrayList<>();
        for (ProjectedColumns projectedColumn : projectedColumns) {
            attrNames.addAll(projectedColumn.getColumnNames(rel));
            attrTypes.addAll(projectedColumn.getColumnTypes(rel));
        }

        Relation result = new RelationBuilder().attributeNames(attrNames).attributeTypes(attrTypes).build();

        if (rel != null) {
            for (int i = 0; i < rel.getSize(); i++) {
                List<Cell> row = rel.getRow(i);
                List<Cell> newRow = new ArrayList<>();
                for (ProjectedColumns projectedColumn : projectedColumns) {
                    newRow.addAll(projectedColumn.getValuesForRow(rel, row));
                }
                result.insert(newRow);
            }
        } else {
            // We can project a single empty row.
            List<Cell> row = List.of();
            List<Cell> newRow = new ArrayList<>();
            for (ProjectedColumns projectedColumn : projectedColumns) {
                newRow.addAll(projectedColumn.getValuesForRow(null, row));
            }
            result.insert(newRow);
        }

        return result;
    }

    @Override
    public Relation select(Relation rel, Predicate p) {
        return ra.select(rel, p);
    }

    @Override
    public Relation project(Relation rel, List<String> attrs) {
        return ra.project(rel, attrs);
    }

    @Override
    public Relation union(Relation rel1, Relation rel2) {
        return ra.union(rel1, rel2);
    }

    @Override
    public Relation diff(Relation rel1, Relation rel2) {
        return ra.diff(rel1, rel2);
    }

    @Override
    public Relation rename(Relation rel, List<String> origAttr, List<String> renamedAttr) {
        return ra.rename(rel, origAttr, renamedAttr);
    }

    @Override
    public Relation cartesianProduct(Relation rel1, Relation rel2) {
        return ra.cartesianProduct(rel1, rel2);
    }

    @Override
    public Relation join(Relation rel1, Relation rel2) {
        return ra.join(rel1, rel2);
    }

    @Override
    public Relation join(Relation rel1, Relation rel2, Predicate p) {
        return ra.join(rel1, rel2, p);
    }
}
