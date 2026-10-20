package app.bottlenote.alcohols.service;

import static app.bottlenote.alcohols.exception.AlcoholExceptionCode.*;
import static app.bottlenote.global.dto.response.AdminResultResponse.ResultCode.ALCOHOL_ATTRIBUTE_GROUP_DELETED;

import app.bottlenote.alcohols.domain.*;
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeGroupRequest;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeGroupItem;
import app.bottlenote.alcohols.exception.AlcoholException;
import app.bottlenote.global.dto.response.AdminResultResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DefaultAdminAlcoholAttributeGroupService implements AdminAlcoholAttributeGroupService {
  private final AlcoholAttributeGroupRepository groupRepository;
  private final AlcoholAttributeRepository attributeRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AdminAlcoholAttributeGroupItem> findAll() {
    return groupRepository.findAllByOrderByDisplayOrderAscIdAsc().stream()
        .map(this::toItem)
        .toList();
  }

  @Override
  @Transactional
  public AdminAlcoholAttributeGroupItem create(AdminAlcoholAttributeGroupRequest request) {
    var group =
        AlcoholAttributeGroup.create(request.name(), request.description(), request.displayOrder());
    requireUniqueName(group.getName(), null);
    return toItem(groupRepository.save(group));
  }

  @Override
  @Transactional
  public AdminAlcoholAttributeGroupItem update(Long id, AdminAlcoholAttributeGroupRequest request) {
    var group = requireLockedGroup(id);
    var candidate =
        AlcoholAttributeGroup.create(request.name(), request.description(), request.displayOrder());
    requireUniqueName(candidate.getName(), id);
    group.update(candidate.getName(), candidate.getDescription(), candidate.getDisplayOrder());
    return toItem(groupRepository.save(group));
  }

  @Override
  @Transactional
  public AdminResultResponse delete(Long id) {
    var group = requireLockedGroup(id);
    if (attributeRepository.existsByGroupId(id))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_IN_USE);
    groupRepository.delete(group);
    return AdminResultResponse.of(ALCOHOL_ATTRIBUTE_GROUP_DELETED, id);
  }

  private AlcoholAttributeGroup requireLockedGroup(Long id) {
    return groupRepository.lockByIds(List.of(id)).stream()
        .findFirst()
        .orElseThrow(() -> new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_NOT_FOUND));
  }

  private void requireUniqueName(String name, Long id) {
    if (groupRepository.existsByNameAndIdNot(name, id))
      throw new AlcoholException(ALCOHOL_ATTRIBUTE_GROUP_DUPLICATE);
  }

  private AdminAlcoholAttributeGroupItem toItem(AlcoholAttributeGroup group) {
    return new AdminAlcoholAttributeGroupItem(
        group.getId(),
        group.getName(),
        group.getDescription(),
        group.getDisplayOrder(),
        group.getCreateAt(),
        group.getLastModifyAt(),
        AlcoholAttributeResponseMapper.toPrincipal(group.getCreatePrincipal()),
        AlcoholAttributeResponseMapper.toPrincipal(group.getLastModifyPrincipal()));
  }
}
