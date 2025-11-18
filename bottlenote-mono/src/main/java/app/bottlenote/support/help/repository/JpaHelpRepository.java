package app.bottlenote.support.help.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.support.help.domain.Help;
import app.bottlenote.support.help.domain.HelpRepository;
import app.bottlenote.support.help.repository.custom.CustomHelpQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaHelpRepository
    extends JpaRepository<Help, Long>, HelpRepository, CustomHelpQueryRepository {}
