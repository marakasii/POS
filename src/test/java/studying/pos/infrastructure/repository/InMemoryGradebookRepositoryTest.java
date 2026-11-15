package studying.pos.infrastructure.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studying.pos.domain.model.Gradebook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests the aggregate storage contract in isolation. */
final class InMemoryGradebookRepositoryTest {
    @Test
    @DisplayName("Сохранённая ведомость доступна следующим сценариям")
    void storesReplacementAggregate() {
        InMemoryGradebookRepository repository =
                new InMemoryGradebookRepository();
        Gradebook replacement = new Gradebook();
        replacement.addStudent("Анна");
        repository.save(replacement);
        assertSame(replacement, repository.load());
        assertEquals("Анна", repository.load().students().getFirst().name());
        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertSame(replacement, repository.load());
    }
}
