package app.bottlenote.support.help.repository;

import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.repository.custom.CustomHelpQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaHelpRepository
    extends JpaRepository<Help, Long>, HelpRepository, CustomHelpQueryRepository {}
