package app.bottlenote.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@Entity(name = "root_admins")
@Table(name = "root_admins")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RootAdmin {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @MapsId
  private User user;
}
