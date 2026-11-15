package studying.pos.domain.services;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import studying.pos.domain.exception.DomainException;
import studying.pos.domain.model.Gradebook;
import studying.pos.domain.strategy.impl.MissingAsZeroPolicy;
import studying.pos.domain.strategy.impl.StrictPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests weighted results against independently calculated examples. */
final class FinalScoreCalculatorTest {
    private final FinalScoreCalculator calculator = new FinalScoreCalculator();

    @ParameterizedTest
    @CsvSource({"40, 60", "0.4, 0.6", "4, 6"})
    @DisplayName("Веса нормализуются, разные максимумы учитываются")
    void calculatesNormalizedWeightedScore(final String firstWeight,
            final String secondWeight) {
        Gradebook book = preparedBook(firstWeight, secondWeight);
        book.assign(1, 1, number("8"));
        book.assign(1, 2, number("15"));
        assertEquals(Optional.of(number("7.70")),
                calculator.calculate(book, 1, new StrictPolicy()));
        assertEquals(calculator.calculate(book, 1, new StrictPolicy()),
                calculator.calculate(book, 1, new MissingAsZeroPolicy()));
    }

    @Test
    @DisplayName("Пропуск не перенормирует веса оставшихся оценок")
    void distinguishesMissingFromZero() {
        Gradebook book = preparedBook("40", "60");
        book.assign(1, 1, number("8"));
        assertTrue(calculator.calculate(book, 1, new StrictPolicy()).isEmpty());
        assertEquals(Optional.of(number("3.20")),
                calculator.calculate(book, 1, new MissingAsZeroPolicy()));
        book.assign(1, 2, BigDecimal.ZERO);
        assertEquals(Optional.of(number("3.20")),
                calculator.calculate(book, 1, new StrictPolicy()));
    }

    @Test
    @DisplayName("Итог следует исправлению, отмене и изменению рубрикатора")
    void neverReturnsAStaleResult() {
        Gradebook book = preparedBook("40", "60");
        book.assign(1, 1, number("8"));
        book.assign(1, 2, number("15"));
        book.correct(1, 1, number("10"));
        assertEquals(Optional.of(number("8.50")),
                calculator.calculate(book, 1, new StrictPolicy()));
        book.cancel(1, 1);
        assertEquals(Optional.of(number("4.50")),
                calculator.calculate(book, 1, new MissingAsZeroPolicy()));
        book.assign(1, 1, number("10"));
        book.addCriterion("Проект", number("10"), number("100"));
        assertTrue(calculator.calculate(book, 1, new StrictPolicy()).isEmpty());
        assertEquals(Optional.of(number("4.25")),
                calculator.calculate(book, 1, new MissingAsZeroPolicy()));
    }

    @Test
    @DisplayName("Пустой рубрикатор и неизвестный студент не дают итог")
    void rejectsUndefinedCalculations() {
        Gradebook book = new Gradebook();
        assertThrows(DomainException.class,
                () -> calculator.calculate(book, 1, new StrictPolicy()));
        book.addStudent("Анна");
        assertThrows(DomainException.class,
                () -> calculator.calculate(book, 1, new MissingAsZeroPolicy()));
    }

    @ParameterizedTest
    @CsvSource({"0, 0.00", "1, 3.33", "3, 10.00"})
    @DisplayName("Ноль, максимум и дробь округляются на итоговой шкале")
    void roundsFinalResult(final String points, final String expected) {
        Gradebook book = new Gradebook();
        book.addStudent("Анна");
        book.addCriterion("Работа", number("3"), BigDecimal.ONE);
        book.assign(1, 1, number(points));
        assertEquals(Optional.of(number(expected)),
                calculator.calculate(book, 1, new StrictPolicy()));
    }

    private Gradebook preparedBook(final String firstWeight,
            final String secondWeight) {
        Gradebook book = new Gradebook();
        book.addStudent("Анна");
        book.addCriterion("Домашняя работа", number("10"), number(firstWeight));
        book.addCriterion("Экзамен", number("20"), number(secondWeight));
        return book;
    }

    private BigDecimal number(final String value) {
        return new BigDecimal(value);
    }
}
