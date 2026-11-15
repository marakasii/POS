package studying.pos.domain.strategy;

import java.math.BigDecimal;
import java.util.Optional;

/** Strategy for interpreting a criterion without an evaluation. */
public interface MissingGradePolicy {
    /**
     * Resolves the normalized contribution of a missing evaluation.
     * @return zero contribution or empty when calculation must wait
     */
    Optional<BigDecimal> missingFraction();

    /**
     * Names the policy in reports and exports.
     * @return stable user-facing policy key
     */
    String name();
}
