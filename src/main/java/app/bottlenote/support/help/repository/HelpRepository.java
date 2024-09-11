package app.bottlenote.support.help.repository;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.constant.HelpType;

import java.util.List;
import java.util.Optional;

public interface HelpRepository {

	Help save(Help help);

	Optional<Help> findById(Long id);

	List<Help> findAll();

	List<Help> findAllByUserId(Long userId);

	List<Help> findAllByUserIdAndType(Long userId, HelpType helpType);

	Optional<Help> findByIdAndUserId(Long id, Long userId);
}
