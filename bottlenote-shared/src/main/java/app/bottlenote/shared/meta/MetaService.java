package app.bottlenote.shared.meta;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetaService {

	public static final String SERVER_PATH_VERSION = "v1";
	private static final String SERVER_VERSION = "1.0.0";
	private static final String SERVER_ENCODING = "UTF-8";

	protected MetaService() {}

	public static MetaInfos createMetaInfo() {
		MetaInfos metaInfos = new MetaInfos();
		metaInfos.add("serverVersion", SERVER_VERSION);
		metaInfos.add("serverPathVersion", SERVER_PATH_VERSION);
		metaInfos.add("serverEncoding", SERVER_ENCODING);
		metaInfos.add("serverResponseTime", LocalDateTime.now(ZoneId.of("Asia/Seoul")));
		return metaInfos;
	}
}