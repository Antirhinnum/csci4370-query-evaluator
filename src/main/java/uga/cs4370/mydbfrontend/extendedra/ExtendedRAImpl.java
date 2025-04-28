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
    public Relation extendedProject(GroupedRelation rel, List<ProjectedAttributes> projectedAttributesList) {

        List<String> attrNames = new ArrayList<>();
        List<Type> attrTypes = new ArrayList<>();
        for (ProjectedAttributes projectedColumn : projectedAttributesList) {
            attrNames.addAll(projectedColumn.getAttrNames(rel));
            attrTypes.addAll(projectedColumn.getAttrTypes(rel));
        }

        Relation result = new RelationBuilder().attributeNames(attrNames).attributeTypes(attrTypes).build();
        if (rel == null || rel.getSize() == 0) {
            // We can project a single empty row.
            List<Cell> row;
            if (rel != null) {
                row = rel.getTypes().stream().map(type -> switch (type) {
                    case INTEGER -> Cell.val(0);
                    case DOUBLE -> Cell.val(0.0);
                    case STRING -> Cell.val("");
                }).toList();
            } else {
                row = List.of();
            }
            List<Cell> newRow = new ArrayList<>();
            for (ProjectedAttributes projectedColumn : projectedAttributesList) {
                newRow.addAll(projectedColumn.projectFromRow(rel, row));
            }
            result.insert(newRow);
        } else {

            // If there are any groups or aggregating functions, ensure that no ProjectedAttributes are trying to project
            // attributes that won't be constant across every row in the group.
            boolean anyAggregatingProjections = projectedAttributesList.stream().anyMatch(ProjectedAttributes::isAggregating);
            if (anyAggregatingProjections || !rel.getGroupedAttributeIndexes().isEmpty()) {
                Set<Integer> safeColumnIndexesToGroup = new HashSet<>(rel.getGroupedAttributeIndexes());

                // We don't know which columns are being aggregated, so figure that out.
                // Make a relation that returns a tracer row.
                final int[] lastRowGotten = {-1};
                GroupedRelation tracerRelation = new GroupedRelation() {
                    @Override
                    public List<Integer> getGroupedAttributeIndexes() {
                        return rel.getGroupedAttributeIndexes();
                    }

                    @Override
                    public int getSize() {
                        return rel.getSize();
                    }

                    @Override
                    public List<Cell> getRow(int i) {
                        List<Cell> row = rel.getRow(i);
                        return new ArrayList<>(row) {
                            @Override
                            public Cell get(int index) {
                                lastRowGotten[0] = index;
                                return super.get(index);
                            }
                        };
                    }

                    @Override
                    public List<Type> getTypes() {
                        return rel.getTypes();
                    }

                    @Override
                    public List<String> getAttrs() {
                        return rel.getAttrs();
                    }

                    @Override
                    public boolean hasAttr(String attr) {
                        return rel.hasAttr(attr);
                    }

                    @Override
                    public int getAttrIndex(String attr) {
                        return rel.getAttrIndex(attr);
                    }

                    @Override
                    public void insert(List<Cell> row) {
                        rel.insert(row);
                    }

                    @Override
                    public void loadData(String path) {
                        rel.loadData(path);
                    }

                    @Override
                    public void print() {
                        rel.print();
                    }
                };

                for (ProjectedAttributes projectedColumn : projectedAttributesList) {
                    if (projectedColumn.isAggregating()) {
                        // Presumably, projectFromRow() will need to call get() on at least one row in order to get the
                        // value it needs to aggregate. Trace which index is used.
                        projectedColumn.projectFromRow(tracerRelation, tracerRelation.getRow(0));
                        if (lastRowGotten[0] != -1) {
                            safeColumnIndexesToGroup.add(lastRowGotten[0]);
                            lastRowGotten[0] = -1;
                        }
                    }
                }

                // We now know all the columns we can safely include in an aggregate query. Now let's try and catch
                // any projections that use unsafe columns.
                List<Cell> evilRow = new ArrayList<>();
                for (int i = 0; i < rel.getTypes().size(); i++) {
                    if (safeColumnIndexesToGroup.contains(i)) {
                        evilRow.add(switch (rel.getTypes().get(i)) {
                            case STRING -> Cell.val("");
                            case INTEGER -> Cell.val(0);
                            case DOUBLE -> Cell.val(0.0);
                        });
                    } else {
                        evilRow.add(null);
                    }
                }

                int k = 0;
                try {
                    for (k = 0; k < projectedAttributesList.size(); k++) {
                        List<Cell> projected = projectedAttributesList.get(k).projectFromRow(rel, evilRow);
                        if (projected.stream().anyMatch(Objects::isNull)) {
                            throw new NullPointerException();
                        }
                    }
                } catch (NullPointerException e) {
                    throw new RuntimeException("The ProjectedAttributes with index " + k + " tried to project a column that wasn't safe to project with an aggregate or group!");
                }
            }

            for (int i = 0; i < rel.getSize(); i++) {
                List<Cell> row = rel.getRow(i);
                List<Cell> newRow = new ArrayList<>();
                for (ProjectedAttributes projectedColumn : projectedAttributesList) {
                    newRow.addAll(projectedColumn.projectFromRow(rel, row));
                }
                result.insert(newRow);

                if (anyAggregatingProjections) {
                    break;
                }
            }
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