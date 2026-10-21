package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AlcoholCandidateRequest;
import app.bottlenote.alcohols.dto.response.AlcoholCandidateResponse;
import app.bottlenote.global.pagination.KeysetPageResponse;

public interface AlcoholCandidateService {
  KeysetPageResponse<AlcoholCandidateResponse> findCandidates(
      Long groupId, AlcoholCandidateRequest request);
}
