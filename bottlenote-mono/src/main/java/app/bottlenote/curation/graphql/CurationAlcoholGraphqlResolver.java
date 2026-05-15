package app.bottlenote.curation.graphql;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.curation.service.CurationAlcoholGraphqlService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
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

  @SchemaMapping(typeName = "Alcohol", field = "rating")
  public Double rating(Alcohol alcohol) {
    return curationAlcoholGraphqlService.rating(alcohol);
  }

  @SchemaMapping(typeName = "Alcohol", field = "totalRatingsCount")
  public Long totalRatingsCount(Alcohol alcohol) {
    return curationAlcoholGraphqlService.totalRatingsCount(alcohol);
  }

  @SchemaMapping(typeName = "Alcohol", field = "reviewCount")
  public Long reviewCount(Alcohol alcohol) {
    return curationAlcoholGraphqlService.reviewCount(alcohol);
  }

  @SchemaMapping(typeName = "Alcohol", field = "totalPickCount")
  public Long totalPickCount(Alcohol alcohol) {
    return curationAlcoholGraphqlService.totalPickCount(alcohol);
  }
}
