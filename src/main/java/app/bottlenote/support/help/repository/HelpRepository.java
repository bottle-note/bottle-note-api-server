package app.bottlenote.support.help.repository;

import app.bottlenote.support.help.domain.Help;
import java.util.List;
import java.util.Optional;

public interface HelpRepository {

	Help save(Help help);

	Optional<Help> findById(Long id);

	List<Help> findAll();

}
