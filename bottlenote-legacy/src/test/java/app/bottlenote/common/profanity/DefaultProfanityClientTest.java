package app.bottlenote.common.profanity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bottlenote.common.profanity.dto.response.ProfanityResponse;
import app.bottlenote.shared.common.exception.CommonException;
import app.bottlenote.shared.common.exception.CommonExceptionCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@DisplayName("[unit] [service] DefaultProfanityClient")
class DefaultProfanityClientTest {

  private static final Logger log = LogManager.getLogger(DefaultProfanityClientTest.class);
  private ProfanityClient profanityClient;

  @BeforeEach
  void setUp() {
    FakeProfanityFeignClient fakeProfanityFeignClient = new FakeProfanityFeignClient();
    profanityClient = new DefaultProfanityClient(fakeProfanityFeignClient);
  }

  @Test
  @DisplayName("비속어 검사를 요청할 수 있다.")
  void test_1() {
    ProfanityResponse response = profanityClient.requestVerificationProfanity("욕설, 개자식");
    String filteredText = "**, ***";
    log.info("response : {}", response);

    assertNotNull(response);
    assertEquals(2, response.detected().size());
    assertEquals(filteredText, response.filtered());
  }

  @Test
  @DisplayName("비속어가 없는 경우 필터링된 텍스트는 원본 텍스트와 동일하다.")
  void test_2() {
    String text = "안녕하세요";
    String filteredText = profanityClient.getFilteredText(text);
    assertEquals(text, filteredText);
  }

  @Test
  @DisplayName("비속어 부분만 필터링된 텍스트를 반환받을수 있다.")
  void test_3() {
    String text = "욕설, 개자식입니다.";
    String filteredText = profanityClient.getFilteredText(text);
    assertEquals("**, ***입니다.", filteredText);
  }

  @Test
  @DisplayName("비속어가 포함된 텍스트는 예외를 발생 시킬 수 있다.")
  void test_4() {
    String text = "욕설, 개자식입니다.";

    CommonException aThrows =
        Assertions.assertThrows(
            CommonException.class,
            () -> {
              profanityClient.validateProfanity(text);
            });

    assertEquals(CommonExceptionCode.CONTAINS_PROFANITY, aThrows.getExceptionCode());
  }

  @Test
  @DisplayName("비속어가 포함되지 않은 텍스트는 예외를 발생시키지 않는다.")
  void test_5() {
    String text = "안녕하세요";
    Assertions.assertDoesNotThrow(() -> profanityClient.validateProfanity(text));
  }
}
