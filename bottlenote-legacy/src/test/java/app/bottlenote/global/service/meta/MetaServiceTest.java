package app.bottlenote.global.service.meta;

import static app.bottlenote.shared.meta.MetaService.createMetaInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bottlenote.shared.meta.MetaInfos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

@Tag("unit")
@DisplayName("[unit] [service] MetaService")
class MetaServiceTest {

  @Test
  void 메타정보를_생성할_수_있다() {
    // given
    // MetaService는 static 메서드만 사용하므로 생성자 호출 불필요

    // when
    MetaInfos metaInfos = createMetaInfo();

    // then
    assertNotNull(metaInfos);
    assertEquals("1.0.0", metaInfos.findByKey("serverVersion"));
    assertEquals("v1", metaInfos.findByKey("serverPathVersion"));
    assertEquals("UTF-8", metaInfos.findByKey("serverEncoding"));
  }

  @Test
  void 생성된_메타정보에_추가적인_값을_추가할_수_있다() {
    // given
    MetaInfos metaInfos = createMetaInfo();
    PageRequest pageRequest = PageRequest.of(0, 10);

    // when
    MetaInfos result = metaInfos.add("page_request", pageRequest);

    // then
    assertNotNull(result);
    assertEquals(5, result.getMetaInfos().size());
    assertEquals(pageRequest, result.getMetaInfos().get("page_request"));
  }

  @Test
  void 생성된_메타정보에서_값을_찾을_수_있다() {
    // given
    MetaInfos metaInfos = createMetaInfo();
    PageRequest pageRequest = PageRequest.of(0, 10);
    metaInfos.add("page_request", pageRequest);

    // when
    PageRequest result = (PageRequest) metaInfos.findByKey("page_request");

    // then
    assertNotNull(result);
    assertEquals(0, result.getPageNumber());
    assertEquals(10, result.getPageSize());
  }
}
