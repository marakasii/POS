package studying.pos.domain.model;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import studying.pos.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests aggregate invariants without a console, DI or storage adapter. */
final class GradebookTest {
    private static final int UNKNOWN_ID = 999;

    @Test
    @DisplayName("Студенты с одинаковыми именами имеют разные ID")
    void separatesStudentsWithTheSameName() {
        Gradebook book = new Gradebook();
        Student first = book.addStudent("  Анна Иванова  ");
        Student second = book.addStudent("Анна Иванова");
        assertEquals("Анна Иванова", first.name());
        assertNotEquals(first, second);
        assertEquals(first, book.student(first.id()));
        assertEquals(List.of(first, second), book.students());
        assertThrows(UnsupportedOperationException.class,
                () -> book.students().clear());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("Некорректное имя не меняет состояние и счётчик ID")
    void rejectsBlankStudentNames(final String name) {
        Gradebook book = new Gradebook();
        assertThrows(DomainException.class, () -> book.addStudent(name));
        assertTrue(book.students().isEmpty());
        assertEquals(1, book.addStudent("Анна").id());
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "-1, 1", "1, 0", "1, -1"})
    @DisplayName("Максимум и вес должны быть положительными")
    void rejectsInvalidCriterionNumbers(final String maximum,
            final String weight) {
        Gradebook book = new Gradebook();
        assertThrows(DomainException.class, () -> book.addCriterion("Работа",
                number(maximum), number(weight)));
        assertTrue(book.criteria().isEmpty());
        Criterion valid = book.addCriterion("Работа", BigDecimal.ONE,
                BigDecimal.ONE);
        assertEquals(1, valid.id());
    }

    @Test
    @DisplayName("Некорректные поля критерия отвергаются моделью")
    void rejectsIncompleteCriteria() {
        Gradebook book = new Gradebook();
        assertThrows(DomainException.class, () -> book.addCriterion(null,
                BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(DomainException.class, () -> book.addCriterion(" ",
                BigDecimal.ONE, BigDecimal.ONE));
        assertThrows(DomainException.class, () -> book.addCriterion("Работа",
                null, BigDecimal.ONE));
        assertThrows(DomainException.class, () -> book.addCriterion("Работа",
                BigDecimal.ONE, null));
        assertTrue(book.criteria().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "10", "7.5"})
    @DisplayName("Можно выставить ноль, максимум и дробный балл")
    void acceptsBoundedScores(final String points) {
        Gradebook book = preparedBook();
        Evaluation evaluation = book.assign(1, 1, number(points));
        assertEquals(0, evaluation.score().value().compareTo(number(points)));
        assertEquals(List.of(evaluation), book.evaluations(1));
        assertThrows(UnsupportedOperationException.class,
                () -> book.evaluations(1).clear());
        assertThrows(UnsupportedOperationException.class,
                () -> book.criteria().clear());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "10.01"})
    @DisplayName("Балл вне диапазона не попадает в ведомость")
    void rejectsOutOfRangeScores(final String points) {
        Gradebook book = preparedBook();
        assertThrows(DomainException.class,
                () -> book.assign(1, 1, number(points)));
        assertTrue(book.evaluations(1).isEmpty());
    }

    @Test
    @DisplayName("Нельзя оценить неизвестного студента или критерий")
    void rejectsUnknownMembership() {
        Gradebook book = preparedBook();
        assertThrows(DomainException.class,
                () -> book.assign(UNKNOWN_ID, 1, BigDecimal.ONE));
        assertThrows(DomainException.class,
                () -> book.assign(1, UNKNOWN_ID, BigDecimal.ONE));
        assertThrows(DomainException.class,
                () -> book.evaluations(UNKNOWN_ID));
        assertTrue(book.evaluations(1).isEmpty());
    }

    @Test
    @DisplayName("Исправление сохраняет идентичность, отмена удаляет оценку")
    void correctsAndCancelsEvaluation() {
        Gradebook book = preparedBook();
        Evaluation original = book.assign(1, 1, number("8"));
        assertThrows(DomainException.class,
                () -> book.assign(1, 1, number("9")));
        Evaluation corrected = book.correct(1, 1, number("9"));
        assertEquals(original, corrected);
        assertEquals(original.hashCode(), corrected.hashCode());
        assertEquals(number("9"), book.evaluations(1).getFirst()
                .score().value());
        assertThrows(DomainException.class,
                () -> book.correct(1, 1, number("12")));
        assertEquals(number("9"), book.evaluations(1).getFirst()
                .score().value());
        book.cancel(1, 1);
        assertTrue(book.evaluations(1).isEmpty());
        assertThrows(DomainException.class, () -> book.cancel(1, 1));
        assertThrows(DomainException.class,
                () -> book.correct(1, 1, BigDecimal.ONE));
    }

    @Test
    @DisplayName("Снимки списков не меняются после добавления данных")
    void returnsStableSnapshots() {
        Gradebook book = preparedBook();
        List<Criterion> before = book.criteria();
        book.addCriterion("Экзамен", number("20"), number("60"));
        assertEquals(1, before.size());
        assertEquals(2, book.criteria().size());
    }

    private Gradebook preparedBook() {
        Gradebook book = new Gradebook();
        book.addStudent("Анна");
        book.addCriterion("Работа", number("10"), number("40"));
        return book;
    }

    private BigDecimal number(final String value) {
        return new BigDecimal(value);
    }
}
