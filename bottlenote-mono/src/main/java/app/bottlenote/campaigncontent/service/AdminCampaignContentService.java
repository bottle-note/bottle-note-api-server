package app.bottlenote.campaigncontent.service;

import static app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode.CAMPAIGN_CONTENT_DUPLICATE_CODE;
import static app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode.CAMPAIGN_CONTENT_HAS_EVENTS;
import static app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode.CAMPAIGN_CONTENT_INVALID_CODE;
import static app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode.CAMPAIGN_CONTENT_INVALID_METRICS_RANGE;
import static app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode.CAMPAIGN_CONTENT_NOT_FOUND;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CAMPAIGN_CONTENT_CREATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CAMPAIGN_CONTENT_DELETED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CAMPAIGN_CONTENT_STATUS_UPDATED;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.CAMPAIGN_CONTENT_UPDATED;

import app.bottlenote.campaigncontent.domain.CampaignContent;
import app.bottlenote.campaigncontent.domain.CampaignContentEventCounts;
import app.bottlenote.campaigncontent.domain.CampaignContentEventRepository;
import app.bottlenote.campaigncontent.domain.CampaignContentExclusion;
import app.bottlenote.campaigncontent.domain.CampaignContentMetricsRepository;
import app.bottlenote.campaigncontent.domain.CampaignContentRepository;
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentCreateRequest;
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentMetricsRequest;
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentSearchRequest;
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentStatusRequest;
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentUpdateRequest;
import app.bottlenote.campaigncontent.dto.response.AdminCampaignContentDetailResponse;
import app.bottlenote.campaigncontent.dto.response.AdminCampaignContentListResponse;
import app.bottlenote.campaigncontent.dto.response.AdminCampaignContentMetricsResponse;
import app.bottlenote.campaigncontent.exception.CampaignContentException;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;
import app.bottlenote.statistics.facade.VisitorStatisticsFacade;
import app.bottlenote.statistics.facade.payload.VisitorExclusionItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCampaignContentService {

  static final int RECENT_PARTICIPANT_DAYS = 7;
  // 방문 텔레메트리 보존 기간(90일)을 넘으면 참여율 분모가 사라진다.
  static final int METRICS_MAX_DAYS = 90;
  private static final int CODE_MAX_LENGTH = 50;
  private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private final CampaignContentRepository campaignContentRepository;
  private final CampaignContentEventRepository campaignContentEventRepository;
  private final CampaignContentMetricsRepository campaignContentMetricsRepository;
  private final VisitorStatisticsFacade visitorStatisticsFacade;
  private final Clock clock;

  @Transactional(readOnly = true)
  public GlobalResponse search(AdminCampaignContentSearchRequest request) {
    List<CampaignContent> campaignContents =
        campaignContentRepository.searchForAdmin(
            request.keyword(), request.isActive(), request.page(), request.size());
    long total = campaignContentRepository.countForAdmin(request.keyword(), request.isActive());

    LocalDate today = LocalDate.now(clock.withZone(ZONE));
    Map<Long, Long> participants =
        campaignContentMetricsRepository.countResultMembers(
            campaignContents.stream().map(CampaignContent::getId).toList(),
            today.minusDays(RECENT_PARTICIPANT_DAYS - 1L).atStartOfDay(),
            today.plusDays(1).atStartOfDay(),
            exclusion());

    List<AdminCampaignContentListResponse> content =
        campaignContents.stream()
            .map(
                campaignContent ->
                    new AdminCampaignContentListResponse(
                        campaignContent.getId(),
                        campaignContent.getCode(),
                        campaignContent.getName(),
                        campaignContent.getDescription(),
                        participants.getOrDefault(campaignContent.getId(), 0L),
                        campaignContent.getIsActive(),
                        campaignContent.getCreateAt()))
            .toList();
    return GlobalResponse.fromPage(
        new PageImpl<>(content, PageRequest.of(request.page(), request.size()), total));
  }

  @Transactional(readOnly = true)
  public AdminCampaignContentDetailResponse getDetail(Long campaignContentId) {
    CampaignContent campaignContent = findCampaignContent(campaignContentId);
    return new AdminCampaignContentDetailResponse(
        campaignContent.getId(),
        campaignContent.getCode(),
        campaignContent.getName(),
        campaignContent.getDescription(),
        campaignContent.getIsActive(),
        campaignContent.getCreateAt(),
        campaignContent.getLastModifyAt());
  }

  @Transactional
  public AdminResultResponse create(AdminCampaignContentCreateRequest request) {
    validateCode(request.code());
    if (campaignContentRepository.existsByCode(request.code())) {
      throw new CampaignContentException(CAMPAIGN_CONTENT_DUPLICATE_CODE);
    }

    CampaignContent saved =
        campaignContentRepository.register(
            CampaignContent.builder()
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .isActive(request.isActive())
                .build());
    return AdminResultResponse.of(CAMPAIGN_CONTENT_CREATED, saved.getId());
  }

  @Transactional
  public AdminResultResponse update(
      Long campaignContentId, AdminCampaignContentUpdateRequest request) {
    CampaignContent campaignContent = findCampaignContent(campaignContentId);
    campaignContent.update(request.name(), request.description(), request.isActive());
    return AdminResultResponse.of(CAMPAIGN_CONTENT_UPDATED, campaignContentId);
  }

  @Transactional
  public AdminResultResponse updateStatus(
      Long campaignContentId, AdminCampaignContentStatusRequest request) {
    CampaignContent campaignContent = findCampaignContent(campaignContentId);
    campaignContent.updateStatus(request.isActive());
    return AdminResultResponse.of(CAMPAIGN_CONTENT_STATUS_UPDATED, campaignContentId);
  }

  @Transactional
  public AdminResultResponse delete(Long campaignContentId) {
    CampaignContent campaignContent = findCampaignContent(campaignContentId);
    // 참여 기록을 지우지 않도록 이벤트가 있으면 삭제 대신 비활성화를 안내한다.
    if (campaignContentEventRepository.existsByCampaignContentId(campaignContentId)) {
      throw new CampaignContentException(CAMPAIGN_CONTENT_HAS_EVENTS);
    }
    campaignContentRepository.remove(campaignContent);
    return AdminResultResponse.of(CAMPAIGN_CONTENT_DELETED, campaignContentId);
  }

  @Transactional(readOnly = true)
  public AdminCampaignContentMetricsResponse getMetrics(
      Long campaignContentId, AdminCampaignContentMetricsRequest request) {
    CampaignContent campaignContent = findCampaignContent(campaignContentId);
    LocalDate today = LocalDate.now(clock.withZone(ZONE));
    LocalDate to = request.to() != null ? request.to() : today;
    LocalDate from =
        request.from() != null ? request.from() : to.minusDays(RECENT_PARTICIPANT_DAYS - 1L);
    validateMetricsRange(from, to, today);

    LocalDateTime fromAt = from.atStartOfDay();
    LocalDateTime toExclusive = to.plusDays(1).atStartOfDay();
    CampaignContentEventCounts counts =
        campaignContentMetricsRepository.countEvents(
            campaignContent.getId(), fromAt, toExclusive, exclusion());
    long activeMembers = visitorStatisticsFacade.countActiveMembers(fromAt, toExclusive);

    return new AdminCampaignContentMetricsResponse(
        from,
        to,
        counts.viewVisitors(),
        counts.startVisitors(),
        counts.finishVisitors(),
        counts.resultMembers(),
        activeMembers,
        rate(counts.startedAndFinishedVisitors(), counts.startVisitors()),
        rate(counts.finishedAndResultVisitors(), counts.finishVisitors()),
        rate(counts.resultMembers(), activeMembers));
  }

  private CampaignContent findCampaignContent(Long campaignContentId) {
    return campaignContentRepository
        .findById(campaignContentId)
        .orElseThrow(() -> new CampaignContentException(CAMPAIGN_CONTENT_NOT_FOUND));
  }

  private void validateCode(String code) {
    if (code == null || code.length() > CODE_MAX_LENGTH || !CODE_PATTERN.matcher(code).matches()) {
      throw new CampaignContentException(CAMPAIGN_CONTENT_INVALID_CODE);
    }
  }

  private void validateMetricsRange(LocalDate from, LocalDate to, LocalDate today) {
    LocalDate oldest = today.minusDays(METRICS_MAX_DAYS - 1L);
    if (from.isAfter(to) || to.isAfter(today) || from.isBefore(oldest)) {
      throw new CampaignContentException(CAMPAIGN_CONTENT_INVALID_METRICS_RANGE);
    }
  }

  private CampaignContentExclusion exclusion() {
    VisitorExclusionItem exclusion = visitorStatisticsFacade.getExclusion();
    return new CampaignContentExclusion(exclusion.deviceTypes(), exclusion.ipPrefixes());
  }

  private static double rate(long numerator, long denominator) {
    if (denominator == 0L) {
      return 0.0;
    }
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
        .doubleValue();
  }
}
