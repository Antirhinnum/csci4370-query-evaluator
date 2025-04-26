package uga.cs4370.mydbfrontend.extendedra;

import uga.cs4370.mydb.RA;
import uga.cs4370.mydb.Relation;

import java.util.List;

public interface ExtendedRA extends RA {

    /**
     * An extended version of {@link #project(Relation, List)}. Instead of just selecting columns from an input relation,
     * this method allows selecting arbitrary data based on the input relation. This includes columns, the all columns
     * expression (*), algebraic expressions, constant values, etc.
     *
     * @param rel              The relation to project from, or null. If null, a relation with one row will be produced.
     * @param projectedColumns The columns to project.
     * @return A new relation with all projected columns.
     */
    Relation extendedProject(Relation rel, List<ProjectedColumns> projectedColumns);
}

