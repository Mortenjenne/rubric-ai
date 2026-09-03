package app.evaluation.llm;

import app.assignment.Criterion;
import app.assignment.Level;
import app.assignment.Rubric;
import app.evaluation.domain.SuggestedGradeValue;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-level: the prompt is the only place where the JSON contract is stated to the model, so
 * these tests pin the parts of it that other code then parses back — a drift between the two
 * ends is otherwise invisible until a live call fails validation twice and the request dies.
 */
class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    @Test
    void asksForASuggestedGradeOnTheSevenPointScaleNotARubricLevel() {
        String system = promptBuilder.build(rubric(), "tekst").systemPrompt();

        for (SuggestedGradeValue grade : SuggestedGradeValue.values()) {
            assertThat(system).contains('"' + grade.label() + '"');
        }
        // The Levels are a separate vocabulary; naming one where the grade is asked for would
        // make every response fail SuggestedGradeValue deserialisation.
        int gradeLine = system.indexOf("\"suggestedGrade\"");
        int levelLine = system.indexOf("\"level\"");
        assertThat(system.substring(gradeLine, levelLine)).doesNotContain(Level.UDMAERKET.label());
    }

    @Test
    void offersEveryRubricLevelAsAFindingLevel() {
        String system = promptBuilder.build(rubric(), "tekst").systemPrompt();

        int levelLine = system.indexOf("\"level\"");
        String fromLevelLine = system.substring(levelLine);
        for (Level level : Level.values()) {
            assertThat(fromLevelLine).contains('"' + level.label() + '"');
        }
    }

    @Test
    void forbidsTreatingMissingKeywordsAsAWeakness() {
        String system = promptBuilder.build(rubric(), "tekst").systemPrompt();

        assertThat(system).contains("Anfør aldrig fraværet af sådanne nøgleord");
        assertThat(system).contains("erhvervsakademiuddannelse");
    }

    @Test
    void carriesEachCriterionWithItsSourceAndLevelDescriptors() {
        String user = promptBuilder.build(rubric(), "Rapportens tekst").userPrompt();

        assertThat(user).contains("Kriterie-id: dare-share-care");
        assertThat(user).contains("Kilde: dare-share-care.md");
        assertThat(user).contains("Udmærket: Alle tre værdier er belagt");
        assertThat(user).contains("Rapportens tekst");
    }

    private Rubric rubric() {
        Map<String, String> levels = new LinkedHashMap<>();
        levels.put(Level.UDMAERKET.label(), "Alle tre værdier er belagt med konkrete episoder.");
        levels.put(Level.MANGELFULDT.label(), "Værdierne er ikke genkendelige i rapportens indhold.");
        Criterion criterion = new Criterion(
                "dare-share-care", "Dare, Share, Care", 10,
                "EK's tre kerneværdier.", List.of("dare-share-care.md"), levels);
        return new Rubric(List.of(criterion));
    }
}
