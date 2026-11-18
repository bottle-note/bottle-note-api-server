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

  public static String extractResourceKey(String viewUrl) {
    if (viewUrl == null || viewUrl.isBlank()) {
      return null;
    }
    int protocolEnd = viewUrl.indexOf("://");
    if (protocolEnd == -1) {
      return viewUrl;
    }
    int firstSlashAfterHost = viewUrl.indexOf("/", protocolEnd + 3);
    if (firstSlashAfterHost == -1) {
      return "";
    }
    return viewUrl.substring(firstSlashAfterHost + 1);
  }
}
