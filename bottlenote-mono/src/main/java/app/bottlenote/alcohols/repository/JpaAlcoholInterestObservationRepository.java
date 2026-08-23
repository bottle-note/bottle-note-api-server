package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholInterestObservation;
import app.bottlenote.alcohols.domain.AlcoholInterestObservationRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaAlcoholInterestObservationRepository
    extends AlcoholInterestObservationRepository, JpaRepository<AlcoholInterestObservation, Long> {}
