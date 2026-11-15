package studying.pos.domain.strategy.impl;

import java.math.BigDecimal;
import java.util.Optional;
import studying.pos.domain.strategy.MissingGradePolicy;

/** Counts missing evaluations as zero without discarding their weights. */
public final class MissingAsZeroPolicy implements MissingGradePolicy {
    @Override
    public Optional<BigDecimal> missingFraction() {
        return Optional.of(BigDecimal.ZERO);
    }

    @Override
    public String name() {
        return "zero";
    }
}
