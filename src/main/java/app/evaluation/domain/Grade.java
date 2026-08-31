package app.evaluation.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The fixed set of marks on the 7-trins-skala a Suggested grade may take. Kept as a closed
 * enum, mirroring {@link app.rubric.Level}, rather than a string pattern, so an out-of-scale
 * value fails at deserialisation instead of needing a separate check.
 */
public enum Grade {
    MINUS_THREE("-3"),
    ZERO("00"),
    TWO("02"),
    FOUR("4"),
    SEVEN("7"),
    TEN("10"),
    TWELVE("12");

    private final String label;

    Grade(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static Grade fromLabel(String label) {
        for (Grade grade : values()) {
            if (grade.label.equals(label)) {
                return grade;
            }
        }
        throw new IllegalArgumentException("Unknown grade: " + label);
    }
}
