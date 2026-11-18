package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.AlcoholRatingObservation;
import app.bottlenote.alcohols.domain.AlcoholRatingObservationRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaAlcoholRatingObservationRepository
    extends AlcoholRatingObservationRepository, JpaRepository<AlcoholRatingObservation, Long> {}
