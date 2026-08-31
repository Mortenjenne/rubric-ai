package app.evaluation.json;

import java.util.List;

public record FindingResponse(
        String criterion,
        String criterionName,
        int weight,
        String level,
        List<String> strengths,
        List<String> weaknesses,
        List<String> improvements,
        List<String> evidence) {
}
