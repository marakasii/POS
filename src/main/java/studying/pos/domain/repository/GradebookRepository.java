package studying.pos.domain.repository;

import studying.pos.domain.model.Gradebook;

/** Outbound domain port for the single-course gradebook. */
public interface GradebookRepository {
    /**
     * Loads the current course aggregate.
     * @return gradebook
     */
    Gradebook load();

    /**
     * Saves a successfully changed course aggregate.
     * @param gradebook validated aggregate
     */
    void save(Gradebook gradebook);
}
