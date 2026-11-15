package studying.pos.infrastructure.export;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import studying.pos.application.model.ResultReport;
import studying.pos.application.model.StudentResult;
import studying.pos.domain.model.Student;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests one CSV adapter using disposable isolated filesystem destinations. */
final class CsvResultExporterTest {
    @TempDir
    private Path directory;

    @Test
    @DisplayName("CSV сохраняет кириллицу, кавычки, переносы и статус пропуска")
    void quotesTextAndPreservesMissingResults() throws IOException {
        ResultReport report = new ResultReport("strict", List.of(
                new StudentResult(new Student(1, "Анна, \"Аня\"\nИванова"),
                        Optional.of(new BigDecimal("7.70")), 2, 2),
                new StudentResult(new Student(2, "Борис"), Optional.empty(),
                        1, 2)));
        Path output = directory.resolve("nested/results.csv");
        new CsvResultExporter().export(report, output);
        assertEquals("student_id,student_name,final_score,evaluated,"
                + "criteria,status,policy\r\n"
                + "1,\"Анна, \"\"Аня\"\"\nИванова\",7.70,2,2,complete,"
                + "\"strict\"\r\n"
                + "2,\"Борис\",,1,2,incomplete,\"strict\"\r\n",
                Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Повторный экспорт заменяет файл, пустой отчёт даёт заголовок")
    void replacesExistingExport() throws IOException {
        Path output = directory.resolve("report.csv");
        Files.writeString(output, "old data must disappear");
        new CsvResultExporter().export(new ResultReport("zero", List.of()),
                output);
        assertEquals("student_id,student_name,final_score,evaluated,"
                + "criteria,status,policy\r\n", Files.readString(output));
    }

    @Test
    @DisplayName("Путь к каталогу даёт проверяемую ошибку записи")
    void reportsUnwritableDestination() {
        assertThrows(IOException.class, () -> new CsvResultExporter().export(
                new ResultReport("strict", List.of()), directory));
    }
}
