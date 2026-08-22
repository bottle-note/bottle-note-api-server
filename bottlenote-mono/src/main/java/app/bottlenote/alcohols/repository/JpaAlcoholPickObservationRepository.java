package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholPickObservation;
import app.bottlenote.alcohols.domain.AlcoholPickObservationRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaAlcoholPickObservationRepository
    extends AlcoholPickObservationRepository, JpaRepository<AlcoholPickObservation, Long> {}
