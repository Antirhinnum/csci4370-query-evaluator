package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.*;
import uga.cs4370.mydbfrontend.Utils;

import java.util.*;

public final class ExtendedRAImpl implements ExtendedRA {
    private final RA ra;

    public ExtendedRAImpl(RA ra) {
        this.ra = ra;
    }

    @Override
    public Relation extendedProject(Relation rel, List<ProjectedAttributes> projectedAttributesList) {

        List<String> attrNames = new ArrayList<>();
        List<Type> attrTypes = new ArrayList<>();
        for (ProjectedAttributes projectedColumn : projectedAttributesList) {
            attrNames.addAll(projectedColumn.getAttrNames(rel));
            attrTypes.addAll(projectedColumn.getAttrTypes(rel));
        }

        Relation result = new RelationBuilder().attributeNames(attrNames).attributeTypes(attrTypes).build();

        if (rel != null) {
            for (int i = 0; i < rel.getSize(); i++) {
                List<Cell> row = rel.getRow(i);
                List<Cell> newRow = new ArrayList<>();
                for (ProjectedAttributes projectedColumn : projectedAttributesList) {
                    newRow.addAll(projectedColumn.projectFromRow(rel, row));
                }
                result.insert(newRow);
            }
        } else {
            // We can project a single empty row.
            List<Cell> row = List.of();
            List<Cell> newRow = new ArrayList<>();
            for (ProjectedAttributes projectedColumn : projectedAttributesList) {
                newRow.addAll(projectedColumn.projectFromRow(null, row));
            }
            result.insert(newRow);
        }

        return result;
    }

    @Override
    public Relation limit(Relation rel, int limit, int offset) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be non-negative");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be positive");
        }

        Relation result = Utils.copySchema(rel);
        for (int i = offset; i < offset + limit && i < rel.getSize(); i++) {
            List<Cell> row = rel.getRow(i);
            result.insert(row);
        }
        return result;
    }

    @Override
    public Relation distinct(Relation rel) {
        Relation result = Utils.copySchema(rel);
        HashSet<List<Cell>> knownRows = new HashSet<>();
        for (int i = 0; i < rel.getSize(); i++) {
            List<Cell> row = rel.getRow(i);
            if (knownRows.add(row)) {
                result.insert(row);
            }
        }
        return result;
    }

    @Override
    public Relation orderBy(Relation rel, List<OrderByColumn> orderings) {
        Relation result = Utils.copySchema(rel);

        Comparator<Cell> cellComparator = (o1, o2) -> {
            if (o1.getType() != o2.getType()) {
                throw new IllegalArgumentException("Cannot compare cells of different types");
            }

            return switch (o1.getType()) {
                case INTEGER -> Integer.compare(o1.getAsInt(), o2.getAsInt());
                case DOUBLE -> Double.compare(o1.getAsDouble(), o2.getAsDouble());
                case STRING -> o1.getAsString().compareTo(o2.getAsString());
            };
        };

        Comparator<List<Cell>> rowComparer = (o1, o2) -> {
            for (OrderByColumn orderByColumn : orderings) {
                Cell cell1 = orderByColumn.ordering().getValueFromRow(rel, o1);
                Cell cell2 = orderByColumn.ordering().getValueFromRow(rel, o2);
                int comparison = cellComparator.compare(cell1, cell2);
                if (comparison != 0) {
                    return (orderByColumn.ascending() ? 1 : -1) * comparison;
                }
            }
            return 0;
        };

        // Use binary search to insert. Doesn't reduce time complexity (still need to shift all elements after insertion),
        // but is slightly faster than linear search for the same thing.
        List<List<Cell>> sortedRows = new ArrayList<>() {
            @Override
            public boolean add(List<Cell> row) {
                int insertionIndex = Collections.binarySearch(this, row, rowComparer);
                if (insertionIndex < 0) {
                    insertionIndex = -insertionIndex - 1;
                }
                super.add(insertionIndex, row);
                return true;
            }
        };

        for (int i = 0; i < rel.getSize(); i++) {
            sortedRows.add(rel.getRow(i));
        }
        for (List<Cell> row : sortedRows) {
            result.insert(row);
        }
        return result;
    }

    @Override
    public Relation intersect(Relation rel1, Relation rel2) {
        if (!rel1.getAttrs().equals(rel2.getAttrs())) {
            throw new IllegalArgumentException("Relations must have the same attributes for union.");
        }
        if (!rel1.getTypes().equals(rel2.getTypes())) {
            throw new IllegalArgumentException("Relations must have the same types for union.");
        }

        Relation result = Utils.copySchema(rel1);
        Set<List<Cell>> knownRows = new HashSet<>();
        for (int i = 0; i < rel1.getSize(); i++) {
            knownRows.add(rel1.getRow(i));
        }
        for (int i = 0; i < rel2.getSize(); i++) {
            List<Cell> row = rel2.getRow(i);
            if (knownRows.contains(row)) {
                result.insert(row);
            }
        }
        return result;
    }

    @Override
    public List<GroupedRelation> groupBy(Relation rel, List<RowValueProducer> groupingValueProducers) {
        Map<List<Cell>, GroupedRelation> groupedRelations = new HashMap<>();
        Set<Integer> groupedAttributeIndexes = new HashSet<>();

        for (int i = 0; i < rel.getSize(); i++) {
            List<Cell> row = rel.getRow(i);
            List<Cell> groupingValues = new ArrayList<>();
            for (RowValueProducer groupingValueProducer : groupingValueProducers) {
                Cell valueFromRow = groupingValueProducer.getValueFromRow(rel, row);
                groupingValues.add(valueFromRow);

                if (i != 0) {
                    continue;
                }

                // On the first iteration, see which columns are being grouped by.
                for (int j = 0; j < row.size(); j++) {
                    if (row.get(j) == valueFromRow) { // Intentional reference equality check
                        groupedAttributeIndexes.add(j);
                        break;
                    }
                }
            }

            if (i == 0 && groupedAttributeIndexes.size() != groupingValueProducers.size()) {
                throw new RuntimeException("Could not find all grouped column indexes");
            }

            if (groupedRelations.containsKey(groupingValues)) {
                GroupedRelation existingGroup = groupedRelations.get(groupingValues);
                existingGroup.insert(row);
            } else {
                Relation relationToGroup = Utils.copySchema(rel);
                relationToGroup.insert(row);
                groupedRelations.put(groupingValues, new GroupedRelationImpl(relationToGroup, new ArrayList<>(groupedAttributeIndexes)));
            }
        }
        return groupedRelations.values().stream().toList();
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