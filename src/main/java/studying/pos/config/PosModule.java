package studying.pos.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import studying.pos.application.port.ResultExporter;
import studying.pos.application.services.PosFacade;
import studying.pos.domain.repository.GradebookRepository;
import studying.pos.domain.services.FinalScoreCalculator;
import studying.pos.infrastructure.console.ConsoleApplication;
import studying.pos.infrastructure.export.CsvResultExporter;
import studying.pos.infrastructure.repository.InMemoryGradebookRepository;

/** Composition root: only the outer layer knows concrete adapters and Guice. */
public final class PosModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(GradebookRepository.class)
                .to(InMemoryGradebookRepository.class).in(Singleton.class);
        bind(ResultExporter.class).to(CsvResultExporter.class);
    }

    /**
     * Builds the facade using ports resolved by the container.
     * @param repository shared aggregate storage
     * @param exporter export adapter
     * @return application facade
     */
    @Provides
    @Singleton
    public PosFacade facade(final GradebookRepository repository,
            final ResultExporter exporter) {
        return new PosFacade(repository, new FinalScoreCalculator(), exporter);
    }

    /**
     * Wires the actual UTF-8 console streams at the edge of the application.
     * @param facade application facade
     * @return console application
     */
    @Provides
    public ConsoleApplication console(final PosFacade facade) {
        return new ConsoleApplication(new InputStreamReader(System.in,
                StandardCharsets.UTF_8), new PrintWriter(System.out, true,
                        StandardCharsets.UTF_8), facade);
    }
}
