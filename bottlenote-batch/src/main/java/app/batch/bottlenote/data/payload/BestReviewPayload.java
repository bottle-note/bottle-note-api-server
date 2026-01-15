package app.batch.bottlenote.data.payload;

import lombok.Builder;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Builder
public record BestReviewPayload(Long id, Long alcoholId) {
  public static class BestReviewMapper implements RowMapper<BestReviewPayload> {
    @Override
    public BestReviewPayload mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new BestReviewPayload(rs.getLong("id"), rs.getLong("alcohol_id"));
    }
  }
}
