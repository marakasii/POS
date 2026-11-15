package studying.pos.domain.strategy.impl;

import java.math.BigDecimal;
import java.util.Optional;
import studying.pos.domain.strategy.MissingGradePolicy;

/** Withholds the final score until every rubric criterion is evaluated. */
public final class StrictPolicy implements MissingGradePolicy {
    @Override
    public Optional<BigDecimal> missingFraction() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "strict";
    }
}
