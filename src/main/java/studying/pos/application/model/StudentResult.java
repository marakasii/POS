package studying.pos.application.model;

import java.math.BigDecimal;
import java.util.Optional;
import studying.pos.domain.model.Student;

/**
 * Read model for a freshly calculated student result.
 * @param student evaluated student
 * @param score final score, absent for incomplete strict results
 * @param evaluated number of evaluated criteria
 * @param criteria total rubric size
 */
public record StudentResult(Student student, Optional<BigDecimal> score,
        int evaluated, int criteria) {
}
