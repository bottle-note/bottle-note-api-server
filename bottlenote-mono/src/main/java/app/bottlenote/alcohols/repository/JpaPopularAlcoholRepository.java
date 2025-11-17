package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.PopularAlcohol;
import app.bottlenote.alcohols.domain.PopularAlcoholRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaPopularAlcoholRepository
    extends PopularAlcoholRepository, JpaRepository<PopularAlcohol, Long> {}
