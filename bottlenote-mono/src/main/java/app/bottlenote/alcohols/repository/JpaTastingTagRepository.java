package app.bottlenote.alcohols.repository;

import app.bottlenote.alcohols.domain.TastingTag;
import app.bottlenote.alcohols.domain.TastingTagRepository;
import app.bottlenote.common.annotation.JpaRepositoryImpl;
import org.springframework.data.repository.CrudRepository;

@JpaRepositoryImpl
public interface JpaTastingTagRepository
    extends TastingTagRepository, CrudRepository<TastingTag, Long> {}
