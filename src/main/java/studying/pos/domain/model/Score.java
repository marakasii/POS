package studying.pos.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import studying.pos.domain.exception.DomainException;

/**
 * An immutable bounded score value object without an independent identity.
 * @param value awarded points
 * @param maximum criterion maximum
 */
public record Score(BigDecimal value, BigDecimal maximum) {
    /**
     * Rejects an impossible score, including an invalid maximum.
     * @param value awarded points
     * @param maximum strictly positive upper bound
     */
    public Score {
        if (maximum == null || maximum.signum() <= 0 || value == null
                || value.signum() < 0 || value.compareTo(maximum) > 0) {
            throw new DomainException("Балл должен быть в диапазоне критерия");
        }
        value = value.stripTrailingZeros();
        maximum = maximum.stripTrailingZeros();
    }

    /**
     * Normalizes points without rounding the final grade prematurely.
     * @return fraction between zero and one
     */
    public BigDecimal fraction() {
        return value.divide(maximum, MathContext.DECIMAL128);
    }
}
