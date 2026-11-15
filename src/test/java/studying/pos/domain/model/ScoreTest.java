package studying.pos.domain.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import studying.pos.domain.exception.DomainException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests bounded numeric values rather than Java-generated accessors. */
final class ScoreTest {
    @Test
    @DisplayName("VO равны по числовому значению независимо от масштаба")
    void normalizesDecimalScale() {
        Score first = new Score(new BigDecimal("8.0"), new BigDecimal("10"));
        Score second = new Score(new BigDecimal("8.00"),
                new BigDecimal("10.0"));
        assertEquals(first, second);
        assertEquals(new BigDecimal("0.8"), first.fraction());
    }

    @ParameterizedTest
    @CsvSource(value = {"null, 10", "1, null", "1, 0", "1, -1"},
            nullValues = "null")
    @DisplayName("VO нельзя создать без балла или с неверным максимумом")
    void rejectsInvalidBounds(final String value, final String maximum) {
        BigDecimal points = value == null ? null : new BigDecimal(value);
        BigDecimal bound = maximum == null ? null : new BigDecimal(maximum);
        assertThrows(DomainException.class, () -> new Score(points, bound));
    }
}
