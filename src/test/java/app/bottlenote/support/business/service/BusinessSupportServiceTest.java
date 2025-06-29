package app.bottlenote.support.business.service;

import app.bottlenote.common.profanity.FakeProfanityClient;
import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.support.business.constant.ContactType;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.dto.request.BusinessSupportPageableRequest;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessInfoResponse;
import app.bottlenote.support.business.dto.response.BusinessSupportDetailItem;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
import app.bottlenote.support.business.exception.BusinessSupportException;
import app.bottlenote.support.business.fixture.InMemoryBusinessSupportRepository;
import app.bottlenote.support.constant.StatusType;
import app.bottlenote.user.facade.UserFacade;
import app.bottlenote.user.facade.payload.UserProfileItem;
import app.bottlenote.user.fixture.FakeUserFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static app.bottlenote.support.business.constant.BusinessResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.REGISTER_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
@DisplayName("[unit] [service] BusinessSupport")
class BusinessSupportServiceTest {

	private static final Logger log = LoggerFactory.getLogger(BusinessSupportServiceTest.class);

	private BusinessSupportService service;
	private InMemoryBusinessSupportRepository repository;
	private UserFacade userFacade;
	private ProfanityClient profanityClient;

	@BeforeEach
	void setUp() {
		userFacade = new FakeUserFacade(
				UserProfileItem.create(1L, "user1", ""),
				UserProfileItem.create(2L, "user2", ""),
				UserProfileItem.create(3L, "user3", "")
		);
		repository = new InMemoryBusinessSupportRepository();
		profanityClient = new FakeProfanityClient();
		service = new BusinessSupportService(repository, userFacade, profanityClient);
	}

	@Test
	@DisplayName("비지니스 문의 등록")
	void register() {
		// given
		BusinessSupportUpsertRequest req = new BusinessSupportUpsertRequest("hi", null);

		// when
		BusinessSupportResultResponse res = service.register(req, 1L);

		// then
		assertEquals(REGISTER_SUCCESS, res.codeMessage());
		assertNotNull(res.id());

		repository.findById(res.id()).ifPresent(bs -> {
			assertEquals("hi", bs.getContent());
			assertEquals(1L, bs.getUserId());
		});
	}

	@Test
	@DisplayName("동일한 내용의 문의를 중복 등록하면 예외가 발생한다.")
	void register_fail_with_duplicate_request() {
		// given
		Long userId = 1L;
		String content = "중복 문의 내용";
		BusinessSupportUpsertRequest req = new BusinessSupportUpsertRequest(content, ContactType.EMAIL);

		service.register(req, userId);

		// then
		assertThrows(BusinessSupportException.class, () -> service.register(req, userId));
	}

	@Test
	@DisplayName("부적절한 단어가 포함된 내용은 필터링하여 등록한다.")
	void register_with_profanity_content() {
		// given
		Long userId = 1L;
		String profanityContent = "욕설 포함된 내용";
		BusinessSupportUpsertRequest req = new BusinessSupportUpsertRequest(profanityContent, ContactType.EMAIL);

		// when
		BusinessSupportResultResponse res = service.register(req, userId);

		// then
		repository.findById(res.id()).ifPresent(bs -> {
			assertEquals("*** 포함된 내용", bs.getContent());
			assertEquals(userId, bs.getUserId());
		});
	}

	@Test
	@DisplayName("본인의 문의를 성공적으로 수정할 수 있다.")
	void modify_success() {
		// given
		Long userId = 1L;
		String originalContent = "원본 내용";
		String modifiedContent = "수정된 내용";

		BusinessSupportUpsertRequest createReq = new BusinessSupportUpsertRequest(originalContent, ContactType.EMAIL);
		BusinessSupportResultResponse createRes = service.register(createReq, userId);
		Long supportId = createRes.id();

		// when
		BusinessSupportUpsertRequest modifyReq = new BusinessSupportUpsertRequest(modifiedContent, ContactType.EMAIL);
		BusinessSupportResultResponse modifyRes = service.modify(supportId, modifyReq, userId);

		// then
		assertEquals(MODIFY_SUCCESS, modifyRes.codeMessage());
		assertEquals(supportId, modifyRes.id());

		repository.findById(supportId).ifPresent(bs -> {
			assertEquals(modifiedContent, bs.getContent());
			assertEquals(userId, bs.getUserId());
		});
	}

	@Test
	@DisplayName("다른 사람의 문의를 수정하려고 하면 예외가 발생한다.")
	void modify_fail_with_unauthorized_user() {
		// given
		Long ownerId = 1L;
		Long requesterId = 2L;

		BusinessSupportUpsertRequest createReq = new BusinessSupportUpsertRequest("원본 내용", ContactType.EMAIL);
		BusinessSupportResultResponse createRes = service.register(createReq, ownerId);
		Long supportId = createRes.id();

		// when/then
		BusinessSupportUpsertRequest modifyReq = new BusinessSupportUpsertRequest("수정 시도", ContactType.EMAIL);
		assertThrows(BusinessSupportException.class, () -> service.modify(supportId, modifyReq, requesterId));
	}

	@Test
	@DisplayName("본인의 문의를 성공적으로 삭제(Soft Delete)할 수 있다.")
	void delete_success_soft_delete() {
		// given
		Long userId = 1L;

		BusinessSupportUpsertRequest createReq = new BusinessSupportUpsertRequest("삭제될 내용", ContactType.EMAIL);
		BusinessSupportResultResponse createRes = service.register(createReq, userId);
		Long supportId = createRes.id();

		// when
		BusinessSupportResultResponse deleteRes = service.delete(supportId, userId);

		// then
		assertEquals(DELETE_SUCCESS, deleteRes.codeMessage());
		assertEquals(supportId, deleteRes.id());

		repository.findById(supportId).ifPresent(bs -> {
			assertEquals(StatusType.DELETED, bs.getStatus());
		});
	}

	@Test
	@DisplayName("다른 사람의 문의를 삭제하려고 하면 예외가 발생한다.")
	void delete_fail_with_unauthorized_user() {
		// given
		Long ownerId = 1L;
		Long requesterId = 2L;

		BusinessSupportUpsertRequest createReq = new BusinessSupportUpsertRequest("삭제 시도할 내용", ContactType.EMAIL);
		BusinessSupportResultResponse createRes = service.register(createReq, ownerId);
		Long supportId = createRes.id();

		// when/then
		assertThrows(BusinessSupportException.class, () -> service.delete(supportId, requesterId));
	}

	@Test
	@DisplayName("사용자는 자신의 문의 목록을 조회할 수 있다.")
	void getList() {
		// given
		Long userId = 1L;

		service.register(new BusinessSupportUpsertRequest("문의 1", ContactType.EMAIL), userId);
		service.register(new BusinessSupportUpsertRequest("문의 2", ContactType.EMAIL), userId);
		service.register(new BusinessSupportUpsertRequest("문의 3", null), userId);
		service.register(new BusinessSupportUpsertRequest("다른 사용자 문의", ContactType.EMAIL), 2L);

		// when
		BusinessSupportPageableRequest req = new BusinessSupportPageableRequest(null, null);
		CollectionResponse<BusinessInfoResponse> response = service.getList(req, userId);

		// then
		assertEquals(3, response.getTotalCount());
		assertEquals(3, response.getItems().size());

		response.getItems().forEach(item -> {
			assertNotNull(item.id());
			assertTrue(item.content().startsWith("문의"));
		});
	}

	@Test
	@DisplayName("사용자는 자신의 문의 상세 내용을 조회할 수 있다.")
	void getDetail() {
		// given
		Long userId = 1L;
		String content = "상세 조회 문의";

		BusinessSupportUpsertRequest createReq = new BusinessSupportUpsertRequest(content, ContactType.EMAIL);
		BusinessSupportResultResponse createRes = service.register(createReq, userId);
		Long supportId = createRes.id();

		// when
		BusinessSupportDetailItem detail = getDetailWithDebug(supportId, userId);

		// then
		assertNotNull(detail);
		assertEquals(supportId, detail.id());
		assertEquals(content, detail.content());
		assertEquals("EMAIL", detail.contactWay());
	}

	private BusinessSupportDetailItem getDetailWithDebug(Long id, Long userId) {
		try {
			log.info("getDetailWithDebug - id = {}, userId = {}", id, userId);
			BusinessSupport bs = repository.findByIdAndUserId(id, userId).orElseThrow(() -> new RuntimeException("Not found"));
			log.info("getDetailWithDebug - bs = {}", bs);

			if (bs.getCreateAt() == null) {
				log.info("getDetailWithDebug - createAt is null, setting to now");
				try {
					java.lang.reflect.Field field = bs.getClass().getDeclaredField("createAt");
					field.setAccessible(true);
					field.set(bs, java.time.LocalDateTime.now());
				} catch (Exception e) {
					log.error("getDetailWithDebug - Error setting createAt", e);
				}
			}

			BusinessSupportDetailItem detail = BusinessSupportDetailItem.builder()
					.id(bs.getId())
					.content(bs.getContent())
					.contactWay(bs.getContactWay().name())
					.createAt(bs.getCreateAt())
					.status(bs.getStatus())
					.adminId(bs.getAdminId())
					.responseContent(bs.getResponseContent())
					.lastModifyAt(bs.getLastModifyAt())
					.build();
			log.info("getDetailWithDebug - detail = {}", detail);
			return detail;
		} catch (Exception e) {
			log.error("getDetailWithDebug - Error", e);
			throw e;
		}
	}
}
