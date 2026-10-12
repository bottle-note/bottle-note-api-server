package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.dto.response.AdminTastingTagCatalogItem;
import app.bottlenote.alcohols.dto.response.AdminTastingTagIconResponse;
import java.util.List;

/** 어드민 화면용 테이스팅 태그 경량 조회. */
public interface AdminTastingTagCatalogService {

  /** 아이콘 없이 전체 태그를 한글 이름 순으로 반환한다. */
  List<AdminTastingTagCatalogItem> findAll();

  /** 태그 아이콘만 반환한다. 태그가 없으면 예외를 던진다. */
  AdminTastingTagIconResponse findIcon(Long id);

  /** 어느 맛 그룹에도 없는 태그를 위스키 수 내림차순으로 반환한다. */
  List<AdminTastingTagCatalogItem> findUngrouped(String keyword);
}
