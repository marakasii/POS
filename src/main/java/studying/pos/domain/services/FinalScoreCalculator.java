package studying.pos.domain.services;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import studying.pos.domain.exception.DomainException;
import studying.pos.domain.model.Criterion;
import studying.pos.domain.model.Evaluation;
import studying.pos.domain.model.Gradebook;
import studying.pos.domain.strategy.MissingGradePolicy;

/** Computes a normalized weighted score on a ten-point scale. */
public final class FinalScoreCalculator {
    private static final BigDecimal SCALE = BigDecimal.TEN;
    private static final int DECIMAL_PLACES = 2;

    /**
     * Calculates from the current rubric and evaluations, without caching.
     * @param gradebook current aggregate
     * @param studentId evaluated student
     * @param policy selected missing-evaluation strategy
     * @return final score or empty when a strict result is incomplete
     */
    public Optional<BigDecimal> calculate(final Gradebook gradebook,
            final int studentId, final MissingGradePolicy policy) {
        Map<Integer, Evaluation> evaluations = gradebook.evaluations(studentId)
                .stream().collect(Collectors.toMap(Evaluation::criterionId,
                        Function.identity()));
        if (gradebook.criteria().isEmpty()) {
            throw new DomainException("Рубрикатор пуст: итог не определён");
        }
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weights = BigDecimal.ZERO;
        for (Criterion criterion : gradebook.criteria()) {
            Evaluation evaluation = evaluations.get(criterion.id());
            Optional<BigDecimal> fraction = evaluation == null
                    ? policy.missingFraction()
                    : Optional.of(evaluation.score().fraction());
            if (fraction.isEmpty()) {
                return Optional.empty();
            }
            weighted = weighted.add(fraction.get().multiply(criterion.weight(),
                    MathContext.DECIMAL128), MathContext.DECIMAL128);
            weights = weights.add(criterion.weight());
        }
        return Optional.of(weighted.multiply(SCALE)
                .divide(weights, MathContext.DECIMAL128)
                .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP));
    }
}
