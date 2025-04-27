package uga.cs4370.mydbfrontend;

import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRAImpl;
import uga.cs4370.mydbimpl.RAImpl;

import java.nio.file.Path;
import java.util.*;

public class Driver {

    private static final String DATA_HEAD = "C:\\Users\\creep\\Documents\\School\\Spring 2025\\CSCI 4370\\Class Activity 02\\mysql-files";
    private static final String DATA_INSTRUCTOR = Path.of(DATA_HEAD, "instructor_export.csv").toString();
    private static final String DATA_DEPARTMENT = Path.of(DATA_HEAD, "department_export.csv").toString();
    private static final String DATA_STUDENT = Path.of(DATA_HEAD, "student_export.csv").toString();
    private static final String DATA_ADVISOR = Path.of(DATA_HEAD, "advisor_export.csv").toString();
    private static final String DATA_SECTION = Path.of(DATA_HEAD, "section_export.csv").toString();
    private static final String DATA_TEACHES = Path.of(DATA_HEAD, "teaches_export.csv").toString();

    public static final boolean DEBUG = false;

    public static void main(String[] args) {

        ExtendedRA ra = new ExtendedRAImpl(new RAImpl());
        List<Nameable<Relation>> relations = initTables();
        SimpleQueryEvaluator qe = new SimpleQueryEvaluator(ra, relations);

        System.out.println("Frontend initialized. Types queries to get results.");

        Scanner sc = new Scanner(System.in);
        Queue<String> commands = new ArrayDeque<>();
        if (args.length > 0) {
            commands.addAll(Arrays.asList(args));
        }

        while (true) {
            if (sc.hasNextLine()) {
                commands.add(sc.nextLine());
            }

            if (commands.isEmpty()) {
                continue;
            }

            String command = commands.remove();
            if (command.startsWith("exit")) {
                break;
            }

            Relation result;
            try {
                result = qe.evaluate(command);
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                if (DEBUG) {
                    System.out.println("Full error: " + e);
                    e.printStackTrace();
                }
                continue;
            }

            if (result == null) {
                System.out.println("An error occurred when evaluating the query!");
            } else {
                result.print();
            }
        }

        sc.close();
    }

    private static List<Nameable<Relation>> initTables() {
        List<Nameable<Relation>> tables = new ArrayList<>();

        Relation instructor = new RelationBuilder().attributeNames(List.of("ID", "name", "dept_name", "salary")).attributeTypes(List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.DOUBLE)).build();
        instructor.loadData(DATA_INSTRUCTOR);
        tables.add(new NameableImpl<>(instructor, "instructor"));

        Relation department = new RelationBuilder().attributeNames(List.of("dept_name", "building", "budget")).attributeTypes(List.of(Type.STRING, Type.STRING, Type.DOUBLE)).build();
        department.loadData(DATA_DEPARTMENT);
        tables.add(new NameableImpl<>(department, "department"));

        Relation student = new RelationBuilder().attributeNames(List.of("ID", "name", "dept_name", "tot_cred")).attributeTypes(List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.DOUBLE)).build();
        student.loadData(DATA_STUDENT);
        tables.add(new NameableImpl<>(student, "student"));

        Relation advisor = new RelationBuilder().attributeNames(List.of("s_ID", "i_ID")).attributeTypes(List.of(Type.INTEGER, Type.INTEGER)).build();
        advisor.loadData(DATA_ADVISOR);
        tables.add(new NameableImpl<>(advisor, "advisor"));

        Relation section = new RelationBuilder().attributeNames(List.of("course_id", "sec_id", "semester", "year", "building", "room_number", "time_slot_id")).attributeTypes(List.of(Type.INTEGER, Type.INTEGER, Type.STRING, Type.INTEGER, Type.STRING, Type.INTEGER, Type.STRING)).build();
        section.loadData(DATA_SECTION);
        tables.add(new NameableImpl<>(section, "section"));

        Relation teaches = new RelationBuilder().attributeNames(List.of("ID", "course_id", "sec_id", "semester", "year")).attributeTypes(List.of(Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.STRING, Type.INTEGER)).build();
        teaches.loadData(DATA_TEACHES);
        tables.add(new NameableImpl<>(teaches, "teaches"));

        return List.copyOf(tables);
    }
}
