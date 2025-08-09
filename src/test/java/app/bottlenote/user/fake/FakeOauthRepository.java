package app.bottlenote.user.fake;

import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.repository.OauthRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;

public class FakeOauthRepository implements OauthRepository {

  private final Map<Long, User> userDatabase = new HashMap<>();
  private final Map<String, User> emailDatabase = new HashMap<>();
  private final Map<String, User> refreshTokenDatabase = new HashMap<>();
  private final Map<String, User> socialUniqueIdDatabase = new HashMap<>();
  private final Map<String, User> nickNameDatabase = new HashMap<>();
  private Long autoIncrementId = 1L;
  private Long nicknameSequence = 1L;

  @Override
  public Optional<User> findByEmail(String email) {
    return Optional.ofNullable(emailDatabase.get(email));
  }

  @Override
  public Optional<User> findByRefreshToken(String refreshToken) {
    return Optional.ofNullable(refreshTokenDatabase.get(refreshToken));
  }

  @Override
  public Optional<User> getFirstUser() {
    return Optional.empty();
  }

  @Override
  public Optional<User> loadGuestUser() {
    return Optional.empty();
  }

  @Override
  public String getNextNicknameSequence() {
    return "User" + (nicknameSequence++);
  }

  @Override
  public Optional<User> findByEmailAndSocialType(String email, String socialType) {
    return emailDatabase.values().stream()
        .filter(user -> user.getEmail().equals(email))
        .filter(
            user ->
                user.getSocialType() != null
                    && user.getSocialType().contains(SocialType.valueOf(socialType)))
        .findFirst();
  }

  @Override
  public Optional<User> findBySocialUniqueId(String socialUniqueId) {
    return Optional.ofNullable(socialUniqueIdDatabase.get(socialUniqueId));
  }

  @Override
  public Optional<User> findByNickName(String nickName) {
    return Optional.ofNullable(nickNameDatabase.get(nickName));
  }

  @Override
  public User save(User user) {
    if (user.getId() == null) {
      ReflectionTestUtils.setField(user, "id", autoIncrementId++);
    }

    Long id = user.getId();
    userDatabase.put(id, user);
    emailDatabase.put(user.getEmail(), user);

    if (user.getRefreshToken() != null) {
      refreshTokenDatabase.put(user.getRefreshToken(), user);
    }

    if (user.getSocialUniqueId() != null) {
      socialUniqueIdDatabase.put(user.getSocialUniqueId(), user);
    }

    if (user.getNickName() != null) {
      nickNameDatabase.put(user.getNickName(), user);
    }

    return user;
  }

  @Override
  public <S extends User> Iterable<S> saveAll(Iterable<S> entities) {
    return null;
  }

  @Override
  public Optional<User> findById(Long id) {
    return Optional.ofNullable(userDatabase.get(id));
  }

  @Override
  public void delete(User user) {
    userDatabase.remove(user.getId());
    emailDatabase.remove(user.getEmail());
    if (user.getRefreshToken() != null) {
      refreshTokenDatabase.remove(user.getRefreshToken());
    }
    if (user.getSocialUniqueId() != null) {
      socialUniqueIdDatabase.remove(user.getSocialUniqueId());
    }
    if (user.getNickName() != null) {
      nickNameDatabase.remove(user.getNickName());
    }
  }

  @Override
  public void deleteAllById(Iterable<? extends Long> longs) {}

  @Override
  public void deleteAll(Iterable<? extends User> entities) {}

  @Override
  public void deleteAll() {}

  @Override
  public boolean existsById(Long id) {
    return userDatabase.containsKey(id);
  }

  @Override
  public Iterable<User> findAll() {
    return null;
  }

  @Override
  public Iterable<User> findAllById(Iterable<Long> longs) {
    return null;
  }

  @Override
  public long count() {
    return userDatabase.size();
  }

  @Override
  public void deleteById(Long aLong) {}

  public void clear() {
    userDatabase.clear();
    emailDatabase.clear();
    refreshTokenDatabase.clear();
    socialUniqueIdDatabase.clear();
    nickNameDatabase.clear();
    autoIncrementId = 1L;
    nicknameSequence = 1L;
  }
}
