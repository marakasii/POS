package studying.pos.domain.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import studying.pos.domain.exception.DomainException;
import studying.pos.domain.factory.EvaluationFactory;

/** Owns the students, rubric and evaluations of a single course. */
public final class Gradebook {
    private final Map<Integer, Student> students = new LinkedHashMap<>();
    private final Map<Integer, Criterion> criteria = new LinkedHashMap<>();
    private final Map<Integer, Map<Integer, Evaluation>> evaluations =
            new LinkedHashMap<>();
    private final EvaluationFactory factory = new EvaluationFactory();
    private int nextStudentId = 1;
    private int nextCriterionId = 1;

    /**
     * Adds a validated student and allocates a stable identity.
     * @param name student name
     * @return created student
     */
    public Student addStudent(final String name) {
        Student student = new Student(nextStudentId, name);
        students.put(student.id(), student);
        evaluations.put(student.id(), new LinkedHashMap<>());
        nextStudentId++;
        return student;
    }

    /**
     * Adds an immutable criterion to the course rubric.
     * @param name criterion name
     * @param maximum maximum score
     * @param weight positive relative weight
     * @return created criterion
     */
    public Criterion addCriterion(final String name, final BigDecimal maximum,
            final BigDecimal weight) {
        Criterion criterion = new Criterion(nextCriterionId, name,
                maximum, weight);
        criteria.put(criterion.id(), criterion);
        nextCriterionId++;
        return criterion;
    }

    /**
     * Lists students without exposing mutable aggregate collections.
     * @return students in insertion order
     */
    public List<Student> students() {
        return List.copyOf(students.values());
    }

    /**
     * Lists the current rubric.
     * @return immutable criterion snapshot
     */
    public List<Criterion> criteria() {
        return List.copyOf(criteria.values());
    }

    /**
     * Resolves a student belonging to this gradebook.
     * @param studentId student identifier
     * @return existing student
     */
    public Student student(final int studentId) {
        Student student = students.get(studentId);
        if (student == null) {
            throw new DomainException("Студент не найден: " + studentId);
        }
        return student;
    }

    /**
     * Resolves a criterion belonging to this rubric.
     * @param criterionId criterion identifier
     * @return existing criterion
     */
    public Criterion criterion(final int criterionId) {
        Criterion criterion = criteria.get(criterionId);
        if (criterion == null) {
            throw new DomainException("Критерий не найден: " + criterionId);
        }
        return criterion;
    }

    /**
     * Lists the current evaluations for a known student.
     * @param studentId student identifier
     * @return immutable evaluation snapshot
     */
    public List<Evaluation> evaluations(final int studentId) {
        student(studentId);
        return List.copyOf(evaluations.get(studentId).values());
    }

    /**
     * Assigns a first evaluation, protecting membership and uniqueness.
     * @param studentId existing student
     * @param criterionId rubric criterion
     * @param value awarded points
     * @return assigned evaluation
     */
    public Evaluation assign(final int studentId, final int criterionId,
            final BigDecimal value) {
        student(studentId);
        Criterion criterion = criterion(criterionId);
        if (evaluations.get(studentId).containsKey(criterionId)) {
            throw new DomainException("Оценка уже есть: используйте correct");
        }
        Evaluation evaluation = factory.create(studentId, criterion, value);
        evaluations.get(studentId).put(criterionId, evaluation);
        return evaluation;
    }

    /**
     * Replaces the score only after all validation succeeds.
     * @param studentId existing student
     * @param criterionId rubric criterion with an existing evaluation
     * @param value replacement points
     * @return corrected evaluation with the same identity
     */
    public Evaluation correct(final int studentId, final int criterionId,
            final BigDecimal value) {
        requireEvaluation(studentId, criterionId);
        Evaluation evaluation = factory.create(studentId,
                criterion(criterionId), value);
        evaluations.get(studentId).put(criterionId, evaluation);
        return evaluation;
    }

    /**
     * Cancels an existing evaluation.
     * @param studentId existing student
     * @param criterionId rubric criterion with an existing evaluation
     */
    public void cancel(final int studentId, final int criterionId) {
        requireEvaluation(studentId, criterionId);
        evaluations.get(studentId).remove(criterionId);
    }

    private void requireEvaluation(final int studentId, final int criterionId) {
        student(studentId);
        criterion(criterionId);
        if (!evaluations.get(studentId).containsKey(criterionId)) {
            throw new DomainException("Оценка не найдена");
        }
    }
}
