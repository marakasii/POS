package studying.pos.application.port;

import java.io.IOException;
import java.nio.file.Path;
import studying.pos.application.model.ResultReport;

/** Outbound application port for exporting calculated results. */
public interface ResultExporter {
    /**
     * Exports a result snapshot to the requested destination.
     * @param report calculated report
     * @param destination output destination
     * @throws IOException when the adapter cannot write the report
     */
    void export(ResultReport report, Path destination) throws IOException;
}
