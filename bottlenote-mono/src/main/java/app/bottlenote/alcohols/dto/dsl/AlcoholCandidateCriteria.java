package app.bottlenote.alcohols.dto.dsl;

import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest;
import java.util.List;

public record AlcoholCandidateCriteria(
    Long groupId, AdminAlcoholCandidateRequest request, List<Long> tastingTagIds) {}
