package app.bottlenote.picks.service;

import app.bottlenote.alcohols.repository.AlcoholQueryRepository;
import app.bottlenote.picks.repository.PicksRepository;
import app.bottlenote.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PicksCommandServiceTest {

	@InjectMocks
	private PicksCommandService picksCommandService;

	@Mock
	private UserRepository userRepository;
	@Mock
	private AlcoholQueryRepository alcoholQueryRepository;
	@Mock
	private PicksRepository picksRepository;


	@Nested
	@DisplayName("술(위스키)을 찜 할 수 있다.")
	class UpdatePicks {

	}

	@Nested
	@DisplayName("술(위스키)을 찜해제 할 수 있다.")
	class UpdateUnPicks {

	}
}
