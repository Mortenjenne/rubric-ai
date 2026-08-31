package app.rubric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * One column of a Rubric: a named band of performance. A Level names a quality,
 * never a grade — the label is what both the bundled Rubric resource and the
 * model's JSON output use on the wire.
 */
public enum Level {
    MANGELFULDT("Mangelfuldt"),
    ACCEPTABELT("Acceptabelt"),
    TILFREDSSTILLENDE("Tilfredsstillende"),
    UDMAERKET("Udmærket");

    private final String label;

    Level(String label) {
        this.label = label;
    }

    @JsonValue
    public String label() {
        return label;
    }

    @JsonCreator
    public static Level fromLabel(String label) {
        for (Level level : values()) {
            if (level.label.equals(label)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown level: " + label);
    }
}
