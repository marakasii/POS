package studying.pos.application.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import studying.pos.application.model.ResultReport;
import studying.pos.application.model.StudentResult;
import studying.pos.application.port.ResultExporter;
import studying.pos.domain.model.Criterion;
import studying.pos.domain.model.Evaluation;
import studying.pos.domain.model.Gradebook;
import studying.pos.domain.model.Student;
import studying.pos.domain.repository.GradebookRepository;
import studying.pos.domain.services.FinalScoreCalculator;
import studying.pos.domain.strategy.MissingGradePolicy;

/** Application facade coordinating repository, domain rules and export. */
public final class PosFacade {
    private final GradebookRepository repository;
    private final FinalScoreCalculator calculator;
    private final ResultExporter exporter;

    /**
     * Accepts external dependencies instead of constructing adapters.
     * @param storage aggregate storage port
     * @param scoreCalculator domain calculation service
     * @param resultExporter report export port
     */
    public PosFacade(final GradebookRepository storage,
            final FinalScoreCalculator scoreCalculator,
            final ResultExporter resultExporter) {
        repository = storage;
        calculator = scoreCalculator;
        exporter = resultExporter;
    }

    /**
     * Registers a student and saves the changed aggregate.
     * @param name student name
     * @return registered student
     */
    public Student addStudent(final String name) {
        Gradebook gradebook = repository.load();
        Student student = gradebook.addStudent(name);
        repository.save(gradebook);
        return student;
    }

    /**
     * Lists registered students.
     * @return immutable student snapshot
     */
    public List<Student> students() {
        return repository.load().students();
    }

    /**
     * Adds a criterion to the rubric and saves it.
     * @param name criterion name
     * @param maximum maximum points
     * @param weight positive relative weight
     * @return created criterion
     */
    public Criterion addCriterion(final String name, final BigDecimal maximum,
            final BigDecimal weight) {
        Gradebook gradebook = repository.load();
        Criterion criterion = gradebook.addCriterion(name, maximum, weight);
        repository.save(gradebook);
        return criterion;
    }

    /**
     * Lists the current rubric.
     * @return immutable criterion snapshot
     */
    public List<Criterion> criteria() {
        return repository.load().criteria();
    }

    /**
     * Assigns a first evaluation.
     * @param studentId student identifier
     * @param criterionId rubric criterion identifier
     * @param value awarded points
     * @return assigned evaluation
     */
    public Evaluation assign(final int studentId, final int criterionId,
            final BigDecimal value) {
        Gradebook gradebook = repository.load();
        Evaluation evaluation = gradebook.assign(studentId, criterionId, value);
        repository.save(gradebook);
        return evaluation;
    }

    /**
     * Corrects an existing evaluation.
     * @param studentId student identifier
     * @param criterionId criterion identifier
     * @param value replacement points
     * @return corrected evaluation
     */
    public Evaluation correct(final int studentId, final int criterionId,
            final BigDecimal value) {
        Gradebook gradebook = repository.load();
        Evaluation evaluation = gradebook.correct(studentId, criterionId,
                value);
        repository.save(gradebook);
        return evaluation;
    }

    /**
     * Cancels an existing evaluation and saves the aggregate.
     * @param studentId student identifier
     * @param criterionId criterion identifier
     */
    public void cancel(final int studentId, final int criterionId) {
        Gradebook gradebook = repository.load();
        gradebook.cancel(studentId, criterionId);
        repository.save(gradebook);
    }

    /**
     * Lists a student's current evaluations.
     * @param studentId student identifier
     * @return evaluation snapshot
     */
    public List<Evaluation> evaluations(final int studentId) {
        return repository.load().evaluations(studentId);
    }

    /**
     * Calculates a student's current result.
     * @param studentId student identifier
     * @param policy missing-evaluation strategy
     * @return fresh result
     */
    public StudentResult total(final int studentId,
            final MissingGradePolicy policy) {
        return result(repository.load(), studentId, policy);
    }

    /**
     * Calculates a report from a single current aggregate snapshot.
     * @param policy missing-evaluation strategy
     * @return report for all students
     */
    public ResultReport report(final MissingGradePolicy policy) {
        Gradebook gradebook = repository.load();
        List<StudentResult> results = gradebook.students().stream()
                .map(student -> result(gradebook, student.id(), policy))
                .toList();
        return new ResultReport(policy.name(), results);
    }

    /**
     * Exports a freshly calculated report through an external port.
     * @param policy missing-evaluation strategy
     * @param destination export destination
     * @throws IOException when the adapter cannot write the report
     */
    public void export(final MissingGradePolicy policy, final Path destination)
            throws IOException {
        exporter.export(report(policy), destination);
    }

    private StudentResult result(final Gradebook gradebook, final int studentId,
            final MissingGradePolicy policy) {
        return new StudentResult(gradebook.student(studentId),
                calculator.calculate(gradebook, studentId, policy),
                gradebook.evaluations(studentId).size(),
                gradebook.criteria().size());
    }
}
