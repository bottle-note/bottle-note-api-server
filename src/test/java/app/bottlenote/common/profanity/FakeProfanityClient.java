package app.bottlenote.common.profanity;

import app.bottlenote.common.exception.CommonException;
import app.bottlenote.common.exception.CommonExceptionCode;
import app.bottlenote.common.profanity.dto.response.DetectedItem;
import app.bottlenote.common.profanity.dto.response.ProfanityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class FakeProfanityClient implements ProfanityClient {
    private static final Logger log = LoggerFactory.getLogger(FakeProfanityClient.class);
    private final String profanityWord = "욕설";

    @Override
    public ProfanityResponse requestVerificationProfanity(String text) {
        log.info("[FakeProfanityClient] requestVerificationProfanity: {}", text);
        boolean containsProfanity = text != null && text.contains(profanityWord);

        if (containsProfanity) {
            String filtered = text.replace(profanityWord, "***");
            List<DetectedItem> detected = new ArrayList<>();
            detected.add(DetectedItem.create(profanityWord));

            return ProfanityResponse.builder()
                    .trackingId(UUID.randomUUID().toString())
                    .status(new ProfanityResponse.Status(200, "Profanity detected", "Profanity found in text", null))
                    .detected(detected)
                    .filtered(filtered)
                    .elapsed("0.1")
                    .build();
        } else {
            return ProfanityResponse.builder()
                    .trackingId(UUID.randomUUID().toString())
                    .status(new ProfanityResponse.Status(200, "No profanity detected", "No profanity found in text", null))
                    .detected(Collections.emptyList())
                    .filtered(text)
                    .elapsed("0.1")
                    .build();
        }
    }

    @Override
    public String getFilteredText(String text) {
        log.info("[FakeProfanityClient] getFilteredText: {}", text);
        if (text == null) {
            return "";
        }
        return text.contains(profanityWord) ? text.replace(profanityWord, "***") : text;
    }

    @Override
    public String filter(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return getFilteredText(content);
    }

    @Override
    public void validateProfanity(String text) {
        log.info("[FakeProfanityClient] validateProfanity: {}", text);
        if (text != null && text.contains(profanityWord)) {
            throw new CommonException(CommonExceptionCode.CONTAINS_PROFANITY);
        }
    }
}
