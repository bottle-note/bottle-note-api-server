package app.bottlenote.common.service;

import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class MetaService {

	@Value("${server.version}")
	private static String serverVersion;

	@Value("${server.path.version}")
	public static String serverPathVersion;

	@Value("${server.encoding}")
	private static String serverEncoding;

	/**
	 * 메타 정보를 생성한다.
	 *
	 * @return - server version : 서버 버전<br>
	 * - server path version : 서버 경로 버전<br>
	 * - server encoding : 서버 인코딩<br>
	 * - server time : 서버 시간<br>
	 */
	public static Map<String, String> createMetaInfo() {
		HashMap<String, String> meta = new HashMap<>();
		meta.put("server_version", serverPathVersion);
		meta.put("server_path_version", serverPathVersion);
		meta.put("server_encoding", serverEncoding);
		meta.put("server_time", LocalDateTime.now(ZoneId.of("Asia/Seoul")).toString());
		return meta;
	}
}
