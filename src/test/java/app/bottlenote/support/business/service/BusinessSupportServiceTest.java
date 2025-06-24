package app.bottlenote.support.business.service;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.dto.request.BusinessSupportUpsertRequest;
import app.bottlenote.support.business.dto.response.BusinessSupportResultResponse;
import app.bottlenote.support.business.repository.BusinessSupportRepository;
import app.bottlenote.user.facade.UserFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static app.bottlenote.support.business.constant.BusinessResultMessage.REGISTER_SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
}
