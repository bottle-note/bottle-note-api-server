package app.bottlenote.review.service;

import app.bottlenote.common.profanity.ProfanityClient;
import app.bottlenote.review.domain.ReviewRepository;
import app.bottlenote.review.fixture.FakeProfanityClient;
import app.bottlenote.review.fixture.FakeUserDomainSupport;
import app.bottlenote.review.fixture.InmemoryReviewRepository;
import app.bottlenote.user.service.domain.UserDomainSupport;
import org.junit.jupiter.api.BeforeEach;

class ReviewReplyServiceTest {

	private ReviewRepository reviewRepository;
	private ProfanityClient profanityClient;
	private UserDomainSupport userDomainSupport;

	private ReviewReplyService reviewReplyService;

	@BeforeEach
	void setUp() {
		reviewRepository = new InmemoryReviewRepository();
		profanityClient = new FakeProfanityClient();
		userDomainSupport = new FakeUserDomainSupport();

		reviewReplyService = new ReviewReplyService(
			reviewRepository,
			profanityClient,
			userDomainSupport
		);
	}
}
