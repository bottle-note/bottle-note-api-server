package app.bottlenote.alcohols.dto.candidate;

import app.bottlenote.alcohols.dto.request.AlcoholCandidateRequest;
import java.util.List;

public record AlcoholCandidateQuery(
    Long groupId, AlcoholCandidateRequest request, List<Long> tastingTagIds) {}
