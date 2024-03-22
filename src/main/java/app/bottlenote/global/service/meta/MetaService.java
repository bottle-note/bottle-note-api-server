package app.bottlenote.global.service.meta;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 메타 정보를 관리하기 위한 서비스입니다.<br>
 * 추가적인 메타 정보를 생성하고 반환할 수 있습니다.
 */
@Slf4j
@Component
public class MetaService {

	private static String serverVersion;
	public static String serverPathVersion;
	private static String serverEncoding;

	/**
	 * appllication.yml 의 설정값을 주입한다.
	 *
	 * @param env the env
	 */
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
	public static MetaInfos createMetaInfo() {
		MetaInfos metaInfos = new MetaInfos();

		metaInfos.add("server_version", serverVersion);
		metaInfos.add("server_path_version", serverPathVersion);
		metaInfos.add("server_encoding", serverEncoding);
		metaInfos.add("server_response_time", LocalDateTime.now(ZoneId.of("Asia/Seoul")).toString());

		return metaInfos;
	}
}
