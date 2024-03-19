package app.bottlenote.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Component
public class MetaService {

	private static String serverVersion;
	public static String serverPathVersion;
	private static String serverEncoding;

	@Autowired
	public void setEnvironment(Environment env) {
		serverVersion = env.getProperty("server.version");
		serverPathVersion = env.getProperty("server.path.version");
		serverEncoding = env.getProperty("server.encoding.charset");
	}


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
		meta.put("server_version", serverVersion);
		meta.put("server_path_version", serverPathVersion);
		meta.put("server_encoding", serverEncoding);
		meta.put("server_response_time", LocalDateTime.now(ZoneId.of("Asia/Seoul")).toString());
		return meta;
	}
}
