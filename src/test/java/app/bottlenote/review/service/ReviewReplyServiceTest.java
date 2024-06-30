package app.bottlenote.review.service;

import app.bottlenote.review.fixture.FakeProfanityClient;
import app.bottlenote.review.fixture.FakeUserDomainSupport;
import org.junit.jupiter.api.BeforeAll;

class ReviewReplyServiceTest {

	//private final JpaReviewRepository reviewRepository;
	//private final ProfanityClient profanityClient;
	//private final UserDomainSupport userDomainSupport;

	private ReviewReplyService reviewReplyService;

	@BeforeAll
	static void beforeAll() {
		new FakeUserDomainSupport();
		new FakeProfanityClient();
	}
}
