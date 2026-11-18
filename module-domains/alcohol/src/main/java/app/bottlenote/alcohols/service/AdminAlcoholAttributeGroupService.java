package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeGroupRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeGroupItem;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.List;

public interface AdminAlcoholAttributeGroupService {
  List<AdminAlcoholAttributeGroupItem> findAll();

  AdminAlcoholAttributeGroupItem create(AdminAlcoholAttributeGroupRequest request);

  AdminAlcoholAttributeGroupItem update(Long id, AdminAlcoholAttributeGroupRequest request);

  AdminResultResponse delete(Long id);
}
