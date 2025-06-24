package app.bottlenote.support.business.service;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.support.business.constant.ContactType;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
import app.bottlenote.support.business.repository.BusinessSupportRepository;
import app.bottlenote.user.facade.UserFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static app.bottlenote.support.business.constant.BusinessResultMessage.DELETE_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.MODIFY_SUCCESS;
import static app.bottlenote.support.business.constant.BusinessResultMessage.REGISTER_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BusinessSupportServiceTest {

	@InjectMocks
	private BusinessSupportService service;
	@Mock
	private BusinessSupportRepository repository;
	@Mock
	private UserFacade userFacade;
	@Mock
	private ProfanityClient profanityClient;

	// BusinessSupport 객체를 Spy로 만들어 delete 메소드 호출을 감시
	@Spy
	private BusinessSupport businessSupport;

	@Test
	@DisplayName("비지니스 문의 등록")
	void register() {
		BusinessSupportUpsertRequest req = new BusinessSupportUpsertRequest("hi", null);
		when(profanityClient.getFilteredText(anyString())).thenReturn("hi");
		when(repository.findTopByUserIdAndContentOrderByIdDesc(anyLong(), anyString()))
				.thenReturn(Optional.empty());
		when(repository.save(any())).thenReturn(BusinessSupport.create(1L, "hi", null));

		BusinessSupportResultResponse res = service.register(req, 1L);
		assertEquals(REGISTER_SUCCESS, res.codeMessage());
	}

	@Test
	@DisplayName("동일한 내용의 문의를 중복 등록하면 예외가 발생한다.")
	void register_fail_with_duplicate_request() {
		// given
		Long userId = 1L;
		String content = "중복 문의 내용";
		BusinessSupportUpsertRequest req = new BusinessSupportUpsertRequest(content, ContactType.EMAIL);
		BusinessSupport existingSupport = BusinessSupport.create(userId, content, ContactType.EMAIL);

		// when
		doNothing().when(userFacade).isValidUserId(userId);
		when(profanityClient.getFilteredText(content)).thenReturn(content);
		when(repository.findTopByUserIdAndContentOrderByIdDesc(userId, content)).thenReturn(Optional.of(existingSupport));

		// then
		assertThrows(IllegalStateException.class, () -> service.register(req, userId));
		verify(repository, never()).save(any(BusinessSupport.class));
	}

	@Test
	@DisplayName("부적절한 단어가 포함된 내용은 필터링하여 등록한다.")
	void register_with_profanity_content() {
		// given
		Long userId = 1L;
		String profanityContent = "나쁜말 포함";
		String filteredContent = "필터링된_텍스트";
		BusinessSupportUpsertRequest req = new BusinessSupportUpsertRequest(profanityContent, ContactType.EMAIL);
		ArgumentCaptor<BusinessSupport> captor = ArgumentCaptor.forClass(BusinessSupport.class);

		BusinessSupport mockedSavedEntity = BusinessSupport.builder()
				.id(123L)
				.userId(userId)
				.content(filteredContent)
				.contactWay(req.contactWay())
				.build();

		// when
		when(profanityClient.getFilteredText(profanityContent)).thenReturn(filteredContent);
		when(repository.save(captor.capture())).thenReturn(mockedSavedEntity);

		// then
		service.register(req, userId);

		// then
		// 1. profanityClient가 정확한 인자와 함께 호출되었는지 검증
		verify(profanityClient, times(1)).getFilteredText(profanityContent);
		// 2. repository.save가 필터링된 내용으로 호출되었는지 검증
		assertEquals(filteredContent, captor.getValue().getContent());
	}

	@Test
	@DisplayName("본인의 문의를 성공적으로 수정할 수 있다.")
	void modify_success() {
		// given
		Long supportId = 1L;
		Long userId = 1L;
		BusinessSupportUpsertRequest req = new BusinessSupportUpsertRequest("수정된 내용", ContactType.EMAIL);
		BusinessSupport origin = BusinessSupport.builder().id(supportId).userId(userId).content("원본 내용").build();

		// when
		when(repository.findById(supportId)).thenReturn(Optional.of(origin));
		when(profanityClient.getFilteredText(req.content())).thenReturn(req.content());

		// then
		BusinessSupportResultResponse res = service.modify(supportId, req, userId);
		assertEquals(MODIFY_SUCCESS, res.codeMessage());
		assertEquals(supportId, res.id());
		assertEquals("수정된 내용", origin.getContent()); // 원본 객체의 내용이 변경되었는지 확인
		assertEquals("new@example.com", origin.getContactWay());
	}

	@Test
	@DisplayName("다른 사람의 문의를 수정하려고 하면 예외가 발생한다.")
	void modify_fail_with_unauthorized_user() {
		// given
		Long supportId = 1L;
		Long ownerId = 1L; // 문의 소유자
		Long requesterId = 2L; // 수정을 시도하는 사용자
		BusinessSupportUpsertRequest req = new BusinessSupportUpsertRequest("수정 시도", ContactType.EMAIL);
		BusinessSupport origin = BusinessSupport.builder().id(supportId).userId(ownerId).build();

		// when
		when(repository.findById(supportId)).thenReturn(Optional.of(origin));

		// then
		assertThrows(IllegalStateException.class, () -> service.modify(supportId, req, requesterId));
	}


	@Test
	@DisplayName("본인의 문의를 성공적으로 삭제(Soft Delete)할 수 있다.")
	void delete_success_soft_delete() {
		// given
		Long supportId = 1L;
		Long userId = 1L;
		// BusinessSupport 엔티티의 실제 인스턴스를 생성하고, 이를 Spy 객체로 감싸서 실제 메소드를 호출하면서도 호출 여부를 추적
		BusinessSupport origin = BusinessSupport.builder().id(supportId).userId(userId).build();
		BusinessSupport spySupport = org.mockito.Mockito.spy(origin);

		// when
		when(repository.findById(supportId)).thenReturn(Optional.of(spySupport));

		// then
		BusinessSupportResultResponse res = service.delete(supportId, userId);
		assertEquals(DELETE_SUCCESS, res.codeMessage());
		assertEquals(supportId, res.id());
		verify(spySupport, times(1)).delete(); // spy 객체의 delete 메소드가 호출되었는지 검증
	}

	@Test
	@DisplayName("다른 사람의 문의를 삭제하려고 하면 예외가 발생한다.")
	void delete_fail_with_unauthorized_user() {
		// given
		Long supportId = 1L;
		Long ownerId = 1L;
		Long requesterId = 2L;
		BusinessSupport origin = BusinessSupport.builder().id(supportId).userId(ownerId).build();

		// when
		when(repository.findById(supportId)).thenReturn(Optional.of(origin));

		// then
		assertThrows(IllegalStateException.class, () -> service.delete(supportId, requesterId));
	}
}
