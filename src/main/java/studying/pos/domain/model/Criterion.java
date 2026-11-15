package studying.pos.domain.model;

import java.math.BigDecimal;
import studying.pos.domain.exception.DomainException;

/**
 * An immutable rubric criterion entity with a stable identity.
 * @param id criterion identifier
 * @param name display name
 * @param maximum maximum allowed score
 * @param weight positive relative contribution to the final score
 */
public record Criterion(int id, String name, BigDecimal maximum,
        BigDecimal weight) {
    /**
     * Protects rubric invariants before the criterion can enter a gradebook.
     * @param id positive identifier
     * @param name nonblank name
     * @param maximum strictly positive maximum
     * @param weight strictly positive weight
     */
    public Criterion {
        if (id <= 0 || name == null || name.isBlank()) {
            throw new DomainException("Нужны положительный ID и имя критерия");
        }
        if (maximum == null || maximum.signum() <= 0
                || weight == null || weight.signum() <= 0) {
            throw new DomainException("Максимум и вес должны быть больше нуля");
        }
        name = name.strip();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Criterion criterion && id == criterion.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
