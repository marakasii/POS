package studying.pos.infrastructure.repository;

import java.util.Objects;
import studying.pos.domain.model.Gradebook;
import studying.pos.domain.repository.GradebookRepository;

/** Stores the single-course aggregate for the lifetime of the application. */
public final class InMemoryGradebookRepository implements GradebookRepository {
    private Gradebook gradebook = new Gradebook();

    @Override
    public Gradebook load() {
        return gradebook;
    }

    @Override
    public void save(final Gradebook aggregate) {
        gradebook = Objects.requireNonNull(aggregate, "gradebook");
    }
}
