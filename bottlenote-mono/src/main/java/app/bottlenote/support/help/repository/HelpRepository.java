package app.bottlenote.support.help.repository;

import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.support.help.constant.HelpType;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.dto.request.HelpPageableRequest;
import app.bottlenote.support.help.dto.response.HelpListResponse;
import java.util.List;
import java.util.Optional;

public interface HelpRepository {

  Help save(Help help);

  Optional<Help> findById(Long id);

  List<Help> findAll();

  List<Help> findAllByUserId(Long userId);

  List<Help> findAllByUserIdAndType(Long userId, HelpType helpType);

  Optional<Help> findByIdAndUserId(Long id, Long userId);

  PageResponse<HelpListResponse> getHelpList(
      HelpPageableRequest helpPageableRequest, Long currentUserId);
}
