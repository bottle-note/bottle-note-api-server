package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.PopularQueryRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaPopularQueryRepository
    extends PopularQueryRepository, CustomPopularQueryRepository, JpaRepository<Alcohol, Long> {}
