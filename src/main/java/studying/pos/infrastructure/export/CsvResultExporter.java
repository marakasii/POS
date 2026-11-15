package studying.pos.infrastructure.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import studying.pos.application.model.ResultReport;
import studying.pos.application.model.StudentResult;
import studying.pos.application.port.ResultExporter;

/** Writes UTF-8 CSV, quoting text and preserving incomplete results. */
public final class CsvResultExporter implements ResultExporter {
    @Override
    public void export(final ResultReport report, final Path destination)
            throws IOException {
        Path absolute = destination.toAbsolutePath();
        Files.createDirectories(absolute.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(absolute,
                StandardCharsets.UTF_8)) {
            writer.write("student_id,student_name,final_score,evaluated,"
                    + "criteria,status,policy\r\n");
            for (StudentResult result : report.students()) {
                writer.write(result.student().id() + ","
                        + quote(result.student().name()) + ","
                        + result.score().map(Object::toString).orElse("") + ","
                        + result.evaluated() + "," + result.criteria() + ","
                        + (result.evaluated() == result.criteria()
                                ? "complete" : "incomplete")
                        + "," + quote(report.policy()) + "\r\n");
            }
        }
    }

    private String quote(final String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
