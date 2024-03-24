package app.bottlenote.global.service.meta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.Field;
import java.util.Map;

import static app.bottlenote.global.service.meta.MetaService.createMetaInfo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MetaServiceTest {

	@BeforeEach
	void setUp() throws NoSuchFieldException, IllegalAccessException {
		// Reflection을 사용하여 private static 필드에 접근
		Field serverVersionField = MetaService.class.getDeclaredField("serverVersion");
		Field serverPathVersionField = MetaService.class.getDeclaredField("serverPathVersion");
		Field serverEncodingField = MetaService.class.getDeclaredField("serverEncoding");

		// private 필드이므로 접근 가능하도록 설정
		serverVersionField.setAccessible(true);
		serverPathVersionField.setAccessible(true);
		serverEncodingField.setAccessible(true);

		// 필드에 값을 할당
		serverVersionField.set(null, "1.0.0");
		serverPathVersionField.set(null, "/v1");
		serverEncodingField.set(null, "UTF-8");
	}

	@Test
	void 메타정보를_생성할_수_있다() {
		// given
		MetaService metaService = new MetaService();

		// when
		MetaInfos metaInfos = createMetaInfo();

		// then
		assertNotNull(metaInfos);
		assertEquals("1.0.0", metaInfos.findByKey("server_version"));
		assertEquals("/v1", metaInfos.findByKey("server_path_version"));
		assertEquals("UTF-8", metaInfos.findByKey("server_encoding"));
	}

	@Test
	void 생성된_메타정보에_추가적인_값을_추가할_수_있다() {
		// given
		MetaInfos metaInfos = createMetaInfo();
		PageRequest pageRequest = PageRequest.of(0, 10);

		// when
		Map<String, Object> result = metaInfos.add("page_request", pageRequest);

		// then
		assertNotNull(result);
		assertEquals(5, result.size());
		assertEquals(pageRequest, result.get("page_request"));
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
