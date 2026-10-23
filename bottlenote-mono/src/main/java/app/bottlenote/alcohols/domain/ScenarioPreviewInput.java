package app.bottlenote.alcohols.domain;

import java.util.List;

public record ScenarioPreviewInput(
    List<ScenarioAnswer> answers,
    Long changedQuestionId,
    Long viewQuestionId,
    Integer page,
    Integer size) {}
