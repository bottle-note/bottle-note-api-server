package app.bottlenote.curation.graphql;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.curation.service.CurationAlcoholGraphqlService;
import app.bottlenote.rating.dto.response.AlcoholRatingStatsResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class CurationAlcoholGraphqlResolver {

  private final CurationAlcoholGraphqlService curationAlcoholGraphqlService;

  @QueryMapping
  public List<Alcohol> alcohols(@Argument List<Long> ids) {
    return curationAlcoholGraphqlService.findAlcohols(ids);
  }

  @SchemaMapping(typeName = "Alcohol", field = "alcoholId")
  public Long alcoholId(Alcohol alcohol) {
    return alcohol.getId();
  }

  @SchemaMapping(typeName = "Alcohol", field = "regionName")
  public String regionName(Alcohol alcohol) {
    return curationAlcoholGraphqlService.regionName(alcohol);
  }

  @BatchMapping(typeName = "Alcohol", field = "rating")
  public Map<Alcohol, Double> ratings(List<Alcohol> alcohols) {
    Map<Long, AlcoholRatingStatsResponse> stats =
        curationAlcoholGraphqlService.ratingStats(alcohols);
    return mapByAlcohol(alcohols, alcohol -> ratingStatsOf(stats, alcohol).rating());
  }

  @BatchMapping(typeName = "Alcohol", field = "totalRatingsCount")
  public Map<Alcohol, Long> totalRatingsCounts(List<Alcohol> alcohols) {
    Map<Long, AlcoholRatingStatsResponse> stats =
        curationAlcoholGraphqlService.ratingStats(alcohols);
    return mapByAlcohol(alcohols, alcohol -> ratingStatsOf(stats, alcohol).totalRatingsCount());
  }

  @BatchMapping(typeName = "Alcohol", field = "reviewCount")
  public Map<Alcohol, Long> reviewCounts(List<Alcohol> alcohols) {
    Map<Long, Long> counts = curationAlcoholGraphqlService.reviewCounts(alcohols);
    return mapByAlcohol(alcohols, alcohol -> counts.getOrDefault(alcohol.getId(), 0L));
  }

  @BatchMapping(typeName = "Alcohol", field = "totalPickCount")
  public Map<Alcohol, Long> totalPickCounts(List<Alcohol> alcohols) {
    Map<Long, Long> counts = curationAlcoholGraphqlService.pickCounts(alcohols);
    return mapByAlcohol(alcohols, alcohol -> counts.getOrDefault(alcohol.getId(), 0L));
  }

  public Double rating(Alcohol alcohol) {
    return curationAlcoholGraphqlService.rating(alcohol);
  }

  public Long totalRatingsCount(Alcohol alcohol) {
    return curationAlcoholGraphqlService.totalRatingsCount(alcohol);
  }

  public Long reviewCount(Alcohol alcohol) {
    return curationAlcoholGraphqlService.reviewCount(alcohol);
  }

  public Long totalPickCount(Alcohol alcohol) {
    return curationAlcoholGraphqlService.totalPickCount(alcohol);
  }

  private <T> Map<Alcohol, T> mapByAlcohol(List<Alcohol> alcohols, Function<Alcohol, T> mapper) {
    if (alcohols == null || alcohols.isEmpty()) {
      return Map.of();
    }
    return alcohols.stream()
        .collect(
            Collectors.toMap(
                Function.identity(), mapper, (left, right) -> left, LinkedHashMap::new));
  }

  private AlcoholRatingStatsResponse ratingStatsOf(
      Map<Long, AlcoholRatingStatsResponse> stats, Alcohol alcohol) {
    if (alcohol == null || alcohol.getId() == null) {
      return new AlcoholRatingStatsResponse(null, 0.0, 0L);
    }
    return stats.getOrDefault(
        alcohol.getId(), new AlcoholRatingStatsResponse(alcohol.getId(), 0.0, 0L));
  }
}
