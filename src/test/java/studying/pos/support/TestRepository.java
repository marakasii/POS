package studying.pos.support;

import studying.pos.domain.model.Gradebook;
import studying.pos.domain.repository.GradebookRepository;

/** Isolated repository fake used by application and console unit tests. */
public final class TestRepository implements GradebookRepository {
    private final Gradebook gradebook = new Gradebook();
    private int saves;

    @Override
    public Gradebook load() {
        return gradebook;
    }

    @Override
    public void save(final Gradebook aggregate) {
        if (aggregate != gradebook) {
            throw new IllegalArgumentException("Unexpected aggregate");
        }
        saves++;
    }

    /**
     * Counts successful write use cases without accessing a real adapter.
     * @return save calls
     */
    public int saves() {
        return saves;
    }
}
