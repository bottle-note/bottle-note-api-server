package app.bottlenote.common.image;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ImageUtil {

  private static String[] splitPath(String imageUrl) {
    return imageUrl.substring(8).split("/");
  }

  public static String getImagePath(String imageUrl) {
    String[] split = splitPath(imageUrl);
    return split[0] + split[1];
  }

  public static String getImageKey(String imageUrl) {
    String[] split = splitPath(imageUrl);
    return split[0] + split[1] + split[2];
  }

  public static String getImageName(String imageUrl) {
    String[] split = splitPath(imageUrl);
    return split[2];
  }
}
