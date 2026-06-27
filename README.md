# Command Line SQL Query Evaluator

Created as an honors option project for CSCI 4370 (Database Management), taught by Dr. Menik.

Commits before commit [84ba3bd](https://github.com/Antirhinnum/csci4370-query-evaluator/commit/84ba3bdd65730c93cb312a669f0669de4ab08012) are the collaborative effort of Matthew Griffith, Nilan Patel, Michael Scott, and Bryce Wellman. Commit [84ba3bd](https://github.com/Antirhinnum/csci4370-query-evaluator/commit/84ba3bdd65730c93cb312a669f0669de4ab08012) and onwards are the sole work of Michael Scott.

The dataset used for this program was provided by Dr. Menik of UGA.

## Compilation and Running

To compile this program, run the following command:

```bash
mvn compile
```

To run this program after compiling, run the following command:

```bash
mvn exec:java
```

## Usage

The program accepts four commands:

| Command            | Description                                                                                                                           |
|--------------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `exit`             | Terminate the program.                                                                                                                |
| `show tables`      | Show a list of all tables available for use with this program.                                                                        |
| `describe <table>` | Show the attribute names and types associated with the provided table. Replace `<table>` with a value from the `show tables` command. |
| `<sql query>`      | Attempts to evaluate the provided SQL query.                                                                                          |

## Example Statements

The following SQL queries (and variations on them) are supported by the program:

```sql
SELECT name FROM instructor;

SELECT DISTINCT dept_name FROM instructor;

SELECT ALL dept_name FROM instructor;

SELECT * FROM instructor;

SELECT '437';

SELECT '437' AS FOO;

SELECT 'A' FROM instructor;

SELECT ID, name, salary / 12 AS 'monthly_salary' FROM instructor WHERE salary / 12 < 5000;

SELECT * FROM instructor, teaches;

SELECT DISTINCT instructor.ID, name, course_id FROM instructor, teaches WHERE instructor.ID = teaches.ID AND instructor.dept_name = 'Physics';

SELECT T.ID, T.name FROM instructor AS T, instructor S WHERE T.salary > S.salary AND S.dept_name = 'Comp. Sci.';

SELECT name FROM instructor WHERE name LIKE 'L%' OR name LIKE '10\%' ESCAPE '\\';

SELECT concat('Information about ', name, ':') as 'Header', length(name) AS 'Name Length', substring(name, 1, 2) AS 'Abbreviaton' FROM instructor;

SELECT name FROM instructor LIMIT 1, 5;

SELECT name FROM instructor LIMIT 5 OFFSET 1;

SELECT course_id, semester, year FROM section ORDER BY year, semester ASC, course_id DESC;

(SELECT course_id FROM section WHERE semester = 'Fall' AND year = 2005) UNION (SELECT course_id FROM section WHERE semester = 'Spring' AND year = 2006);

(SELECT course_id FROM section WHERE semester = 'Fall' AND year = 2002) INTERSECT (SELECT course_id FROM section WHERE semester = 'Spring' AND year = 2007);

(SELECT course_id FROM section WHERE semester = 'Fall' AND year = 2005) EXCEPT (SELECT course_id FROM section WHERE semester = 'Spring' AND year = 2006);

SELECT avg(salary) FROM instructor WHERE dept_name = 'Comp. Sci.';

SELECT COUNT(DISTINCT ID) FROM teaches WHERE semester = 'Spring' AND year = 2018;

SELECT COUNT(*) FROM course;

SELECT dept_name, min(salary) AS min, max(salary) AS max, avg(salary) AS avg FROM instructor GROUP BY dept_name;

SELECT dept_name, avg(salary) AS avg_salary FROM instructor GROUP BY dept_name HAVING avg(salary) > 50000;
```

Keywords may be in any case. Queries do nto need to end with a semicolon. Source tables for column names will be inferred if possible.

The following aggregate keywords are available:
- `CONCAT`
- `SUBSTRING`
- `TRIM`
- `LENGTH`
- `COUNT`
- `AVG`
- `MIN`
- `MAX`
- `SUM`
