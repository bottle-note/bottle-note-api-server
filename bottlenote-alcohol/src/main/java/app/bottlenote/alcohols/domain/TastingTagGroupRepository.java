package app.bottlenote.alcohols.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;
import java.util.Optional;

/** 맛 그룹 포트. 그룹은 태그 연결까지 포함한 애그리게이트로 읽고 쓴다. */
@DomainRepository
public interface TastingTagGroupRepository {

  /** 계열·노출 순서·ID 순. 태그 연결을 함께 읽는다. */
  List<TastingTagGroup> findAllWithTags();

  Optional<TastingTagGroup> findById(Long id);

  Optional<TastingTagGroup> findByCode(String code);

  boolean existsByCode(String code);

  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, Long id);

  TastingTagGroup save(TastingTagGroup group);

  void delete(TastingTagGroup group);

  List<TastingTagGroup> lockTastingGroups(java.util.Collection<String> codes);

  List<String> findTastingGroupCodesByTagId(Long tagId);
}
