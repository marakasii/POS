package studying.pos.domain.factory;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studying.pos.domain.exception.DomainException;
import studying.pos.domain.model.Criterion;
import studying.pos.domain.model.Evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests that the factory uses the actual criterion maximum. */
final class EvaluationFactoryTest {
    @Test
    @DisplayName("Фабрика связывает оценку с критерием и его диапазоном")
    void usesCriterionBounds() {
        EvaluationFactory factory = new EvaluationFactory();
        Criterion criterion = new Criterion(2, "Экзамен",
                new BigDecimal("20"), BigDecimal.ONE);
        Evaluation evaluation = factory.create(1, criterion,
                new BigDecimal("15"));
        assertEquals(2, evaluation.criterionId());
        assertEquals(new BigDecimal("0.75"), evaluation.score().fraction());
        assertThrows(DomainException.class, () -> factory.create(1, criterion,
                new BigDecimal("20.01")));
    }
}
