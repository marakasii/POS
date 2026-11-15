package studying.pos.application.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Immutable report shared by console presentation and export adapters.
 * @param policy applied missing-grade policy
 * @param students ordered student results
 */
public record ResultReport(String policy, List<StudentResult> students) {
    private static final int DECIMAL_PLACES = 2;

    /**
     * Takes a defensive copy so reports remain stable after later changes.
     * @param policy applied policy key
     * @param students ordered student results
     */
    public ResultReport {
        students = List.copyOf(students);
    }

    /**
     * Averages only results that were actually calculated.
     * @return mean final score, or empty when no result is available
     */
    public Optional<BigDecimal> average() {
        List<BigDecimal> scores = students.stream()
                .flatMap(result -> result.score().stream()).toList();
        if (scores.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO,
                BigDecimal::add);
        return Optional.of(sum.divide(BigDecimal.valueOf(scores.size()),
                DECIMAL_PLACES, RoundingMode.HALF_UP));
    }
}
