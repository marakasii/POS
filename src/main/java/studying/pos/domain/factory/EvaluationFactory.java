package studying.pos.domain.factory;

import java.math.BigDecimal;
import studying.pos.domain.model.Criterion;
import studying.pos.domain.model.Evaluation;
import studying.pos.domain.model.Score;

/** Creates evaluations using the actual rubric criterion's score bounds. */
public final class EvaluationFactory {
    /**
     * Creates an evaluation after aggregate membership has been checked.
     * @param studentId existing student identifier
     * @param criterion actual criterion resolved by the aggregate
     * @param value awarded points
     * @return valid evaluation
     */
    public Evaluation create(final int studentId, final Criterion criterion,
            final BigDecimal value) {
        return new Evaluation(studentId, criterion.id(),
                new Score(value, criterion.maximum()));
    }
}
