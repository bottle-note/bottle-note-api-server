package app.bottlenote.global.config;

import app.bottlenote.global.config.jpa.AuditorAwareImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;


@Tag("unit")
@DisplayName("[unit] [infra] JpaAuditing")
@ExtendWith(MockitoExtension.class)
class JpaAuditingTest {

	@Mock
	private SecurityContext securityContext;

	@Mock
	private Authentication authentication;

	@Mock
	private UserDetails userDetails;

	@InjectMocks
	private AuditorAwareImpl auditorAware;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.setContext(securityContext);
	}

	@DisplayName("현재 로그인 한 유저가 Auditing 된다.")
	@Test
	void auditing_success_when_is_authenticated() {
		// given
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(true);
		when(authentication.getPrincipal()).thenReturn(userDetails);
		when(userDetails.getUsername()).thenReturn("testUser");

		// then
		Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

		// ghen
		assertEquals("testUser", currentAuditor.get());
	}

	@DisplayName("로그인하지 않은 유저는 Auditing되지 않는다.")
	@Test
	void auditing_fail_when_is_not_authenticated() {
		// Given
		when(securityContext.getAuthentication()).thenReturn(authentication);
		when(authentication.isAuthenticated()).thenReturn(false);

		// When
		Optional<String> currentAuditor = auditorAware.getCurrentAuditor();

		// Then
		assertTrue(currentAuditor.isEmpty());
	}


}
