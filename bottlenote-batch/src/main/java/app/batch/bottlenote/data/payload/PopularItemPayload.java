package app.batch.bottlenote.data.payload;

import lombok.Builder;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

@Builder
public record PopularItemPayload(
	Long alcoholId,
	Integer year,
	Integer month,
	Integer day,
	BigDecimal reviewScore,
	BigDecimal ratingScore,
	BigDecimal pickScore,
	BigDecimal popularScore
) {
	public static class PopularItemMapper implements RowMapper<PopularItemPayload> {
		@Override
		public PopularItemPayload mapRow(ResultSet rs, int rowNum) throws SQLException {
			LocalDate currentDate = LocalDate.now();
			return PopularItemPayload.builder()
				.alcoholId(rs.getLong("alcohol_id"))
				.year(currentDate.getYear())
				.month(currentDate.getMonthValue())
				.day(currentDate.getDayOfMonth())
				.reviewScore(rs.getObject("review_score") != null ? rs.getBigDecimal("review_score") : BigDecimal.ZERO)
				.ratingScore(rs.getObject("rating_score") != null ? rs.getBigDecimal("rating_score") : BigDecimal.ZERO)
				.pickScore(rs.getObject("pick_score") != null ? rs.getBigDecimal("pick_score") : BigDecimal.ZERO)
				.popularScore(rs.getObject("popular_score") != null ? rs.getBigDecimal("popular_score") : BigDecimal.ZERO)
				.build();
		}
	}
}
