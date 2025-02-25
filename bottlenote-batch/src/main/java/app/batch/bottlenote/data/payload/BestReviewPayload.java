package app.batch.bottlenote.data.payload;

import lombok.Builder;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Builder
public record BestReviewPayload(
	Long id,
	Long alcoholId,
	Double popularityScore,
	Integer likeCount,
	Integer unlikeCount,
	Integer replyCount,
	Integer imageCount,
	Integer reviewCount
) {
	public static class BestReviewMapper implements RowMapper<BestReviewPayload> {
		@Override
		public BestReviewPayload mapRow(ResultSet rs, int rowNum) throws SQLException {
			return BestReviewPayload.builder()
				.id(rs.getLong("id"))
				.alcoholId(rs.getLong("alcohol_id"))
				.popularityScore(rs.getDouble("popularityScore"))
				.likeCount(rs.getInt("likeCount"))
				.unlikeCount(rs.getInt("unlikeCount"))
				.replyCount(rs.getInt("replyCount"))
				.imageCount(rs.getInt("imageCount"))
				.reviewCount(rs.getInt("reviewCount"))
				.build();
		}
	}
}
