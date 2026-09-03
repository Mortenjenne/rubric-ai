package app.evaluation.llm;

import app.assignment.Criterion;
import app.assignment.Level;
import app.assignment.Rubric;
import app.evaluation.domain.SuggestedGradeValue;
import org.springframework.stereotype.Component;

/**
 * Assembles the two prompts from the active Rubric and a Submission's text. Both prompts are
 * in Danish, because everything the model reads and writes here is Danish and a language switch
 * mid-prompt is one more thing for it to get wrong.
 *
 * <p>The system prompt carries only what is true of every evaluation: the advisory framing, the
 * AP-level calibration, the rule that Criteria are met by substance rather than by keyword, and
 * the JSON contract. Everything specific to the assignment — the Criteria, their Level
 * descriptors and the Source material behind them — comes from the Rubric, so that changing what
 * is assessed means editing the Rubric resource rather than this class.
 */
@Component
public class PromptBuilder {

    public LlmRequest build(Rubric rubric, String submissionText) {
        return new LlmRequest(systemPrompt(), userPrompt(rubric, submissionText));
    }

    private String systemPrompt() {
        return """
                Du er en fagfælle, der hjælper en underviser på Erhvervsakademi København med at \
                vurdere en studerendes praktikrapport op mod et fast bedømmelsesskema. Du leverer \
                en struktureret, vejledende vurdering. Dit svar er aldrig en endelig karakter eller \
                en afgørelse — den bedømmelse er underviserens. Dit svar er et udgangspunkt for \
                underviserens egen læsning, ikke en erstatning for den.

                NIVEAU OG FORVENTNINGER
                Uddannelsen er en erhvervsakademiuddannelse — en kort videregående uddannelse, ikke \
                en universitetsuddannelse. Forvent praksisnær refleksion over konkret arbejde i en \
                virksomhed. Forvent ikke akademisk stringens, forskningsmetode, litteraturgennemgang \
                eller teoretisk dybde på universitetsniveau. Læg ikke krav ind i vurderingen, som \
                ikke fremgår af bedømmelsesskemaet.

                KRITISK VURDERINGSREGEL
                Led efter implicit og indholdsmæssigt belæg for kriterierne. Forvent ikke, og kræv \
                ikke, at den studerende bruger kriteriernes eller værdiernes præcise navne — den \
                studerende behøver for eksempel ikke at skrive ordene "Dare", "Share" eller "Care". \
                Anfør aldrig fraværet af sådanne nøgleord som en svaghed eller et forbedringsforslag. \
                Vurder i stedet, om indholdet — de konkrete episoder, handlinger, valg og \
                refleksioner, den studerende beskriver — viser det, kriteriet handler om.

                BELÆG
                Byg hver vurdering på, hvad rapporten faktisk indeholder. Kan du ikke pege på et \
                sted i teksten, der understøtter en påstand, så skriv den ikke. En svaghed skal \
                handle om indhold, der mangler eller kun er tyndt belagt — ikke om ordvalg, \
                formuleringer eller manglende fagtermer. Hvert forbedringsforslag skal være \
                konkret og kunne handles på i netop denne rapport.

                Placer hvert kriterium på det niveau, hvis beskrivelse passer bedst på rapporten, \
                og brug alene niveaubeskrivelserne i bedømmelsesskemaet som målestok.

                SVARFORMAT
                Svar udelukkende med rå JSON: ét enkelt JSON-objekt, uden markdown-kodeblokke, \
                uden tekst før eller efter, og uden kommentarer.

                JSON-objektet skal have præcis disse felter på øverste niveau:
                - "overallAssessment": et sammenhængende afsnit i prosa
                - "suggestedGrade": præcis én af strengene %s (7-trins-skalaen)
                - "findings": en liste med præcis ét element pr. kriterium i brugerbeskeden, i \
                samme rækkefølge som de står der
                - "dialogueQuestions": en liste med fire til seks spørgsmål, som underviseren kan \
                tage op til den mundtlige praktikeksamen

                Hvert element i "findings" skal have præcis disse felter:
                - "criterion": kriteriets id, kopieret nøjagtigt som angivet i brugerbeskeden
                - "level": præcis én af %s
                - "strengths": en liste af strenge
                - "weaknesses": en liste af strenge
                - "improvements": en liste af strenge
                - "evidence": en liste af strenge, hver enkelt et ordret uddrag kopieret tegn for \
                tegn fra rapportens tekst — omskriv eller genskab aldrig et citat

                Al tekst i svaret skal være på dansk.
                """.formatted(quotedLabels(gradeLabels()), quotedLabels(levelLabels()));
    }

    private String userPrompt(Rubric rubric, String submissionText) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("=== BEDØMMELSESSKEMA ===\n\n");

        for (Criterion criterion : rubric.getCriteria()) {
            prompt.append("Kriterie-id: ").append(criterion.getKey()).append('\n');
            prompt.append("Navn: ").append(criterion.getName()).append('\n');
            prompt.append("Vægt: ").append(criterion.getWeight()).append('\n');
            if (criterion.getSourceReferences() != null && !criterion.getSourceReferences().isEmpty()) {
                prompt.append("Kilde: ").append(String.join(", ", criterion.getSourceReferences())).append('\n');
            }
            prompt.append("Beskrivelse: ").append(criterion.getDescription()).append('\n');
            for (Level level : Level.values()) {
                String descriptor = criterion.getLevels().get(level.label());
                if (descriptor != null) {
                    prompt.append(level.label()).append(": ").append(descriptor).append('\n');
                }
            }
            prompt.append('\n');
        }

        prompt.append("=== PRAKTIKRAPPORTENS TEKST ===\n\n").append(submissionText);
        return prompt.toString();
    }

    private String[] gradeLabels() {
        SuggestedGradeValue[] grades = SuggestedGradeValue.values();
        String[] labels = new String[grades.length];
        for (int i = 0; i < grades.length; i++) {
            labels[i] = grades[i].label();
        }
        return labels;
    }

    private String[] levelLabels() {
        Level[] levels = Level.values();
        String[] labels = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            labels[i] = levels[i].label();
        }
        return labels;
    }

    /** Renders enum labels as the quoted, comma-separated list the model is asked to choose from. */
    private String quotedLabels(String[] labels) {
        StringBuilder rendered = new StringBuilder();
        for (String label : labels) {
            if (!rendered.isEmpty()) {
                rendered.append(", ");
            }
            rendered.append('"').append(label).append('"');
        }
        return rendered.toString();
    }
}
