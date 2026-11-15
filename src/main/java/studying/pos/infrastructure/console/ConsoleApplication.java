package studying.pos.infrastructure.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Path;
import studying.pos.application.model.ResultReport;
import studying.pos.application.model.StudentResult;
import studying.pos.application.services.PosFacade;
import studying.pos.domain.exception.DomainException;
import studying.pos.domain.model.Criterion;
import studying.pos.domain.model.Evaluation;
import studying.pos.domain.model.Student;
import studying.pos.domain.strategy.MissingGradePolicy;
import studying.pos.domain.strategy.impl.MissingAsZeroPolicy;
import studying.pos.domain.strategy.impl.StrictPolicy;

/** Line-oriented inbound adapter; business validation stays in the domain. */
public final class ConsoleApplication {
    private static final int CRITERION_FIELDS = 3;
    private static final int GRADE_FIELDS = 4;
    private static final int SCORE_INDEX = 3;
    private final BufferedReader input;
    private final PrintWriter output;
    private final PosFacade facade;

    /**
     * Creates a console adapter with replaceable text streams.
     * @param inputReader input stream, also suitable for scripted demos
     * @param outputWriter user-readable output
     * @param applicationFacade application scenario facade
     */
    public ConsoleApplication(final Reader inputReader,
            final PrintWriter outputWriter,
            final PosFacade applicationFacade) {
        input = new BufferedReader(inputReader);
        output = outputWriter;
        facade = applicationFacade;
    }

    /**
     * Reads commands until exit or EOF; expected errors preserve the session.
     * @throws IOException when the input stream itself cannot be read
     */
    public void run() throws IOException {
        output.println("ПОС: преподавательская оценка студентов."
                + " help — помощь.");
        boolean running = true;
        while (running) {
            output.print("pos> ");
            output.flush();
            String line = input.readLine();
            if (line == null) {
                break;
            }
            if (line.isBlank()) {
                continue;
            }
            try {
                running = execute(line.strip());
            } catch (IllegalArgumentException exception) {
                output.println("Ошибка: " + exception.getMessage());
            } catch (IOException exception) {
                output.println("Ошибка экспорта: " + exception.getMessage());
            }
        }
        output.println("Работа завершена.");
        output.flush();
    }

    private boolean execute(final String line) throws IOException {
        String[] parts = line.split("\\s+", 2);
        String arguments = parts.length == 2 ? parts[1].strip() : "";
        switch (parts[0]) {
            case "student" -> students(arguments);
            case "criterion" -> criteria(arguments);
            case "grade" -> grades(arguments);
            case "total" -> total(arguments);
            case "report" -> report(arguments);
            case "export" -> export(arguments);
            case "help" -> help();
            case "exit" -> {
                return false;
            }
            default -> throw new DomainException("Неизвестная команда: help");
        }
        return true;
    }

    private void students(final String arguments) {
        String[] parts = arguments.split("\\s+", 2);
        if (parts[0].equals("add") && parts.length == 2) {
            Student student = facade.addStudent(parts[1]);
            output.println("Студент добавлен: " + student.id()
                    + " | " + student.name());
        } else if (arguments.equals("list")) {
            output.println("Студенты: " + facade.students().size());
            for (Student student : facade.students()) {
                output.println(student.id() + " | " + student.name());
            }
        } else {
            throw new DomainException("Формат: student add ИМЯ / student list");
        }
    }

    private void criteria(final String arguments) {
        if (arguments.startsWith("add ")) {
            String[] fields = arguments.substring("add ".length())
                    .split("\\|", -1);
            if (fields.length != CRITERION_FIELDS) {
                throw new DomainException("Формат: criterion add"
                        + " ИМЯ | MAX | ВЕС");
            }
            Criterion criterion = facade.addCriterion(fields[0],
                    decimal(fields[1]), decimal(fields[2]));
            output.println("Критерий добавлен: " + criterion.id());
        } else if (arguments.equals("list")) {
            output.println("Критерии: " + facade.criteria().size());
            for (Criterion criterion : facade.criteria()) {
                output.println(criterion.id() + " | " + criterion.name()
                        + " | max=" + criterion.maximum()
                        + " | weight=" + criterion.weight());
            }
        } else {
            throw new DomainException("Формат: criterion add / criterion list");
        }
    }

    private void grades(final String arguments) {
        String[] fields = arguments.split("\\s+");
        switch (fields[0]) {
            case "set", "correct" -> {
                requireFields(fields, GRADE_FIELDS);
                int studentId = identifier(fields[1]);
                int criterionId = identifier(fields[2]);
                BigDecimal value = decimal(fields[SCORE_INDEX]);
                if (fields[0].equals("set")) {
                    facade.assign(studentId, criterionId, value);
                } else {
                    facade.correct(studentId, criterionId, value);
                }
                output.println("Оценка сохранена.");
            }
            case "cancel" -> {
                requireFields(fields, CRITERION_FIELDS);
                facade.cancel(identifier(fields[1]), identifier(fields[2]));
                output.println("Оценка отменена.");
            }
            case "list" -> {
                requireFields(fields, 2);
                int studentId = identifier(fields[1]);
                output.println("Оценки студента " + studentId + ":");
                for (Evaluation evaluation : facade.evaluations(studentId)) {
                    output.println("Критерий " + evaluation.criterionId()
                            + " | " + evaluation.score().value().toPlainString()
                            + " / " + evaluation.score().maximum()
                                    .toPlainString());
                }
            }
            default -> throw new DomainException("Формат: grade set/correct/"
                    + "cancel/list; подробности — help");
        }
    }

    private void total(final String arguments) {
        String[] fields = arguments.split("\\s+");
        if (fields.length > 2) {
            throw new DomainException("Формат: total STUDENT_ID [strict|zero]");
        }
        MissingGradePolicy policy = policy(fields.length == 2 ? fields[1] : "");
        printResult(facade.total(identifier(fields[0]), policy));
    }

    private void report(final String arguments) {
        ResultReport report = facade.report(policy(arguments));
        output.println("Ведомость; политика=" + report.policy());
        report.students().forEach(this::printResult);
        long calculated = report.students().stream()
                .filter(result -> result.score().isPresent()).count();
        output.println("Студентов: " + report.students().size()
                + "; рассчитано: " + calculated + "; средний итог: "
                + report.average().map(Object::toString).orElse("нет данных"));
    }

    private void export(final String arguments) throws IOException {
        String[] fields = arguments.split("\\s+", 2);
        requireFields(fields, 2);
        Path destination = Path.of(fields[1]);
        facade.export(policy(fields[0]), destination);
        output.println("Экспорт выполнен: " + destination.toAbsolutePath());
    }

    private void printResult(final StudentResult result) {
        output.println(result.student().id() + " | " + result.student().name()
                + " | итог=" + result.score().map(Object::toString)
                        .orElse("не определён")
                + " | оценено=" + result.evaluated() + "/" + result.criteria());
    }

    private MissingGradePolicy policy(final String key) {
        return switch (key) {
            case "", "strict" -> new StrictPolicy();
            case "zero" -> new MissingAsZeroPolicy();
            default -> throw new DomainException("Политика: strict или zero");
        };
    }

    private int identifier(final String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new DomainException("ID должен быть целым числом");
        }
    }

    private BigDecimal decimal(final String value) {
        try {
            return new BigDecimal(value.strip().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new DomainException("Балл, максимум и вес"
                    + " должны быть числами");
        }
    }

    private void requireFields(final String[] fields, final int count) {
        if (fields.length != count) {
            throw new DomainException("Неверное число аргументов: см. help");
        }
    }

    private void help() {
        output.println(String.join("\n",
                "student add ИМЯ / student list",
                "criterion add НАЗВАНИЕ | MAX | ВЕС / criterion list",
                "grade set STUDENT_ID CRITERION_ID БАЛЛ",
                "grade correct STUDENT_ID CRITERION_ID БАЛЛ",
                "grade cancel STUDENT_ID CRITERION_ID",
                "grade list STUDENT_ID",
                "total STUDENT_ID [strict|zero]",
                "report [strict|zero]",
                "export strict|zero ПУТЬ К ФАЙЛУ.csv",
                "help / exit",
                "Дробные числа: 7.5 или 7,5. По умолчанию политика strict.",
                "Память хранится до выхода. Экспорт перезаписывает файл."));
    }
}
