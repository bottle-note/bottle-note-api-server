package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.user.dto.request.FollowPageableRequest;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.dto.response.FollowSearchResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.FollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

@RestController
@RequestMapping("/api/v1/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @GetMapping("/{userId}/relation-list")
    public ResponseEntity<?> findFollowList(@PathVariable Long userId, @ModelAttribute FollowPageableRequest pageableRequest) {

        PageResponse<FollowSearchResponse> pageResponse = followService.getRelationList(userId, pageableRequest);

        return GlobalResponse.ok(
                pageResponse.content(),
                MetaService.createMetaInfo().add("pageable", pageResponse.cursorPageable())
        );
    }

    @PostMapping
    public ResponseEntity<?> updateFollowStatus(@RequestBody @Valid FollowUpdateRequest request) {
        Long userId = getUserIdByContext()
                .orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

        return GlobalResponse.ok(followService.updateFollowStatus(request, userId));
    }
}
