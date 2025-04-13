package uga.cs4370.mydbfrontend;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import uga.cs4370.mydb.RA;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbimpl.RAImpl;

public class Driver {

    public static void main(String[] args) {
        final String QUERY = "SELECT i.name as name, '100' as grade, dept_name, salary FROM instructor i WHERE salary > 100000 AND salary < 110000";
        // final String QUERY = "SELECT '100' AS h";

        final Map<String, Relation> TABLES = initTables();

        try {
            System.out.printf("Parsing and evaluating query: \"%s\"\n", QUERY);
            Statement statement = CCJSqlParserUtil.parse(QUERY);
            RA ra = new RAImpl();
            RASQLVisitor visitor = new RASQLVisitor(ra, TABLES);
            Relation result = statement.accept(visitor, null);
            System.out.printf("Results:\n");
            result.print();
        } catch (JSQLParserException jsqlpe) {
            // pass
        }
    }

    private static Map<String, Relation> initTables() {
        Map<String, Relation> tables = new HashMap<>();

        Relation instructor = new RelationBuilder()
                .attributeNames(List.of("ID", "name", "dept_name", "salary"))
                .attributeTypes(List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.DOUBLE))
                .build();
        instructor.loadData(DATA_INSTRUCTOR);
        tables.put("instructor", instructor);

        Relation department = new RelationBuilder()
                .attributeNames(List.of("dept_name", "building", "budget"))
                .attributeTypes(List.of(Type.STRING, Type.STRING, Type.DOUBLE))
                .build();
        department.loadData(DATA_DEPARTMENT);
        tables.put("department", department);

        Relation student = new RelationBuilder()
                .attributeNames(List.of("ID", "name", "dept_name", "tot_cred"))
                .attributeTypes(List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.DOUBLE))
                .build();
        student.loadData(DATA_STUDENT);
        tables.put("student", student);

        Relation advisor = new RelationBuilder()
                .attributeNames(List.of("s_ID", "i_ID"))
                .attributeTypes(List.of(Type.INTEGER, Type.INTEGER))
                .build();
        advisor.loadData(DATA_ADVISOR);
        tables.put("advisor", advisor);

        Relation section = new RelationBuilder()
                .attributeNames(List.of("course_id", "sec_id", "semester", "year", "building", "room_number", "time_slot_id"))
                .attributeTypes(List.of(Type.INTEGER, Type.INTEGER, Type.STRING, Type.INTEGER, Type.STRING, Type.INTEGER, Type.STRING))
                .build();
        section.loadData(DATA_SECTION);
        tables.put("section", section);

        Relation teaches = new RelationBuilder()
                .attributeNames(List.of("ID", "course_id", "sec_id", "semester", "year"))
                .attributeTypes(List.of(Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.STRING, Type.INTEGER))
                .build();
        teaches.loadData(DATA_TEACHES);
        tables.put("teaches", teaches);

        return Map.copyOf(tables);
    }

    private static final String DATA_HEAD = "C:\\Users\\creep\\Documents\\School\\Spring 2025\\CSCI 4370\\Class Activity 02\\mysql-files";
    private static final String DATA_INSTRUCTOR = Path.of(DATA_HEAD, "instructor_export.csv").toString();
    private static final String DATA_DEPARTMENT = Path.of(DATA_HEAD, "department_export.csv").toString();
    private static final String DATA_STUDENT = Path.of(DATA_HEAD, "student_export.csv").toString();
    private static final String DATA_ADVISOR = Path.of(DATA_HEAD, "advisor_export.csv").toString();
    private static final String DATA_SECTION = Path.of(DATA_HEAD, "section_export.csv").toString();
    private static final String DATA_TEACHES = Path.of(DATA_HEAD, "teaches_export.csv").toString();
}
