package app.bottlenote.support.business.fixture;

import app.bottlenote.support.business.constant.ContactType;
import app.bottlenote.support.business.domain.BusinessSupport;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BusinessSupportTestFactory {

	@Autowired
	private EntityManager em;

	@Transactional
	public BusinessSupport persist(Long userId) {
		BusinessSupport bs = BusinessSupport.create(userId, "문의", ContactType.EMAIL);
		em.persist(bs);
		em.flush();
		return bs;
	}
}
