package studying.pos.application.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import studying.pos.application.model.ResultReport;
import studying.pos.domain.exception.DomainException;
import studying.pos.domain.services.FinalScoreCalculator;
import studying.pos.domain.strategy.impl.StrictPolicy;
import studying.pos.support.TestRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests application coordination with fake outbound ports. */
final class PosFacadeTest {
    private static final int SUCCESSFUL_WRITES = 5;
    @Test
    @DisplayName("Сохраняются только успешные изменения агрегата")
    void savesSuccessfulChangesOnly() {
        TestRepository repository = new TestRepository();
        PosFacade facade = facade(repository);
        facade.addStudent("Анна");
        facade.addCriterion("Работа", BigDecimal.TEN, BigDecimal.ONE);
        int before = repository.saves();
        assertThrows(DomainException.class,
                () -> facade.assign(1, 1, new BigDecimal("12")));
        assertEquals(before, repository.saves());
        assertTrue(facade.evaluations(1).isEmpty());
        facade.assign(1, 1, new BigDecimal("8"));
        facade.correct(1, 1, new BigDecimal("9"));
        assertEquals(new BigDecimal("9.00"), facade.total(1,
                new StrictPolicy()).score().orElseThrow());
        facade.cancel(1, 1);
        assertEquals(SUCCESSFUL_WRITES, repository.saves());
        assertEquals(1, facade.students().size());
        assertEquals(1, facade.criteria().size());
        assertTrue(facade.total(1, new StrictPolicy()).score().isEmpty());
    }

    @Test
    @DisplayName("Экспорт получает актуальный снимок, старый не изменяется")
    void exportsFreshReport() throws IOException {
        TestRepository repository = new TestRepository();
        AtomicReference<ResultReport> exported = new AtomicReference<>();
        AtomicReference<Path> destination = new AtomicReference<>();
        PosFacade facade = new PosFacade(repository, new FinalScoreCalculator(),
                (report, path) -> {
                    exported.set(report);
                    destination.set(path);
                });
        facade.addStudent("Анна");
        facade.addCriterion("Работа", BigDecimal.TEN, BigDecimal.ONE);
        facade.assign(1, 1, new BigDecimal("8"));
        ResultReport old = facade.report(new StrictPolicy());
        facade.correct(1, 1, new BigDecimal("9"));
        Path target = Path.of("result.csv");
        facade.export(new StrictPolicy(), target);
        assertSame(target, destination.get());
        assertEquals("strict", exported.get().policy());
        assertEquals(new BigDecimal("9.00"), exported.get().students()
                .getFirst().score().orElseThrow());
        assertEquals(new BigDecimal("8.00"), old.students().getFirst()
                .score().orElseThrow());
    }

    @Test
    @DisplayName("Ошибка адаптера экспорта передаётся вызывающему коду")
    void propagatesExportFailure() {
        PosFacade facade = new PosFacade(new TestRepository(),
                new FinalScoreCalculator(), (report, path) -> {
                    throw new IOException("disk full");
                });
        assertThrows(IOException.class,
                () -> facade.export(new StrictPolicy(), Path.of("out.csv")));
    }

    private PosFacade facade(final TestRepository repository) {
        return new PosFacade(repository, new FinalScoreCalculator(),
                (report, path) -> {
                    throw new AssertionError("Export was not requested");
                });
    }
}
