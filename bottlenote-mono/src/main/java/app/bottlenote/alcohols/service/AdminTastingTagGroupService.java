package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupCreateRequest;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagRoleRequest;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupTagsRequest;
import app.bottlenote.alcohols.dto.request.AdminTastingTagGroupUpdateRequest;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupListResponse;
import app.bottlenote.global.dto.response.AdminResultResponse;

public interface AdminTastingTagGroupService {

  AdminTastingTagGroupListResponse list();

  AdminTastingTagGroupItem get(String code);

  AdminResultResponse create(AdminTastingTagGroupCreateRequest request);

  AdminResultResponse update(String code, AdminTastingTagGroupUpdateRequest request);

  AdminResultResponse delete(String code);

  AdminResultResponse addTags(String code, AdminTastingTagGroupTagsRequest request);

  AdminResultResponse changeTagRole(
      String code, Long tagId, AdminTastingTagGroupTagRoleRequest request);

  AdminResultResponse removeTag(String code, Long tagId);
}
