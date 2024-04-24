package app.bottlenote.alcohols.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Populars {
    private final Long whiskyId;  // 1,
    private final String korName;  // '글렌피딕',
    private final String engName;  // 'glen fi',
    private final Double rating;  // 3.5,
    private final String category;  // 'single molt',
    private final String imagePath;  // "https://i.imgur.com/TE2nmYV.png"

    public static Populars of(Long whiskyId, String korName, String engName, Double rating, String category, String imagePath) {
        return new Populars(whiskyId, korName, engName, rating, category, imagePath);
    }
}
