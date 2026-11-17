package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholEngagementObservation;
import app.bottlenote.alcohols.domain.AlcoholEngagementObservationRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaAlcoholEngagementObservationRepository
    extends AlcoholEngagementObservationRepository,
        JpaRepository<AlcoholEngagementObservation, Long> {}
