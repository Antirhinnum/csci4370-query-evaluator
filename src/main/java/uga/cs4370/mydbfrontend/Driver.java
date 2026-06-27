package uga.cs4370.mydbfrontend;

import uga.cs4370.mydb.Cell;
import uga.cs4370.mydb.Relation;
import uga.cs4370.mydb.RelationBuilder;
import uga.cs4370.mydb.Type;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRA;
import uga.cs4370.mydbfrontend.extendedra.ExtendedRAImpl;
import uga.cs4370.mydbimpl.RAImpl;

import java.nio.file.Path;
import java.util.*;

public class Driver {

    public static final boolean DEBUG = false;
    private static Path pathHead;

    public static void main(String[] args) {

        pathHead = Path.of(System.getProperty("user.dir"), "src", "main", "resources");
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

            final String SHOW_TABLES_COMMAND = "show tables";
            if (command.equalsIgnoreCase(SHOW_TABLES_COMMAND)) {
                Relation relationsRelation = new RelationBuilder().attributeNames(List.of("table")).attributeTypes(List.of(Type.STRING)).build();
                for (Nameable<Relation> relation : relations) {
                    List<Cell> row = List.of(Cell.val(relation.getName()));
                    relationsRelation.insert(row);
                }
                relationsRelation.print();
                continue;
            }

            final String DESCRIBE_COMMAND_PREFIX = "describe ";
            if (command.toLowerCase().startsWith(DESCRIBE_COMMAND_PREFIX)) {
                String name = command.substring(DESCRIBE_COMMAND_PREFIX.length());
                Optional<Nameable<Relation>> matchingRelation = relations.stream().filter(r -> r.getName().equals(name)).findFirst();
                if (matchingRelation.isPresent()) {
                    Relation relation = matchingRelation.get().getValue();
                    Relation description = Utils.describeRelation(relation);
                    description.print();
                } else {
                    System.out.println("Error: No known relation with name \"" + name + "\".");
                }
                continue;
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

        tables.add(initTable("advisor", List.of("s_ID", "i_ID"), List.of(Type.INTEGER, Type.INTEGER)));
        tables.add(initTable("classroom", List.of("building", "room_number", "capacity"), List.of(Type.STRING, Type.STRING, Type.INTEGER)));
        tables.add(initTable("course", List.of("course_id", "title", "dept_name", "credits"), List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.INTEGER)));
        tables.add(initTable("department", List.of("dept_name", "building", "budget"), List.of(Type.STRING, Type.STRING, Type.DOUBLE)));
        tables.add(initTable("instructor", List.of("ID", "name", "dept_name", "salary"), List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.DOUBLE)));
        tables.add(initTable("prereq", List.of("course_id", "prereq_id"), List.of(Type.INTEGER, Type.INTEGER)));
        tables.add(initTable("section", List.of("course_id", "sec_id", "semester", "year", "building", "room_number", "time_slot_id"), List.of(Type.INTEGER, Type.INTEGER, Type.STRING, Type.INTEGER, Type.STRING, Type.INTEGER, Type.STRING)));
        tables.add(initTable("student", List.of("ID", "name", "dept_name", "tot_cred"), List.of(Type.INTEGER, Type.STRING, Type.STRING, Type.DOUBLE)));
        tables.add(initTable("takes", List.of("ID", "course_id", "sec_id", "semester", "year", "grade"), List.of(Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.STRING, Type.INTEGER, Type.STRING)));
        tables.add(initTable("teaches", List.of("ID", "course_id", "sec_id", "semester", "year"), List.of(Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.STRING, Type.INTEGER)));
        tables.add(initTable("time_slot", List.of("time_slot_id", "day", "start_hr", "start_min", "end_hr", "end_min"), List.of(Type.STRING, Type.STRING, Type.INTEGER, Type.INTEGER, Type.INTEGER, Type.INTEGER)));

        return List.copyOf(tables);
    }

    private static Nameable<Relation> initTable(String name, List<String> attributes, List<Type> types) {
        Relation result = new RelationBuilder().attributeNames(attributes).attributeTypes(types).build();
        result.loadData(Path.of(pathHead.toString(), name + "_export.csv").toString());
        return new NameableImpl<>(result, name);
    }
}
