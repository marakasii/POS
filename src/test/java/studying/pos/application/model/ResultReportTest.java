package studying.pos.application.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studying.pos.domain.model.Student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests summary semantics and immutable report snapshots. */
final class ResultReportTest {
    @Test
    @DisplayName("Среднее не считает отсутствующий итог нулём")
    void averagesCalculatedScoresOnly() {
        List<StudentResult> results = new ArrayList<>();
        results.add(new StudentResult(new Student(1, "Анна"),
                Optional.of(new BigDecimal("7.70")), 2, 2));
        results.add(new StudentResult(new Student(2, "Борис"),
                Optional.empty(), 1, 2));
        ResultReport report = new ResultReport("strict", results);
        results.clear();
        assertEquals(2, report.students().size());
        assertEquals(Optional.of(new BigDecimal("7.70")), report.average());
        assertThrows(UnsupportedOperationException.class,
                () -> report.students().clear());
    }

    @Test
    @DisplayName("Пустая сводка не выдумывает средний балл")
    void hasNoAverageWithoutCalculatedResults() {
        assertTrue(new ResultReport("strict", List.of()).average().isEmpty());
        ResultReport report = new ResultReport("strict", List.of(
                new StudentResult(new Student(1, "Анна"), Optional.empty(),
                        0, 1)));
        assertTrue(report.average().isEmpty());
    }
}
