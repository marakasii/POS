package studying.pos.domain.model;

import studying.pos.domain.exception.DomainException;

/**
 * A student entity identified by its identifier, independently of its name.
 * @param id stable identifier inside the gradebook
 * @param name nonblank display name
 */
public record Student(int id, String name) {
    /**
     * Validates the student identity and name.
     * @param id stable positive identifier
     * @param name display name
     */
    public Student {
        if (id <= 0 || name == null || name.isBlank()) {
            throw new DomainException("Нужны положительный ID и имя студента");
        }
        name = name.strip();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Student student && id == student.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
