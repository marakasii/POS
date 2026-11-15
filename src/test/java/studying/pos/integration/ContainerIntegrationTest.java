package studying.pos.integration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import studying.Main;
import studying.pos.application.services.PosFacade;
import studying.pos.config.PosModule;
import studying.pos.domain.repository.GradebookRepository;
import studying.pos.domain.strategy.impl.StrictPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cross-adapter checks excluded from the unit-only JaCoCo report. */
@Tag("integration")
final class ContainerIntegrationTest {
    @TempDir
    private Path directory;

    @Test
    @DisplayName("DI собирает рабочие порты и разделяемое хранилище")
    void wiresWorkingRepositoryAndExporter() throws IOException {
        Injector injector = Guice.createInjector(new PosModule());
        PosFacade facade = injector.getInstance(PosFacade.class);
        assertSame(facade, injector.getInstance(PosFacade.class));
        facade.addStudent("Анна");
        facade.addCriterion("Работа", BigDecimal.TEN, BigDecimal.ONE);
        facade.assign(1, 1, new BigDecimal("8"));
        assertEquals(1, injector.getInstance(GradebookRepository.class)
                .load().students().size());
        Path output = directory.resolve("result.csv");
        facade.export(new StrictPolicy(), output);
        assertTrue(Files.readString(output).contains("8.00,1,1,complete"));
    }

    @Test
    @DisplayName("Настоящий Main запускает консоль через DI")
    void startsRealEntryPoint() throws IOException {
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try (PrintStream replacement = new PrintStream(captured, true,
                StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(
                    "student add Анна\nstudent list\nexit\n"
                            .getBytes(StandardCharsets.UTF_8)));
            System.setOut(replacement);
            Main.main(new String[0]);
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }
        String output = captured.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("1 | Анна"));
        assertTrue(output.contains("Работа завершена."));
    }
}
