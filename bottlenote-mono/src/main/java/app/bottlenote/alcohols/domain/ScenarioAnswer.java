package app.bottlenote.alcohols.domain;

import app.bottlenote.alcohols.constant.ScenarioTypes.AnswerKind;
import java.math.BigDecimal;

public record ScenarioAnswer(
    Long questionId, AnswerKind kind, String choiceId, BigDecimal min, BigDecimal max) {}
