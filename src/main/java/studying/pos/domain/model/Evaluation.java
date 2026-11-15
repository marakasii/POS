package studying.pos.domain.model;

import java.util.Objects;
import studying.pos.domain.exception.DomainException;

/**
 * An evaluation entity identified by the student and criterion pair.
 * @param studentId evaluated student
 * @param criterionId rubric criterion
 * @param score validated bounded score
 */
public record Evaluation(int studentId, int criterionId, Score score) {
    /**
     * Validates the local shape of an evaluation.
     * Membership is protected by the gradebook aggregate.
     * @param studentId positive student identifier
     * @param criterionId positive criterion identifier
     * @param score bounded score
     */
    public Evaluation {
        if (studentId <= 0 || criterionId <= 0 || score == null) {
            throw new DomainException("Некорректные данные оценки");
        }
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Evaluation evaluation
                && studentId == evaluation.studentId
                && criterionId == evaluation.criterionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentId, criterionId);
    }
}
