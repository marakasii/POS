package studying.pos.infrastructure.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import studying.pos.application.model.ResultReport;
import studying.pos.application.services.PosFacade;
import studying.pos.domain.services.FinalScoreCalculator;
import studying.pos.support.TestRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests command parsing and presentation with isolated outbound port fakes. */
final class ConsoleApplicationTest {
    @Test
    @DisplayName("Команды выполняют полный жизненный цикл оценки")
    void handlesSuccessfulCommands() throws IOException {
        TestRepository repository = new TestRepository();
        List<ResultReport> exports = new ArrayList<>();
        String script = String.join("\n", "help", "student add Анна Иванова",
                "student add Борис", "student list",
                "criterion add Домашняя работа | 10 | 40",
                "criterion add Экзамен | 20 | 60", "criterion list",
                "grade set 1 1 8", "grade set 1 2 15", "grade list 1",
                "total 1", "total 2 strict", "report strict",
                "grade correct 1 1 10", "total 1 strict",
                "grade cancel 1 1", "total 1 zero", "report zero",
                "export zero путь с пробелами.csv", "exit", "unknown");
        PosFacade facade = new PosFacade(repository, new FinalScoreCalculator(),
                (report, path) -> {
                    assertEquals("путь с пробелами.csv", path.toString());
                    exports.add(report);
                });
        String output = run(script, facade);
        assertTrue(output.contains("1 | Анна Иванова"));
        assertTrue(output.contains("2 | Экзамен | max=20 | weight=60"));
        assertTrue(output.contains("Критерий 1 | 8 / 10"));
        assertTrue(output.contains("итог=7.70 | оценено=2/2"));
        assertTrue(output.contains("итог=8.50 | оценено=2/2"));
        assertTrue(output.contains("итог=4.50 | оценено=1/2"));
        assertTrue(output.contains("итог=не определён | оценено=0/2"));
        assertTrue(output.contains("средний итог: 7.70"));
        assertTrue(output.contains("средний итог: 2.25"));
        assertTrue(output.contains("Экспорт выполнен:"));
        assertTrue(output.contains("Работа завершена."));
        assertEquals(1, exports.size());
        assertFalse(output.contains("Ошибка:"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "unknown", "student", "student add", "student delete 1",
        "criterion add Работа | 10", "criterion remove 1",
        "criterion add Работа | abc | 40", "grade", "grade set 1 1",
        "grade set abc 1 8", "grade set 1 1 abc", "grade set 1 1 12",
        "grade set 1 99 8", "grade set 99 1 8", "grade list",
        "grade cancel 1", "grade correct 1 1 8", "grade unknown",
        "total", "total 1 zero extra", "total 1 invalid", "report invalid",
        "export strict", "export invalid output.csv"
    })
    @DisplayName("Некорректная команда не прерывает последующие действия")
    void recoversAfterInvalidCommand(final String command) throws IOException {
        TestRepository repository = preparedRepository();
        PosFacade facade = new PosFacade(repository, new FinalScoreCalculator(),
                (report, path) -> {
                    throw new AssertionError("Unexpected export");
                });
        String output = run(command + "\nstudent list\nexit", facade);
        assertTrue(output.contains("Ошибка:"), output);
        assertTrue(output.contains("Студенты: 1"), output);
        assertTrue(repository.load().evaluations(1).isEmpty());
    }

    @Test
    @DisplayName("Пустой ввод и EOF завершаются штатно, запятая допустима")
    void acceptsDecimalCommaAndEof() throws IOException {
        TestRepository repository = preparedRepository();
        PosFacade facade = new PosFacade(repository, new FinalScoreCalculator(),
                (report, path) -> { });
        String output = run("\n  \ngrade set 1 1 7,5\ntotal 1\nreport",
                facade);
        assertTrue(output.contains("итог=7.50"));
        assertTrue(output.contains("средний итог: 7.50"));
        assertTrue(output.contains("Работа завершена."));
        assertFalse(output.contains("Ошибка:"));
    }

    @Test
    @DisplayName("Пустая ведомость отображается без выдуманного среднего")
    void presentsEmptyCollections() throws IOException {
        PosFacade facade = new PosFacade(new TestRepository(),
                new FinalScoreCalculator(), (report, path) -> { });
        String output = run("student list\ncriterion list\nreport\nexit",
                facade);
        assertTrue(output.contains("Студенты: 0"));
        assertTrue(output.contains("Критерии: 0"));
        assertTrue(output.contains("средний итог: нет данных"));
    }

    @Test
    @DisplayName("Ошибка записи экспорта сохраняет рабочую сессию")
    void recoversAfterExportFailure() throws IOException {
        PosFacade facade = new PosFacade(new TestRepository(),
                new FinalScoreCalculator(), (report, path) -> {
                    throw new IOException("disk full");
                });
        String output = run("export strict out.csv\nstudent list\nexit",
                facade);
        assertTrue(output.contains("Ошибка экспорта: disk full"));
        assertTrue(output.contains("Студенты: 0"));
    }

    @Test
    @DisplayName("Ошибка чтения входного потока передаётся точке запуска")
    void propagatesInputFailure() {
        Reader broken = new Reader() {
            @Override
            public int read(final char[] buffer, final int offset,
                    final int length) throws IOException {
                throw new IOException("broken input");
            }

            @Override
            public void close() {
            }
        };
        ConsoleApplication console = new ConsoleApplication(broken,
                new PrintWriter(new StringWriter()), new PosFacade(
                        new TestRepository(), new FinalScoreCalculator(),
                        (report, path) -> { }));
        assertThrows(IOException.class, console::run);
    }

    private TestRepository preparedRepository() {
        TestRepository repository = new TestRepository();
        repository.load().addStudent("Анна");
        repository.load().addCriterion("Работа", BigDecimal.TEN,
                BigDecimal.ONE);
        return repository;
    }

    private String run(final String script, final PosFacade facade)
            throws IOException {
        StringWriter output = new StringWriter();
        new ConsoleApplication(new StringReader(script),
                new PrintWriter(output), facade).run();
        return output.toString();
    }
}
